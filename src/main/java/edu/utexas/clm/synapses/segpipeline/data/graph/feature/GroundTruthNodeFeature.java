package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 *
 */
public class GroundTruthNodeFeature extends SparseLabelNodeFeature
{
    private final Collection<SparseLabel> annotations;

    public GroundTruthNodeFeature(final Collection<SparseLabel> annotations)
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
        int ovlpCount = 0;
        int newCount;
        for (final SparseLabel annotation : annotations)
        {
            if (sl.getIndex() == annotation.getIndex() &&
                    sl.intersect(annotation) &&
                    (newCount = sl.intersection(annotation).area()) > ovlpCount)
            {
                sl.getFeature()[offset] = annotation.getValue();
                ovlpCount = newCount;
            }
        }
    }


}
