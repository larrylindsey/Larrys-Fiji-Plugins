package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.archipelago.data.Duplex;
import ij.IJ;

import java.io.Serializable;
import java.util.*;

/**
 *
 */
public class SparseVectorEdgeGraph implements Serializable
{

    private final int vectorSize;
    private final Map<Duplex<Integer, Integer>, float[]> edges;
    private final float[] zeroVector;


    public SparseVectorEdgeGraph(final int vectorSize)
    {
        this.vectorSize = vectorSize;
        edges = new HashMap<Duplex<Integer, Integer>, float[]>();
        zeroVector = new float[this.vectorSize];
    }


//    public void setEdgeValue(final int from, final int to, final int d, final float value)
//    {
//        getOrCreateEdgeValues(from, to)[d] = value;
//    }

    public int getVectorSize()
    {
        return vectorSize;
    }

    public float[] getOrCreateEdgeValues(final int from, final int to)
    {
        return getOrCreateEdgeValues(new Duplex<Integer, Integer>(from, to));
    }

    public float[] getOrCreateEdgeValues(final Duplex<Integer, Integer> key)
    {
        float[] value = edges.get(key);

        if (value == null)
        {
            value = new float[vectorSize];
            // Is this redundant?
            for (int i = 0; i < vectorSize; ++i)
            {
                value[i] = 0;
            }
            edges.put(key, value);
        }

        return value;
    }

    public SparseVectorEdgeGraph thresholdGraph(final EdgeThreshold et)
    {
        return thresholdGraph(et, 0);
    }

    public SparseVectorEdgeGraph thresholdGraph(final EdgeThreshold et, final int newSize)
    {
        final SparseVectorEdgeGraph sveg = new SparseVectorEdgeGraph(newSize);

        for (Duplex<Integer, Integer> dup : edges.keySet())
        {
            if (et.threshold(edges.get(dup)))
            {
                final float[] newVal = new float[newSize];
                sveg.edges.put(dup, newVal);
                for (int i = 0; i < newVal.length; ++i)
                {
                    newVal[i] = 0;
                }
            }
        }

        return sveg;
    }

    public void merge(final SparseVectorEdgeGraph sveg)
    {
        if (sveg.vectorSize != vectorSize)
        {
            throw new RuntimeException(
                    "Cannot merge SparseVectorEdgeGraph's of differing cardinality.\n" +
                            "This: " + getVectorSize() + ", That: " + sveg.getVectorSize());
        }

        for (Duplex<Integer, Integer> dup : sveg.getEdges())
        {
            final float[] ourEdgeVal = getOrCreateEdgeValues(dup);
            final float[] theirEdgeVal = sveg.getEdgeValues(dup);

            for (int i = 0; i < vectorSize; ++i)
            {
                if (theirEdgeVal[i] > ourEdgeVal[i])
                {
                    ourEdgeVal[i] = theirEdgeVal[i];
                }
            }
        }
    }

    public Set<Duplex<Integer, Integer>> getEdges()
    {
        final TreeSet<Duplex<Integer,Integer>> set =
                new TreeSet<Duplex<Integer, Integer>>(duplexComparator());
        set.addAll(edges.keySet());

        return set;
    }

    public float[] getEdgeValues(final Duplex<Integer, Integer> key)
    {
        float[] values = edges.get(key);
        return values == null ? zeroVector.clone() : values;
    }

    public float[] getEdgeValues(final int a, final int b)
    {
        return getEdgeValues(new Duplex<Integer, Integer>(a, b));
    }


    public static Comparator<Duplex<Integer, Integer>> duplexComparator()
    {
        return new Comparator<Duplex<Integer, Integer>>()
        {
            public int intSignum(final int a)
            {
                return a > 0 ? 1 : a == 0 ? 0 : -1;
            }

            public int compare(Duplex<Integer, Integer> o1, Duplex<Integer, Integer> o2)
            {
                int comp1 = o1.a.compareTo(o2.a);
                if (comp1 == 0)
                {
                    return intSignum(o1.b.compareTo(o2.b));
                }
                else
                {
                    return intSignum(comp1);
                }
            }
        };
    }
}
