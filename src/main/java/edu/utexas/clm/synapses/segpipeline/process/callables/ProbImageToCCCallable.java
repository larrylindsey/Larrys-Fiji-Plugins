package edu.utexas.clm.synapses.segpipeline.process.callables;

import edu.utexas.clm.archipelago.data.Duplex;
import edu.utexas.clm.archipelago.data.FileChunk;
import edu.utexas.clm.synapses.segpipeline.PipelineUtils;
import edu.utexas.clm.synapses.segpipeline.process.OpenGray;
import edu.utexas.clm.synapses.segpipeline.process.ProbImageToConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.io.ImgOpener;
import net.imglib2.io.ImgSaver;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 *
 */
public class ProbImageToCCCallable<T extends RealType<T> & NativeType<T>>
        implements Callable<Duplex<Integer, FileChunk>>, Serializable
{
    
    private final FileChunk file;
    private final int strelSize;
    private final float threshold;
    private final boolean gt;
    
    public ProbImageToCCCallable(final FileChunk file, final int strelSize,
                                 final float threshold, final boolean gt)
    {
        this.file = file;
        this.strelSize = strelSize;
        this.threshold = threshold;
        this.gt = gt;
    }

    public Duplex<Integer, FileChunk> call() throws Exception
    {
        final ImgPlus<T> img = new ImgOpener().openImg(file.getData());
        final Img<IntType> label =
                new PlanarImgFactory<IntType>().create(PipelineUtils.getDim(img), new IntType());
        final ProbImageToConnectedComponents<T> ptcc =
                new ProbImageToConnectedComponents<T>(
                        new OpenGray<BitType>(OpenGray.diskStrel(strelSize)));
        final int maxVal;
        final FileChunk outputFile = new FileChunk(outputPath());

        ptcc.setThreshold(threshold, gt);

        maxVal = ptcc.compute(img, label);

        new ImgSaver().saveImg(outputFile.getData(), label);

        return new Duplex<Integer, FileChunk>(maxVal, outputFile);
    }

    public String outputPath()
    {
        final int dot = file.getData().lastIndexOf(".");
        return file.getData().substring(0, dot) + "_label.tif";
    }



}
