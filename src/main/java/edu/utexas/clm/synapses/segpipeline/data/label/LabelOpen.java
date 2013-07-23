package edu.utexas.clm.synapses.segpipeline.data.label;

/**
 *
 */
public class LabelOpen extends AbstractLabelDoubleMorph
{

    private final int[][][] strels;

    public LabelOpen(final int[][]... strels)
    {
        this.strels = strels;
    }

    public SparseLabel process(final SparseLabel input)
    {
        final SparseLabel halfway = input.subtract(seriallyDilatedBorder(input, strels));
        reverseStrels(strels);
        return halfway.union(seriallyDilatedBorder(halfway, strels));
    }
}
