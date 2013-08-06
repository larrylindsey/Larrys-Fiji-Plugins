package edu.utexas.clm.synapses.segpipeline.data.graph;

import edu.utexas.clm.synapses.segpipeline.data.graph.feature.GroundTruthFeature;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.SparseLabelEdgeFeature;
import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class GroundTruthSVEGFactory extends SVEGFactory
{



    public GroundTruthSVEGFactory(final SerialSparseLabels labels,
                                  final SerialSparseLabels positiveAnnotations,
                                  final SerialSparseLabels negativeAnnotations)
    {
        super(labels, new GroundTruthFeature(positiveAnnotations, negativeAnnotations));
    }
}
