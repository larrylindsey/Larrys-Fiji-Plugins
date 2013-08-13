package edu.utexas.clm.synapses.segpipeline.data.label.filter;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class SizeGrelFilter implements LabelFilter
{
    private final int minSize;

    public SizeGrelFilter(final int minSize)
    {
        this.minSize = minSize;
    }

    public boolean filter(SparseLabel sl)
    {
        return sl.area() >= minSize;
    }
}
