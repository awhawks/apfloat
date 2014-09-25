package org.apfloat.internal;

/**
 * Interface for producing Runnable objects to be run in parallel.
 *
 * @since 1.1
 * @version 1.1
 * @author Mikko Tommila
 */

public interface ParallelRunnable
{
    /**
     * Get the length of the whole set to be run in parallel.
     *
     * @return The length.
     */

    public int getLength();

    /**
     * Get the Runnable object for the specified stride.
     *
     * @param startValue The starting value for the stride.
     * @param length The length of the stride.
     *
     * @return The Runnable object for the specified stride.
     */

    public Runnable getRunnable(int startValue, int length);
}
