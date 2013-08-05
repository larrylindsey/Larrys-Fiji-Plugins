package edu.utexas.clm.synapses.segpipeline.plugin;

import edu.utexas.clm.synapses.segpipeline.data.label.*;
import edu.utexas.clm.synapses.segpipeline.process.Threshold;
import hr.irb.fastRandomForest.FastRandomForest;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ShortProcessor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Random;

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


    public void wekaToy()
    {
        final FastRandomForest rf = new FastRandomForest();
        final Classifier wc = rf;
        final Instances trainData;
        final Instances info;

        final ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        final ArrayList<String> names = new ArrayList<String>();

        rf.setNumTrees(3);
        rf.setNumFeatures(3);
        rf.setSeed(new Random().nextInt());
        rf.setNumThreads(1);

        attributes.add(new Attribute("Value1"));
        attributes.add(new Attribute("Value2"));
        names.add("Class1");
        names.add("Class2");
        attributes.add(new Attribute("class", names));

        trainData = new Instances("location", attributes, 1);
        trainData.setClassIndex(trainData.numAttributes() - 1);

        trainData.add(new DenseInstance(1.0, new double[]{1.0, 1.0, 0.0}));
        trainData.add(new DenseInstance(1.0, new double[]{1.0, 2.0, 0.0}));
        trainData.add(new DenseInstance(1.0, new double[]{2.0, 2.0, 0.0}));
        trainData.add(new DenseInstance(1.0, new double[]{3.0, 2.0, 0.0}));
        trainData.add(new DenseInstance(1.0, new double[]{3.0, 3.0, 0.0}));
        trainData.add(new DenseInstance(1.0, new double[]{3.0, 4.0, 0.0}));
        trainData.add(new DenseInstance(1.0, new double[]{3.0, 5.0, 0.0}));
        trainData.add(new DenseInstance(1.0, new double[]{4.0, 5.0, 0.0}));
        trainData.add(new DenseInstance(1.0, new double[]{5.0, 5.0, 0.0}));
        trainData.add(new DenseInstance(1.0, new double[]{5.0, 6.0, 0.0}));
        trainData.add(new DenseInstance(1.0, new double[]{5.0, 7.0, 0.0}));

        trainData.add(new DenseInstance(1.0, new double[]{2.0, 1.0, 1.0}));
        trainData.add(new DenseInstance(1.0, new double[]{3.0, 1.0, 1.0}));
        trainData.add(new DenseInstance(1.0, new double[]{4.0, 1.0, 1.0}));
        trainData.add(new DenseInstance(1.0, new double[]{4.0, 2.0, 1.0}));
        trainData.add(new DenseInstance(1.0, new double[]{4.0, 3.0, 1.0}));
        trainData.add(new DenseInstance(1.0, new double[]{4.0, 4.0, 1.0}));
        trainData.add(new DenseInstance(1.0, new double[]{5.0, 4.0, 1.0}));
        trainData.add(new DenseInstance(1.0, new double[]{6.0, 4.0, 1.0}));
        trainData.add(new DenseInstance(1.0, new double[]{6.0, 5.0, 1.0}));
        trainData.add(new DenseInstance(1.0, new double[]{6.0, 6.0, 1.0}));
        trainData.add(new DenseInstance(1.0, new double[]{6.0, 7.0, 1.0}));


        info = new Instances("location", attributes, 1);
        info.setClassIndex(info.numAttributes() - 1);

        try
        {

            wc.buildClassifier(trainData);

            for (int x = 1; x < 8; ++x)
            {
                for (int y = 1; y < 8; ++y)
                {
                    DenseInstance ins = new DenseInstance(1.0, new double[]{x, y, 0.0});
                    String msg = "For " + x + ", " + y + ": ";
                    ins.setDataset(info);
                    for (double d : wc.distributionForInstance(ins))
                    {
                        msg += d + ", ";
                    }
                    IJ.log(msg);
                }
            }

        }
        catch (Exception e)
        {
            IJ.log("nuts");
            e.printStackTrace();

        }

    }

    public void run(String s)
    {



        /*try
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

            graphFactory = new SVEGFactory(labels, new OrientationEdgeFeature());

            graph = graphFactory.makeSVEG();

        }
        catch (Exception e)
        {
            IJ.error("Error: " + e);
            e.printStackTrace();
        }*/

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
