package edu.utexas.clm.synapses.segpipeline.data.label.operations;

import java.util.ArrayList;

/**
 *
 */
public final class Strels
{
    private Strels(){}

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
