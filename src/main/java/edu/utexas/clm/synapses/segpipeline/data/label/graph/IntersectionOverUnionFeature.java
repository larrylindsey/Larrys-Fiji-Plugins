package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class IntersectionOverUnionFeature extends AbstractSetOperationFeature
{

    public void extractFeature(SparseLabel sl0, SparseLabel sl1, float[] vector, int offset)
    {
        float areaIntersect = sl0.intersection(sl1).area();
        if (areaIntersect > 0)
        {
            vector[offset] = areaIntersect / ((float)sl0.union(sl1).area());
        }

    }
}
