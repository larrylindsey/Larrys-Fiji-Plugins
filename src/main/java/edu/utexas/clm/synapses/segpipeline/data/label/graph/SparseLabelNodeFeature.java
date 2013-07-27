package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public abstract class SparseLabelNodeFeature
{
    private boolean enabled = true;

    /**
     * Returns the number of values extracted for this feature.
     * @return the number of values extracted for this feature.
     */
    public abstract int numDimensions();

    /**
     * Calculate a feature value for a SparseLabel, placing the result in an array
     * of floating-point values, beginning at the offset value.
     * @param sl the SparseLabel node
     * @param vector the feature vector
     * @param offset the offset into the feature vector at which this SparseLabelNodeFeature should
     *               begin recording feature values.
     */
    public abstract void extractFeature(final SparseLabel sl, final float[] vector,
                                        final int offset);

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
}
