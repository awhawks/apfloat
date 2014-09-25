package org.apfloat.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;

/**
 * Class for running Runnable objects in parallel using
 * multiple threads.
 *
 * @since 1.1
 * @version 1.2.1
 * @author Mikko Tommila
 */

public class ParallelRunner
{
    private ParallelRunner()
    {
    }

    /**
     * Run Runnable objects in parallel.
     * The whole length of the ParallelRunnable is split to multiple strides.
     * The number of strides is the number of processors from
     * {@link ApfloatContext#getNumberOfProcessors()}. The Runnable for processing
     * each stride is run in parallel using the ExecutorService retrieved from
     * {@link ApfloatContext#getExecutorService()}.
     *
     * @param parallelRunnable The ParallelRunnable containing the Runnable objects to be run.
     */

    public static void runParallel(ParallelRunnable parallelRunnable)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        int numberOfProcessors = ctx.getNumberOfProcessors();
        ExecutorService executorService = ctx.getExecutorService();
        Future[] futures = new Future[numberOfProcessors - 1];
        int length = parallelRunnable.getLength();

        // Split the whole length to the number of processors available and start threads (if any)
        for (int i = 0; i < numberOfProcessors; i++)
        {
            int startValue = (int) ((long) length * i / numberOfProcessors),
                nextValue = (int) ((long) length * (i + 1) / numberOfProcessors);

            Runnable runnable = parallelRunnable.getRunnable(startValue, nextValue - startValue);
            if (i < futures.length)
            {
                // Dispatch all but the last runnable to separate threads (may be zero if only one processor)
                futures[i] = executorService.submit(runnable);
            }
            else
            {
                // Run the last runnable in the current thread (it may be the only one)
                runnable.run();
            }
        }

        // Wait for all execution threads to complete (if any)
        try
        {
            for (Future future : futures)
            {
                future.get();
            }
        }
        catch (InterruptedException ie)
        {
            throw new ApfloatRuntimeException("Waiting for dispatched threads to complete was interrupted", ie);
        }
        catch (ExecutionException ee)
        {
            throw new ApfloatRuntimeException("Thread execution failed", ee);
        }
    }
}
