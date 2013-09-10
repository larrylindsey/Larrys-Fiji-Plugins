package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public abstract class AbstractSetOperationFeature extends SparseLabelEdgeFeature
{
    public int numDimensions()
    {
        return 1;
    }

    public Collection<SparseLabel> accept(final SVEGFactory factory, final SparseLabel sl)
    {
        return acceptCrossPlaneNeighbors(factory, sl);
    }
}
