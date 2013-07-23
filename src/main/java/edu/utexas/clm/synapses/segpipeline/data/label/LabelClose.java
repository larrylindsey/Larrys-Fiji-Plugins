package edu.utexas.clm.synapses.segpipeline.data.label;

/**
 *
 */
public class LabelClose extends AbstractLabelDoubleMorph
{
    private final int[][][] strels;

    public LabelClose(final int[][]... strels)
    {
        this.strels = strels;
    }

    public SparseLabel process(final SparseLabel input)
    {
        final SparseLabel halfway = input.union(seriallyDilatedBorder(input, strels));
        reverseStrels(strels);
        return halfway.subtract(seriallyDilatedBorder(halfway, strels));
    }}
