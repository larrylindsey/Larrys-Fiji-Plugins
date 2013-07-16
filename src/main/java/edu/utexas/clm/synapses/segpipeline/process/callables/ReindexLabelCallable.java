package edu.utexas.clm.synapses.segpipeline.process.callables;

import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.data.FileChunk;
import edu.utexas.clm.synapses.segpipeline.process.ProbImageToConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.io.ImgOpener;
import net.imglib2.io.ImgSaver;
import net.imglib2.type.numeric.integer.IntType;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 *
 */
public class ReindexLabelCallable implements Callable<FileChunk>, Serializable
{
    private final FileChunk file;
    private final int k;

    public ReindexLabelCallable(FileChunk file, int k)
    {
        this.file = file;
        this.k = k;
    }

    public FileChunk call() throws Exception
    {
        if (k > 0)
        {
            final Img<IntType> img = new ImgOpener().openImg(
                    file.getData(), new PlanarImgFactory<IntType>(), new IntType());
            ProbImageToConnectedComponents.addToLabel(img, k);
            FijiArchipelago.log("Saving to file " + file.getData());
            new ImgSaver().saveImg(file.getData(), img);
        }
        return file;
    }
}
