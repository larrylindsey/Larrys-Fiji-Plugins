package edu.utexas.clm.synapses.segpipeline.data.label.operations;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 *
 */
public abstract class AbstractLabelMorph implements LabelOperation
{

    protected SparseLabel dilatedBorder(final SparseLabel input, int[][] strel)
    {
        TreeSet<Integer> dilatedBorder = new TreeSet<Integer>();
        final int height = input.getHeight(), width = input.getWidth();
        final int[] linearStrel = convertStrel(input.getWidth(), strel);
        final int[] dilatedIdx;
        int t = 0;

        for (int i : input.boundaryIterable())
        {
            for (int j = 0; j < linearStrel.length; ++j)
            {
                if (check(i, j, width, height, strel))
                {
                    dilatedBorder.add(i + linearStrel[j]);
                }
            }
        }

        dilatedIdx = new int[dilatedBorder.size()];

        for (int i : dilatedBorder)
        {
            dilatedIdx[t++] = i;
        }

        return new SparseLabel(input, dilatedIdx);
    }

    private boolean check(final int i, final int j, final int w, final int h, final int[][] strel)
    {
        int xo = i % w + strel[j][0];
        int yo = i / w + strel[j][1];
        return xo > 0 && xo < w && yo > 0 && yo < h;
    }

    private int[] convertStrel(final int width, final int strel[][])
    {
        final int[] linearStrel = new int[strel.length];
        for (int i = 0; i < strel.length; ++i)
        {
            linearStrel[i] = strel[i][0] + width * strel[i][1];
        }
        return linearStrel;
    }

    public static int[][] diskStrel(final int r)
    {
        int[][] strel;
        final ArrayList<int[]> v = new ArrayList<int[]>();
        final int r2 = r*r;

        for (int i = 0; i <= r; ++i)
        {
            for (int j = 0; j <= r; ++j)
            {
                if (i*i + j*j <= r2)
                {
                    v.add(new int[]{i,j});
                    v.add(new int[]{-i,j});
                    v.add(new int[]{i,-j});
                    v.add(new int[]{-i,-j});
                }
            }
        }

        strel = new int[v.size()][];

        for (int i = 0; i < v.size(); ++i)
        {
            strel[i] = v.get(i);
        }

        return strel;
    }

}
