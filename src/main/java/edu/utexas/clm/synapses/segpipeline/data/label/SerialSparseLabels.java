package edu.utexas.clm.synapses.segpipeline.data.label;


import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.synapses.segpipeline.data.label.filter.LabelFilter;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.LabelOperation;
import edu.utexas.clm.synapses.segpipeline.data.label.operations.OperationCallable;
import edu.utexas.clm.synapses.segpipeline.process.Multithreaded;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Callable;
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

    private class FindOverlapCallable implements Callable<HashMap<Integer, TreeSet<SparseLabel>>>
    {
        final SparseLabel sl0;
        final List<SparseLabel> opLabels;
        final TreeSet<SparseLabel> origLabels;
        final int begin;


        public FindOverlapCallable(final SparseLabel label, final TreeSet<SparseLabel> origLabels,
                                   final List<SparseLabel> opLabels, final int begin)
        {
            sl0 = label;
            this.opLabels = opLabels;
            this.origLabels = origLabels;
            this.begin = begin;
        }

        public HashMap<Integer, TreeSet<SparseLabel>> call() throws Exception
        {
            final HashMap<Integer, TreeSet<SparseLabel>> overlap =
                    new HashMap<Integer, TreeSet<SparseLabel>>();

            for (int i = begin; i < opLabels.size(); ++i)
            {
                final SparseLabel sl1 = opLabels.get(i);
                // If sl0 and sl1 intersect
                if (sl0.intersect(sl1))
                {
                    getOrCreate(sl0.getValue(), overlap).add(find(origLabels, sl1));
                    getOrCreate(sl1.getValue(), overlap).add(find(origLabels, sl0));
                }
            }

            return overlap;
        }
    }

    // serialLabels.get(i) gets all SparseLabels from section with index i.
    private final HashMap<Integer, TreeSet<SparseLabel>> serialLabels;
    // overlapMap.get(i) gets the map for all arealists in section i.
    // overlapMap.get(i).get(j) gets all SparseLabels that intersect the SparseLabel with value j.
    private final HashMap<Integer, HashMap<Integer, TreeSet<SparseLabel>>> overlapMap;

    private transient HashMap<Integer, Img<? extends RealType>> imageMap;

    private boolean isOverlapPopulated;
    private ExecutorService service;


    public SerialSparseLabels()
    {
        serialLabels = new HashMap<Integer, TreeSet<SparseLabel>>();
        overlapMap = new HashMap<Integer, HashMap<Integer, TreeSet<SparseLabel>>>();
        imageMap = null;
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

    public boolean add(final SparseLabel sl)
    {
        return getOrCreate(sl.getIndex()).add(sl);
    }

    public ArrayList<SparseLabel> getLabels(final int index)
    {
        if (serialLabels.containsKey(index))
        {
            return new ArrayList<SparseLabel>(serialLabels.get(index));
        }
        else
        {
            return new ArrayList<SparseLabel>(0);
        }
    }

    public ArrayList<SparseLabel> getLabelsByValue(final int value)
    {
        final SparseLabel comparison = new SparseLabel(value, 0, 0);
        final ArrayList<SparseLabel> valueLabels = new ArrayList<SparseLabel>();

        for (final TreeSet<SparseLabel> set : serialLabels.values())
        {
            final SparseLabel sl = set.floor(comparison);
            if (sl != null && sl.getValue() == value)
            {
                valueLabels.add(sl);
            }
        }

        return valueLabels;
    }

    public SparseLabel getFirstLabelByValue(final int value)
    {
        final SparseLabel comparison = new SparseLabel(value, 0, 0);

        for (final TreeSet<SparseLabel> set : serialLabels.values())
        {
            final SparseLabel sl = set.floor(comparison);
            if (sl != null && sl.getValue() == value)
            {
                return sl;
            }
        }

        return null;
    }

    public SparseLabel getLabelByValue(final int value, final int index)
    {
        final SparseLabel comparison = new SparseLabel(value, 0, 0);
        final TreeSet<SparseLabel> indexSet = serialLabels.get(index);

        if (indexSet != null)
        {
            final SparseLabel sl = indexSet.floor(comparison);
            return sl != null && sl.getValue() == value ? sl : null;
        }
        else
        {
            return null;
        }
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

    private void mergeMaps(final HashMap<Integer, TreeSet<SparseLabel>> small,
                           final HashMap<Integer, TreeSet<SparseLabel>> large)
    {
        for (int key : small.keySet())
        {
            final TreeSet<SparseLabel> set = getOrCreate(key, large);
            set.addAll(small.get(key));
        }
    }

    public void buildOverlapMap(final LabelOperation op)
    {
        final ArrayList<Integer> keys = new ArrayList<Integer>(serialLabels.keySet());

        for (final int key : keys)
        {
            final TreeSet<SparseLabel> labels = serialLabels.get(key);
            //TODO check performance vs initial capacity
            final HashMap<Integer, TreeSet<SparseLabel>> overlap =
                    new HashMap<Integer, TreeSet<SparseLabel>>(labels.size() * 4 / 3);
            final ExecutorService local = localService();
            final ArrayList<Future<HashMap<Integer, TreeSet<SparseLabel>>>> futures =
                    new ArrayList<Future<HashMap<Integer, TreeSet<SparseLabel>>>>(labels.size());

            //overlapMap.remove(key);
            overlapMap.put(key, overlap);

            // Operate over all labels. This should take a while.
            final ArrayList<SparseLabel> operatedLabels = operateOverLabels(labels, op);

            for (int i = 0; i < operatedLabels.size(); ++i)
            {
                futures.add(local.submit(new FindOverlapCallable(operatedLabels.get(i), labels,
                        operatedLabels, i + 1)));
            }


            try
            {
                //TODO: Check if this is really faster than using a synchronized map
                for (final Future<HashMap<Integer, TreeSet<SparseLabel>>> future : futures)
                {
                    // get the overlap map for an individual sparselabel
                    final HashMap<Integer, TreeSet<SparseLabel>> map = future.get();

                    mergeMaps(map, overlap);
                }
            }
            catch (ExecutionException ee)
            {
                throw new RuntimeException(ee);
            }
            catch (InterruptedException ie)
            {
                throw new RuntimeException(ie);
            }

            /*// Create a temp list.
            final ArrayList<SparseLabel> currentLabels = new ArrayList<SparseLabel>(operatedLabels);

            // For each dilated Label
            for (final SparseLabel sl0 : operatedLabels)
            {
                currentLabels.remove(sl0);
                // and for each remaining dilated label in our temp list
                for (final SparseLabel sl1 : currentLabels)
                {
                    // If sl0 and sl1 intersect
                    if (sl0.intersect(sl1))
                    {
                        getOrCreate(sl0.getValue(), overlap).add(find(labels, sl1));
                        getOrCreate(sl1.getValue(), overlap).add(find(labels, sl0));
                    }
                }
            }*/
        }
    }

    /**
     * Replaces the label with the same index and value as newLabel with newLabel, if it exists.
     * @param newLabel the label to put into this SerialSparseLabels
     * @return true if there was a label to replace, false otherwise.
     */
    public boolean replace(final SparseLabel newLabel)
    {
        final TreeSet<SparseLabel> indexSet = serialLabels.get(newLabel.getIndex());

        if (indexSet != null)
        {
            final SparseLabel sl = indexSet.floor(newLabel);

            indexSet.remove(sl);
            indexSet.add(newLabel);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean filterLabels(final LabelFilter filter)
    {
        final ArrayList<SparseLabel> removeLabels = new ArrayList<SparseLabel>();

        for (final SparseLabel sl : this)
        {
            if (!filter.filter(sl))
            {
                removeLabels.add(sl);
            }
        }

        this.removeAll(removeLabels);

        return !removeLabels.isEmpty();
    }

    public boolean operateInPlace(final LabelOperation op)
    {
        final ArrayList<Future<SparseLabel>> futures =
                new ArrayList<Future<SparseLabel>>(this.size());

        for (final SparseLabel sl : this)
        {
            futures.add(service.submit(new OperationCallable(op, sl)));
        }


        try
        {
            // First pass. Don't perform the replacement until everything finishes.
            for (final Future<SparseLabel> future : futures)
            {
                future.get();
            }

            // Now, we do the replacement
            for (final Future<SparseLabel> future : futures)
            {
                replace(future.get());
            }
            return true;
        }
        catch (ExecutionException ee)
        {
            IJ.error("" + ee);
            ee.printStackTrace();
            return false;
        }
        catch (InterruptedException ie)
        {
            IJ.error("" + ie);
            ie.printStackTrace();
            return false;
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


    public void associateImage(final Img<? extends RealType> img, final int index)
    {
        if (serialLabels.containsKey(index))
        {
            if (imageMap == null)
            {
                imageMap = new HashMap<Integer, Img<? extends RealType>>();
            }

            imageMap.put(index, img);
        }
        else
        {
            System.out.println("Printing keys:");
            for (Integer key : serialLabels.keySet())
            {
                System.out.println("Key " + key);
            }

            throw new IllegalArgumentException("Tried to associate image for index " + index +
                    " but there are no labels with which to associate.");
        }
    }

    public void associateImage(final ImagePlus imp, final int index)
    {
        Img<? extends RealType> img = ImagePlusAdapter.wrapReal(imp);
        associateImage(img, index);
    }

    public Img<? extends RealType> getImage(final SparseLabel sl)
    {
        return getImage(sl.getIndex());
    }

    public Img<? extends RealType> getImage(final int index)
    {
        return imageMap == null ? null : imageMap.get(index);
    }

    public void clearImageMap()
    {
        imageMap.clear();
        imageMap = null;
    }

    private ExecutorService localService()
    {
        if (Cluster.isClusterService(service))
        {
            return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        else
        {
            return service;
        }
    }

    public void setService(ExecutorService service)
    {
        this.service = service;
    }

    public void setNumProcessors(final int np)
    {
        service = Executors.newFixedThreadPool(np > 0 ?
                np : Runtime.getRuntime().availableProcessors());
    }


}
