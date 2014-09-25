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
 * @version 1.5
 * @author Mikko Tommila
 */

public abstract class DoubleParallelFNTStrategy
    extends DoubleTableFNTStrategy
{
    // Runnable for calculating the row transforms in parallel
    private class TableFNTRunnable
        implements Runnable
    {
        public TableFNTRunnable(int length, boolean isInverse, ArrayAccess arrayAccess, double[] wTable, int[] permutationTable)
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
        private double[] wTable;
        private int[] permutationTable;
    }

    // Runnable for multiplying elements in the matrix
    private class MultiplyRunnable
        implements Runnable
    {
        public MultiplyRunnable(ArrayAccess arrayAccess, int startRow, int rows, int columns, double w, double scaleFactor)
        {
            this.arrayAccess = arrayAccess;
            this.startRow = startRow;
            this.rows = rows;
            this.columns = columns;
            this.w = w;
            this.scaleFactor = scaleFactor;
        }

        public void run()
        {
            double[] data = this.arrayAccess.getDoubleData();
            int position = this.arrayAccess.getOffset();
            double tmp = modPow(this.w, (double) this.startRow);

            for (int i = 0; i < this.rows; i++)
            {
                double tmp2 = this.scaleFactor;

                for (int j = 0; j < this.columns; j++, position++)
                {
                    data[position] = modMultiply(data[position], tmp2);
                    tmp2 = modMultiply(tmp2, tmp);
                }

                tmp = modMultiply(tmp, this.w);
            }
        }

        private ArrayAccess arrayAccess;
        private int startRow;
        private int rows;
        private int columns;
        private double w;
        private double scaleFactor;
    }

    /**
     * Default constructor.
     */

    protected DoubleParallelFNTStrategy()
    {
    }

    /**
     * Get an object on which to synchronize data storage access.
     * If <code>length</code> is more than the shared memory treshold, then use shared memory,
     * otherwise create a dummy key that will not cause any real synchronization.
     * The shared memory threshold is determined using
     * {@link ApfloatContext#getSharedMemoryTreshold()}.
     *
     * @param length The length of data that will be accessed.
     *
     * @return The object on which the memory access should be synchronized.
     */

    protected static Object getSharedMemoryLockKey(long length)
    {
        Object key;
        ApfloatContext ctx = ApfloatContext.getContext();

        if (length > ctx.getSharedMemoryTreshold() / 8)
        {
            // Data size is big: synchronize on shared memory lock
            key = ctx.getSharedMemoryLock();
        }
        else
        {
            // Data size is small: no synchronization - create a dummy lock key object
            key = new Object();
        }

        return key;
    }

    /**
     * Start the synchronization for the given lock key.
     * The key must be released using the {@link #unlock(Object)} method,
     * in the <code>finally</code> block of the immediately following
     * <code>try</code> block, just like for concurrency locks.
     * The number of processors is taken from {@link ApfloatContext#getNumberOfProcessors()}.
     *
     * @param key The lock key for synchronization.
     */

    protected static void lock(Object key)
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        int numberOfProcessors = ctx.getNumberOfProcessors();
        ParallelRunner.lock(key, numberOfProcessors);
    }

    /**
     * Finish the synchronization for the given lock key.
     * The key must be first locked using the {@link #lock(Object)} method.
     *
     * @param key The lock key for synchronization.
     */

    protected static void unlock(Object key)
    {
        ParallelRunner.unlock(key);
    }

    /**
     * Multiply each matrix element <code>(i, j)</code> by <code>w<sup>i * j</sup> * scaleFactor</code>.
     * The matrix size is n<sub>1</sub> x n<sub>2</sub>.
     *
     * @param key The lock key for synchronization.
     * @param arrayAccess The memory array to multiply.
     * @param startRow Which row in the whole matrix the starting row in the <code>arrayAccess</code> is.
     * @param rows The number of rows in the <code>arrayAccess</code> to multiply.
     * @param columns The number of columns in the matrix (= n<sub>2</sub>).
     * @param w The n:th root of unity (where n = n<sub>1</sub> * n<sub>2</sub>).
     * @param scaleFactor An extra factor by which all elements are multiplied.
     */

    protected void multiplyElements(Object key, final ArrayAccess arrayAccess, final int startRow, final int rows, final int columns, final double w, final double scaleFactor)
        throws ApfloatRuntimeException
    {
        ParallelRunnable parallelRunnable = new ParallelRunnable()
        {
            public int getLength()
            {
                return rows;
            }

            public Runnable getRunnable(int strideStartRow, int strideRows)
            {
                ArrayAccess subArrayAccess = arrayAccess.subsequence(strideStartRow * columns, strideRows * columns);
                return new MultiplyRunnable(subArrayAccess, startRow + strideStartRow, strideRows, columns, w, scaleFactor);
            }
        };

        ParallelRunner.runParallel(key, parallelRunnable);
    }

    /**
     * Transform the rows of the data matrix.
     * If only one processor is available, it runs all transforms in the current
     * thread. If more than one processor are available, it dispatches the calculations
     * to multiple threads to parallelize the calculation. The number of processors is
     * determined using {@link ApfloatContext#getNumberOfProcessors()}.
     *
     * @param key The lock key for synchronization.
     * @param length Length of one transform (one row).
     * @param count Number of rows.
     * @param isInverse <code>true</code> if an inverse transform is performed, <code>false</code> if a forward transform is performed.
     * @param arrayAccess The memory array to split to rows and to transform.
     * @param wTable Table of powers of n:th root of unity (where n is the transform <code>length</code>).
     * @param permutationTable Table of permutation indexes, or <code>null</code> if no permutation should be done.
     */

    protected void transformRows(Object key, final int length, final int count, final boolean isInverse, final ArrayAccess arrayAccess, final double[] wTable, final int[] permutationTable)
        throws ApfloatRuntimeException
    {
        ParallelRunnable parallelRunnable = new ParallelRunnable()
        {
            public int getLength()
            {
                return count;
            }

            public Runnable getRunnable(int startIndex, int strideCount)
            {
                ArrayAccess subArrayAccess = arrayAccess.subsequence(startIndex * length, strideCount * length);
                return new TableFNTRunnable(length, isInverse, subArrayAccess, wTable, permutationTable);
            }
        };

        ParallelRunner.runParallel(key, parallelRunnable);
    }
}
