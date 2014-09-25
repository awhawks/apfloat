package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.NTTStrategy;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;
import org.apfloat.spi.Util;
import static org.apfloat.internal.IntModConstants.*;

/**
 * Fast Number Theoretic Transform that uses a "two-pass"
 * algorithm to calculate a very long transform on data that
 * resides on a mass storage device. The storage medium should
 * preferably be a solid state disk for good performance;
 * on normal hard disks performance is usually inadequate.<p>
 *
 * The "two-pass" algorithm only needs to do two passes through
 * the data set. In comparison, a basic FFT algorithm of length 2<sup>n</sup>
 * needs to do n passes through the data set. Although the
 * algorithm is fairly optimal in terms of amount of data transferred
 * between the mass storage and main memory, the mass storage access is
 * not linear but done in small incontinuous pieces, so due to disk
 * seek times the performance can be quite lousy.<p>
 *
 * When the data to be transformed is considered to be an
 * n<sub>1</sub> x n<sub>2</sub> matrix of data, instead of a linear array,
 * the two passes go as follows:<p>
 *
 * <ol>
 *   <li>Do n<sub>2</sub> transforms of length n<sub>1</sub> by transforming the matrix columns.
 *       Do this by fetching n<sub>1</sub> x b blocks in memory so that the
 *       blocks are as large as possible but fit in main memory.</li>
 *   <li>Then do n<sub>1</sub> transforms of length n<sub>2</sub> by transforming the matrix rows.
 *       Do this also by fetching b x n<sub>2</sub> blocks in memory so that the blocks just
 *       fit in the available memory.</li>
 * </ol>
 *
 * The algorithm requires reading blocks of b elements from the mass storage device.
 * The smaller the amount of memory compared to the transform length is, the smaller
 * is b also. Reading very short blocks of data from hard disks can be prohibitively
 * slow.<p>
 *
 * When reading the column data to be transformed, the data can be transposed to
 * rows by reading the b-length blocks to proper locations in memory and then
 * transposing the b x b blocks.<p>
 *
 * In a convolution algorithm the data elements can remain in any order after
 * the transform, as long as the inverse transform can transform it back.
 * The convolution's element-by-element multiplication is not sensitive
 * to the order in which the elements are, of course.<p>
 *
 * This algorithm is parallelized so that the row transforms are done in parallel
 * using multiple threads, if the number of processors is greater than one
 * in {@link ApfloatContext#getNumberOfProcessors() }.<p>
 *
 * This transform uses the maximum amount of memory available as retrieved from
 * {@link ApfloatContext#getMaxMemoryBlockSize() }. All access on memory is synchronized on
 * the shared memory lock retrieved from {@link ApfloatContext#getSharedMemoryLock() }.<p>
 *
 * All access to this class must be externally synchronized.
 *
 * @see DataStorage#getTransposedArray(int,int,int,int)
 *
 * @version 1.5
 * @author Mikko Tommila
 */

public class IntTwoPassFNTStrategy
    extends IntParallelFNTStrategy
    implements NTTStrategy
{
    /**
     * Default constructor.
     */

    public IntTwoPassFNTStrategy()
    {
    }

    public void transform(DataStorage dataStorage, int modulus)
        throws ApfloatRuntimeException
    {
        long length = dataStorage.getSize();            // Transform length n

        if (length > MAX_TRANSFORM_LENGTH)
        {
            throw new TransformLengthExceededException("Maximum transform length exceeded: " + length + " > " + MAX_TRANSFORM_LENGTH);
        }

        if (length < 2)
        {
            return;
        }

        assert (length == (length & -length));          // Must be a power of two

        // Treat the input data as a n1 x n2 matrix

        int logLength = Util.log2down(length),
            n1 = logLength >> 1,
            n2 = logLength - n1;

        n1 = 1 << n1;
        n2 = 1 << n2;

        // Now n2 >= n1

        setModulus(MODULUS[modulus]);                                           // Modulus
        int w = getForwardNthRoot(PRIMITIVE_ROOT[modulus], length),         // Forward n:th root
                w1 = modPow(w, (int) n2);                                   // Forward n1:th root
        int[] wTable = createWTable(w1, n1);
        int[] permutationTable = Scramble.createScrambleTable(n1);

        Object key = getSharedMemoryLockKey(length);

        lock(key);
        try
        {
            int maxBlockSize = getMaxMemoryBlockSize(length),   // Maximum memory array size that can be allocated
                b;

            if (n1 > maxBlockSize || n2 > maxBlockSize)
            {
                throw new ApfloatInternalException("Not enough memory available to fit one row or column of matrix to memory; n1=" + n1 + ", n2=" + n2 + ", available=" + maxBlockSize);
            }

            b = maxBlockSize / n1;

            for (int i = 0; i < n2; i += b)
            {
                // Read the data in n1 x b blocks, transposed
                ArrayAccess arrayAccess = dataStorage.getTransposedArray(DataStorage.READ_WRITE, i, b, n1);

                // Do b transforms of size n1
                transformRows(key, n1, b, false, arrayAccess, wTable, permutationTable);

                arrayAccess.close();
            }

            if (n1 != n2)
            {
                int w2 = modPow(w, (int) n1);             // Forward n2:th root
                wTable = createWTable(w2, n2);
            }

            b = maxBlockSize / n2;

            for (int i = 0; i < n1; i += b)
            {
                // Read the data in b x n2 blocks
                ArrayAccess arrayAccess = dataStorage.getArray(DataStorage.READ_WRITE, i * n2, b * n2);

                // Multiply each matrix element by w^(i*j)
                multiplyElements(key, arrayAccess, i, b, n2, w, (int) 1);

                // Do b transforms of size n2
                transformRows(key, n2, b, false, arrayAccess, wTable, null);

                arrayAccess.close();
            }
        }
        finally
        {
            unlock(key);
        }
    }

    public void inverseTransform(DataStorage dataStorage, int modulus, long totalTransformLength)
        throws ApfloatRuntimeException
    {
        long length = dataStorage.getSize();            // Transform length n

        if (Math.max(length, totalTransformLength) > MAX_TRANSFORM_LENGTH)
        {
            throw new TransformLengthExceededException("Maximum transform length exceeded: " + Math.max(length, totalTransformLength) + " > " + MAX_TRANSFORM_LENGTH);
        }

        if (length < 2)
        {
            return;
        }

        assert (length == (length & -length));          // Must be a power of two

        // Treat the input data as a n1 x n2 matrix

        int logLength = Util.log2down(length),
            n1 = logLength >> 1,
            n2 = logLength - n1;

        n1 = 1 << n1;
        n2 = 1 << n2;

        // Now n2 >= n1

        setModulus(MODULUS[modulus]);                                           // Modulus
        int w = getInverseNthRoot(PRIMITIVE_ROOT[modulus], length),         // Inverse n:th root
                w2 = modPow(w, (int) n1),                                   // Inverse n2:th root
                inverseTotalTransformLength = modDivide((int) 1, (int) totalTransformLength);
        int[] wTable = createWTable(w2, n2);
        int[] permutationTable = Scramble.createScrambleTable(n1);

        Object key = getSharedMemoryLockKey(length);

        lock(key);
        try
        {
            int maxBlockSize = getMaxMemoryBlockSize(length),   // Maximum memory array size that can be allocated
                b;

            if (n1 > maxBlockSize || n2 > maxBlockSize)
            {
                throw new ApfloatInternalException("Not enough memory available to fit one row or column of matrix to memory; n1=" + n1 + ", n2=" + n2 + ", available=" + maxBlockSize);
            }

            b = maxBlockSize / n2;

            for (int i = 0; i < n1; i += b)
            {
                // Read the data in b x n2 blocks
                ArrayAccess arrayAccess = dataStorage.getArray(DataStorage.READ_WRITE, i * n2, b * n2);

                // Do b transforms of size n2
                transformRows(key, n2, b, true, arrayAccess, wTable, null);

                // Multiply each matrix element by w^(i*j) / n
                multiplyElements(key, arrayAccess, i, b, n2, w, inverseTotalTransformLength);

                arrayAccess.close();
            }

            if (n1 != n2)
            {
                // n2 = 2 * n1
                for (int i = 1; i < n1; i++)
                {
                    wTable[i] = wTable[2 * i];
                }
            }

            b = maxBlockSize / n1;

            for (int i = 0; i < n2; i += b)
            {
                // Read the data in n1 x b blocks, transposed
                ArrayAccess arrayAccess = dataStorage.getTransposedArray(DataStorage.READ_WRITE, i, b, n1);

                // Do b transforms of size n1
                transformRows(key, n1, b, true, arrayAccess, wTable, permutationTable);

                arrayAccess.close();
            }
        }
        finally
        {
            unlock(key);
        }
    }

    private int getMaxMemoryBlockSize(long length)
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        long maxMemoryBlockSize = Util.round2down(Math.min(ctx.getMaxMemoryBlockSize() / 4, Integer.MAX_VALUE));
        int maxBlockSize = (int) Math.min(length, maxMemoryBlockSize);

        return maxBlockSize;
    }
}
