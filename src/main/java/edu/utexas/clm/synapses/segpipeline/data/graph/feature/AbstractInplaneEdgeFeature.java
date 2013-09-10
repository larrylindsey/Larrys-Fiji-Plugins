package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.ArrayList;

/**
 *
 */
public abstract class AbstractInplaneEdgeFeature extends SparseLabelEdgeFeature
{

    protected int restrictIndex;
    protected final boolean indexRestricted;

    protected AbstractInplaneEdgeFeature(final int restrictIndex)
    {
        this.restrictIndex = restrictIndex;
        indexRestricted = restrictIndex >= 0;
    }

    public Iterable<SparseLabel> accept(final SVEGFactory factory, final SparseLabel sl)
    {
        if (indexRestricted && restrictIndex != sl.getIndex())
        {
            return new ArrayList<SparseLabel>();
        }
        else
        {
            return acceptInPlaneNeighbors(factory, sl);
        }
    }
}
