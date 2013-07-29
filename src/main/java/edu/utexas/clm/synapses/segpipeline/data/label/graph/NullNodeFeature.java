package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class NullNodeFeature extends SparseLabelNodeFeature
{

    private static final NullNodeFeature feature = new NullNodeFeature();

    public int numDimensions() {
        return 0;
    }

    public void extractFeature(SparseLabel sl){ }

    public boolean enabled()
    {
        return false;
    }

    public static NullNodeFeature getFeature()
    {
        return feature;
    }

}
