package org.apfloat.internal;

import java.util.RandomAccess;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.ConvolutionStrategy;
import org.apfloat.spi.NTTStrategy;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.DataStorage;
import static org.apfloat.internal.FloatModConstants.*;

/**
 * Convolution methods in the transform domain for the <code>float</code> type.
 * Multiplication can be done in linear time in the transform domain, where
 * the multiplication is simply an element-by-element multiplication.<p>
 *
 * This implementation uses three Number Theoretic Transforms to do the
 * convolution and the Chinese Remainder Theorem to get the final result.<p>
 *
 * All access to this class must be externally synchronized.
 *
 * @version 1.6
 * @author Mikko Tommila
 */

public class Float3NTTConvolutionStrategy
    extends FloatModMath
    implements ConvolutionStrategy
{
    // Runnable for multiplying elements in place
    private class MultiplyInPlaceRunnable
        implements Runnable
    {
        public MultiplyInPlaceRunnable(DataStorage sourceAndDestination, DataStorage source)
        {
            this.sourceAndDestination = sourceAndDestination;
            this.source = source;
        }

        public void run()
        {
            long size = this.sourceAndDestination.getSize();
            DataStorage.Iterator dest = this.sourceAndDestination.iterator(DataStorage.READ_WRITE, 0, size),
                                 src = this.source.iterator(DataStorage.READ, 0, size);

            while (size > 0)
            {
                dest.setFloat(modMultiply(dest.getFloat(), src.getFloat()));

                dest.next();
                src.next();
                size--;
            }
        }

        private DataStorage sourceAndDestination,
                            source;
    }

    // Runnable for squaring elements in place
    private class SquareInPlaceRunnable
        implements Runnable
    {
        public SquareInPlaceRunnable(DataStorage sourceAndDestination)
        {
            this.sourceAndDestination = sourceAndDestination;
        }

        public void run()
        {
            long size = this.sourceAndDestination.getSize();
            DataStorage.Iterator iterator = this.sourceAndDestination.iterator(DataStorage.READ_WRITE, 0, size);

            while (size > 0)
            {
                float value = iterator.getFloat();
                iterator.setFloat(modMultiply(value, value));

                iterator.next();
                size--;
            }
        }

        private DataStorage sourceAndDestination;
    }

    /**
     * Creates a new convoluter that uses the specified
     * transform for transforming the data.
     *
     * @param radix The radix that will be used.
     * @param transform The transform that will be used.
     */

    public Float3NTTConvolutionStrategy(int radix, NTTStrategy transform)
    {
        this.transform = transform;
        this.carryCRT = new FloatCarryCRT(radix);
    }

    public DataStorage convolute(DataStorage x, DataStorage y, long resultSize)
        throws ApfloatRuntimeException
    {
        if (x == y)
        {
            return autoConvolute(x, resultSize);
        }

        long length = this.transform.getTransformLength(x.getSize() + y.getSize());

        DataStorage result;
        lock(length);
        try
        {
            DataStorage resultMod0 = convoluteOne(x, y, length, 0, false),
                        resultMod1 = convoluteOne(x, y, length, 1, false),
                        resultMod2 = convoluteOne(x, y, length, 2, true);

            result = this.carryCRT.carryCRT(resultMod0, resultMod1, resultMod2, resultSize);
        }
        finally
        {
            unlock();
        }
        return result;
    }

    // Performs a convolution modulo one modulus, of the specified transform length
    private DataStorage convoluteOne(DataStorage x, DataStorage y, long length, int modulus, boolean cached)
        throws ApfloatRuntimeException
    {
        DataStorage tmpY = createCachedDataStorage(length);
        tmpY.copyFrom(y, length);                               // Using a cached data storage here can avoid an extra write
        this.transform.transform(tmpY, modulus);
        tmpY = createDataStorage(tmpY);

        DataStorage tmpX = createCachedDataStorage(length);
        tmpX.copyFrom(x, length);
        this.transform.transform(tmpX, modulus);

        multiplyInPlace(tmpX, tmpY, modulus);

        this.transform.inverseTransform(tmpX, modulus, length);
        tmpX = (cached ? tmpX : createDataStorage(tmpX));

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

        DataStorage result;
        lock(length);
        try
        {
            DataStorage resultMod0 = autoConvoluteOne(x, length, 0, false),
                        resultMod1 = autoConvoluteOne(x, length, 1, false),
                        resultMod2 = autoConvoluteOne(x, length, 2, true);

            result = this.carryCRT.carryCRT(resultMod0, resultMod1, resultMod2, resultSize);
        }
        finally
        {
            unlock();
        }
        return result;
    }

    // Performs a autoconvolution modulo one modulus, of the specified transform length
    private DataStorage autoConvoluteOne(DataStorage x, long length, int modulus, boolean cached)
        throws ApfloatRuntimeException
    {
        DataStorage tmp = createCachedDataStorage(length);
        tmp.copyFrom(x, length);
        this.transform.transform(tmp, modulus);

        squareInPlace(tmp, modulus);

        this.transform.inverseTransform(tmp, modulus, length);
        tmp = (cached ? tmp : createDataStorage(tmp));

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

    private void multiplyInPlace(final DataStorage sourceAndDestination, final DataStorage source, int modulus)
        throws ApfloatRuntimeException
    {
        assert (sourceAndDestination != source);

        final long size = sourceAndDestination.getSize();

        setModulus(MODULUS[modulus]);

        if (size <= Integer.MAX_VALUE && this.parallelRunner != null &&                         // Only if the size fits in an integer, but with memory arrays it should
            sourceAndDestination instanceof RandomAccess && source instanceof RandomAccess)     // Only if the data storage supports efficient parallel random access
        {
            ParallelRunnable parallelRunnable = new ParallelRunnable()
            {
                public int getLength()
                {
                    return (int) size;
                }

                public Runnable getRunnable(int offset, int length)
                {
                    DataStorage subSourceAndDestination = sourceAndDestination.subsequence(offset, length);
                    DataStorage subSource = source.subsequence(offset, length);
                    return new MultiplyInPlaceRunnable(subSourceAndDestination, subSource);
                }
            };

            this.parallelRunner.runParallel(parallelRunnable);
        }
        else
        {
            new MultiplyInPlaceRunnable(sourceAndDestination, source).run();    // Just run in current thread without parallelization
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

    private void squareInPlace(final DataStorage sourceAndDestination, int modulus)
        throws ApfloatRuntimeException
    {
        final long size = sourceAndDestination.getSize();

        setModulus(MODULUS[modulus]);

        if (size <= Integer.MAX_VALUE && this.parallelRunner != null &&     // Only if the size fits in an integer, but with memory arrays it should
            sourceAndDestination instanceof RandomAccess)                   // Only if the data storage supports efficient parallel random access
        {
            ParallelRunnable parallelRunnable = new ParallelRunnable()
            {
                public int getLength()
                {
                    return (int) size;
                }

                public Runnable getRunnable(int offset, int length)
                {
                    DataStorage subSourceAndDestination = sourceAndDestination.subsequence(offset, length);
                    return new SquareInPlaceRunnable(subSourceAndDestination);
                }
            };

            this.parallelRunner.runParallel(parallelRunnable);
        }
        else
        {
            new SquareInPlaceRunnable(sourceAndDestination).run();      // Just run in current thread without parallelization
        }
    }

    private void lock(long length)
    {
        assert(!this.locked);

        if (this.transform instanceof ParallelNTTStrategy)
        {
            ApfloatContext ctx = ApfloatContext.getContext();
            int numberOfProcessors = ctx.getNumberOfProcessors();
            this.parallelRunner = new ParallelRunner(numberOfProcessors);

            ((ParallelNTTStrategy) this.transform).setParallelRunner(parallelRunner);
            this.carryCRT.setParallelRunner(parallelRunner);

            if (length > ctx.getSharedMemoryTreshold() / 4)
            {
                // Data size is big: synchronize on shared memory lock
                Object key = ctx.getSharedMemoryLock();

                this.parallelRunner.lock(key);

                this.locked = true;
            }
        }
    }

    private void unlock()
    {
        if (this.locked)
        {
            this.parallelRunner.unlock();
        }
    }

    private static DataStorage createCachedDataStorage(long size)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        DataStorageBuilder dataStorageBuilder = ctx.getBuilderFactory().getDataStorageBuilder();
        return dataStorageBuilder.createCachedDataStorage(size * 4);
    }

    private static DataStorage createDataStorage(DataStorage dataStorage)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        DataStorageBuilder dataStorageBuilder = ctx.getBuilderFactory().getDataStorageBuilder();
        return dataStorageBuilder.createDataStorage(dataStorage);
    }

    private NTTStrategy transform;
    private FloatCarryCRT carryCRT;
    private ParallelRunner parallelRunner;
    private boolean locked;
}
