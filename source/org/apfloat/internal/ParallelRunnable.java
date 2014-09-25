package org.apfloat.internal;

import org.apfloat.spi.Util;

/**
 * Abstract class for producing Runnable objects to be run in parallel.
 *
 * @since 1.1
 * @version 1.7.0
 * @author Mikko Tommila
 */

public abstract class ParallelRunnable
{
    /**
     * Subclass constructor.
     */

    protected ParallelRunnable()
    {
        // Set the batch size to be some balanced value with respect to the batch size and the number of batches
        this.preferredBatchSize = Util.sqrt4down(getLength());
    }

    /**
     * Get the length of the whole set to be run in parallel.
     *
     * @return The length.
     */

    public abstract int getLength();

    /**
     * Get the Runnable object for the specified short stride.
     *
     * @param startValue The starting value for the stride.
     * @param length The length of the stride.
     *
     * @return The Runnable object for the specified stride.
     */

    public Runnable getRunnable(int startValue, int length)
    {
        return getRunnable((long) startValue, (long) length);
    }

    /**
     * Get the Runnable object for the specified long stride.
     * The ParallelRunner never calls this method; it must
     * be manually called for oversize ParallelRunnables.
     *
     * @param startValue The starting value for the stride.
     * @param length The length of the stride.
     *
     * @return The Runnable object for the specified stride.
     */

    public Runnable getRunnable(long startValue, long length)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Get the preferred batch size.
     *
     * @return The preferred batch size.
     *
     * @since 1.7.0
     */

    public int getPreferredBatchSize()
    {
        return this.preferredBatchSize;
    }

    private int preferredBatchSize;
}
