package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.spi.NTTStrategy;

/**
 * Convolution using three Number Theoretic Transforms
 * and the CRT to get the final result, using multiple threads in parallel.<p>
 *
 * This algorithm is parallelized so that all operations are done in parallel
 * using multiple threads, if the number of processors is greater than one
 * in {@link ApfloatContext#getNumberOfProcessors()}.<p>
 *
 * If the data block to be transformed is larger than the shared memory treshold setting
 * in the current ApfloatContext, this class will synchronize all data access on
 * the shared memory lock retrieved from {@link ApfloatContext#getSharedMemoryLock()}.<p>
 *
 * All access to this class must be externally synchronized.
 *
 * @since 1.7.0
 * @version 1.7.0
 * @author Mikko Tommila
 */

public class ParallelThreeNTTConvolutionStrategy
    extends ThreeNTTConvolutionStrategy
{
    /**
     * Creates a new convoluter that uses the specified
     * transform for transforming the data.
     *
     * @param radix The radix to be used.
     * @param nttStrategy The transform to be used.
     */

    public ParallelThreeNTTConvolutionStrategy(int radix, NTTStrategy nttStrategy)
    {
        super(radix, nttStrategy);
    }

    protected void lock(long length)
    {
        assert(!this.locked);

        if (super.nttStrategy instanceof Parallelizable &&
            super.carryCRTStrategy instanceof Parallelizable &&
            super.stepStrategy instanceof Parallelizable)
        {
            ApfloatContext ctx = ApfloatContext.getContext();
            int numberOfProcessors = ctx.getNumberOfProcessors();
            this.parallelRunner = new ParallelRunner(numberOfProcessors);

            ((Parallelizable) super.nttStrategy).setParallelRunner(this.parallelRunner);
            ((Parallelizable) super.carryCRTStrategy).setParallelRunner(this.parallelRunner);
            ((Parallelizable) super.stepStrategy).setParallelRunner(this.parallelRunner);

            if (length > ctx.getSharedMemoryTreshold() / ctx.getBuilderFactory().getElementSize())
            {
                // Data size is big: synchronize on shared memory lock
                Object key = ctx.getSharedMemoryLock();

                this.parallelRunner.lock(key);

                this.locked = true;
            }
        }
    }

    protected void unlock()
    {
        if (this.locked)
        {
            this.parallelRunner.unlock();
        }
    }

    private ParallelRunner parallelRunner;
    private boolean locked;
}
