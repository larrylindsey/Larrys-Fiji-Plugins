package edu.utexas.clm.synapses.segpipeline.data.label;

/**
 *
 */
public abstract class AbstractLabelDoubleMorph extends AbstractLabelMorph
{

    protected void reverseStrels(final int[][][] strels)
    {
        final int n = strels.length;
        int[][] tempStrel;
        for (int i = 0; i < n / 2; ++i)
        {
            final int k = n - i - 1;
            tempStrel = strels[i];
            strels[i] = strels[k];
            strels[k] = tempStrel;
        }
    }

    protected SparseLabel seriallyDilatedBorder(final SparseLabel input, final int[][][] strels)
    {
        SparseLabel border = dilatedBorder(input, strels[0]);

        for (int i = 1; i < strels.length; ++i)
        {
            border = new LabelDilate(strels[i]).process(border);
        }

        return border;
    }

}
