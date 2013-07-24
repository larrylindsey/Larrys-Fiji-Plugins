package edu.utexas.clm.synapses.segpipeline.data.label.operations;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class LabelDilate extends AbstractLabelMorph
{
    private final int[][] strel;

    public LabelDilate(final int[][] strel)
    {
        this.strel = strel;
    }

    public SparseLabel process(SparseLabel input)
    {
        return input.union(dilatedBorder(input, strel));
    }
}
