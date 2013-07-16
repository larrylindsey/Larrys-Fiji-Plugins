package edu.utexas.clm.synapses.segpipeline;

import net.imglib2.img.Img;

/**
 *
 */
public final class PipelineUtils
{
    private PipelineUtils(){}

    public static long[] getDim(Img<?> img)
    {
        long[] dim = new long[img.numDimensions()];
        img.dimensions(dim);
        return dim;
    }

}
