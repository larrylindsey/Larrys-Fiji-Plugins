package edu.utexas.clm.synapses.segpipeline.data.label;


import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelOperation;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.OperationCallable;
import edu.utexas.clm.synapses.segpipeline.process.Multithreaded;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 *
 */
public class SerialSparseLabels extends AbstractCollection<SparseLabel> implements Serializable,
        Multithreaded
{
    private class LabelIterator implements Iterator<SparseLabel>
    {
        private Iterator<SparseLabel> currIterator = null;
        private final Iterator<TreeSet<SparseLabel>> listIterator;

        public LabelIterator()
        {
            listIterator = serialLabels.values().iterator();
        }

        public boolean hasNext()
        {
            return listIterator.hasNext() ||
                    (currIterator != null && currIterator.hasNext());
        }

        public SparseLabel next()
        {
            if (currIterator == null || !currIterator.hasNext())
            {
                currIterator = listIterator.next().iterator();
            }
            return currIterator.next();
        }

        public void remove()
        {
            currIterator.remove();
        }
    }

    // serialLabels.get(i) gets all SparseLabels from section with index i.
    private final HashMap<Integer, TreeSet<SparseLabel>> serialLabels;
    // overlapMap.get(i) gets the map for all arealists in section i.
    // overlapMap.get(i).get(j) gets all SparseLabels that intersect the SparseLabel with value j.
    private final HashMap<Integer, HashMap<Integer, TreeSet<SparseLabel>>> overlapMap;

    private boolean isOverlapPopulated;
    private ExecutorService service;


    public SerialSparseLabels()
    {
        serialLabels = new HashMap<Integer, TreeSet<SparseLabel>>();
        overlapMap = new HashMap<Integer, HashMap<Integer, TreeSet<SparseLabel>>>();
        isOverlapPopulated = false;
        setNumProcessors(0);
    }

    public SerialSparseLabels(final SerialSparseLabels labelsIn)
    {
        this();
        for (Integer key : labelsIn.serialLabels.keySet())
        {
            serialLabels.put(key, new TreeSet<SparseLabel>(serialLabels.get(key)));
        }
    }

    public void putLabel(final SparseLabel sl)
    {
        getOrCreate(sl.getIndex()).add(sl);
    }

    public void putLabels(final Collection<SparseLabel> labels)
    {
        for (final SparseLabel sl : labels)
        {
            putLabel(sl);
        }
    }

    public ArrayList<SparseLabel> getLabels(final int index)
    {
        return new ArrayList<SparseLabel>(serialLabels.get(index));
    }

    public SerialSparseLabels subSeries(int beg, int end)
    {
        final SerialSparseLabels sub = new SerialSparseLabels();
        sub.isOverlapPopulated = isOverlapPopulated;

        for (int i = beg; i <= end; ++i)
        {
            if (isOverlapPopulated)
            {
                HashMap<Integer, TreeSet<SparseLabel>> overlap = overlapMap.get(i);
                if (overlap != null)
                {
                    sub.overlapMap.put(i, overlap);
                }
            }

            sub.serialLabels.put(i, serialLabels.get(i));

        }

        return sub;
    }

    /**
     * Creates and returns an Iterator that iterates over all of the SparseLabels at all of the
     * indices in this SerialSparseLabels
     * @return an Iterator that iterates over all of the SparseLabels in this SerialSparseLabels
     */
    public Iterator<SparseLabel> iterator()
    {
        return new LabelIterator();
    }

    @Override
    public int size()
    {
        int s = 0;
        for (final TreeSet<SparseLabel> set: serialLabels.values())
        {
            s += set.size();
        }
        return s;
    }

    private TreeSet<SparseLabel> getOrCreate(final Integer key)
    {
        return getOrCreate(key, serialLabels);
    }

    private TreeSet<SparseLabel> getOrCreate(final Integer key,
                                             final HashMap<Integer, TreeSet<SparseLabel>> table)
    {
        TreeSet<SparseLabel> set = table.get(key);
        if (set == null)
        {
            set = new TreeSet<SparseLabel>(SparseLabel.valueComparator());
            table.put(key, set);
        }
        return set;
    }

    public boolean remove(final Object o)
    {
        return o instanceof SparseLabel && remove((SparseLabel)o);
    }

    public boolean remove(final SparseLabel sl)
    {
        final TreeSet<SparseLabel> set;
        return null != (set = serialLabels.get(sl.getIndex())) && set.remove(sl);
    }

    private SparseLabel find(final TreeSet<SparseLabel> labels, final SparseLabel sl)
    {
        final SparseLabel floor = labels.floor(sl);
        if (floor == null || floor.getValue() != sl.getValue())
        {
            throw new RuntimeException("Could not find original label for " + sl.getValue());
        }
        return floor;
    }

    public void buildOverlapMap(final LabelOperation op)
    {
        for (int key : serialLabels.keySet())
        {
            final TreeSet<SparseLabel> labels = serialLabels.get(key);
            //TODO check performance vs initial capacity
            final HashMap<Integer, TreeSet<SparseLabel>> overlap =
                    new HashMap<Integer, TreeSet<SparseLabel>>(labels.size() * 4 / 3);

            overlapMap.remove(key);
            overlapMap.put(key, overlap);

            // Operate over all labels. This should take a while.
            final ArrayList<SparseLabel> operatedLabels = operateOverLabels(labels, op);

            // Create a temp list.
            final ArrayList<SparseLabel> currentLabels = new ArrayList<SparseLabel>(operatedLabels);

            // For each dilated Label
            for (final SparseLabel sl0 : operatedLabels)
            {
                // and for each remaining dilated label in our temp list
                for (final SparseLabel sl1 : currentLabels)
                {
                    // If sl0 and sl1 intersect
                    if (sl0.intersect(sl1))
                    {
                        getOrCreate(sl0.getValue()).add(find(labels, sl1));
                        getOrCreate(sl1.getValue()).add(find(labels, sl0));
                    }
                }
                currentLabels.remove(sl0);
            }
        }
    }

    private ArrayList<SparseLabel> operateOverLabels(final Collection<SparseLabel> labels,
                                                     final LabelOperation op)
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

    public ArrayList<SparseLabel> getOverlap(final SparseLabel sl)
    {
        final HashMap<Integer, TreeSet<SparseLabel>> overlap = overlapMap.get(sl.getIndex());
        final ArrayList<SparseLabel> overlapLabels = new ArrayList<SparseLabel>();

        if (overlap != null)
        {
            TreeSet<SparseLabel> labelSet = overlap.get(sl.getValue());
            if (labelSet != null)
            {
                overlapLabels.addAll(labelSet);
            }
        }

        return overlapLabels;
    }


    public void setService(ExecutorService service)
    {
        this.service = service;
    }

    public void setNumProcessors(int np)
    {
        service = Executors.newFixedThreadPool(np > 0 ?
                np : Runtime.getRuntime().availableProcessors());
    }


}
