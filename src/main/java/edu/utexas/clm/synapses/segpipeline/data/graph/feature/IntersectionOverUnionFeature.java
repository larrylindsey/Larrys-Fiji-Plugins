package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class IntersectionOverUnionFeature extends AbstractSetOperationFeature
{

    public void extractFeature(final SVEGFactory factory,
                               final SparseLabel sl0, final SparseLabel sl1, final int offset)
    {
        float areaIntersect = sl0.intersection(sl1).area();
        if (areaIntersect > 0)
        {
            factory.getVector(sl0, sl1)[offset] = areaIntersect / ((float)sl0.union(sl1).area());
        }

    }
}
