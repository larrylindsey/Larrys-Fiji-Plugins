package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public abstract class AbstractSetOperationFeature extends SparseLabelFeature
{
    public int numDimensions()
    {
        return 1;
    }

    public Collection<SparseLabel> accept(SparseLabel sl, final SerialSparseLabels all)
    {
        final ArrayList<SparseLabel> acceptList =
                new ArrayList<SparseLabel>(all.getLabels(sl.getIndex() + 1).size() / 2);
        for (final SparseLabel currSl : all.getLabels(sl.getIndex() + 1))
        {
            if (currSl.isBoundingBoxOverlap(sl))
            {
                acceptList.add(currSl);
            }
        }
        acceptList.trimToSize();
        return acceptList;
    }

}
