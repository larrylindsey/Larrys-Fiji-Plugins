package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.Collection;

/**
 *
 */
public class GroundTruthNodeFeature extends SparseLabelNodeFeature
{
    private final Collection<SparseLabel> positiveAnnotations, negativeAnnotations;

    public GroundTruthNodeFeature(final Collection<SparseLabel> positiveAnnotations,
                                  final Collection<SparseLabel> negativeAnnotations)
    {
        this.positiveAnnotations = positiveAnnotations;
        this.negativeAnnotations = negativeAnnotations;
    }

    @Override
    public int numDimensions()
    {
        return 2;
    }

    @Override
    public void extractFeature(final SparseLabel sl)
    {
        for (final SparseLabel annotation : negativeAnnotations)
        {
            if (sl.getIndex() == annotation.getIndex() &&
                    sl.intersect(annotation))
            {
                sl.getFeature()[offset] = annotation.getValue();
            }
        }

        for (final SparseLabel annotation : positiveAnnotations)
        {
            if (sl.getIndex() == annotation.getIndex() &&
                    sl.intersect(annotation))
            {
                sl.getFeature()[offset + 1] = annotation.getValue();
            }
        }

    }


}
