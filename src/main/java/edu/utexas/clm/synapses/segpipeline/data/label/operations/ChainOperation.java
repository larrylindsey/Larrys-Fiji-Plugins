package edu.utexas.clm.synapses.segpipeline.data.label.operations;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

/**
 *
 */
public class ChainOperation implements LabelOperation
{
    private final LabelOperation[] operations;

    public ChainOperation(final LabelOperation... operations)
    {
        this.operations = operations;
    }

    public SparseLabel process(final SparseLabel input)
    {
        SparseLabel outputLabel = input;
        for (LabelOperation op : operations)
        {
            outputLabel = op.process(outputLabel);
        }
        return outputLabel;
    }
}
