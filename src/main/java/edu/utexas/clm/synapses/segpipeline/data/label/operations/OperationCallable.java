package edu.utexas.clm.synapses.segpipeline.data.label.operations;

import edu.utexas.clm.synapses.segpipeline.data.label.SparseLabel;

import java.util.concurrent.Callable;

/**
 *
 */
public class OperationCallable implements Callable<SparseLabel>
{
    private final LabelOperation operation;
    private final SparseLabel sl;

    public OperationCallable(final LabelOperation operation, SparseLabel sl)
    {
        this.operation = operation;
        this.sl = sl;
    }

    public SparseLabel call()
    {
        return operation.process(sl);
    }

}
