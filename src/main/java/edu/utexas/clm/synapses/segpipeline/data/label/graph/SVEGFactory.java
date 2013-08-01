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

    private final ArrayList<SparseLabelEdgeFeature> edgeFeatures;
    private final SerialSparseLabels labels;
    private SparseVectorEdgeGraph sveg = null;

    public SVEGFactory(final SerialSparseLabels labels, final SparseLabelEdgeFeature... features)
    {
        edgeFeatures = new ArrayList<SparseLabelEdgeFeature>();
        this.labels = labels;

        for (final SparseLabelEdgeFeature feature : features)
        {
            addFeature(feature);
        }
    }

    public void addFeature(final SparseLabelEdgeFeature slef)
    {
        edgeFeatures.add(slef);
    }

    private void addNodeFeatureToCollection(final SparseLabelNodeFeature slnf,
                                           final Collection<SparseLabelNodeFeature> collection)
    {
        for (final SparseLabelNodeFeature nf : collection)
        {
            if (nf.equals(slnf))
            {
                return;
            }
        }

        collection.add(slnf);
    }

    private void setNodeOffsetForEdgeFeature(final SparseLabelEdgeFeature edgeFeature,
                                             final Collection<SparseLabelNodeFeature> nodeFeatures)
    {
        final SparseLabelNodeFeature edgesNodeFeature = edgeFeature.nodeFeature();
        for (SparseLabelNodeFeature nodeFeature : nodeFeatures)
        {
            if (nodeFeature.equals(edgesNodeFeature))
            {
                edgeFeature.setNodeOffset(nodeFeature.getOffset());
            }
        }
    }

    private void cacheNodeFeatures(final Iterable<SparseLabelEdgeFeature> edgeFeatures)
    {
        final ArrayList<SparseLabelNodeFeature> nodeFeatures =
                new ArrayList<SparseLabelNodeFeature>();
        int nDim = 0;

        // Populate a list of unique node features from those returned by each edge feature.
        for (final SparseLabelEdgeFeature slef : edgeFeatures)
        {
            addNodeFeatureToCollection(slef.nodeFeature(), nodeFeatures);
        }

        // Set the offset for each node feature, with respect to the feature array in SparseLabel
        for (final SparseLabelNodeFeature slnf : nodeFeatures)
        {
            slnf.setOffset(nDim);
            nDim += slnf.numDimensions();
        }

        /*
        Its possible to have two different NodeFeatures such that they are dot-equals, but are
        not the same object. This could happen, for instance, when two edge feature instantiate a
        node feature from the same class, with the same parameters. Presumably, the edge feature
        will want to read the results from the feature array. Here, we make sure that it gets the
        correct offset.
         */
        for (final SparseLabelEdgeFeature slef : edgeFeatures)
        {
            setNodeOffsetForEdgeFeature(slef, nodeFeatures);
        }

        for (final SparseLabel sl : labels)
        {
            sl.setFeatureSize(nDim);

            for (final SparseLabelNodeFeature slnf : nodeFeatures)
            {
                if (slnf.enabled())
                {
                    slnf.extractFeature(sl);
                }

            }
        }
    }

    /**
     * Make a SparseVectorEdgeGraph. This function is NOT threadsafe.
     * @return the created graph.
     */
    public SparseVectorEdgeGraph makeSVEG()
    {
        int eDim = 0; // edge feature cardinality
        int i = 0;
        final int[] offsets = new int[edgeFeatures.size()];

        cacheNodeFeatures(edgeFeatures);

        for (SparseLabelEdgeFeature feature : edgeFeatures)
        {
            offsets[i++] = eDim;
            eDim += feature.numDimensions();
        }

        sveg = new SparseVectorEdgeGraph(eDim);
        i = 0; // reset feature index to 0;

        /*
        For each enabled feature
            For each label sl0 in all Labels
                Pre-filter accepted labels from currentLabels into acceptedLabels
                For each label sl1 in acceptedLabels with value greater than sl0
                    Measure the feature between sl0 and sl1, apply it to sveg.

         */
        for (SparseLabelEdgeFeature feature : edgeFeatures)
        {
            final int offset = offsets[i++];

            if (feature.enabled())
            {
                for (final SparseLabel sl0 : labels)
                {
                    final Iterable<SparseLabel> acceptedLabels =
                            feature.accept(this, sl0);
                    for (final SparseLabel sl1 : acceptedLabels)
                    {
                        if (sl1.getValue() > sl0.getValue())
                        {
                            feature.extractFeature(this, sl0, sl1, offset);
                        }
                    }
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
