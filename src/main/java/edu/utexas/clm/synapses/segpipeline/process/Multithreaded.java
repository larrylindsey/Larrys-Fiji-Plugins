package edu.utexas.clm.synapses.segpipeline.process;

import java.util.concurrent.ExecutorService;

/**
 *
 */
public interface Multithreaded
{
    /**
     * Sets explicitly the ExecutorService used for multithread purposes.
     * @param service the ExecutorService to use for multithreading purposes.
     */
    public void setService(final ExecutorService service);

    /**
     * Sets the number of processors to use when multithreading. When set to 0 or less, np defaults
     * to the number of processors available to the system.
     * @param np the number of processors to use when multithreading.
     */
    public void setNumProcessors(final int np);
}

