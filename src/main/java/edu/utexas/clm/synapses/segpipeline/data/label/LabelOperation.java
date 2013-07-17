package edu.utexas.clm.synapses.segpipeline.data.label;

/**
 *
 */
public interface LabelOperation
{
    public SparseLabel process(final SparseLabel input);
}
