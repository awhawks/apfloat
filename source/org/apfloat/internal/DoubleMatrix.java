package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.ArrayAccess;
import org.apfloat.spi.Util;

/**
 * Optimized matrix transposition methods for the <code>double</code> type.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class DoubleMatrix
{
    private DoubleMatrix()
    {
    }

    /**
     * Transpose a n<sub>1</sub> x n<sub>2</sub> matrix.<p>
     *
     * Both n<sub>1</sub> and n<sub>2</sub> must be powers of two.
     * Additionally, one of these must be true:<p>
     *
     * n<sub>1</sub> = n<sub>2</sub><br>
     * n<sub>1</sub> = 2*n<sub>2</sub><br>
     * n<sub>2</sub> = 2*n<sub>1</sub><br>
     *
     * @param arrayAccess Accessor to the matrix data. This data will be transposed.
     * @param n1 Number of rows in the matrix.
     * @param n2 Number of columns in the matrix.
     */

    public static void transpose(ArrayAccess arrayAccess, int n1, int n2)
        throws ApfloatRuntimeException
    {
        double[] data = arrayAccess.getDoubleData();
        int offset = arrayAccess.getOffset();

        if (n1 != (n1 & -n1) ||
            n2 != (n2 & -n2) ||
            n1 <= 0 || n2 <= 0)
        {
            throw new ApfloatRuntimeException("Matrix size must be a power of two, not " + n1 + " x " + n2);
        }
        if (n1 == n2)
        {
            // Simply transpose

            transposeSquare(data, offset, n1, n1);
        }
        else if (n2 == 2 * n1)
        {
            // First transpose two n1 x n1 blocks
            transposeSquare(data, offset, n1, n2);
            transposeSquare(data, offset + n1, n1, n2);

            // Then permute the rows to correct order
            permuteWideToTall(data, offset, n1, n2);
        }
        else if (n1 == 2 * n2)
        {
            // First permute the rows to correct order
            permuteTallToWide(data, offset, n1, n2);

            // Then transpose two n2 x n2 blocks
            transposeSquare(data, offset, n2, n1);
            transposeSquare(data, offset + n2, n2, n1);
        }
        else
        {
            throw new ApfloatRuntimeException("Must be n1 = n2, n1 = 2*n2 or n2 = 2*n1; matrix is " + n1 + " x " + n2);
        }
    }

    /**
     * Transpose a square n<sub>1</sub> x n<sub>1</sub> block of n<sub>1</sub> x n<sub>2</sub> matrix.<p>
     *
     * Both n<sub>1</sub> and n<sub>2</sub> must be powers of two,
     * and n<sub>1</sub> <= n<sub>2</sub>.
     *
     * @param arrayAccess Accessor to the matrix data. This data will be transposed.
     * @param n1 Number of rows and columns in the block to be transposed.
     * @param n2 Number of columns in the matrix.
     */

    public static void transposeSquare(ArrayAccess arrayAccess, int n1, int n2)
        throws ApfloatRuntimeException
    {
        transposeSquare(arrayAccess.getDoubleData(), arrayAccess.getOffset(), n1, n2);
    }

    // Move a b x b block from source to dest
    private static void moveBlock(double[] source, int sourceOffset, int sourceWidth, double[] dest, int destOffset, int destWidth, int b)
    {
        for (int i = 0; i < b; i++)
        {
            System.arraycopy(source, sourceOffset, dest, destOffset, b);

            destOffset += destWidth;
            sourceOffset += sourceWidth;
        }
    }

    // Transpose two b x b blocks of matrix with specified width
    // data based on offset1 is accessed in columns, data based on offset2 in rows
    private static void transpose2blocks(double[] data, int offset1, int offset2, int width, int b)
    {
        for (int i = 0, position1 = offset2; i < b; i++, position1 += width)
        {
            for (int j = 0, position2 = offset1 + i; j < b; j++, position2 += width)
            {
                double tmp = data[position1 + j];
                data[position1 + j] = data[position2];
                data[position2] = tmp;
            }
        }
    }

    // Transpose a b x b block of matrix with specified width
    private static void transposeBlock(double[] data, int offset, int width, int b)
    {
        for (int i = 0, position1 = offset; i < b; i++, position1 += width)
        {
            for (int j = i + 1, position2 = offset + j * width + i; j < b; j++, position2 += width)
            {
                double tmp = data[position1 + j];
                data[position1 + j] = data[position2];
                data[position2] = tmp;
            }
        }
    }

    // Transpose a square n1 x n1 block of n1 x n2 matrix in b x b blocks
    private static void transposeSquare(double[] data, int offset, int n1, int n2)
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        int cacheBurstBlockSize = Util.round2down(ctx.getCacheBurst() / 8),   // Cache burst in doubles
            cacheBlockSize = Util.sqrt4down(ctx.getCacheL1Size() / 8),        // Transpose block size b that fits in processor L1 cache
            cacheTreshold = Util.round2down(ctx.getCacheL2Size() / 8);        // Size of matrix that fits in L2 cache

        if (n1 <= cacheBurstBlockSize || n1 <= cacheBlockSize)
        {
            // Whole matrix fits in L1 cache

            transposeBlock(data, offset, n2, n1);
        }
        else if (n1 * n2 <= cacheTreshold)
        {
            // Whole matrix fits in L2 cache (but not in L1 cache)
            // Sometimes the first algorithm (the block above) is faster, if your L2 cache is very fast

            int b = cacheBurstBlockSize;

            for (int i = 0, position1 = offset; i < n1; i += b, position1 += b * n2)
            {
                transposeBlock(data, position1 + i, n2, b);

                for (int j = i + b, position2 = offset + j * n2 + i; j < n1; j += b, position2 += b * n2)
                {
                    transpose2blocks(data, position1 + j, position2, n2, b);
                }
            }
        }
        else
        {
            // Whole matrix doesn't fit in L2 cache
            // This algorithm works fastest if L1 cache size is set correctly

            int b = cacheBlockSize;

            double[] tmp1 = new double[b * b],
                      tmp2 = new double[b * b];

            for (int i = 0, position1 = offset; i < n1; i += b, position1 += b * n2)
            {
                moveBlock(data, position1 + i, n2, tmp1, 0, b, b);
                transposeBlock(tmp1, 0, b, b);
                moveBlock(tmp1, 0, b, data, position1 + i, n2, b);

                for (int j = i + b, position2 = offset + j * n2 + i; j < n1; j += b, position2 += b * n2)
                {
                    moveBlock(data, position1 + j, n2, tmp1, 0, b, b);
                    transposeBlock(tmp1, 0, b, b);

                    moveBlock(data, position2, n2, tmp2, 0, b, b);
                    transposeBlock(tmp2, 0, b, b);

                    moveBlock(tmp2, 0, b, data, position1 + j, n2, b);
                    moveBlock(tmp1, 0, b, data, position2, n2, b);
                }
            }
        }
    }

    // Permute the rows of matrix to correct order, must be n2 = 2*n1
    private static void permuteWideToTall(double[] data, int offset, int n1, int n2)
    {
        assert (n2 == 2 * n1);

        if (n2 < 4)
        {
            return;
        }

        double[] tmp = new double[n1];
        boolean[] isRowDone = new boolean[n2];

        int j = 1;
        do
        {
            int o = j,
                m = j;

            System.arraycopy(data, offset + n1 * m, tmp, 0, n1);

            isRowDone[m] = true;

            m = (m < n1 ? 2 * m : 2 * (m - n1) + 1);

            while (m != j)
            {
                isRowDone[m] = true;

                System.arraycopy(data, offset + n1 * m, data, offset + n1 * o, n1);

                o = m;
                m = (m < n1 ? 2 * m : 2 * (m - n1) + 1);
            }

            System.arraycopy(tmp, 0, data, offset + n1 * o, n1);

            while (isRowDone[j])
            {
                j++;
            }
        } while (j < n2 - 1);
    }

    // Permute the rows of matrix to correct order, must be n1 = 2*n2
    private static void permuteTallToWide(double[] data, int offset, int n1, int n2)
    {
        assert (n1 == 2 * n2);

        if (n1 < 4)
        {
            return;
        }

        double[] tmp = new double[n2];
        boolean[] isRowDone = new boolean[n1];

        int j = 1;
        do
        {
            int o = j,
                m = j;

            System.arraycopy(data, offset + n2 * m, tmp, 0, n2);

            isRowDone[m] = true;

            m = ((m & 1) != 0 ? m / 2 + n2 : m / 2);

            while (m != j)
            {
                isRowDone[m] = true;

                System.arraycopy(data, offset + n2 * m, data, offset + n2 * o, n2);

                o = m;
                m = ((m & 1) != 0 ? m / 2 + n2 : m / 2);
            }

            System.arraycopy(tmp, 0, data, offset + n2 * o, n2);

            while (isRowDone[j])
            {
                j++;
            }
        } while (j < n1 - 1);
    }
}
