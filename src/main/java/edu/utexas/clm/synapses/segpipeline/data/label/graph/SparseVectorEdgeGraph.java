package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.archipelago.data.Duplex;

import java.io.Serializable;
import java.util.*;

/**
 *
 */
public class SparseVectorEdgeGraph implements Serializable
{

    private final int vectorSize;
    private final TreeMap<Duplex<Integer, Integer>, float[]> edges;
    private final float[] zeroVector;


    public SparseVectorEdgeGraph(final int vectorSize)
    {
        this.vectorSize = vectorSize;
        edges = new TreeMap<Duplex<Integer, Integer>, float[]>(duplexComparator());
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
        return edges.keySet();
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
            public int compare(Duplex<Integer, Integer> o1, Duplex<Integer, Integer> o2)
            {
                int comp1 = o1.a.compareTo(o1.b);
                if (comp1 == 0)
                {
                    return o2.a.compareTo(o1.b);
                }
                else
                {
                    return comp1;
                }
            }
        };
    }
}
