package edu.utexas.clm.synapses.segpipeline.data.graph;

import edu.utexas.clm.archipelago.data.Duplex;

/**
 *
 */
public interface EdgeMap
{
    public void map(final float[] inVector,
                    final float[] outVector,
                    final Duplex<Integer, Integer> edgeKey);

    public int size();

    public boolean acceptSize(final int size);
}
