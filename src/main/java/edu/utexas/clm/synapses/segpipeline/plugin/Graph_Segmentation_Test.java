package edu.utexas.clm.synapses.segpipeline.plugin;

import edu.utexas.clm.archipelago.data.Duplex;
import edu.utexas.clm.synapses.segpipeline.data.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.graph.SparseVectorEdgeGraph;
import edu.utexas.clm.synapses.segpipeline.data.graph.WekaClassifierMap;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.GroundTruthFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.InplaneOverlapHistogramFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.IntersectionOverUnionFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.MaxOverlapFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.OrientationEdgeFeature;
import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabelFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.AbstractLabelMorph;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelDilate;
import hr.irb.fastRandomForest.FastRandomForest;
import ij.IJ;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 *
 */
public class Graph_Segmentation_Test implements PlugIn
{
    private String dir;

    public void run(String s)
    {
        final OpenDialog od = new OpenDialog("Select Graph XML");
        final File file;
        dir = od.getDirectory();

        if (!dir.endsWith("/"))
        {
            dir += "/";
        }

        file = new File(dir + od.getFileName());

        try
        {
            final DocumentBuilder docBuilder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = docBuilder.parse(file);

            final SparseVectorEdgeGraph testGraph, trainGraph, trainAnnotationGraph, mapGraph,
                    mapTrainGraph;

            final Element trainElement = (Element)doc.getElementsByTagName("TrainLabels").item(0);
            final Element testElement = (Element)doc.getElementsByTagName("TestLabels").item(0);

            final FastRandomForest rf = new FastRandomForest();
            final WekaClassifierMap map = new WekaClassifierMap(rf);

            trainGraph = getGraph(trainElement);
            trainAnnotationGraph = getAnnotationGraph(trainElement);
            testGraph = getGraph(testElement);

            IJ.log("Train graph has " + trainGraph.getEdges().size() + " edges");
            IJ.log("Annotation graph has " + trainAnnotationGraph.getEdges().size() + " edges");
            IJ.log("Test graph has " + testGraph.getEdges().size() + " edges");

            rf.setNumTrees(2);
            rf.setNumFeatures(trainGraph.getVectorSize());

            IJ.log("Training classifier");
            map.trainClassifier(trainGraph, trainAnnotationGraph);

            IJ.log("Mapping test edges");
            mapGraph = testGraph.mapEdges(map);
            IJ.log("Mapped test graph has " + mapGraph.getEdges().size() + " edges");

            IJ.log("Printing edges");
            for (Duplex<Integer, Integer> key : mapGraph.getEdges())
            {
                IJ.log("" + key.a + ", " + key.b + ", " + mapGraph.getEdgeValues(key)[0] + ";");
            }

            IJ.log("Mapping train edges");
            mapTrainGraph = trainGraph.mapEdges(map);
            IJ.log("Mapped train graph has " + mapTrainGraph.getEdges().size() + " edges");

            IJ.log("Printing edges");
            for (Duplex<Integer, Integer> key : mapTrainGraph.getEdges())
            {
                IJ.log("" + key.a + ", " + key.b + ", " + mapTrainGraph.getEdgeValues(key)[0] + ";");
            }


            IJ.log("Donesies");
        }
        catch (ParserConfigurationException pce)
        {
            rethrow(pce);
        }
        catch (SAXException saxe)
        {
            rethrow(saxe);
        }
        catch (IOException ioe)
        {
            rethrow(ioe);
        }
    }

    private SparseVectorEdgeGraph getGraph(final Element e)
    {

        final SVEGFactory factory = new SVEGFactory(getLabels(e),
                new OrientationEdgeFeature(),
                new InplaneOverlapHistogramFeature(8, 256),
                new MaxOverlapFeature(),
                new IntersectionOverUnionFeature());
        return factory.makeSVEG();
    }

    private SparseVectorEdgeGraph getAnnotationGraph(final Element e)
    {
        final SVEGFactory factory = new SVEGFactory(getLabels(e),
                new GroundTruthFeature(
                        getLabels(getChild(e, "Positive")), getLabels(getChild(e, "Negative"))));
        return factory.makeSVEG();
    }


    private Element getChild(final Element e, final String name)
    {
        NodeList nl = e.getElementsByTagName(name);
        return (Element)nl.item(0);
    }

    private SerialSparseLabels getLabels(final Element e)
    {
        final SerialSparseLabels labels = new SerialSparseLabels();
        final SparseLabelFactory factory;
        final int w, h;
        final NodeList nl = e.getElementsByTagName("Label");

        w = Integer.parseInt(e.getAttribute("width"));
        h = Integer.parseInt(e.getAttribute("height"));

        factory = new SparseLabelFactory(w, h);

        for (int i = 0; i < nl.getLength(); ++i)
        {
            Element labelElement = (Element)nl.item(i);
            String path = dir + labelElement.getAttribute("src");
            int index = Integer.parseInt(labelElement.getAttribute("index"));
            factory.makeLabels(path, index, labels);
        }

        labels.buildOverlapMap(new LabelDilate(AbstractLabelMorph.diskStrel(16)));

        IJ.log("Built Labels");
        for (SparseLabel label : labels)
        {
            IJ.log("Value " + label.getValue() + " index " + label.getIndex() + " size " + label.area());
            String msg = "";
            for (SparseLabel sl : labels.getOverlap(label))
            {
                msg += sl.getValue() + " ";
            }
            if (!msg.equals(""))
            {
                IJ.log("Overlaps " + msg);
            }
        }

        IJ.log("");

        return labels;
    }

    public void rethrow(final Throwable t)
    {
        t.printStackTrace();
        throw new RuntimeException(t);
    }
}
