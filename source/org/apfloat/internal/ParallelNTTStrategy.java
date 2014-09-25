package org.apfloat.internal;

import org.apfloat.spi.NTTStrategy;

/**
 * Number Theoretic Transform that can be run using multiple threads in parallel.
 *
 * @version 1.5.1
 * @author Mikko Tommila
 */

public interface ParallelNTTStrategy
    extends NTTStrategy
{
    /**
     * Set the parallel runner to be used when executing the transform.
     *
     * @param parallelRunner The parallel runner.
     */

    public void setParallelRunner(ParallelRunner parallelRunner);
}
