package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.Collection;

/**
 *
 */
public class DenseGroundTruthEdgeFeature extends SparseLabelEdgeFeature
{
    private final Collection<SparseLabel> annotations;

    public DenseGroundTruthEdgeFeature(final Collection<SparseLabel> annotations)
    {
        this.annotations = annotations;
    }


    @Override
    public int numDimensions()
    {
        return 1;
    }

    public SparseLabelNodeFeature nodeFeature()
    {
        return new DenseGroundTruthNodeFeature(annotations);
    }

    @Override
    public void extractFeature(SVEGFactory factory, SparseLabel sl0, SparseLabel sl1, int offset)
    {
        float feat0 = sl0.getFeature()[nodeOffset], feat1 = sl1.getFeature()[nodeOffset];
        if (feat0 > 0 && feat1 > 0)
        {
            factory.getVector(sl0, sl1)[offset] = feat0 == feat1 ? 0f : 1f;
        }

    }

    @Override
    public Iterable<SparseLabel> accept(SVEGFactory factory, SparseLabel sl)
    {
        return acceptAllNeighbors(factory, sl);
    }

    @Override
    public String name()
    {
        return "Dense Ground Truth";
    }
}
