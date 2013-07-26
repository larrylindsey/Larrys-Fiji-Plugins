package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelOperation;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.OperationCallable;
import edu.utexas.clm.synapses.segpipeline.process.Multithreaded;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 */
public class InplaneOverlapHistogramFeature extends SparseLabelFeature implements Multithreaded
{
    private static final Comparator<SparseLabel> valueComparator = SparseLabel.valueComparator();

    private final ArrayList<TreeSet<SparseLabel>> overlapMap;
    private final int bins, binIncrement, restrictIndex;
    private final LabelOperation op;
    private final boolean indexRestricted;
    private int operatingIndex, valueOffset;
    private transient ExecutorService service;



    public InplaneOverlapHistogramFeature(final int restrictIndex, final LabelOperation op,
                                          final int bins, final int binIncrement)
    {
        this.op = op;
        this.bins = bins;
        this.binIncrement = binIncrement;
        this.restrictIndex = restrictIndex;
        indexRestricted = restrictIndex >= 0;
        operatingIndex = -1;
        valueOffset = -1;
        overlapMap = new ArrayList<TreeSet<SparseLabel>>();
        setNumProcessors(0);
    }

    @Override
    public int numDimensions()
    {
        return bins;
    }

    @Override
    public void extractFeature(final SparseLabel sl0, final SparseLabel sl1, final float[] vector,
                               final int offset)
    {
        // Zero out our section of the vector
        for (int i = 0; i < bins; ++i)
        {
            vector[i + offset] = 0;
        }

        if (!indexRestricted || restrictIndex == sl0.getIndex())
        {
            final ArrayList<SparseLabel> nbd0 =
                    new ArrayList<SparseLabel>(overlapMap.get(offsetValue(sl0)));
            nbd0.retainAll(overlapMap.get(offsetValue(sl1)));
            for (final SparseLabel sl : nbd0)
            {
                final int bin = Math.max(sl.area() / binIncrement, bins - 1);
                vector[offset + bin]++;
            }
        }
    }

    /**
     * Returns an Iterable over the SparseLabels in all that, once operated on by this feature's
     * SparseLabelOperator, have overlapping bounding boxes with the SparseLabel sl. This
     * SparseLabelFeature caches operated SparseLabels, and assumes that the SerialSparseLabels
     * passed in is static, and that the requested SparseLabels will be in order both in
     * value and plane-index. Failure to meet this assumption will result in very large increases
     * in computational overhead.
     *
     * @param sl the SparseLabel to test
     * @param all all SparseLabel nodes for a given graph
     * @return an ArrayList of accepted SparseLabels for the given SparseLabel sl
     */
    public Iterable<SparseLabel> accept(final SparseLabel sl, final SerialSparseLabels all)
    {
        if (indexRestricted && restrictIndex != sl.getIndex())
        {
            return new ArrayList<SparseLabel>();
        }
        else
        {
            final ArrayList<SparseLabel> planeList = all.getLabels(sl.getIndex());
            return getAcceptedLabels(planeList, sl);
        }
    }

    /**
     * Sets explicitly the ExecutorService used to build the overlap map.
     * @param service the ExecutorService to use for multithreading purposes.
     */
    public void setService(final ExecutorService service)
    {
        this.service = service;
    }

    /**
     * Sets the number of processors to use when multithreading. When set to 0 or less, np defaults
     * to the number of processors available to the JVM, as reported by
     * Runtime.availableProcessors()
     * @param np the number of processors to use when multithreading.
     */
    public void setNumProcessors(final int np)
    {
        service = Executors.newFixedThreadPool(np > 0 ?
                np : Runtime.getRuntime().availableProcessors());
    }


    private int offsetValue(final SparseLabel sl)
    {
        return sl.getValue() - valueOffset;
    }

    private ArrayList<SparseLabel> getAcceptedLabels(final ArrayList<SparseLabel> labels,
                                                     final SparseLabel sl)
    {
        if (operatingIndex != sl.getIndex())
        {
            buildOverlapMap(labels);
        }
        return new ArrayList<SparseLabel>(overlapMap.get(offsetValue(sl)));
    }

    /**
     * Creates a new ArrayList from labels such that ArrayList.get(i) returns either null or a
     * SparseLabel sl such that sl.getValue() is equal to i.
     * @param labels the List to make linear
     * @return a linearly indexed List.
     */
    private ArrayList<SparseLabel> makeLinearList(final List<SparseLabel> labels)
    {
        final int maxVal = offsetValue(labels.get(labels.size() - 1));
        final ArrayList<SparseLabel> linearLabels = new ArrayList<SparseLabel>(maxVal);

        for (int i = 0; i < maxVal + 1; ++i)
        {
            linearLabels.add(null);
        }

        for (final SparseLabel sl : labels)
        {
            linearLabels.set(offsetValue(sl), sl);
        }

        return linearLabels;
    }

    private ArrayList<SparseLabel> operateOverLabels(final List<SparseLabel> labels)
    {
        final ArrayList<Future<SparseLabel>> futures =
                new ArrayList<Future<SparseLabel>>(labels.size());
        final ArrayList<SparseLabel> operatedLabels = new ArrayList<SparseLabel>(labels.size());

        for (final SparseLabel sl : labels)
        {
            futures.add(service.submit(new OperationCallable(op, sl)));
        }

        try
        {
            for (final Future<SparseLabel> future : futures)
            {
                operatedLabels.add(future.get());
            }
        }
        catch (InterruptedException ie)
        {
            throw new RuntimeException(ie);
        }
        catch (ExecutionException ee)
        {
            throw new RuntimeException(ee);
        }

        return operatedLabels;
    }


    private void buildOverlapMap(final List<SparseLabel> labels)
    {
        // Assume labels is sorted

        // Operate over all labels. This should take a while.
        final ArrayList<SparseLabel> operatedLabels = operateOverLabels(labels);
        // Create a temp list.
        final ArrayList<SparseLabel> currentLabels = new ArrayList<SparseLabel>(operatedLabels);
        final ArrayList<SparseLabel> linearLabels;

        // Clear the map, then collect the garbage.
        overlapMap.clear();
        System.gc();
        valueOffset = labels.get(0).getValue();

        // Linearize the original list, for O(1) lookup.
        linearLabels = makeLinearList(labels);

        // Prepopulate the overlap map.
        for (int i = valueOffset; i < labels.get(labels.size() - 1).getValue(); ++i)
        {
            overlapMap.add(new TreeSet<SparseLabel>(valueComparator));
        }

        // For each dilated Label
        for (final SparseLabel sl0 : operatedLabels)
        {
            // and for each remaining dilated label in our temp list
            for (final SparseLabel sl1 : currentLabels)
            {
                // If sl0 and sl1 intersect
                if (sl0.intersect(sl1))
                {
                    // Put the original labels in the overlap map
                    int oVal0 = offsetValue(sl0), oVal1 = offsetValue(sl1);
                    overlapMap.get(oVal0).add(linearLabels.get(oVal1));
                    overlapMap.get(oVal1).add(linearLabels.get(oVal0));
                    // Note that linearLabels.get(i) may return null
                    // We should never encounter this, because all sl0, sl1 should yield
                    // offset values that correspond to actual SparseLabels in linearLabels.
                    // IE, an NPE associated with the overlapMap probably indicates a bug in
                    // makeLinearList.
                }
            }
            currentLabels.remove(sl0);
        }
    }
}
