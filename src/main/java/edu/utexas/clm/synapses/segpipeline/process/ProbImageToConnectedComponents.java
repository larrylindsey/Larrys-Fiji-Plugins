package edu.utexas.clm.synapses.segpipeline.process;

import ij.ImagePlus;
import ij.ImageStack;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.labeling.AllConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.*;

public class ProbImageToConnectedComponents implements Algorithm, Benchmark
{
    private static class IntTypeNamer implements Iterator<IntType>
    {
        int i;

        public IntTypeNamer(int start)
        {
            i = start;
        }

        public boolean hasNext()
        {
            return true;
        }

        public IntType next()
        {
            return new IntType(i++);
        }

        public void remove()
        {

        }

        public int getLastValue()
        {
            return i;
        }
    }


    public static <T extends RealType<T>> Img<IntType> connectedComponents(
            final Img<T> img, final float threshold, final boolean gt, final float[] maxVal,
            final int idx)
    {
        final long[] dimensions = new long[img.numDimensions()];
        final IntTypeNamer namer = new IntTypeNamer(1);
        Img<BitType> imgT;
        Img<IntType> label;
        NativeImgLabeling<IntType, IntType> labeling;

        img.dimensions(dimensions);
        label = new ArrayImgFactory<IntType>().create(dimensions, new IntType());
        labeling = new NativeImgLabeling<IntType, IntType>(label);
        imgT = new ArrayImgFactory<BitType>().create(dimensions, new BitType());


        thresholdImg(img, imgT, threshold, gt);

        AllConnectedComponents.labelAllConnectedComponents(
                labeling,
                imgT,
                namer,
                new long[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}});

        maxVal[idx - 1] = namer.getLastValue();

        return label;
    }

    public static <T extends RealType<T>> void addToLabel(Img<T> img, int addVal)
    {
        final Cursor<T> cursor = img.cursor();
        T it;

        while (cursor.hasNext())
        {
            cursor.fwd();
            it = cursor.get();
            if (it.getRealFloat() > 0)
            {
                it.setReal(it.getRealFloat() + addVal);
            }
        }
    }


    private long pTime;
    private final ExecutorService service;
    private String errorMsg;
    private final ImagePlus imp;
    private float threshold;
    private boolean thresholdGT;
    private long[] maxVal;
    private ArrayList<Img<IntType>> ccImgs;

    public ProbImageToConnectedComponents(ImagePlus imp)
    {
        this(imp, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    public ProbImageToConnectedComponents(ImagePlus imp, ExecutorService service)
    {
        this.service = service;
        this.imp = imp;
        errorMsg = "";
        threshold = 0.5f;
        thresholdGT = true;
    }

    public void setThreshold(final float t, final boolean gt)
    {
        threshold = t;
        thresholdGT = gt;
    }


    public boolean checkInput()
    {
        return imp.getType() != ImagePlus.COLOR_RGB && imp.getType() != ImagePlus.COLOR_256;
    }

    public ArrayList<Future<Img<IntType>>> connectedComponentsFutures()
    {
        final ImageStack is = imp.getImageStack();
        maxVal = new long[is.getSize()];
        final ArrayList<Future<Img<IntType>>> ccFutures =
                new ArrayList<Future<Img<IntType>>>(is.getSize());


        if (is.getSize() > 1)
        {
            for (int i = 1; i <= is.getSize(); ++i)
            {
                final String nid = String.format("%05d", i);
                final ImagePlus impSlice = new ImagePlus(imp.getTitle() + " " + nid,
                        is.getProcessor(i));
                ccFutures.add(service.submit(new ConnectedComponentsCallable(impSlice, i)));
            }
        }
        else
        {
            ccFutures.add(service.submit(new ConnectedComponentsCallable(imp, 1)));
        }

        return ccFutures;
    }

    public boolean process()
    {
        final long sTime = System.currentTimeMillis();
        final ArrayList<Future<Img<IntType>>> ccFutures = connectedComponentsFutures();
        final ArrayList<Future<Img<IntType>>> riFutures =
                new ArrayList<Future<Img<IntType>>>(ccFutures.size() - 1);
        ccImgs = new ArrayList<Img<IntType>>(ccFutures.size());

        try
        {
            for (int i = 0; i < ccFutures.size(); ++i)
            {
                if (i == 0)
                {
                    ccImgs.add(ccFutures.get(i).get());
                }
                else
                {
                    riFutures.add(service.submit(new ReindexCallable(ccFutures.get(i).get(), i)));
                }
            }

            for (Future<Img<IntType>> future : riFutures)
            {
                ccImgs.add(future.get());
            }
        }
        catch (InterruptedException ie)
        {
            errorMsg = ie.toString();
            return false;
        }
        catch (ExecutionException ee)
        {
            errorMsg = ee.toString();
            return false;
        }

        pTime = System.currentTimeMillis() - sTime;
        return true;
    }
    


    public ImageStack getImageStack()
    {
        final ImageStack outputStack = new ImageStack(imp.getWidth(), imp.getHeight(),
                ColorModel.getRGBdefault());
        for (final Img<IntType> img : ccImgs)
        {

            outputStack.addSlice(ImageJFunctions.wrapFloat(img, "Image").getProcessor());
        }
        return outputStack;
    }

    public ArrayList<Img<IntType>> getImgs()
    {
        return new ArrayList<Img<IntType>>(ccImgs);
    }


    public String getErrorMessage() {
        return errorMsg;
    }

    public long getProcessingTime() {
        return pTime;
    }

    public static <T extends RealType> void thresholdImg(Img<T> fimg, Img<BitType> bimg,
                                                         float threshold, boolean gt)
    {
        Cursor<BitType> target = bimg.localizingCursor();
        RandomAccess<T> source = fimg.randomAccess();
        T t = fimg.firstElement();
        double max = t.getMaxValue();
        boolean test;

        while ( target.hasNext())
        {

            target.fwd();
            source.setPosition(target);
            test = source.get().getRealDouble() / max >= threshold;
            target.get().set(test == gt);
        }
    }
}
