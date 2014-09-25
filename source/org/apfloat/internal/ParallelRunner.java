package org.apfloat.internal;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;

/**
 * Class for running Runnable objects in parallel using
 * multiple threads.<p>
 *
 * The general paradigm for executing parallel tasks is:
 * <pre>
 * ParallelRunner parallelRunner = new ParallelRunner(numberOfProcessors);
 * parallelRunner.lock(key);
 * try
 * {
 *     parallelRunner.runParallel(parallelRunnable1);
 *     parallelRunner.runParallel(parallelRunnable2);
 *     parallelRunner.runParallel(parallelRunnable3);
 * }
 * finally
 * {
 *     parallelRunner.unlock();
 * }
 * </pre>
 *
 * The <code>key</code> is any object that is used to determine distinct
 * groups for the synchronization. For example, the shared memory lock object
 * from {@link ApfloatContext#getSharedMemoryLock()} can be used as the key
 * to limit memory consumption when the shared memory treshold is exceeded.<p>
 *
 * To avoid any blocking due to synchronization, a <code>ParallelRunner</code>
 * can also be used without calling the {@link #lock(Object)} and {@link #unlock()}
 * methods.
 *
 * @since 1.1
 * @version 1.7.0
 * @author Mikko Tommila
 */

public class ParallelRunner
{
    private static class ParallelRunnableTask
        implements Runnable
    {
        public ParallelRunnableTask(int numberOfProcessors)
        {
            this.numberOfProcessors = numberOfProcessors;
            this.position = new AtomicInteger();
            this.parallelRunnable = new AtomicReference<ParallelRunnable>();
        }

        // Add the given number of threads to the pool and if the task is running, also kick of additional threads to do the work
        public synchronized void add(int numberOfProcessors)
        {
            // Mark the increased number of available threads
            this.numberOfProcessors += numberOfProcessors;

            // If the task is running, then dispatch more threads
            if (this.parallelRunnable.get() != null)
            {
                submit(numberOfProcessors);
            }

            // Note that this method returns immediately, first, to get out of the synchronization of this object quickly,
            // and second, to allow the calling lock() method to also get out of the synchronization and begin waiting
        }

        public void run(ParallelRunnable parallelRunnable)
            throws ApfloatRuntimeException
        {
            // Initialize and start other parallel threads, if any - synchronized
            start(parallelRunnable);

            // Run this thread as one of the parallel worker threads - NOT synchronized, allows add() from other threads concurrently
            run();

            // Wait for all of the other parallel threads to finish also - synchronized
            join();
        }

        // Do the actual work in a parallel task using a small batch at a time
        public void run()
        {
            ParallelRunnable parallelRunnable = this.parallelRunnable.get();
            int maxPosition = parallelRunnable.getLength();
            while (this.position.get() < maxPosition)
            {
                int batchSize = Math.max(MINIMUM_BATCH_SIZE, parallelRunnable.getPreferredBatchSize());
                int startValue = this.position.getAndAdd(batchSize);
                int length = Math.min(batchSize, maxPosition - startValue);
                if (length > 0)
                {
                    Runnable runnable = parallelRunnable.getRunnable(startValue, length);
                    runnable.run();
                }
            }
        }

        // Initialize and start parallel threads
        private synchronized void start(ParallelRunnable parallelRunnable)
        {
            // Mark that a task is now running
            this.parallelRunnable.set(parallelRunnable);

            // Reset position of parallel runnables
            this.position.set(0);

            // Start parallel threads, if any
            submit(this.numberOfProcessors - 1);
        }

        // Submit parallel tasks to the executor service
        private void submit(int numberOfProcessors)
        {
            assert (Thread.holdsLock(this));

            // Submit tasks to the executor service
            if (numberOfProcessors > 0)
            {
                if (this.futures == null)
                {
                    this.futures = new ArrayList<Future<?>>();
                }

                ApfloatContext ctx = ApfloatContext.getContext();
                ExecutorService executorService = ctx.getExecutorService();
                for (int i = 0; i < numberOfProcessors; i++)
                {
                    // Dispatch a task to a separate thread to execute the run() method of this object, to participate in the actual work
                    // The parallelRunnable and position are atomic, so that the other threads can see them even though this thread has not yet exited synchronization
                    Future<?> future = executorService.submit(this);
                    this.futures.add(future);
                }
            }
        }

        private synchronized void join()
            throws ApfloatRuntimeException
        {
            // Wait for the parallel execution threads to complete, if any
            if (this.futures != null)
            {
                try
                {
                    for (Future<?> future : this.futures)
                    {
                        future.get();
                    }
                }
                catch (InterruptedException ie)
                {
                    throw new ApfloatInternalException("Waiting for dispatched threads to complete was interrupted", ie);
                }
                catch (ExecutionException ee)
                {
                    throw new ApfloatInternalException("Thread execution failed", ee);
                }

                // Reset the futures since they are now completed
                this.futures = null;
            }

            // Mark that the task is no longer running
            this.parallelRunnable.set(null);
        }

        private int numberOfProcessors;
        private AtomicReference<ParallelRunnable> parallelRunnable;
        private AtomicInteger position;
        private List<Future<?>> futures;
    }

    /**
     * Create an instance of a parallel runner.
     *
     * @param numberOfProcessors The number of parallel threads to use.
     */

    public ParallelRunner(int numberOfProcessors)
    {
        assert (numberOfProcessors > 0);
        this.numberOfProcessors = numberOfProcessors;
    }

    /**
     * Start the synchronization for the given lock key.
     * The key must be released using the {@link #unlock()} method,
     * in the <code>finally</code> block of the immediately following
     * <code>try</code> block, just like for concurrency locks.
     *
     * @param key The lock key for synchronization.
     */

    public void lock(Object key)
    {
        lock(key, this.numberOfProcessors);
        this.key = key;
    }

    /**
     * Finish the synchronization.
     * The key must be first locked using the {@link #lock(Object)} method.
     */

    public void unlock()
    {
        unlock(this.key);
    }

    /**
     * Run Runnable objects in parallel.
     * The whole length of the ParallelRunnable is split to multiple, small strides.
     * The strides are run in parallel using multiple threads. The number of threads
     * is the total number of threads contributed by the concurrent calls to the
     * {@link #lock(Object)} method with the same <code>key</code>. The Runnables
     * for processing the strides are run using the ExecutorService retrieved from
     * {@link ApfloatContext#getExecutorService()}.
     *
     * @param parallelRunnable The ParallelRunnable containing the Runnable objects to be run.
     */

    public void runParallel(ParallelRunnable parallelRunnable)
        throws ApfloatRuntimeException
    {
        if (this.key != null)
        {
            // Run inside locked block with key, possibly with contribution from other threads locking with the same key
            runParallel(this.key, parallelRunnable);
        }
        else
        {
            // Run without locking, stand-alone
            new ParallelRunnableTask(this.numberOfProcessors).run(parallelRunnable);
        }
    }

    private static void lock(Object key, int numberOfProcessors)
    {
        assert (numberOfProcessors > 0);
        synchronized (ParallelRunner.tasks)
        {
            boolean locked = false;
            ParallelRunnableTask addedTask = null;
            while (!locked)
            {
                ParallelRunnableTask task = ParallelRunner.tasks.get(key);
                if (task == null)
                {
                    // Task with the given key is not locked yet, so lock it
                    task = new ParallelRunnableTask(numberOfProcessors);
                    ParallelRunner.tasks.put(key, task);
                    locked = true;
                }
                else
                {
                    // Task with the given key is already locked, so give the available threads for its use
                    if (task != addedTask)
                    {
                        // But only do it once per task
                        // Note that wait() may spuriously wake up the thread at any time
                        // Also note that if more than two threads are competing for acquiring the lock then the task may actually change between calls
                        task.add(numberOfProcessors);
                        addedTask = task;
                    }

                    // Wait until the currently locked task is done, then retry (or it may spuriously wake up at any time)
                    try
                    {
                        ParallelRunner.tasks.wait();
                    }
                    catch (InterruptedException ie)
                    {
                        throw new ApfloatInternalException("Waiting for lock notification was interrupted", ie);
                    }
                }
            }
        }
    }

    private static void unlock(Object key)
    {
        synchronized (ParallelRunner.tasks)
        {
            // Unregister the task with the given lock
            ParallelRunnableTask task = ParallelRunner.tasks.remove(key);
            assert (task != null);

            // Wake up any other threads waiting for the lock to be released
            ParallelRunner.tasks.notifyAll();
        }
    }

    private static void runParallel(Object key, ParallelRunnable parallelRunnable)
        throws ApfloatRuntimeException
    {
        ParallelRunnableTask task;
        synchronized (ParallelRunner.tasks)
        {
            // Get the task with the given lock
            task = ParallelRunner.tasks.get(key);
            assert (task != null);
        }

        // Run using parallel threads, if any, until complete
        task.run(parallelRunnable);
    }

    private int numberOfProcessors;
    private Object key;

    private static final int MINIMUM_BATCH_SIZE = 16;

    private static Map<Object, ParallelRunnableTask> tasks = new IdentityHashMap<Object, ParallelRunnableTask>();
}
