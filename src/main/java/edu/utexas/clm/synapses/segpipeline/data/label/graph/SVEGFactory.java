package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class SVEGFactory implements Serializable
{

    // For now, graphs are assumed to be non-directional for computational simplicity

    public final ArrayList<SparseLabelFeature> features;

    public SVEGFactory()
    {
        features = new ArrayList<SparseLabelFeature>();
    }

    public void addFeature(final SparseLabelFeature slf)
    {
        features.add(slf);
    }

    public SparseVectorEdgeGraph makeSVEG(final SerialSparseLabels labels)
    {
        int fdim = 0; // vector feature cardinality
        int i = 0;
        final int[] offsets = new int[features.size()];
        final SparseVectorEdgeGraph sveg;

        for (SparseLabelFeature feature : features)
        {
            offsets[i++] = fdim;
            fdim += feature.numDimensions();

        }

        sveg = new SparseVectorEdgeGraph(fdim);
        i = 0; // reset feature index to 0;

        /*
        For each feature
            Copy labels into currentLabels
            For each label sl0 in all Labels
                Pre-filter accepted labels from currentLabels into acceptedLabels
                For each label sl1 in acceptedLabels
                    Measure the feature between sl0 and sl1, apply it to sveg.
            Remove sl0 from currentLabels
         */
        for (SparseLabelFeature feature : features)
        {
            final int offset = offsets[i++];

            if (feature.enabled())
            {
                final SerialSparseLabels currentLabels = new SerialSparseLabels(labels);

                for (final SparseLabel sl0 : labels)
                {
                    final Iterable<SparseLabel> acceptedLabels =
                            feature.accept(sl0, currentLabels);
                    for (final SparseLabel sl1 : acceptedLabels)
                    {
                        final float[] featureVector =
                                sveg.getOrCreateEdgeValues(sl0.getValue(), sl1.getValue());
                        feature.extractFeature(sl0, sl1, featureVector, offset);
                    }
                    currentLabels.remove(sl0);
                }
            }
        }

        return sveg;
    }


}
