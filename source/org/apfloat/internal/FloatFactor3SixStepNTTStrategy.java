package org.apfloat.internal;

import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;
import org.apfloat.spi.Util;
import static org.apfloat.internal.FloatModConstants.*;

/**
 * A transform that implements a 3-point transform on
 * top of the six-step Number Theoretic Transform that does
 * transforms of length 2<sup>n</sup>.
 *
 * @version 1.5.1
 * @author Mikko Tommila
 */

public class FloatFactor3SixStepNTTStrategy
    extends FloatParallelFNTStrategy
{
    // Runnable for transforming the columns
    private class ColumnTransformRunnable
        implements Runnable
    {
        public ColumnTransformRunnable(boolean isInverse, ArrayAccess arrayAccess0, ArrayAccess arrayAccess1, ArrayAccess arrayAccess2, int startColumn, int columns, float w, float ww, float w1, float w2)
        {
            this.isInverse = isInverse;
            this.arrayAccess0 = arrayAccess0;
            this.arrayAccess1 = arrayAccess1;
            this.arrayAccess2 = arrayAccess2;
            this.startColumn = startColumn;
            this.columns = columns;
            this.w = w;
            this.ww = ww;
            this.w1 = w1;
            this.w2 = w2;
        }

        public void run()
        {
            float tmp1 = modPow(this.w, this.startColumn),
                    tmp2 = modPow(this.ww, this.startColumn);

            float[] data0 = this.arrayAccess0.getFloatData(),
                      data1 = this.arrayAccess1.getFloatData(),
                      data2 = this.arrayAccess2.getFloatData();
            int offset0 = this.arrayAccess0.getOffset() + this.startColumn,
                offset1 = this.arrayAccess1.getOffset() + this.startColumn,
                offset2 = this.arrayAccess2.getOffset() + this.startColumn;


            for (int i = 0; i < this.columns; i++)
            {
                // 3-point WFTA on the corresponding array elements

                float x0 = data0[offset0 + i],
                        x1 = data1[offset1 + i],
                        x2 = data2[offset2 + i],
                        t;

                if (this.isInverse)
                {
                    // Multiply before transform
                    x1 = modMultiply(x1, tmp1);
                    x2 = modMultiply(x2, tmp2);
                }

                // Transform columns
                t = modAdd(x1, x2);
                x2 = modSubtract(x1, x2);
                x0 = modAdd(x0, t);
                t = modMultiply(t, this.w1);
                x2 = modMultiply(x2, this.w2);
                t = modAdd(t, x0);
                x1 = modAdd(t, x2);
                x2 = modSubtract(t, x2);

                if (!this.isInverse)
                {
                    // Multiply after transform
                    x1 = modMultiply(x1, tmp1);
                    x2 = modMultiply(x2, tmp2);
                }

                data0[offset0 + i] = x0;
                data1[offset1 + i] = x1;
                data2[offset2 + i] = x2;

                tmp1 = modMultiply(tmp1, this.w);
                tmp2 = modMultiply(tmp2, this.ww);
            }
        }

        private boolean isInverse;
        private ArrayAccess arrayAccess0;
        private ArrayAccess arrayAccess1;
        private ArrayAccess arrayAccess2;
        private int startColumn;
        private int columns;
        private float w;
        private float ww;
        private float w1;
        private float w2;
    }

    /**
     * Creates a new factor-3 transform strategy on top of a six-step transform.
     * The underlying transform needs to be capable of only doing transforms of
     * length 2<sup>n</sup>.
     *
     * @param factor2Strategy The underlying transformation strategy, that can be capable of only doing radix-2 transforms.
     */

    public FloatFactor3SixStepNTTStrategy(FloatSixStepFNTStrategy factor2Strategy)
    {
        this.factor2Strategy = factor2Strategy;
    }

    public void setParallelRunner(ParallelRunner parallelRunner)
    {
        super.setParallelRunner(parallelRunner);
        this.factor2Strategy.setParallelRunner(parallelRunner);
    }

    public void transform(DataStorage dataStorage, int modulus)
        throws ApfloatRuntimeException
    {
        long length = dataStorage.getSize();

        if (length > MAX_TRANSFORM_LENGTH)
        {
            throw new TransformLengthExceededException("Maximum transform length exceeded: " + length + " > " + MAX_TRANSFORM_LENGTH);
        }
        else if (length > Integer.MAX_VALUE)
        {
            throw new ApfloatInternalException("Maximum array length exceeded: " + length);
        }

        int power2length = (int) (length & -length);

        if (length == power2length)
        {
            // Transform length is a power of two
            this.factor2Strategy.transform(dataStorage, modulus);
        }
        else
        {
            // Transform length is three times a power of two
            assert (length == 3 * power2length);

            setModulus(MODULUS[modulus]);                                       // Modulus
            float w = getForwardNthRoot(PRIMITIVE_ROOT[modulus], length),     // Forward n:th root
                    w3 = modPow(w, (float) power2length);                     // Forward 3rd root

            ArrayAccess arrayAccess = dataStorage.getArray(DataStorage.READ_WRITE, 0, (int) length),
                        arrayAccess0 = arrayAccess.subsequence(0, power2length),
                        arrayAccess1 = arrayAccess.subsequence(power2length, power2length),
                        arrayAccess2 = arrayAccess.subsequence(2 * power2length, power2length);

            // Transform the columns
            transformColumns(false, arrayAccess0, arrayAccess1, arrayAccess2, power2length, w, w3);

            // Transform the rows
            this.factor2Strategy.transform(arrayAccess0, modulus);
            this.factor2Strategy.transform(arrayAccess1, modulus);
            this.factor2Strategy.transform(arrayAccess2, modulus);

            arrayAccess.close();
        }
    }

    public void inverseTransform(DataStorage dataStorage, int modulus, long totalTransformLength)
        throws ApfloatRuntimeException
    {
        long length = dataStorage.getSize();

        if (Math.max(length, totalTransformLength) > MAX_TRANSFORM_LENGTH)
        {
            throw new TransformLengthExceededException("Maximum transform length exceeded: " + Math.max(length, totalTransformLength) + " > " + MAX_TRANSFORM_LENGTH);
        }
        else if (length > Integer.MAX_VALUE)
        {
            throw new ApfloatInternalException("Maximum array length exceeded: " + length);
        }

        int power2length = (int) (length & -length);

        if (length == power2length)
        {
            // Transform length is a power of two
            this.factor2Strategy.inverseTransform(dataStorage, modulus, totalTransformLength);
        }
        else
        {
            // Transform length is three times a power of two
            assert (length == 3 * power2length);

            setModulus(MODULUS[modulus]);                                       // Modulus
            float w = getInverseNthRoot(PRIMITIVE_ROOT[modulus], length),     // Inverse n:th root
                    w3 = modPow(w, (float) power2length);                     // Inverse 3rd root

            ArrayAccess arrayAccess = dataStorage.getArray(DataStorage.READ_WRITE, 0, (int) length),
                        arrayAccess0 = arrayAccess.subsequence(0, power2length),
                        arrayAccess1 = arrayAccess.subsequence(power2length, power2length),
                        arrayAccess2 = arrayAccess.subsequence(2 * power2length, power2length);

            // Transform the rows
            this.factor2Strategy.inverseTransform(arrayAccess0, modulus, totalTransformLength);
            this.factor2Strategy.inverseTransform(arrayAccess1, modulus, totalTransformLength);
            this.factor2Strategy.inverseTransform(arrayAccess2, modulus, totalTransformLength);

            // Transform the columns
            transformColumns(true, arrayAccess0, arrayAccess1, arrayAccess2, power2length, w, w3);

            arrayAccess.close();
        }
    }

    public long getTransformLength(long size)
    {
        // Calculates the needed transform length, that is
        // a power of two, or three times a power of two
        return Util.round23up(size);
    }

    // Transform the columns using a 3-point transform
    private void transformColumns(final boolean isInverse, final ArrayAccess arrayAccess0, final ArrayAccess arrayAccess1, final ArrayAccess arrayAccess2, final int size, final float w, final float w3)
        throws ApfloatRuntimeException
    {
        final float ww = modMultiply(w, w),
                      w1 = negate(modDivide((float) 3, (float) 2)),
                      w2 = modAdd(w3, modDivide((float) 1, (float) 2));

        ParallelRunnable parallelRunnable = new ParallelRunnable()
        {
            public int getLength()
            {
                return size;
            }

            public Runnable getRunnable(int strideStartColumn, int strideColumns)
            {
                return new ColumnTransformRunnable(isInverse, arrayAccess0, arrayAccess1, arrayAccess2, strideStartColumn, strideColumns, w, ww, w1, w2);
            }
        };

        super.parallelRunner.runParallel(parallelRunnable);
    }

    private FloatSixStepFNTStrategy factor2Strategy;
}
