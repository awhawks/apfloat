package org.apfloat.internal;

import java.math.BigInteger;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.DataStorage;
import static org.apfloat.internal.IntModConstants.*;

/**
 * Class for performing the final step of a three-modulus
 * Number Theoretic Transform based convolution. Works for the
 * <code>int</code> type.
 *
 * @version 1.1
 * @author Mikko Tommila
 */

public class IntCarryCRT
    extends IntCRTMath
{
    /**
     * Creates a carry-CRT object using the specified radix.
     *
     * @param radix The radix that will be used.
     */

    public IntCarryCRT(int radix)
    {
        super(radix);
    }

    /**
     * Calculate the final result of a three-NTT convolution.<p>
     *
     * Performs a Chinese Remainder Theorem (CRT) on each element
     * of the three result data sets to get the result of each element
     * modulo the product of the three moduli. Then it calculates the carries
     * to get the final result.<p>
     *
     * Note that the return value's initial word may be zero or non-zero,
     * depending on how large the result is.<p>
     *
     * Assumes that <code>MODULUS[0] > MODULUS[1] > MODULUS[2]</code>.
     *
     * @param resultMod0 The result modulo <code>MODULUS[0]</code>.
     * @param resultMod1 The result modulo <code>MODULUS[1]</code>.
     * @param resultMod2 The result modulo <code>MODULUS[2]</code>.
     * @param resultSize The number of elements needed in the final result.
     *
     * @return The final result with the CRT performed and the carries calculated.
     */

    public DataStorage carryCRT(DataStorage resultMod0, DataStorage resultMod1, DataStorage resultMod2, long resultSize)
        throws ApfloatRuntimeException
    {
        long size = Math.min(resultSize + 2, resultMod0.getSize());     // Some extra precision if not full result is required

        ApfloatContext ctx = ApfloatContext.getContext();
        DataStorageBuilder dataStorageBuilder = ctx.getBuilderFactory().getDataStorageBuilder();
        DataStorage dataStorage = dataStorageBuilder.createDataStorage(resultSize * 4);
        dataStorage.setSize(resultSize);

        DataStorage.Iterator src0 = resultMod0.iterator(DataStorage.READ, size, 0),
                             src1 = resultMod1.iterator(DataStorage.READ, size, 0),
                             src2 = resultMod2.iterator(DataStorage.READ, size, 0),
                             dst = dataStorage.iterator(DataStorage.WRITE, resultSize, 0);

        int[] carryResult = new int[3],
                  sum = new int[3],
                  tmp = new int[3];

        for (long i = size; i > 0; i--)
        {
            int y0 = MATH_MOD_0.modMultiply(T0, src0.getInt()),
                    y1 = MATH_MOD_1.modMultiply(T1, src1.getInt()),
                    y2 = MATH_MOD_2.modMultiply(T2, src2.getInt());

            multiply(M12, y0, sum);
            multiply(M02, y1, tmp);

            if (add(tmp, sum) != 0 ||
                compare(sum, M012) >= 0)
            {
                subtract(M012, sum);
            }

            multiply(M01, y2, tmp);

            if (add(tmp, sum) != 0 ||
                compare(sum, M012) >= 0)
            {
                subtract(M012, sum);
            }

            add(sum, carryResult);

            int result = divide(carryResult);

            if (i < resultSize)
            {
                dst.setInt(result);
                dst.next();
            }

            src0.next();
            src1.next();
            src2.next();
        }

        dst.setInt(carryResult[2]);
        dst.close();

        assert (carryResult[0] == 0);
        assert (carryResult[1] == 0);

        return dataStorage;
    }

    private static final IntModMath MATH_MOD_0,
                                        MATH_MOD_1,
                                        MATH_MOD_2;
    private static final int T0,
                                 T1,
                                 T2;
    private static final int[] M01,
                                   M02,
                                   M12,
                                   M012;

    static
    {
        MATH_MOD_0 = new IntModMath();
        MATH_MOD_1 = new IntModMath();
        MATH_MOD_2 = new IntModMath();

        MATH_MOD_0.setModulus(MODULUS[0]);
        MATH_MOD_1.setModulus(MODULUS[1]);
        MATH_MOD_2.setModulus(MODULUS[2]);

        // Probably sub-optimal, but it's a one-time operation

        BigInteger base = BigInteger.valueOf(Math.abs((long) MAX_POWER_OF_TWO_BASE)),   // In int case the base is 0x80000000
                   m0 = BigInteger.valueOf((long) MODULUS[0]),
                   m1 = BigInteger.valueOf((long) MODULUS[1]),
                   m2 = BigInteger.valueOf((long) MODULUS[2]),
                   m01 = m0.multiply(m1),
                   m02 = m0.multiply(m2),
                   m12 = m1.multiply(m2);

        T0 = m12.modInverse(m0).intValue();
        T1 = m02.modInverse(m1).intValue();
        T2 = m01.modInverse(m2).intValue();

        M01 = new int[2];
        M02 = new int[2];
        M12 = new int[2];
        M012 = new int[3];

        BigInteger[] qr = m01.divideAndRemainder(base);
        M01[0] = qr[0].intValue();
        M01[1] = qr[1].intValue();

        qr = m02.divideAndRemainder(base);
        M02[0] = qr[0].intValue();
        M02[1] = qr[1].intValue();

        qr = m12.divideAndRemainder(base);
        M12[0] = qr[0].intValue();
        M12[1] = qr[1].intValue();

        qr = m0.multiply(m12).divideAndRemainder(base);
        M012[2] = qr[1].intValue();
        qr = qr[0].divideAndRemainder(base);
        M012[0] = qr[0].intValue();
        M012[1] = qr[1].intValue();
    }
}
