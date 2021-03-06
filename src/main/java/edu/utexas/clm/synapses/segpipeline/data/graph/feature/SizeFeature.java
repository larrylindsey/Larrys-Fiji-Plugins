package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.ArrayList;

/**
 *
 */
public class SizeFeature extends SparseLabelEdgeFeature
{
    @Override
    public int numDimensions() {
        return 2;
    }

    @Override
    public void extractFeature(SVEGFactory factory, SparseLabel sl0, SparseLabel sl1, int offset)
    {
        float area0 = sl0.area(), area1 = sl1.area();

        factory.getVector(sl0, sl1)[offset] = Math.max(area0, area1);
        factory.getVector(sl0, sl1)[offset + 1] = area0 > area1 ? area0 / area1 : area1 / area0;
    }

    @Override
    public Iterable<SparseLabel> accept(SVEGFactory factory, SparseLabel sl)
    {
        return acceptAllNeighbors(factory, sl);
    }

    @Override
    public String name()
    {
        return "Size Feature";
    }
}
