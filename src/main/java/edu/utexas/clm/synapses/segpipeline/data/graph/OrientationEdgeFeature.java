package edu.utexas.clm.synapses.segpipeline.data.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class OrientationEdgeFeature extends AbstractInplaneEdgeFeature
{
    public static final float halfCircle = (float)Math.PI;

    public OrientationEdgeFeature()
    {
        this(-1);
    }

    public OrientationEdgeFeature(int restrictIndex)
    {
        super(restrictIndex);
    }

    @Override
    public int numDimensions()
    {
        return 4;
    }

    /**
     * Extracts the following features between two neighboring labels in the same plane:
     * - Difference between the angle bisecting each labels orientation angles and the angle of
     *   their offset
     * - The difference between their orientation angles
     * - The similarity of their eccentricities, as max(e0 / e1, e1 / e0), where e0 is the
     *   eccentricity of the label sl0 and e1 of sl1
     * - The maximum of ecc0 and ecc1
     */
    public void extractFeature(SVEGFactory factory, SparseLabel sl0, SparseLabel sl1, int offset)
    {
        if (!indexRestricted || restrictIndex == sl0.getIndex())
        {
            final float[] vector = factory.getVector(sl0, sl1);
            final float[] nodeFeat0 = sl0.getFeature();
            final float[] nodeFeat1 = sl1.getFeature();

            final float cx0 = nodeFeat0[nodeOffset], cy0 = nodeFeat0[nodeOffset + 1];
            final float cx1 = nodeFeat1[nodeOffset], cy1 = nodeFeat1[nodeOffset + 1];
            final float angle0 = halfArc(nodeFeat0[nodeOffset + 2]);
            final float angle1 = halfArc(nodeFeat1[nodeOffset + 2]);
            final float ecc0 = nodeFeat0[nodeOffset + 3];
            final float ecc1 = nodeFeat1[nodeOffset + 3];

            final float ctrAngle = (float)Math.atan2(cy1 - cy0, cx1 - cx0);
            final float angleDiff = angleDifference(angle0, angle1);
            final float eccSim = eccentricitySimilarity(ecc0, ecc1);

            vector[offset] = angleDifference(ctrAngle, bisectAngle(angle0, angle1));
            vector[offset + 1] = angleDiff;
            vector[offset + 2] = eccSim;
            vector[offset + 3] = Math.max(ecc0, ecc1);
        }
    }

    public static float bisectAngle(final float angle0, final float angle1)
    {
        final double x0 = Math.cos(angle0), y0 = Math.sin(angle0);
        final double x1 = Math.cos(angle1), y1 = Math.sin(angle1);

        return (float)Math.atan2(y0 + y1, x0 + x1);
    }

    public static float halfArc(final float angle)
    {
        float halfArc = angle;

        while (halfArc < 0)
        {
            halfArc += halfCircle;
        }

        while (halfArc >= halfCircle)
        {
            halfArc -= halfCircle;
        }

        return halfArc;
    }

    public static float quarterArc(final float angle)
    {
        float halfArc = angle;

        while (halfArc < 0)
        {
            halfArc += halfCircle / 2;
        }

        while (halfArc >= halfCircle / 2)
        {
            halfArc -= halfCircle / 2;
        }

        return halfArc;
    }

    public static float angleDifference(final float angle0, final float angle1)
    {
        /*
        We want the angle of the minimal arc between points on the unit circle at these two angles
        If angle1 is to the right of angle0, then angle1 - angle0 might be close to PI, otoh,
        angle0 - angle1 should be small, as we expect. We take the minimum of the subtraction of
        either direction to protect against this case, and because we want the "absolute value" here
        anyway.
        */
        return Math.min(quarterArc(angle1 - angle0),
                quarterArc(angle0 - angle1));
    }

    public static float eccentricitySimilarity(final float ecc0, final float ecc1)
    {
        return Math.min(ecc0 / ecc1, ecc1 / ecc0);
    }

    public SparseLabelNodeFeature nodeFeature()
    {
        return CentroidOrientationEccentricityFeature.getFeature();
    }

}
