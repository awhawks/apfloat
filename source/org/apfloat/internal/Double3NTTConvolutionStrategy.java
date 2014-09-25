package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.ConvolutionStrategy;
import org.apfloat.spi.NTTStrategy;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.DataStorage;
import static org.apfloat.internal.DoubleModConstants.*;

/**
 * Convolution methods in the transform domain for the <code>double</code> type.
 * Multiplication can be done in linear time in the transform domain, where
 * the multiplication is simply an element-by-element multiplication.<p>
 *
 * This implementation uses three Number Theoretic Transforms to do the
 * convolution and the Chinese Remainder Theorem to get the final result.
 *
 * @version 1.1
 * @author Mikko Tommila
 */

public class Double3NTTConvolutionStrategy
    extends DoubleModMath
    implements ConvolutionStrategy
{
    /**
     * Creates a new convoluter that uses the specified
     * transform for transforming the data.
     *
     * @param radix The radix that will be used.
     * @param transform The transform that will be used.
     */

    public Double3NTTConvolutionStrategy(int radix, NTTStrategy transform)
    {
        this.radix = radix;
        this.transform = transform;
    }

    public DataStorage convolute(DataStorage x, DataStorage y, long resultSize)
        throws ApfloatRuntimeException
    {
        if (x == y)
        {
            return autoConvolute(x, resultSize);
        }

        long length = this.transform.getTransformLength(x.getSize() + y.getSize());

        DataStorage resultMod0 = convoluteOne(x, y, length, 0),
                    resultMod1 = convoluteOne(x, y, length, 1),
                    resultMod2 = convoluteOne(x, y, length, 2);

        return new DoubleCarryCRT(this.radix).carryCRT(resultMod0, resultMod1, resultMod2, resultSize);
    }

    // Performs a convolution modulo one modulus, of the specified transform length
    private DataStorage convoluteOne(DataStorage x, DataStorage y, long length, int modulus)
        throws ApfloatRuntimeException
    {
        DataStorage tmpY = createDataStorage(length);
        tmpY.copyFrom(y, length);
        this.transform.transform(tmpY, modulus);

        DataStorage tmpX = createDataStorage(length);
        tmpX.copyFrom(x, length);
        this.transform.transform(tmpX, modulus);

        multiplyInPlace(tmpX, tmpY, modulus);

        this.transform.inverseTransform(tmpX, modulus, length);

        return tmpX;
    }

    /**
     * Convolutes a data set with itself.
     *
     * @param dataStorage x The data set.
     * @param resultSize Number of elements needed in the result data.
     *
     * @return The convolved data.
     */

    private DataStorage autoConvolute(DataStorage x, long resultSize)
        throws ApfloatRuntimeException
    {
        long length = this.transform.getTransformLength(x.getSize() * 2);

        DataStorage resultMod0 = autoConvoluteOne(x, length, 0),
                    resultMod1 = autoConvoluteOne(x, length, 1),
                    resultMod2 = autoConvoluteOne(x, length, 2);

        return new DoubleCarryCRT(this.radix).carryCRT(resultMod0, resultMod1, resultMod2, resultSize);
    }

    // Performs a autoconvolution modulo one modulus, of the specified transform length
    private DataStorage autoConvoluteOne(DataStorage x, long length, int modulus)
        throws ApfloatRuntimeException
    {
        DataStorage tmp = createDataStorage(length);
        tmp.copyFrom(x, length);
        this.transform.transform(tmp, modulus);

        squareInPlace(tmp, modulus);

        this.transform.inverseTransform(tmp, modulus, length);

        return tmp;
    }

    /**
     * Linear multiplication in the number theoretic domain.
     * The operation is <code>sourceAndDestination[i] *= source[i] (mod m)</code>.<p>
     *
     * For maximum performance, <code>sourceAndDestination</code>
     * should be in memory if possible.
     *
     * @param sourceAndDestination The first source data storage, which is also the destination.
     * @param source The second source data storage.
     * @param modulus Which modulus to use (0, 1, 2)
     */

    private void multiplyInPlace(DataStorage sourceAndDestination, DataStorage source, int modulus)
        throws ApfloatRuntimeException
    {
        assert (sourceAndDestination != source);

        long size = sourceAndDestination.getSize();
        DataStorage.Iterator dest = sourceAndDestination.iterator(DataStorage.READ_WRITE, 0, size),
                             src = source.iterator(DataStorage.READ, 0, size);

        setModulus(MODULUS[modulus]);

        while (size > 0)
        {
            dest.setDouble(modMultiply(dest.getDouble(), src.getDouble()));

            dest.next();
            src.next();
            size--;
        }
    }

    /**
     * Linear squaring in the number theoretic domain.
     * The operation is <code>sourceAndDestination[i] *= sourceAndDestination[i] (mod m)</code>.<p>
     *
     * For maximum performance, <code>sourceAndDestination</code>
     * should be in memory if possible.
     *
     * @param sourceAndDestination The source data storage, which is also the destination.
     * @param modulus Which modulus to use (0, 1, 2)
     */

    private void squareInPlace(DataStorage sourceAndDestination, int modulus)
        throws ApfloatRuntimeException
    {
        long size = sourceAndDestination.getSize();
        DataStorage.Iterator iterator = sourceAndDestination.iterator(DataStorage.READ_WRITE, 0, size);

        setModulus(MODULUS[modulus]);

        while (size > 0)
        {
            double value = iterator.getDouble();
            iterator.setDouble(modMultiply(value, value));

            iterator.next();
            size--;
        }
    }

    private static DataStorage createDataStorage(long size)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        DataStorageBuilder dataStorageBuilder = ctx.getBuilderFactory().getDataStorageBuilder();
        return dataStorageBuilder.createDataStorage(size * 8);
    }

    private NTTStrategy transform;
    private int radix;
}
