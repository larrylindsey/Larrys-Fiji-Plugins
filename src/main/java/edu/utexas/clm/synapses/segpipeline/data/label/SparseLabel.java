package edu.utexas.clm.synapses.segpipeline.data.label;

import java.util.Arrays;
import java.util.Iterator;

/**
 *
 */
public class SparseLabel
{
    private int[] idx;
    private int width, height;
    private int val;

    public SparseLabel(final int val, final int width, final int height)
    {
        this(val, width, height, new int[0]);
    }

    public SparseLabel(final int val, final int width, final int height, final int[] idx)
    {
        // idx *must* be sorted, ascending. This is not checked, and will break things.
        this.val = val;
        this.width = width;
        this.height = height;
        this.idx = idx.clone();
    }

    public SparseLabel(final SparseLabel sl)
    {
        val = sl.val;
        width = sl.width;
        height = sl.height;
        idx = sl.idx.clone();
    }

    public int area()
    {
        return idx.length;
    }

    public int[] getIdx()
    {
        return idx;
    }

    public int getValue()
    {
        return val;
    }

    public void setValue(final int val)
    {
        this.val = val;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public SparseLabel intersection(final SparseLabel sl)
    {
        final int[] tempIdx = new int[idx.length];
        final int[] isectIdx;

        int i = 0, j = 0, k = 0;
        while (i < idx.length && j < sl.idx.length)
        {
            if (idx[i] == sl.idx[j])
            {
                tempIdx[k] = idx[i];
                ++i;
                ++j;
                ++k;
            }
            else if (idx[i] < sl.idx[j])
            {
                ++i;
            }
            else
            {
                ++j;
            }
        }

        isectIdx = new int[k];
        for (int l = 0; l < k; ++l)
        {
            isectIdx[l] = tempIdx[l];
        }

        return new SparseLabel(val, width, height, isectIdx);
    }

    public SparseLabel union(final SparseLabel sl)
    {
        final int[] tempIdx = new int[idx.length + sl.idx.length];
        final int[] unionIdx;

        int i = 0, j = 0, k = 0;
        while (i < idx.length && j < sl.idx.length)
        {
            if (idx[i] == sl.idx[j])
            {
                tempIdx[k] = idx[i];
                i++;
                j++;
                k++;
            }
            else if (idx[i] < sl.idx[j])
            {
                tempIdx[k] = idx[i];
                i++;
                k++;
            }
            else
            {
                tempIdx[k] = sl.idx[j];
                j++;
                k++;
            }
        }

        unionIdx = new int[k];
        for (int l = 0; l < k; ++l)
        {
            unionIdx[l] = tempIdx[l];
        }

        return new SparseLabel(val, width, height, unionIdx);
    }

    private boolean isBoundary(int i)
    {
        /*
         Case 1: West
           i > 0                    : if i is zero, there can be no west neighbor
           idx[i] % width > 0       : if idx[i] % width == 0, we are at the left boundary
           idx[i - 1] == idx[i] - 1 : Look for a value k in idx such that k = idx[i] - 1.
                                      Since idx is sorted, we only check idx[i - 1].
         Case 2: East
           i + 1 < idx.length       : if i + 1 >= idx.length, there can be no east neighbor
           idx[i] % width !~ -1     : idx[i] modulo with ~ -1 indicates that we are at the right
                                      boundary. Since this is Java, we test idx[i] + 1 > 0
           idx[i + 1] == idx[i] + 1 : Same as for West, but the opposite direction.
         Case 3: North
           Search idx in [i - width, i) for the value idx[i] - width. Since we're sorted, use
           binarySearch.
         Case 4: South
           Search idx in (i, i + width] for idx[i] + width
         */
        return (!(i > 0 && idx[i] % width > 0 && idx[i - 1] == idx[i] - 1) ||
                !(i + 1 < idx.length && idx[i] + 1 % width > 0 && idx[i + 1] == idx[i] + 1) ||
                !(Arrays.binarySearch(idx, Math.max(i - width, 0), i, idx[i] - width) > 0) ||
                !(Arrays.binarySearch(idx, i + 1, Math.min(i + width + 1, idx.length - 1), idx[i] + width) > 0));
    }


    /**
     * Returns an Iterable over the boundary locations of this label. This method is not threadsafe,
     * ie, modifying the label concurrently will lead to unpredictable behavior.
     * @return an Iterable over the boundary locations of this label
     */
    public Iterable<Integer> boundaryIterable()
    {
        return new Iterable<Integer>()
        {
            public Iterator<Integer> iterator()
            {
                return new Iterator<Integer>()
                {
                    int i = 0;

                    public boolean hasNext()
                    {
                        boolean b = false;
                        while (i < idx.length && !(b = isBoundary(i)))
                        {
                            ++i;
                        }
                        return b;
                    }

                    public Integer next()
                    {
                        return idx[i++];
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }


   /* private int[] convertIdx(SparseLabel sl)
    {
        // If the image size is the same, we can just return the same indices.
        if (sl.width == width && sl.height == height)
        {
            return sl.idx;
        }
        else
        {
            // Otherwise, we need to so some math.
            final int[] cidx = new int[sl.idx.length];
            int j = 0;
            for (int i = 0; i < sl.idx.length; ++i)
            {
                int k = sl.idx[i];
                int y = k / sl.width;
                int x = k  - (y * sl.width);
                if (y < height && x < width)
                {
                    cidx[j++] = x + y * width;
                }
            }

            if (j == cidx.length)
            {
                return cidx;
            }
            else
            {
                //This can happen if sl's size is bigger, and there are points out-of-bounds
                final int[] cpidx = new int[j];
                for (int i = 0; i < j; ++i)
                {
                    cpidx[i] = cidx[i];
                }
                return cpidx;
            }
        }
    }*/


}
