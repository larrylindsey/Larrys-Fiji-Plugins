package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelOperation;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.OperationCallable;
import edu.utexas.clm.synapses.segpipeline.process.Multithreaded;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 */
public class InplaneOverlapHistogramFeature extends SparseLabelEdgeFeature
{
    private static final Comparator<SparseLabel> valueComparator = SparseLabel.valueComparator();

    //private final ArrayList<TreeSet<SparseLabel>> overlapMap;
    private final int bins, binIncrement, restrictIndex;
    private final boolean indexRestricted;
    private transient ExecutorService service;



    public InplaneOverlapHistogramFeature(final int restrictIndex,
                                          final int bins, final int binIncrement)
    {
        this.bins = bins;
        this.binIncrement = binIncrement;
        this.restrictIndex = restrictIndex;
        indexRestricted = restrictIndex >= 0;
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
        float[] vector = factory.getVector(sl0, sl1);
        // Zero out our section of the vector
        for (int i = 0; i < bins; ++i)
        {
            vector[i + offset] = 0;
        }

        if (!indexRestricted || restrictIndex == sl0.getIndex())
        {
            final ArrayList<SparseLabel> nbd0 =
                    factory.getLabels().getOverlap(sl0);
            nbd0.retainAll(factory.getLabels().getOverlap(sl1));

            for (final SparseLabel sl : nbd0)
            {
                final int bin = Math.max(sl.area() / binIncrement, bins - 1);
                vector[offset + bin]++;
            }
        }
    }

    public Iterable<SparseLabel> accept(final SVEGFactory factory, final SparseLabel sl)
    {
        if (indexRestricted && restrictIndex != sl.getIndex())
        {
            return new ArrayList<SparseLabel>();
        }
        else
        {
            return factory.getLabels().getOverlap(sl);
        }
    }
}