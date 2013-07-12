package edu.utexas.clm.synapses.segpipeline.process;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
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
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.ErodeGray;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Calculates a connected-components label image from an input image by thresholding it (after
 * a preprocess step) to a given level, then running AllConnectedComponents. Provides a function
 * to re-index the label image.
 * @param <A>
 */
public class ProbImageToConnectedComponents<A extends RealType<A>> implements Algorithm, Benchmark
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

    private long pTime;
    private String errorMsg;
    private final Img<A> img;
    private float threshold;
    private boolean thresholdGT;
    private final UnaryOperation<Img<A>, Img<A>> preProcess;
    private long[] dim;
    private Img<IntType> ccLabel;
    private int maxVal;

    public ProbImageToConnectedComponents(final Img<A> img)
    {
        this(img, new IdentityOperation<Img<A>>());
    }

    public ProbImageToConnectedComponents(final Img<A> img,
                                          final UnaryOperation<Img<A>, Img<A>> preProcess)
    {
        this.img = img;
        this.preProcess = preProcess;
        errorMsg = "";
        threshold = 0.5f;
        thresholdGT = true;
        dim = new long[img.numDimensions()];
        img.dimensions(dim);
        ccLabel = null;
        maxVal = -1;
    }

    public void setThreshold(final float t, final boolean gt)
    {
        threshold = t;
        thresholdGT = gt;
    }

    public boolean checkInput()
    {
        return true;
    }



    /*public ArrayList<Future<Img<IntType>>> connectedComponentsFutures()
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
    } */

    public boolean process()
    {
        final long sTime = System.currentTimeMillis();
        final Img<BitType> imgT = new ArrayImgFactory<BitType>().create(dim, new BitType());
        ccLabel = new ArrayImgFactory<IntType>().create(dim, new IntType());

        thresholdImg(preProcess.compute(img, img.copy()), imgT, threshold, thresholdGT);
        maxVal = connectedComponents(imgT, ccLabel);

        pTime = System.currentTimeMillis() - sTime;
        return true;
    }
    
    public String getErrorMessage() {
        return errorMsg;
    }

    public long getProcessingTime() {
        return pTime;
    }

    public int getMaxVal()
    {
        return maxVal;
    }

    public Img<IntType> getLabel()
    {
        return ccLabel;
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

    public static int connectedComponents(final Img<BitType> img, final Img<IntType> label)
    {
        final IntTypeNamer namer = new IntTypeNamer(1);
        final NativeImgLabeling<IntType, IntType> labeling =
                new NativeImgLabeling<IntType, IntType>(label);

        AllConnectedComponents.labelAllConnectedComponents(
                labeling,
                img,
                namer,
                new long[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}});

        return namer.getLastValue();
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

    public static void addToLabel(ImageProcessor ip, int addVal)
    {
        int v;
        for (int x = 0; x < ip.getWidth(); ++x)
        {
            for (int y = 0; y < ip.getHeight(); ++y)
            {
                v = ip.get(x, y);
                if (v > 0)
                {
                    ip.set(x, y, v + addVal);
                }
            }
        }
    }

    private static long[][] diskStrel(final int r)
    {
        long[][] strel;
        final ArrayList<long[]> v = new ArrayList<long[]>();
        final int r2 = r*r;

        for (int i = 0; i <= r; ++i)
        {
            for (int j = 0; j <= r; ++j)
            {
                if (i*i + j*j <= r2)
                {
                    v.add(new long[]{i,j});
                    v.add(new long[]{-i,j});
                    v.add(new long[]{i,-j});
                    v.add(new long[]{-i,-j});
                }
            }
        }

        strel = new long[v.size()][];

        for (int i = 0; i < v.size(); ++i)
        {
            strel[i] = v.get(i);
        }

        return strel;
    }

    public static <T extends RealType<T>> ErodeGray<T, Img<T>> erodeDisk(int r, T type)
    {
        return new ErodeGray<T, Img<T>>(diskStrel(r));
    }

}
