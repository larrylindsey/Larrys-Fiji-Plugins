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

    private final Collection<SparseLabel> positiveAnnotations, negativeAnnotations;

    public GroundTruthFeature(final Collection<SparseLabel> positiveAnnotations,
                              final Collection<SparseLabel> negativeAnnotations)
    {
        this.positiveAnnotations = positiveAnnotations;
        this.negativeAnnotations = negativeAnnotations;
    }

    @Override
    public int numDimensions()
    {
        return 1;
    }

    @Override
    public void extractFeature(SVEGFactory factory,
                               SparseLabel sl0, SparseLabel sl1, int offset)
    {
        for (final SparseLabel annotation : positiveAnnotations)
        {
            if (sl0.getIndex() == annotation.getIndex() &&
                    sl0.intersect(annotation))
            {
                if (sl1.intersect(annotation))
                {
                    factory.getVector(sl0, sl1)[offset] = VALUE_POS;
                    IJ.log("" + sl0.getValue() + " " + sl1.getValue() + " " + VALUE_POS);
                    return;
                }
            }
        }

        for (final SparseLabel annotation : negativeAnnotations)
        {
            if (sl0.getIndex() == annotation.getIndex() &&
                    sl0.intersect(annotation))
            {
                if (sl1.intersect(annotation))
                {
                    factory.getVector(sl0, sl1)[offset] = VALUE_NEG;
                    IJ.log("" + sl0.getValue() + " " + sl1.getValue() + " " + VALUE_NEG);
                    return;
                }
            }
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
        ArrayList<SparseLabel> list =
                new ArrayList<SparseLabel>(factory.getLabels().getOverlap(sl));

        for (final SparseLabel label : factory.getLabels().getLabels(sl.getIndex() + 1))
        {
            if (label.intersect(sl))
            {
                list.add(label);
            }
        }

        return list;
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
