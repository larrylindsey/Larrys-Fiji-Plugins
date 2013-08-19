package edu.utexas.clm.synapses.segpipeline.plugin;

import edu.utexas.clm.archipelago.data.Duplex;
import edu.utexas.clm.synapses.segpipeline.data.graph.EdgeMap;
import edu.utexas.clm.synapses.segpipeline.data.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.graph.SparseVectorEdgeGraph;
import edu.utexas.clm.synapses.segpipeline.data.graph.WekaClassifierMap;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.DenseGroundTruthEdgeFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.GroundTruthFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.ImageHistogramSimilarityFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.InplaneOverlapHistogramFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.IntersectionOverUnionFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.MaxOverlapFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.OrientationEdgeFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.SizeFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.SparseLabelEdgeFeature;
import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabelFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.filter.SizeGrelFilter;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelClose;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelDilate;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.Strels;
import hr.irb.fastRandomForest.FastRandomForest;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import net.imglib2.img.imageplus.ImagePlusImg;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

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
            final SerialSparseLabels trainLabels, positiveLabels, negativeLabels, testLabels;

            final Element trainElement = (Element)doc.getElementsByTagName("TrainLabels").item(0);
            final Element testElement = (Element)doc.getElementsByTagName("TestLabels").item(0);

            final FastRandomForest rf = new FastRandomForest();
            final WekaClassifierMap map = new WekaClassifierMap(rf);

//            boolean logged = false;

            trainLabels = getLabels(trainElement, 32);
            positiveLabels = getLabels(getChild(trainElement, "Positive"));
            //negativeLabels = getLabels(getChild(trainElement, "Negative"));
            testLabels = getLabels(testElement, 32);

//            for (SparseLabel neg : negativeLabels)
//            {
//                for (SparseLabel label : trainLabels)
//                {
//                    if (neg.getIndex() == label.getIndex() && neg.intersect(label))
//                    {
//                        IJ.log("Negative label " + neg.getValue() + " intersects " + label.getValue());
//                        logged = true;
//                    }
//                }
//            }

//            if (!logged)
//            {
//                IJ.log("No negative intersections");
//            }

            trainGraph = getGraph(trainElement, trainLabels);
            trainAnnotationGraph = getAnnotationGraph(trainElement,
                    trainLabels, positiveLabels);
            testGraph = getGraph(testElement, testLabels);

            IJ.log(new ArrayList<SparseLabelEdgeFeature>(trainAnnotationGraph.getEdgeFeatures()).get(0).toString());

            IJ.log("Train graph has " + trainGraph.getEdges().size() + " edges");
            IJ.log("Annotation graph has " + trainAnnotationGraph.getEdges().size() + " edges");
            IJ.log("Test graph has " + testGraph.getEdges().size() + " edges");

//            rf.setNumTrees((int)Math.sqrt(testGraph.getVectorSize()));
//            rf.setNumTrees(2);
//            rf.setNumFeatures(trainGraph.getVectorSize());

            IJ.log("Training classifier");
            map.trainClassifier(trainGraph, trainAnnotationGraph);

            IJ.log("Mapping test edges");
            mapGraph = testGraph.mapEdges(map);
            IJ.log("Mapped test graph has " + mapGraph.getEdges().size() + " edges");

            //displayEquivalentLabels(testLabels, mapGraph, 1425);

            IJ.log("%%% Begin matlab assignment %%%");
            IJ.log("trainEdges5 = [");
            for (Duplex<Integer, Integer> edge : trainAnnotationGraph.getEdges())
            {
                IJ.log("\t" + edge.a + ", " + edge.b + ", " +
                        trainAnnotationGraph.getEdgeValues(edge)[0] + ";");
            }
            IJ.log("];\n");

            IJ.log("testEdges5 = [");
            for (Duplex<Integer, Integer> edge : mapGraph.getEdges())
            {
                IJ.log("\t" + edge.a + ", " + edge.b + ", " +
                        mapGraph.getEdgeValues(edge)[0] + ";");
            }
            IJ.log("];\n");

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

    private void displayEquivalentLabels(final SerialSparseLabels labels,
                                         final SparseVectorEdgeGraph trainMapped,
                                         final int value)
    {
        TreeSet<SparseLabel> eqLabels = trainMapped.equivalentLabels(value,
                new EdgeMap()
                {

                    public void map(float[] inVector, float[] outVector,
                                    Duplex<Integer, Integer> edgeKey)
                    {
                        outVector[0] = inVector[0] <= 0 ? 0f : 1f;
                    }

                    public int size()
                    {
                        return 1;
                    }

                    public boolean acceptSize(int size)
                    {
                        return size == 1;
                    }
                }, labels);
        int[] indexArray = labels.indices();
        int[] indexToSlice = new int[indexArray[indexArray.length -1]];
        ByteProcessor[] processors = new ByteProcessor[indexArray.length];
        ImageStack stack = new ImageStack(labels.getWidth(), labels.getHeight(), indexArray.length);

        for (int i = 0; i < indexToSlice.length; ++i)
        {
            indexToSlice[i] = -1;
        }

        for (int i = 0; i < indexArray.length; ++i)
        {
            indexToSlice[indexArray[i]] = i;
            processors[i] = new ByteProcessor(labels.getWidth(), labels.getHeight());
        }

        for (final SparseLabel sl : eqLabels)
        {
            int index = indexToSlice[sl.getIndex()];
            if (index >= 0)
            {
                SparseLabelFactory.addLabelTo(processors[index], sl, labels.getImage(sl));
            }
            else
            {
                IJ.log("Couldn't find index " + index);
            }
        }

        for (int i = 0 ; i < processors.length; ++i)
        {
            stack.setProcessor(processors[i], i + 1);
        }

        new ImagePlus("Connected components", stack).show();

    }

    private SparseVectorEdgeGraph getGraph(final Element e,
                                           final SerialSparseLabels labels)
    {
        IJ.log("Creating SVEG factory");
        final SVEGFactory factory = new SVEGFactory(labels,
                new OrientationEdgeFeature(),
                new InplaneOverlapHistogramFeature(32, 256),
                new MaxOverlapFeature(),
                new IntersectionOverUnionFeature(),
                new SizeFeature(),
                new ImageHistogramSimilarityFeature());
        IJ.log("Associating images");
        associateImages(e, labels);
        IJ.log("Closing labels in place");
        labels.operateInPlace(new LabelClose(Strels.diskStrel(4)));
        IJ.log("Building overlap map");
        labels.buildOverlapMap(new LabelDilate(Strels.diskStrel(16)));
        IJ.log("Making SVEG");
        return factory.makeSVEG();
    }

    private SparseVectorEdgeGraph getAnnotationGraph(final Element e,
                                                     final SerialSparseLabels labels,
                                                     final SerialSparseLabels positive)
    {
        final SVEGFactory factory = new SVEGFactory(labels,
                new DenseGroundTruthEdgeFeature(positive));
        return factory.makeSVEG();
    }


    private Element getChild(final Element e, final String name)
    {
        NodeList nl = e.getElementsByTagName(name);
        return (Element)nl.item(0);
    }

    private SerialSparseLabels getLabels(final Element e, final int minSize)
    {
        final SerialSparseLabels labels = getLabels(e);
        IJ.log("Removing labels smaller than " + minSize);
        labels.filterLabels(new SizeGrelFilter(32));
        IJ.log("" + labels.size() + " left");
        return labels;
    }

    private SerialSparseLabels getLabels(final Element e)
    {
        final SerialSparseLabels labels = new SerialSparseLabels();
        final SparseLabelFactory factory;
        final int w, h;
        final NodeList nl = e.getChildNodes();

        w = Integer.parseInt(e.getAttribute("width"));
        h = Integer.parseInt(e.getAttribute("height"));

        factory = new SparseLabelFactory(w, h);

        for (int i = 0; i < nl.getLength(); ++i)
        {
            if (nl.item(i).getNodeName().equals("Label"))
            {
                Element labelElement = (Element)nl.item(i);
                String path = dir + labelElement.getAttribute("src");
                int index = Integer.parseInt(labelElement.getAttribute("index"));
                if (!factory.makeLabels(path, index, labels))
                {
                    throw new RuntimeException("Could not add labels from image " + path);
                }
                IJ.log("Added " + path + " for index " + index);
            }
        }

        IJ.log("Got " + labels.size() + " labels");

        return labels;
    }

    private void associateImages(final Element e, final SerialSparseLabels labels)
    {
        final NodeList nl = e.getChildNodes();

        for (int i = 0; i < nl.getLength(); ++i)
        {
            if (nl.item(i).getNodeName().equals("Image"))
            {
                Element imageElement = (Element)nl.item(i);
                String path = dir + imageElement.getAttribute("src");
                int index = Integer.parseInt(imageElement.getAttribute("index"));
                labels.associateImage(IJ.openImage(path), index);
            }
        }
    }

    public void rethrow(final Throwable t)
    {
        t.printStackTrace();
        throw new RuntimeException(t);
    }
}
