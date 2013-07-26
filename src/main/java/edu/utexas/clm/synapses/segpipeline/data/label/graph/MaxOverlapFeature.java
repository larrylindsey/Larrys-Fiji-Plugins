package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class MaxOverlapFeature extends AbstractSetOperationFeature
{
    public void extractFeature(SparseLabel sl0, SparseLabel sl1, float[] vector, int offset)
    {
        float areaIntersect = sl0.intersection(sl1).area();
        if (areaIntersect > 0)
        {
            float area0 = sl0.area();
            float area1 = sl1.area();
            vector[offset] = Math.max(areaIntersect / area0, areaIntersect / area1);
        }
    }
}
