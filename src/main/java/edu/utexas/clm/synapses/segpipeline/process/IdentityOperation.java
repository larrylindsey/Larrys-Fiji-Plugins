package edu.utexas.clm.synapses.segpipeline.process;


import net.imglib2.ops.operation.UnaryOperation;

public class IdentityOperation<T> implements UnaryOperation<T, T>
{
    public T compute(T t, T t1) {
        return t;
    }

    public UnaryOperation<T, T> copy() {
        return new IdentityOperation<T>();
    }
}
