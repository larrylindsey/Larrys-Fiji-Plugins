package edu.utexas.clm.synapses.segpipeline.process;

import edu.utexas.clm.archipelago.data.Duplex;
import edu.utexas.clm.archipelago.data.FileChunk;
import edu.utexas.clm.synapses.segpipeline.PipelineUtils;
import edu.utexas.clm.synapses.segpipeline.process.callables.ProbImageToCCCallable;
import edu.utexas.clm.synapses.segpipeline.process.callables.ReindexLabelCallable;
import ij.process.ImageProcessor;
import net.imglib2.Cursor;
import net.imglib2.algorithm.labeling.AllConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 * Calculates a connected-components label image from an input image by thresholding it (after
 * a preprocess step) to a given level, then running AllConnectedComponents. Provides a function
 * to re-index the label image.
 * @param <A>
 */
public class ProbImageToConnectedComponents<A extends RealType<A>>

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
    private float threshold;
    private boolean thresholdGT;
    private final UnaryOperation<Img<BitType>, Img<BitType>> postProcess;

    public ProbImageToConnectedComponents()
    {
        this(new IdentityOperation<Img<BitType>>());
    }

    public ProbImageToConnectedComponents(
            final UnaryOperation<Img<BitType>,Img<BitType>> postProcess)
    {
        this.postProcess = postProcess;
        threshold = 0.5f;
        thresholdGT = true;
    }

    public void setThreshold(final float t, final boolean gt)
    {
        threshold = t;
        thresholdGT = gt;
    }

    public int compute(Img<A> img, Img<IntType> label)
    {
        final Threshold<A> thOperator = new Threshold<A>(
                Threshold.fractionOfMax(img, threshold), thresholdGT);
        final long[] dim = PipelineUtils.getDim(img);
        final Img<BitType> imgT;
        final Img<BitType> imgTPost;
        final int maxVal;

        imgT = thOperator.compute(img, new ArrayImgFactory<BitType>().create(dim, new BitType()));

        imgTPost = postProcess.compute(imgT, imgT.copy());

        maxVal = connectedComponents(imgTPost, label);

        return maxVal;
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

    public static ArrayList<FileChunk> batchPITCC(final ArrayList<File> imageFiles,
                                                  final ExecutorService es,
                                                  final float th,
                                                  final boolean gt,
                                                  final int strSz)
            throws ExecutionException, InterruptedException
    {
        final ArrayList<Future<Duplex<Integer, FileChunk>>> ccResults =
                new ArrayList<Future<Duplex<Integer, FileChunk>>>();
        final ArrayList<Future<FileChunk>> riResults =
                new ArrayList<Future<FileChunk>>();
        final ArrayList<FileChunk> files = new ArrayList<FileChunk>();
        int reindexValue = 0;

        for (File f : imageFiles)
        {
            ccResults.add(es.submit(new ProbImageToCCCallable(new FileChunk(f.getAbsolutePath()),
                    strSz, th, gt)));
        }

        for (Future<Duplex<Integer, FileChunk>> future : ccResults)
        {
            final Duplex<Integer, FileChunk> dup = future.get();
            riResults.add(es.submit(new ReindexLabelCallable(dup.b, reindexValue)));
            reindexValue += dup.a - 1;
        }

        for (Future<FileChunk> future : riResults)
        {
            files.add(future.get());
        }

        return files;
    }
}
