package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.Collection;

/**
 *
 */
public class DenseGroundTruthNodeFeature extends SparseLabelNodeFeature
{
    private final Collection<SparseLabel> annotations;

    public DenseGroundTruthNodeFeature(final Collection<SparseLabel> annotations)
    {
        this.annotations = annotations;
    }

    @Override
    public int numDimensions()
    {
        return 1;
    }

    @Override
    public void extractFeature(final SparseLabel sl)
    {
        for (final SparseLabel annotation : annotations)
        {
            if (sl.getIndex() == annotation.getIndex() &&
                    sl.intersect(annotation))
            {
                sl.getFeature()[offset] = annotation.getValue();
                return;
            }
        }
        sl.getFeature()[offset] = 0;
    }


}
