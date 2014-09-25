package org.apfloat.internal;

import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.NTTStrategy;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.Util;

/**
 * A transform that implements a 3-point transform on
 * top of another Number Theoretic Transform that does
 * transforms of length 2<sup>n</sup>.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class DoubleFactor3NTTStrategy
    extends DoubleModMath
    implements NTTStrategy,
               DoubleModConstants
{
    /**
     * Creates a new factor-3 transform strategy on top of an existing transform.
     * The underlying transform needs to be capable of only doing transforms of
     * length 2<sup>n</sup>.
     *
     * @param factor2Strategy The underlying transformation strategy, that can be capable of only doing radix-2 transforms.
     */

    public DoubleFactor3NTTStrategy(NTTStrategy factor2Strategy)
    {
        this.factor2Strategy = factor2Strategy;
    }

    public void transform(DataStorage dataStorage, int modulus)
        throws ApfloatRuntimeException
    {
        long length = dataStorage.getSize(),
             power2length = (length & -length);

        if (length > MAX_TRANSFORM_LENGTH)
        {
            throw new ApfloatRuntimeException("Maximum transform length exceeded: " + length + " > " + MAX_TRANSFORM_LENGTH);
        }

        if (length == power2length)
        {
            // Transform length is a power of two
            factor2Strategy.transform(dataStorage, modulus);
        }
        else
        {
            // Transform length is three times a power of two
            assert (length == 3 * power2length);

            setModulus(MODULUS[modulus]);                                       // Modulus
            double w = getForwardNthRoot(PRIMITIVE_ROOT[modulus], length),     // Forward n:th root
                    w3 = modPow(w, (double) power2length);                     // Forward 3rd root

            DataStorage dataStorage0 = dataStorage.subsequence(0, power2length),
                        dataStorage1 = dataStorage.subsequence(power2length, power2length),
                        dataStorage2 = dataStorage.subsequence(2 * power2length, power2length);

            // Transform the columns
            transformColumns(false, dataStorage0, dataStorage1, dataStorage2, power2length, w, w3);

            // Transform the rows
            factor2Strategy.transform(dataStorage0, modulus);
            factor2Strategy.transform(dataStorage1, modulus);
            factor2Strategy.transform(dataStorage2, modulus);
        }
    }

    public void inverseTransform(DataStorage dataStorage, int modulus, long totalTransformLength)
        throws ApfloatRuntimeException
    {
        long length = dataStorage.getSize(),
             power2length = (length & -length);

        if (Math.max(length, totalTransformLength) > MAX_TRANSFORM_LENGTH)
        {
            throw new ApfloatRuntimeException("Maximum transform length exceeded: " + Math.max(length, totalTransformLength) + " > " + MAX_TRANSFORM_LENGTH);
        }

        if (length == power2length)
        {
            // Transform length is a power of two
            factor2Strategy.inverseTransform(dataStorage, modulus, totalTransformLength);
        }
        else
        {
            // Transform length is three times a power of two
            assert (length == 3 * power2length);

            setModulus(MODULUS[modulus]);                                       // Modulus
            double w = getInverseNthRoot(PRIMITIVE_ROOT[modulus], length),     // Inverse n:th root
                    w3 = modPow(w, (double) power2length);                     // Inverse 3rd root

            DataStorage dataStorage0 = dataStorage.subsequence(0, power2length),
                        dataStorage1 = dataStorage.subsequence(power2length, power2length),
                        dataStorage2 = dataStorage.subsequence(2 * power2length, power2length);

            // Transform the rows
            factor2Strategy.inverseTransform(dataStorage0, modulus, totalTransformLength);
            factor2Strategy.inverseTransform(dataStorage1, modulus, totalTransformLength);
            factor2Strategy.inverseTransform(dataStorage2, modulus, totalTransformLength);

            // Transform the columns
            transformColumns(true, dataStorage0, dataStorage1, dataStorage2, power2length, w, w3);
        }
    }

    public long getTransformLength(long size)
    {
        // Calculates the needed transform length, that is
        // a power of two, or three times a power of two
        return Util.round23up(size);
    }

    // Transform the columns using a 3-point transform
    private void transformColumns(boolean isInverse, DataStorage dataStorage0, DataStorage dataStorage1, DataStorage dataStorage2, long size, double w, double w3)
        throws ApfloatRuntimeException
    {
        DataStorage.Iterator iterator0 = dataStorage0.iterator(DataStorage.READ_WRITE, 0, size),
                             iterator1 = dataStorage1.iterator(DataStorage.READ_WRITE, 0, size),
                             iterator2 = dataStorage2.iterator(DataStorage.READ_WRITE, 0, size);
        double ww = modMultiply(w, w),
                w1 = negate(modDivide((double) 3, (double) 2)),
                w2 = modAdd(w3, modDivide((double) 1, (double) 2)),
                tmp1 = (double) 1,
                tmp2 = (double) 1;

        while (size > 0)
        {
            // 3-point WFTA on the corresponding array elements

            double x0 = iterator0.getDouble(),
                    x1 = iterator1.getDouble(),
                    x2 = iterator2.getDouble(),
                    t;

            if (isInverse)
            {
                // Multiply before transform
                x1 = modMultiply(x1, tmp1);
                x2 = modMultiply(x2, tmp2);
            }

            // Transform columns
            t = modAdd(x1, x2);
            x2 = modSubtract(x1, x2);
            x0 = modAdd(x0, t);
            t = modMultiply(t, w1);
            x2 = modMultiply(x2, w2);
            t = modAdd(t, x0);
            x1 = modAdd(t, x2);
            x2 = modSubtract(t, x2);

            if (!isInverse)
            {
                // Multiply after transform
                x1 = modMultiply(x1, tmp1);
                x2 = modMultiply(x2, tmp2);
            }

            iterator0.setDouble(x0);
            iterator1.setDouble(x1);
            iterator2.setDouble(x2);

            iterator0.next();
            iterator1.next();
            iterator2.next();

            tmp1 = modMultiply(tmp1, w);
            tmp2 = modMultiply(tmp2, ww);

            size--;
        }
    }

    private NTTStrategy factor2Strategy;
}
