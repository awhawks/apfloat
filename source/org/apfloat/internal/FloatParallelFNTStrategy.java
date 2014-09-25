package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.ArrayAccess;

/**
 * Common methods to calculate Fast Number Theoretic Transforms
 * in parallel using multiple threads.<p>
 *
 * Note that to get any performance gain from running many
 * threads in parallel, the JVM must be executing native threads.
 * If the JVM is running in green threads mode, there is no
 * advantage of having multiple threads, as the JVM will in fact
 * execute just one thread and divide its time to multiple
 * simulated threads.
 *
 * @version 1.0.1
 * @author Mikko Tommila
 */

public abstract class FloatParallelFNTStrategy
    extends FloatTableFNTStrategy
{
    // Thread for calculating the row transforms in parallel
    private class TableFNTThread
        extends Thread
    {
        public TableFNTThread(int length, boolean isInverse, ArrayAccess arrayAccess, float[] wTable, int[] permutationTable)
        {
            this.length = length;               // Transform length
            this.isInverse = isInverse;
            this.arrayAccess = arrayAccess;
            this.wTable = wTable;
            this.permutationTable = permutationTable;
        }

        public void run()
        {
            int maxI = this.arrayAccess.getLength();

            for (int i = 0; i < maxI; i += this.length)
            {
                ArrayAccess arrayAccess = this.arrayAccess.subsequence(i, this.length);

                if (this.isInverse)
                {
                    inverseTableFNT(arrayAccess, this.wTable, this.permutationTable);
                }
                else
                {
                    tableFNT(arrayAccess, this.wTable, this.permutationTable);
                }
            }
        }

        private int length;
        private boolean isInverse;
        private ArrayAccess arrayAccess;
        private float[] wTable;
        private int[] permutationTable;
    }

    /**
     * Default constructor.
     */

    protected FloatParallelFNTStrategy()
    {
    }

    /**
     * Get an object on which to synchronize data storage access.
     * If <code>length</code> is more than the memory treshold, use shared memory,
     * otherwise create a dummy lock that will not cause any real synchronization.
     * The memory threshold is determined using
     * {@link ApfloatContext#getMemoryTreshold()}.
     *
     * @param length The length of data that will be accessed.
     *
     * @return The object on which the memory access should be synchronized.
     */

    public static Object getMemoryLock(long length)
    {
        Object lock;
        ApfloatContext ctx = ApfloatContext.getContext();

        if (length > ctx.getMemoryTreshold() / 4)
        {
            // Data size is big: synchronize on shared memory lock
            lock = ctx.getSharedMemoryLock();
        }
        else
        {
            // Data size is small: no synchronization - create a dummy lock object
            lock = new Object();
        }

        return lock;
    }

    /**
     * Multiply each matrix element <code>(i, j)</code> by <code>w<sup>i * j</sup> * scaleFactor</code>.
     * The matrix size is n<sub>1</sub> x n<sub>2</sub>.
     *
     * @param arrayAccess The memory array to multiply.
     * @param startRow Which row in the whole matrix the starting row in the <code>arrayAccess</code> is.
     * @param rows The number of rows in the <code>arrayAccess</code> to multiply.
     * @param columns The number of columns in the matrix (= n<sub>2</sub>).
     * @param w The n:th root of unity (where n = n<sub>1</sub> * n<sub>2</sub>).
     * @param scaleFactor An extra factor by which all elements are multiplied.
     */

    protected void multiplyElements(ArrayAccess arrayAccess, int startRow, int rows, int columns, float w, float scaleFactor)
        throws ApfloatRuntimeException
    {
        float[] data = arrayAccess.getFloatData();
        int position = arrayAccess.getOffset();
        float tmp = modPow(w, (float) startRow);

        for (int i = 0; i < rows; i++)
        {
            float tmp2 = scaleFactor;

            for (int j = 0; j < columns; j++, position++)
            {
                data[position] = modMultiply(data[position], tmp2);
                tmp2 = modMultiply(tmp2, tmp);
            }

            tmp = modMultiply(tmp, w);
        }
    }

    /**
     * Transform the rows of the data matrix.
     * If only one processor is available, it runs all transforms in the current
     * thread. If more than one processor are available, it dispatches the calculations
     * to multiple threads to parallelize the calculation. The number of processors is
     * determined using {@link ApfloatContext#getNumberOfProcessors()}.
     *
     * @param length Length of one transform (one row).
     * @param count Number of rows.
     * @param isInverse <code>true</code> if an inverse transform is performed, <code>false</code> if a forward transform is performed.
     * @param arrayAccess The memory array to split to rows and to transform.
     * @param wTable Table of powers of n:th root of unity (where n is the transform <code>length</code>).
     * @param permutationTable Table of permutation indexes, or <code>null</code> if no permutation should be done.
     */

    protected void transformRows(int length, int count, boolean isInverse, ArrayAccess arrayAccess, float[] wTable, int[] permutationTable)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        int numberOfProcessors = ctx.getNumberOfProcessors();

        if (numberOfProcessors == 1)
        {
            // Everything is done in this thread

            for (int i = 0; i < count; i++)
            {
                ArrayAccess subArrayAccess = arrayAccess.subsequence(i * length, length);

                if (isInverse)
                {
                    inverseTableFNT(subArrayAccess, wTable, permutationTable);
                }
                else
                {
                    tableFNT(subArrayAccess, wTable, permutationTable);
                }
            }
        }
        else
        {
            // Dispatch multiple threads in parallel to calculate row transforms
            runThreads(numberOfProcessors, length, count, isInverse, arrayAccess, wTable, permutationTable);
        }
    }

    // Run the row transforms in parallel using multiple threads
    private void runThreads(int numberOfThreads, int length, int count, boolean isInverse, ArrayAccess arrayAccess, float[] wTable, int[] permutationTable)
        throws ApfloatRuntimeException
    {
        Thread[] threads = new Thread[numberOfThreads];

        // Split the whole range of rows to the number of processors available and start threads
        for (int i = 0; i < numberOfThreads; i++)
        {
            int startIndex = count * i / numberOfThreads,
                endIndex   = count * (i + 1) / numberOfThreads;

            ArrayAccess subArrayAccess = arrayAccess.subsequence(startIndex * length, (endIndex - startIndex) * length);

            threads[i] = new TableFNTThread(length, isInverse, subArrayAccess, wTable, permutationTable);
            threads[i].start();
        }

        // Wait for all started threads to complete
        try
        {
            for (int i = 0; i < numberOfThreads; i++)
            {
                threads[i].join();
            }
        }
        catch (InterruptedException ie)
        {
            throw new ApfloatRuntimeException("Waiting for dispatched threads to complete was interrupted", ie);
        }
    }
}
