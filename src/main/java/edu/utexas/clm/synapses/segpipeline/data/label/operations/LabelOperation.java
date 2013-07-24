package edu.utexas.clm.synapses.segpipeline.data.label.operations;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public interface LabelOperation
{
    public SparseLabel process(final SparseLabel input);
}
