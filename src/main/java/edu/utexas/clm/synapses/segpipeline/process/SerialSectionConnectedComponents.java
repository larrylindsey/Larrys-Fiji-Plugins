package edu.utexas.clm.synapses.segpipeline.process;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.labeling.AllConnectedComponents;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.*;

public class SerialSectionConnectedComponents implements Algorithm, Benchmark
{

    private class ConnectedComponentsCallable implements Callable<Img<IntType>>
    {
        private Iterator<IntType> namer = new Iterator<IntType>()
        {

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
        };


        private final ImagePlus sliceImp;
        private int i;
        private final int idx;

        public ConnectedComponentsCallable(final ImagePlus sliceImp, int idx)
        {
            i = 1;
            this.sliceImp = sliceImp;
            this.idx = idx;
        }

        public Img<IntType> call() throws Exception
        {
            Img<UnsignedByteType> img = ImagePlusAdapter.wrap(sliceImp);
            Img<BitType> imgT;
            Img<IntType> label;

            long[] dimensions = new long[img.numDimensions()];
            NativeImgLabeling<IntType, IntType> labeling;


            img.dimensions(dimensions);
            label = new ArrayImgFactory<IntType>().create(dimensions, new IntType());
            labeling = new NativeImgLabeling<IntType, IntType>(label);
            imgT = new ArrayImgFactory<BitType>().create(dimensions, new BitType());


            thresholdImg(img, imgT);

            AllConnectedComponents.labelAllConnectedComponents(
                    labeling,
                    imgT,
                    namer,
                    new long[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}});

            maxVal[idx - 1] = i;

            return label;
        }
    }

    private class ReindexCallable implements Callable<Img<IntType>>
    {

        private final int idx;
        private final Img<IntType> img;

        public ReindexCallable(final Img<IntType> img, final int idx)
        {
            this.idx = idx;
            this.img = img;
        }

        public Img<IntType> call() throws Exception
        {
            final Cursor<IntType> cursor = img.cursor();
            int addVal = 0;
            IntType it;

            for (int i = 0; i < idx; ++i)
            {
                addVal += maxVal[i];
            }

            IJ.log("Index " + idx + " maxVal(i-1): " + maxVal[idx - 1] + ", addVal: " + addVal);

            while (cursor.hasNext())
            {
                cursor.fwd();
                it = cursor.get();
                if (it.get() > 0)
                {
                    it.set(it.get() + addVal);
                }
            }

            return img;
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

    public SerialSectionConnectedComponents(ImagePlus imp)
    {
        this(imp, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    public SerialSectionConnectedComponents(ImagePlus imp, ExecutorService service)
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

    public <T extends RealType> void thresholdImg(Img<T> fimg, Img<BitType> bimg)
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
            target.get().set(test == thresholdGT);
        }
    }
}
