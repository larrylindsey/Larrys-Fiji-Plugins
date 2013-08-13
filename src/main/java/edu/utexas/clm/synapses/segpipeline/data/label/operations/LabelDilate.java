package edu.utexas.clm.synapses.segpipeline.data.label.operations;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class LabelDilate implements LabelOperation
{
    private final int[][] strel;

    public LabelDilate(final int[][] strel)
    {
        this.strel = strel;
    }

    public SparseLabel process(SparseLabel input)
    {
        return input.union(new DilatedBorderOperation(strel).process(input));
    }
}
