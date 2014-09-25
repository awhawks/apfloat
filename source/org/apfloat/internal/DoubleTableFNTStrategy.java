package org.apfloat.internal;

import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.NTTStrategy;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;
import org.apfloat.spi.Util;

/**
 * Fast Number Theoretic Transform that uses lookup tables
 * for powers of n:th root of unity and permutation indexes.<p>
 *
 * All access to this class must be externally synchronized.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class DoubleTableFNTStrategy
    extends DoubleModMath
    implements NTTStrategy,
               DoubleModConstants
{
    /**
     * Default constructor.
     */

    public DoubleTableFNTStrategy()
    {
    }

    public void transform(DataStorage dataStorage, int modulus)
        throws ApfloatRuntimeException
    {
        long length = dataStorage.getSize();            // Transform length n

        if (length > MAX_TRANSFORM_LENGTH)
        {
            throw new ApfloatRuntimeException("Maximum transform length exceeded: " + length + " > " + MAX_TRANSFORM_LENGTH);
        }
        else if (length > Integer.MAX_VALUE)
        {
            throw new ApfloatRuntimeException("Maximum array length exceeded: " + length);
        }

        setModulus(MODULUS[modulus]);                                       // Modulus
        double w = getForwardNthRoot(PRIMITIVE_ROOT[modulus], length);     // Forward n:th root
        double[] wTable = createWTable(w, (int) length);

        ArrayAccess arrayAccess = dataStorage.getArray(DataStorage.READ_WRITE, 0, (int) length);

        tableFNT(arrayAccess, wTable, null);

        arrayAccess.close();
    }

    public void inverseTransform(DataStorage dataStorage, int modulus, long totalTransformLength)
        throws ApfloatRuntimeException
    {
        long length = dataStorage.getSize();            // Transform length n

        if (Math.max(length, totalTransformLength) > MAX_TRANSFORM_LENGTH)
        {
            throw new ApfloatRuntimeException("Maximum transform length exceeded: " + Math.max(length, totalTransformLength) + " > " + MAX_TRANSFORM_LENGTH);
        }
        else if (length > Integer.MAX_VALUE)
        {
            throw new ApfloatRuntimeException("Maximum array length exceeded: " + length);
        }

        setModulus(MODULUS[modulus]);                                       // Modulus
        double w = getInverseNthRoot(PRIMITIVE_ROOT[modulus], length);     // Inverse n:th root
        double[] wTable = createWTable(w, (int) length);

        ArrayAccess arrayAccess = dataStorage.getArray(DataStorage.READ_WRITE, 0, (int) length);

        inverseTableFNT(arrayAccess, wTable, null);

        divideElements(arrayAccess, (double) totalTransformLength);

        arrayAccess.close();
    }

    public long getTransformLength(long size)
    {
        return Util.round2up(size);
    }

    /**
     * Forward (Sande-Tukey) fast Number Theoretic Transform.
     * Data length must be a power of two.
     *
     * @param arrayAccess The data array to transform.
     * @param wTable Table of powers of n:th root of unity <code>w</code> modulo the current modulus.
     * @param permutationTable Table of permutation indexes, or <code>null</code> if the data should not be permuted.
     */

    protected void tableFNT(ArrayAccess arrayAccess, double[] wTable, int[] permutationTable)
        throws ApfloatRuntimeException
    {
        int nn, offset, istep, mmax, r;
        double[] data;

        data   = arrayAccess.getDoubleData();
        offset = arrayAccess.getOffset();
        nn     = arrayAccess.getLength();

        assert (nn == (nn & -nn));

        if (nn < 2)
        {
            return;
        }

        r = 1;
        mmax = nn >> 1;
        while (mmax > 0)
        {
            istep = mmax << 1;

            // Optimize first step when wr = 1

            for (int i = offset; i < offset + nn; i += istep)
            {
                int j = i + mmax;
                double a = data[i];
                double b = data[j];
                data[i] = modAdd(a, b);
                data[j] = modSubtract(a, b);
            }

            int t = r;

            for (int m = 1; m < mmax; m++)
            {
                for (int i = offset + m; i < offset + nn; i += istep)
                {
                    int j = i + mmax;
                    double a = data[i];
                    double b = data[j];
                    data[i] = modAdd(a, b);
                    data[j] = modMultiply(wTable[t], modSubtract(a, b));
                }
                t += r;
            }
            r <<= 1;
            mmax >>= 1;
        }

        if (permutationTable != null)
        {
            DoubleScramble.scramble(data, offset, permutationTable);
        }
    }

    /**
     * Inverse (Cooley-Tukey) fast Number Theoretic Transform.
     * Data length must be a power of two.
     *
     * @param arrayAccess The data array to transform.
     * @param wTable Table of powers of n:th root of unity <code>w</code> modulo the current modulus.
     * @param permutationTable Table of permutation indexes, or <code>null</code> if the data should not be permuted.
     */

    protected void inverseTableFNT(ArrayAccess arrayAccess, double[] wTable, int[] permutationTable)
        throws ApfloatRuntimeException
    {
        int nn, offset, istep, mmax, r;
        double[] data;

        data   = arrayAccess.getDoubleData();
        offset = arrayAccess.getOffset();
        nn     = arrayAccess.getLength();

        assert (nn == (nn & -nn));

        if (nn < 2)
        {
            return;
        }

        if (permutationTable != null)
        {
            DoubleScramble.scramble(data, offset, permutationTable);
        }

        r = nn;
        mmax = 1;
        while (nn > mmax)
        {
            istep = mmax << 1;
            r >>= 1;

            // Optimize first step when w = 1

            for (int i = offset; i < offset + nn; i += istep)
            {
                int j = i + mmax;
                double wTemp = data[j];
                data[j] = modSubtract(data[i], wTemp);
                data[i] = modAdd(data[i], wTemp);
            }

            int t = r;

            for (int m = 1; m < mmax; m++)
            {
                for (int i = offset + m; i < offset + nn; i += istep)
                {
                    int j = i + mmax;
                    double wTemp = modMultiply(wTable[t], data[j]);
                    data[j] = modSubtract(data[i], wTemp);
                    data[i] = modAdd(data[i], wTemp);
                }
                t += r;
            }
            mmax = istep;
        }
    }

    private void divideElements(ArrayAccess arrayAccess, double divisor)
        throws ApfloatRuntimeException
    {
        double inverseFactor = modDivide((double) 1, divisor);
        double[] data = arrayAccess.getDoubleData();
        int length = arrayAccess.getLength(),
            offset = arrayAccess.getOffset();

        for (int i = 0; i < length; i++)
        {
            data[i + offset] = modMultiply(data[i + offset], inverseFactor);
        }
    }
}
