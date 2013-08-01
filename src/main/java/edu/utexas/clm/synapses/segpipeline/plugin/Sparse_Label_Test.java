package edu.utexas.clm.synapses.segpipeline.plugin;

import edu.utexas.clm.archipelago.data.Duplex;
import edu.utexas.clm.synapses.segpipeline.data.label.*;
import edu.utexas.clm.synapses.segpipeline.data.label.graph.InplaneOverlapHistogramFeature;
import edu.utexas.clm.synapses.segpipeline.data.label.graph.IntersectionOverUnionFeature;
import edu.utexas.clm.synapses.segpipeline.data.label.graph.MaxOverlapFeature;
import edu.utexas.clm.synapses.segpipeline.data.label.graph.OrientationEdgeFeature;
import edu.utexas.clm.synapses.segpipeline.data.label.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.graph.SparseVectorEdgeGraph;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.AbstractLabelMorph;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.ChainOperation;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelDilate;
import edu.utexas.clm.synapses.segpipeline.process.ProbImageToConnectedComponents;
import edu.utexas.clm.synapses.segpipeline.process.Threshold;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ShortProcessor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 *
 */
public class Sparse_Label_Test implements PlugIn
{

    long st = -1;

    public Img<BitType> getThresholdedImage(final String path) throws ImgIOException
    {
        final ImgOpener opener = new ImgOpener();
        final Img<UnsignedByteType> img;
        final Img<BitType> th;
        final long[] dim = new long[2];
        final UnsignedByteType half = new UnsignedByteType(128);
        Threshold<UnsignedByteType> threshold = new Threshold<UnsignedByteType>(half, true);

        img = opener.openImg(path,
            new ArrayImgFactory<UnsignedByteType>(), new UnsignedByteType());
        img.dimensions(dim);
        th = new ArrayImgFactory<BitType>().create(dim, new BitType());

        threshold.compute(img, th);

        return th;
    }


    public void run(String s)
    {
        try
        {
            final Img<BitType> th0 =
                    getThresholdedImage("/nfs/data0/home/larry/Series/Toy/bw01.png");
            final Img<BitType> th1 =
                    getThresholdedImage("/nfs/data0/home/larry/Series/Toy/bw02.png");
            final Img<BitType> th2 =
                    getThresholdedImage("/nfs/data0/home/larry/Series/Toy/bw03.png");
            final long[] dim = new long[2];
            int[][] strel = AbstractLabelMorph.diskStrel(3);
            final Img<IntType> label0, label1, label2;
            int n, m;

            final SerialSparseLabels labels = new SerialSparseLabels();
            final SVEGFactory graphFactory;
            final SparseVectorEdgeGraph graph;

            th0.dimensions(dim);
            label0 = new ArrayImgFactory<IntType>().create(dim, new IntType());
            label1 = new ArrayImgFactory<IntType>().create(dim, new IntType());
            label2 = new ArrayImgFactory<IntType>().create(dim, new IntType());

            n = ProbImageToConnectedComponents.connectedComponents(th0, label0);
            m = ProbImageToConnectedComponents.connectedComponents(th1, label1);
            ProbImageToConnectedComponents.connectedComponents(th2, label2);
            ProbImageToConnectedComponents.addToLabel(label1, n - 1);
            ProbImageToConnectedComponents.addToLabel(label2, n + m - 2);

            IJ.log("First section had " + n + " components");

            ImageJFunctions.show(label0, "Label 0");

            ImageJFunctions.show(label1, "Label 1");

            ImageJFunctions.show(label2, "Label 2");

            SparseLabelFactory factory = new SparseLabelFactory((int)th0.dimension(0),
                    (int)th0.dimension(1));

            factory.makeLabels(label0, 0, labels);
            factory.makeLabels(label1, 1, labels);
            factory.makeLabels(label2, 2, labels);

            labels.buildOverlapMap(new ChainOperation(new LabelDilate(strel),
                    new LabelDilate(strel)));

/*
            for (SparseLabel sl : labels)
            {
                Collection<SparseLabel> overlap = labels.getOverlap(sl);
                if (overlap.isEmpty())
                {
                    IJ.log("No overlaps for " + sl.getValue());
                }
                else
                {
                    String msg = "Overlaps for " + sl.getValue() + ": ";
                    for (SparseLabel slo : overlap)
                    {
                        msg += slo.getValue() + " ";
                    }
                    IJ.log(msg);
                }
            }

            IJ.log("Creating graph factory");
*/

            graphFactory = new SVEGFactory(labels, new OrientationEdgeFeature());

//            IJ.log("Creating graph");

            graph = graphFactory.makeSVEG();

/*
            for (Duplex<Integer, Integer> key : graph.getEdges())
            {
                float[] vector = graph.getEdgeValues(key);
                String msg = "For edge " + key.a + " - " + key.b + ": ";
                for (float f : vector)
                {
                    msg += String.format("%5.4f ", f);
                }
                IJ.log(msg);
            }

            for (SparseLabel sl : labels)
            {
                float[] vector = sl.getFeature();
                String msg = "For node " + sl.getValue() + ": ";
                for (float f : vector)
                {
                    msg += String.format("%5.4f ", f);
                }
                IJ.log(msg);
            }
*/
        }
        catch (Exception e)
        {
            IJ.error("Error: " + e);
            e.printStackTrace();
        }

    }

    private void tic()
    {
        st = System.currentTimeMillis();
    }

    private void toc(String message)
    {
        long pt = System.currentTimeMillis() - st;
        IJ.log(message + ": took " + pt + "ms");
    }

    public SparseLabel getLabel(SparseLabelFactory factory, Img<IntType> img, int l)
    {
        ArrayList<SparseLabel> labels = new ArrayList<SparseLabel>();
        factory.makeLabels(img, 1, labels);
        for (SparseLabel sl : labels)
        {
            if (sl.getValue() == l)
            {
                return sl;
            }
        }
        return new SparseLabel(l, factory.getWidth(), factory.getHeight());
    }

    public void showLabel(SparseLabel sl, String name)
    {
        ShortProcessor sp = new ShortProcessor(sl.getWidth(), sl.getHeight());
        SparseLabelFactory.addLabelTo(sp, sl);
        new ImagePlus(name, sp).show();
    }
}
