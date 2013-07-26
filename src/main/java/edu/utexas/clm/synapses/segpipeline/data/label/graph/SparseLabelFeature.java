package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.io.Serializable;


/**
 *
 */
public abstract class SparseLabelFeature implements Serializable
{

    private boolean enabled = true;

    /**
     * Returns the number of values extracted for this feature. For example, this would be one for
     * an area-overlap feature, or, say, 10 for a histogram feature with 10 bins.
     * @return the number of values extracted for this feature.
     */
    public abstract int numDimensions();

    /**
     * Calculate the feature value between two SparseLabels, placing the result in an array
     * of floating-point values, beginning at the offset value.
     * @param sl0 the first SparseLabel node
     * @param sl1 the second SparseLabel node
     * @param vector the feature vector
     * @param offset the offset into the feature vector at which this SparseLabelFeature should
     *               begin recording feature values.
     */
    public abstract void extractFeature(final SparseLabel sl0, final SparseLabel sl1,
                               final float[] vector, final int offset);


    /**
     * Return a collection of SparseLabels that could share an edge with the SparseLabel sl. This
     * is intended to be a first-pass rejection step, to reduce computational overhead of calling
     * extractFeature. Many Features may simply return all.
     * @param sl the SparseLabel to test
     * @param all all SparseLabel nodes for a given graph
     * @return a Collection containing all SparseLabels that could have a relationship with sl.
     */
    public abstract Iterable<SparseLabel> accept(final SparseLabel sl, final SerialSparseLabels all);

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
