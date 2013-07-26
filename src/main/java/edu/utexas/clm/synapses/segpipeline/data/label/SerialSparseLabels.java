package edu.utexas.clm.synapses.segpipeline.data.label;


import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


/**
 *
 */
public class SerialSparseLabels extends AbstractCollection<SparseLabel> implements Serializable
{
    private class LabelIterator implements Iterator<SparseLabel>
    {
        private Iterator<SparseLabel> currIterator = null;
        private final Iterator<ArrayList<SparseLabel>> listIterator;

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


    private final HashMap<Integer, ArrayList<SparseLabel>> serialLabels;

    public SerialSparseLabels()
    {
        serialLabels = new HashMap<Integer, ArrayList<SparseLabel>>();
    }

    public SerialSparseLabels(final SerialSparseLabels labelsIn)
    {
        this();
        for (Integer key : labelsIn.serialLabels.keySet())
        {
            serialLabels.put(key, new ArrayList<SparseLabel>(serialLabels.get(key)));
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
        return serialLabels.get(index);
    }

    public SerialSparseLabels subSeries(int beg, int end)
    {
        final SerialSparseLabels sub = new SerialSparseLabels();
        for (int i = beg; i <= end; ++i)
        {
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
        for (final ArrayList<SparseLabel> list : serialLabels.values())
        {
            s += list.size();
        }
        return s;
    }

    private ArrayList<SparseLabel> getOrCreate(Integer key)
    {
        ArrayList<SparseLabel> list = serialLabels.get(key);
        if (list == null)
        {
            list = new ArrayList<SparseLabel>();
            serialLabels.put(key, list);
        }
        return list;
    }

    public boolean remove(final Object o)
    {
        return o instanceof SparseLabel && remove((SparseLabel)o);
    }

    public boolean remove(final SparseLabel sl)
    {
        final ArrayList<SparseLabel> list;
        return null != (list = serialLabels.get(sl.getIndex())) && list.remove(sl);
    }
}
