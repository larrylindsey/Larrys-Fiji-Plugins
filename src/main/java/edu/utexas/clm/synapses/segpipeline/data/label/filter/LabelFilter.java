package edu.utexas.clm.synapses.segpipeline.data.label.filter;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public interface LabelFilter
{
    public boolean filter(final SparseLabel sl);
}
