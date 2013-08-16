package edu.utexas.clm.synapses.segpipeline.data.graph;

import edu.utexas.clm.archipelago.data.Duplex;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.NullEdgeFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.SparseLabelEdgeFeature;
import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.io.Serializable;
import java.util.*;

/**
 *
 */
public class SparseVectorEdgeGraph implements Serializable
{

    public final static float NIL_VALUE = -1f;
    private final int vectorSize;
    private final Map<Duplex<Integer, Integer>, float[]> edges;
    private final float[] zeroVector;
    private final Collection<SparseLabelEdgeFeature> edgeFeatures;
    private final SerialSparseLabels labels;


    public SparseVectorEdgeGraph(final int vectorSize)
    {
        this(vectorSize, null, null);
    }

    public SparseVectorEdgeGraph(final int vectorSize,
                                 final Collection<SparseLabelEdgeFeature> edgeFeatures,
                                 final SerialSparseLabels labels)
    {
        this.vectorSize = vectorSize;
        edges = new HashMap<Duplex<Integer, Integer>, float[]>();
        zeroVector = new float[this.vectorSize];
        this.edgeFeatures = edgeFeatures;
        this.labels = labels;
    }


//    public void setEdgeValue(final int from, final int to, final int d, final float value)
//    {
//        getOrCreateEdgeValues(from, to)[d] = value;
//    }

    public SerialSparseLabels getLabels()
    {
        return labels;
    }

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

            for (int i = 0; i < vectorSize; ++i)
            {
                value[i] = NIL_VALUE;
            }
            edges.put(key, value);
        }

        return value;
    }

    public boolean containsKey(final Duplex<Integer, Integer> key)
    {
        return edges.containsKey(key);
    }

    /**
     * Returns a collection of the features used to create this graph, as in from an SVEGFactory,
     * or a NullEdgeFeature of size n if no such collection was provided to the constructor,
     * where n is the cardinality of the edges of this graph.
     * @return ditto.
     */
    public Collection<? extends SparseLabelEdgeFeature> getEdgeFeatures()
    {
        return edgeFeatures == null ? Collections.singleton(new NullEdgeFeature(vectorSize)) :
                edgeFeatures;
    }

    public SparseVectorEdgeGraph mapEdges(final EdgeMap map)
    {
        if (map.acceptSize(vectorSize))
        {
            final SparseVectorEdgeGraph sveg = new SparseVectorEdgeGraph(map.size());

            for (final Duplex<Integer, Integer> edgeKey : edges.keySet())
            {
                map.map(getEdgeValues(edgeKey), sveg.getOrCreateEdgeValues(edgeKey), edgeKey);
            }

            return sveg;
        }
        else
        {
            throw new RuntimeException("Map would not accept a graph of size " + vectorSize);
        }
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

    public TreeSet<SparseLabel> equivalentLabels(final int value, final EdgeMap map)
    {
        final List<Duplex<Integer, Integer>> edgeList;
        final int minEq;
        final TreeSet<SparseLabel> eqLabels;
        final TreeSet<Integer> eqVals;

        if (map.size() != 1)
        {
            throw new IllegalArgumentException("Map must have size of 1");
        }

        if (!map.acceptSize(vectorSize))
        {
            throw new IllegalArgumentException("Map must accept a graph with vector size " +
                    vectorSize);
        }

        // Sorted edge list
        edgeList = new ArrayList<Duplex<Integer, Integer>>(edges.keySet());
        Collections.sort(edgeList, duplexComparator());

        minEq = minimumEquivalentLabel(value, edgeList);
        eqVals = equivalentValues(minEq, edgeList);

        eqLabels = new TreeSet<SparseLabel>();

        for (int val : eqVals)
        {
            eqLabels.addAll(labels.getLabelsByValue(val));
        }

        return eqLabels;
    }

    private TreeSet<Integer> equivalentValues(int value, List<Duplex<Integer,Integer>> edgeList)
    {
        final int off = edgeList.get(0).a;
        final boolean[] mark = new boolean[edgeList.get(edgeList.size() - 1).b - off];
        final TreeSet<Integer> q = new TreeSet<Integer>(), ints = new TreeSet<Integer>();

        for (int i = 0; i < mark.length; ++i)
        {
            mark[i] = false;
        }

        q.add(value);
        mark[value - off] = true;

        while (!q.isEmpty())
        {
            int v = q.pollFirst();
            int w;
            ints.add(v);

            for (int i = 0; i < edgeList.size() && edgeList.get(i).a <= v; ++i)
            {
                if ((w = edgeList.get(i).a) == v && !mark[w - off])
                {
                    q.add(w);
                    mark[w - off] = true;
                }
            }
        }

        return ints;
    }

    private HashMap<Integer, TreeSet<Integer>> buildEdgeMap()
    {
        final HashMap<Integer, TreeSet<Integer>> map =
                new HashMap<Integer, TreeSet<Integer>>();
        for (final Duplex<Integer, Integer> key : edges.keySet())
        {
            TreeSet<Integer> set = map.get(key.a);
            if (set == null)
            {
                set = new TreeSet<Integer>();
                map.put(key.a, set);
            }
            set.add(key.b);
        }
        return map;
    }

    private int minimumEquivalentLabel(final int value,
                                       final List<Duplex<Integer, Integer>> edgeList)
    {
        int min = Integer.MAX_VALUE;
        int nextMin = value;
        while (nextMin < min)
        {
            min = nextMin;
            for (int i = 0; i < edgeList.size() && edgeList.get(i).a < min && min == nextMin; ++i)
            {
                if (edgeList.get(i).b == min)
                {
                    nextMin = edgeList.get(i).a;
                }
            }
        }

        return min;
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
