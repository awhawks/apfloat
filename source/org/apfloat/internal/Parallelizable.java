package org.apfloat.internal;

/**
 * Any task that can use a {@link ParallelRunner} to execute operations in parallel.
 *
 * @since 1.7.0
 * @version 1.7.0
 * @author Mikko Tommila
 */

public interface Parallelizable
{
    /**
     * Set the parallel runner to be used when executing the task.
     *
     * @param parallelRunner The parallel runner.
     */

    public void setParallelRunner(ParallelRunner parallelRunner);
}
