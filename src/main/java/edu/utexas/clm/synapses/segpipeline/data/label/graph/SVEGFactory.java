package edu.utexas.clm.synapses.segpipeline.data.label.graph;

import edu.utexas.clm.synapses.segpipeline.data.label.SerialSparseLabels;
import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 */
public class SVEGFactory implements Serializable
{

    // For now, graphs are assumed to be non-directional for computational simplicity

    private final ArrayList<SparseLabelEdgeFeature> edgeFeatures;
    private final ArrayList<SparseLabelNodeFeature> nodeFeatures;
    private final SerialSparseLabels labels;
    private SparseVectorEdgeGraph sveg = null;

    public SVEGFactory(SerialSparseLabels labels)
    {
        edgeFeatures = new ArrayList<SparseLabelEdgeFeature>();
        nodeFeatures = new ArrayList<SparseLabelNodeFeature>();
        this.labels = labels;
    }

    public void addFeature(final SparseLabelEdgeFeature slef)
    {
        edgeFeatures.add(slef);
    }

    public void addFeature(final SparseLabelNodeFeature slnf)
    {
        nodeFeatures.add(slnf);
    }

    /**
     * Make a SparseVectorEdgeGraph. This function is NOT threadsafe.
     * @return the created graph.
     */
    public SparseVectorEdgeGraph makeSVEG()
    {
        int eDim = 0; // edge feature cardinality
        int nDim = 0; // node feature cardinality
        int i = 0;
        final int[] offsets = new int[edgeFeatures.size()];
        final HashMap<Integer, int[]> nodeFeatures;

        for (SparseLabelEdgeFeature feature : edgeFeatures)
        {
            offsets[i++] = eDim;
            eDim += feature.numDimensions();
        }



        sveg = new SparseVectorEdgeGraph(eDim);
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
        for (SparseLabelEdgeFeature feature : edgeFeatures)
        {
            final int offset = offsets[i++];

            if (feature.enabled())
            {
                final SerialSparseLabels currentLabels = new SerialSparseLabels(labels);

                for (final SparseLabel sl0 : labels)
                {
                    final Iterable<SparseLabel> acceptedLabels =
                            feature.accept(this, sl0, currentLabels);
                    for (final SparseLabel sl1 : acceptedLabels)
                    {
                        feature.extractFeature(this, sl0, sl1, offset);
                    }
                    currentLabels.remove(sl0);
                }
            }
        }

        return sveg;
    }

    public float[] getVector(final SparseLabel sl0, final SparseLabel sl1)
    {
        return sveg.getOrCreateEdgeValues(sl0.getValue(), sl1.getValue());
    }

    public SerialSparseLabels getLabels()
    {
        return labels;
    }
}
