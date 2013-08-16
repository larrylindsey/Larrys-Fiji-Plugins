package edu.utexas.clm.synapses.segpipeline.data.label;

import weka.core.pmml.SparseArray;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 *
 */
public class SparseLabel
{
    private int[] idx;
    private int width, height;
    private int val;
    private int index;
    private int minx, miny, maxx, maxy;
    private float[] nodeFeature;

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
        this.index = 1;
        nodeFeature = new float[0];
    }

    public SparseLabel(final SparseLabel sl)
    {
        this(sl, sl.idx.clone());
    }

    public SparseLabel(final SparseLabel sl, final int[] idx)
    {
        val = sl.val;
        width = sl.width;
        height = sl.height;
        index = sl.index;
        this.idx = idx;
        nodeFeature = sl.nodeFeature.clone();
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public int getIndex()
    {
        return index;
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

    private void calculateBoundingBox()
    {
        int minx = width, maxx = 0, miny = height, maxy = 0;
        int x, y;
        for (int i : idx)
        {
            x = i % width;
            y = i / width;
            if (x < minx)
            {
                minx = x;
            }
            if (y < miny)
            {
                miny = y;
            }
            if (x > maxx)
            {
                maxx = x;
            }
            if (y > maxy)
            {
                maxy = y;
            }
        }

        this.minx = minx;
        this.miny = miny;
        this.maxx = maxx;
        this.maxy = maxy;
    }

    public boolean isBoundingBoxOverlap(final SparseLabel sl)
    {
        return maxx >= sl.minx && minx <= sl.maxx && maxy >= sl.miny && miny <= sl.maxy;
    }

    public boolean intersect(final SparseLabel sl)
    {

        if (sl != null && isBoundingBoxOverlap(sl)) //TODO: is this faster?
        {
            int i = 0, j = 0;
            while (i < idx.length && j < sl.idx.length)
            {
                if (idx[i] == sl.idx[j])
                {
                    return true;
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
        }
        return false;
    }

    public SparseLabel intersection(final SparseLabel sl)
    {
        final int[] tempIdx = new int[idx.length];

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

        return new SparseLabel(this, trimArray(tempIdx, k));
    }

    public SparseLabel subtract(final SparseLabel sl)
    {
        final int[] tempIdx = new int[idx.length];

        int i = 0, j = 0, k = 0;
        while (i < idx.length && j < sl.idx.length)
        {
            if (idx[i] == sl.idx[j])
            {
                ++i;
                ++j;
            }
            else if (idx[i] < sl.idx[j])
            {
                tempIdx[k] = idx[i];
                ++k;
                ++i;
            }
            else
            {
                while (j < sl.idx.length && idx[i] > sl.idx[j])
                {
                    ++j;
                }
            }
        }

        while (i < idx.length)
        {
            tempIdx[k] = idx[i];
            ++i;
            ++k;
        }

        return new SparseLabel(this, trimArray(tempIdx, k));
    }

    public SparseLabel union(final SparseLabel sl)
    {
        final int[] tempIdx = new int[idx.length + sl.idx.length];

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

        return new SparseLabel(this, trimArray(tempIdx, k));
    }

    private int[] trimArray(final int[] array, final int l)
    {
        final int[] trimmed = new int[l];
        for (int i = 0; i < l; ++i)
        {
            trimmed[i] = array[i];
        }
        return trimmed;
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

    public static Comparator<SparseLabel> valueComparator()
    {
        return new Comparator<SparseLabel>()
        {
            public int compare(SparseLabel o1, SparseLabel o2)
            {
                return o1.getValue() - o2.getValue();
            }
        };
    }

    /**
     * Returns true if Object o is a SparseLabel with the same value and index as this one. This function
     * assumes that only one SparseLabel exists with this given value and index, or that any other such SparseLabel
     * is equivalent.
     * @param o an Object that may or may not be equal to this SparseLabel.
     * @return true if Object o is a SparseLabel with the same value as this one.
     */
    public boolean equals(final Object o)
    {
        SparseLabel other;
        return (o instanceof SparseLabel) &&
                (other = (SparseLabel)o).val == val
                && other.index == index;
    }

    public void setFeatureSize(int size)
    {
        nodeFeature = new float[size];
        for (int i = 0; i < size; ++i)
        {
            nodeFeature[i] = 0;
        }
    }

    public float[] getFeature()
    {
        return nodeFeature;
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
