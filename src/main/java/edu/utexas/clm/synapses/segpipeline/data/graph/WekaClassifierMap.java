package edu.utexas.clm.synapses.segpipeline.data.graph;

import edu.utexas.clm.archipelago.data.Duplex;
import edu.utexas.clm.synapses.segpipeline.data.graph.feature.SparseLabelEdgeFeature;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Set;

/**
 *
 */
public class WekaClassifierMap implements EdgeMap
{
    private final Classifier classifier;
    private Instances dataInfo;

    /**
     * Creates a classifier map, given an already-trained classifier.
     * @param classifier the Weka classifier to use on the edges.
     */
    public WekaClassifierMap(final Classifier classifier)
    {
        this.classifier = classifier;
        dataInfo = null;
    }

    public void trainClassifier(final SparseVectorEdgeGraph featGraph,
                                final SparseVectorEdgeGraph gtGraph)
    {
        trainClassifier(featGraph, gtGraph, 2);
    }

    /**
     * Train this classifier map.
     * @param featGraph a graph with feature edges, corresponding to the same features as used in
     *                  a test graph.
     * @param gtGraph a graph containing all edges for which there is ground truth. There should be
     *                only one edge value, corresponding to the edge's class.
     * @param nClasses the number of classes
     */
    public void trainClassifier(final SparseVectorEdgeGraph featGraph,
                                final SparseVectorEdgeGraph gtGraph,
                                final int nClasses)
    {
        final Set<Duplex<Integer, Integer>> keys = gtGraph.getEdges();
        final ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        final ArrayList<String> names = new ArrayList<String>();
        final Instances trainData;
        final int size = featGraph.getVectorSize();

        // Enumerate classes
        for (int i = 0; i < nClasses; ++i)
        {
            names.add("Class " + i);
        }

        // Add attributes
        for (SparseLabelEdgeFeature feat : featGraph.getEdgeFeatures())
        {
            for (int i = 0; i < feat.numDimensions(); ++i)
            {
                attributes.add(new Attribute(feat.name() + " " + i));
            }
        }
        attributes.add(new Attribute("class", names));

        trainData = new Instances("Edge Value", attributes, keys.size());
        dataInfo = new Instances("Edge Value", attributes, 1);

        trainData.setClassIndex(trainData.numAttributes() - 1);
        dataInfo.setClassIndex(dataInfo.numAttributes() - 1);

        for (final Duplex<Integer, Integer> key : keys)
        {
            final double[] dVector = new double[size + 1];
            final float[] fVector = featGraph.getEdgeValues(key);

            for (int i = 0; i < size; ++i)
            {
                dVector[i] = fVector[i];
            }
            dVector[size] = gtGraph.getEdgeValues(key)[0];

            trainData.add(new DenseInstance(1.0, dVector));
        }

        try
        {
            classifier.buildClassifier(trainData);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Maps the input vector to a class index, via the classifyInstance method of Classifier.
     * {@inheritDoc}
     */
    public void map(final float[] inVector, final float[] outVector,
                    final Duplex<Integer, Integer> edgeKey)
    {
        final double[] vector = new double[inVector.length + 1];
        final DenseInstance ins;

        for (int i = 0; i < inVector.length; ++i)
        {
            vector[i] = inVector[i];
        }
        vector[vector.length - 1] = 0;

        ins = new DenseInstance(1.0, vector);
        ins.setDataset(dataInfo);

        try
        {
            outVector[0] = (float)classifier.classifyInstance(ins);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public int size()
    {
        return 1;
    }

    public boolean acceptSize(int size)
    {
        return true;
    }
}
