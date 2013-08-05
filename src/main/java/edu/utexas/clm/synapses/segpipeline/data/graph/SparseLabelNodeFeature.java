package edu.utexas.clm.synapses.segpipeline.data.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public abstract class SparseLabelNodeFeature
{
    private boolean enabled = true;
    protected int offset = 0;

    /**
     * Returns the number of values extracted for this feature.
     * @return the number of values extracted for this feature.
     */
    public abstract int numDimensions();

    /**
     * Calculate a feature value for a SparseLabel, placing the result in the float array returned
     * by sl.getFeature(), beginning at the offset for this feature.
     * @param sl the SparseLabel node

     */
    public abstract void extractFeature(final SparseLabel sl);

    public void setOffset(final int off)
    {
        offset = off;
    }

    public int getOffset()
    {
        return offset;
    }

    /**
     * True if this feature is enabled, false if not.
     * @return true if this feature is enabled, false if not.
     */
    public boolean enabled()
    {
        return enabled;
    }

    public void setEnabled(final boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Returns true if o is equal to this SparseLabelNodeFeature
     * Equality is determined as follows
     *     o must have the same class as this SparseLabelNodeFeature
     *     o must have the same number of dimensions as this SparseLabelNodeFeature
     * In this case, the feature computation is assumed to be the same for both this and o.
     *
     * @param o the Object to test
     * @return true if o is equal to this SparseLabelNodeFeature
     */
    public boolean equals(Object o)
    {
        return o.getClass().equals(getClass()) &&
                ((SparseLabelNodeFeature)o).numDimensions() == this.numDimensions();

    }
}
