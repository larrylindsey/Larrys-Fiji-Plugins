package edu.utexas.clm.synapses.segpipeline.data.label.operations;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class LabelErode extends AbstractLabelMorph
{
    private final int[][] strel;

    public LabelErode(final int[][] strel)
    {
        this.strel = strel;
    }

    public SparseLabel process(SparseLabel input)
    {
        return input.subtract(dilatedBorder(input, strel));
    }
}
