package edu.utexas.clm.synapses.segpipeline.data.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.io.Serializable;


/**
 *
 */
public abstract class SparseLabelEdgeFeature implements Serializable
{

    private boolean enabled = true;
    protected int nodeOffset = -1;

    /**
     * Returns the number of values extracted for this feature. For example, this would be one for
     * an area-overlap feature, or, say, 10 for a histogram feature with 10 bins.
     * @return the number of values extracted for this feature.
     */
    public abstract int numDimensions();

    /**
     * Optionally calculate edge value between two nodes, placing it in the array returned by
     * factory.getVector(sl0, sl1), beginning at the specified offset.
     * @param factory the SVEGfactory to populate
     * @param sl0 the first SparseLabel node
     * @param sl1 the second SparseLabel node
     * @param offset the offset into the feature vector at which this SparseLabelEdgeFeature should
     *               begin recording feature values.
     */
    public abstract void extractFeature(final SVEGFactory factory, final SparseLabel sl0,
                                        final SparseLabel sl1, final int offset);


    /**
     * Return a collection of SparseLabels that could share an edge with the SparseLabel sl. This
     * is intended to be a first-pass rejection step, to reduce computational overhead of calling
     * extractFeature. Many Features may simply return all.
     * @param factory the SVEGfactory to populate
     * @param sl the SparseLabel to test
     * @return a Collection containing all SparseLabels that could have a relationship with sl.
     */
    public abstract Iterable<SparseLabel> accept(final SVEGFactory factory, final SparseLabel sl);

    /**
     * Returns a SparseLabelNodeFeature that will precompute node-centric data required by
     * this edge feature. This implementation returns a null feature.
     * @return a SparseLabelNodeFeature that will precompute node-centric data required by
     * this edge feature
     */
    public SparseLabelNodeFeature nodeFeature()
    {
        return NullNodeFeature.getFeature();
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

    public void setNodeOffset(final int off)
    {
        nodeOffset = off;
    }

}
