package edu.utexas.clm.synapses.segpipeline.data.graph.feature;

import edu.utexas.clm.synapses.segpipeline.data.graph.SVEGFactory;
import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;
import ij.IJ;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class GroundTruthFeature extends SparseLabelEdgeFeature
{
    public static final float VALUE_POS = 0f;
    public static final float VALUE_NEG = 1f;

    private final SerialSparseLabels positiveAnnotations, negativeAnnotations;

    public GroundTruthFeature(final SerialSparseLabels positiveAnnotations,
                              final SerialSparseLabels negativeAnnotations)
    {
        this.positiveAnnotations = positiveAnnotations;
        this.negativeAnnotations = negativeAnnotations;
    }

    @Override
    public int numDimensions()
    {
        return 1;
    }

    private boolean coOverlap(final SparseLabel sl0, final SparseLabel sl1,
                              final SerialSparseLabels annotations)
    {
        for (final SparseLabel annotation : annotations.getLabels(sl0.getIndex()))
        {
            if (sl0.intersect(annotation))
            {
                /*
                sl0 and sl1 may have different indices, so we check for another annotation with
                the same value as the one that overlaps sl0, but with the same index as sl1.

                otherAnnotation may be assigned null, but SparseLabel.intersect(null) returns false.
                */
                final SparseLabel otherAnnotation =
                        annotations.getLabelByValue(annotation.getValue(), sl1.getIndex());
                if (sl1.intersect(otherAnnotation))
                {
                    return true;
                }
            }
        }


        return false;
    }

    @Override
    public void extractFeature(SVEGFactory factory,
                               SparseLabel sl0, SparseLabel sl1, int offset)
    {
        if (coOverlap(sl0, sl1, positiveAnnotations))
        {
            factory.getVector(sl0, sl1)[offset] = VALUE_POS;
            IJ.log("" + sl0.getValue() + " " + sl1.getValue() + " " + VALUE_POS);
            return;
        }

        if (coOverlap(sl0, sl1, negativeAnnotations))
        {
            factory.getVector(sl0, sl1)[offset] = VALUE_NEG;
            IJ.log("" + sl0.getValue() + " " + sl1.getValue() + " " + VALUE_NEG);
        }

//        if (sl0.getFeature()[nodeOffset] > 0 &&
//                sl0.getFeature()[nodeOffset] == sl1.getFeature()[nodeOffset])
//        {
//            IJ.log("Negative: " + sl0.getValue() + " " + sl1.getValue());
//            factory.getVector(sl0,sl1)[offset] = 1;
//        }
//        else if (sl0.getFeature()[nodeOffset + 1] > 0 &&
//                sl0.getFeature()[nodeOffset + 1] == sl1.getFeature()[nodeOffset + 1])
//        {
//            IJ.log("Positive: " + sl0.getValue() + " " + sl1.getValue());
//            factory.getVector(sl0,sl1)[offset] = 0;
//        }
//        else
//        {
//            IJ.log("Got unknown assignment");
//        }
    }

    @Override
    public Iterable<SparseLabel> accept(SVEGFactory factory, SparseLabel sl)
    {
        return acceptAllNeighbors(factory, sl);
    }

    @Override
    public String name()
    {
        return "Ground Truth Class";
    }

//    public SparseLabelNodeFeature nodeFeature()
//    {
//        return new GroundTruthNodeFeature(positiveAnnotations, negativeAnnotations);
//    }
}
