package edu.utexas.clm.synapses.segpipeline.data.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 *
 */
public class InplaneOverlapHistogramFeature extends AbstractInplaneEdgeFeature
{
    private final int bins, binIncrement;
    private transient ExecutorService service;

    public InplaneOverlapHistogramFeature(final int bins, final int binIncrement)
    {
        this(-1, bins, binIncrement);
    }

    public InplaneOverlapHistogramFeature(final int restrictIndex,
                                          final int bins, final int binIncrement)
    {
        super(restrictIndex);
        this.bins = bins;
        this.binIncrement = binIncrement;
    }

    @Override
    public int numDimensions()
    {
        return bins;
    }

    @Override
    public void extractFeature(final SVEGFactory factory, final SparseLabel sl0,
                               final SparseLabel sl1, final int offset)
    {
        if (!indexRestricted || restrictIndex == sl0.getIndex())
        {
            final float[] vector = factory.getVector(sl0, sl1);
            final ArrayList<SparseLabel> nbd0 =
                    factory.getLabels().getOverlap(sl0);

            // Zero out our section of the vector
            for (int i = 0; i < bins; ++i)
            {
                vector[i + offset] = 0;
            }

            nbd0.retainAll(factory.getLabels().getOverlap(sl1));

            for (final SparseLabel sl : nbd0)
            {
                final int bin = Math.min(sl.area() / binIncrement, bins - 1);
                vector[offset + bin]++;
            }
        }
    }
}
