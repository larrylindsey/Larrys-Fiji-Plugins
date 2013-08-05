package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.ArrayList;

/**
 *
 */
public class NullEdgeFeature extends SparseLabelEdgeFeature
{
    private final int size;

    public NullEdgeFeature(final int size)
    {
        this.size = size;
    }

    @Override
    public int numDimensions()
    {
        return size;
    }

    /**
     * Does nothing
     * {@inheritDoc}
     */
    public void extractFeature(SVEGFactory factory, SparseLabel sl0, SparseLabel sl1, int offset){}

    @Override
    public Iterable<SparseLabel> accept(SVEGFactory factory, SparseLabel sl)
    {
        return new ArrayList<SparseLabel>();
    }

    public String name()
    {
        return "Null";
    }
}
