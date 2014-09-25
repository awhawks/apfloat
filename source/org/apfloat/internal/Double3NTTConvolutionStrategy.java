package org.apfloat.internal;

import java.util.RandomAccess;

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
 * convolution and the Chinese Remainder Theorem to get the final result.<p>
 *
 * After transforming the data, the in-place multiplication (or squaring)
 * of the data elements is done using a parallel algorithm, if the data
 * fits in memory.<p>
 *
 * All access to this class must be externally synchronized.
 *
 * @version 1.6.1
 * @author Mikko Tommila
 */

public class Double3NTTConvolutionStrategy
    extends DoubleModMath
    implements ConvolutionStrategy
{
    // Runnable for multiplying elements in place
    private class MultiplyInPlaceRunnable
        implements Runnable
    {
        public MultiplyInPlaceRunnable(DataStorage sourceAndDestination, DataStorage source, long offset, long length)
        {
            this.sourceAndDestination = sourceAndDestination;
            this.source = source;
            this.offset = offset;
            this.length = length;
        }

        public void run()
        {
            DataStorage.Iterator dest = this.sourceAndDestination.iterator(DataStorage.READ_WRITE, this.offset, this.offset + this.length),
                                 src = this.source.iterator(DataStorage.READ, this.offset, this.offset + this.length);

            while (this.length > 0)
            {
                dest.setDouble(modMultiply(dest.getDouble(), src.getDouble()));

                dest.next();
                src.next();
                this.length--;
            }
        }

        private DataStorage sourceAndDestination,
                            source;
        private long offset,
                     length;
    }

    // Runnable for squaring elements in place
    private class SquareInPlaceRunnable
        implements Runnable
    {
        public SquareInPlaceRunnable(DataStorage sourceAndDestination, long offset, long length)
        {
            this.sourceAndDestination = sourceAndDestination;
            this.offset = offset;
            this.length = length;
        }

        public void run()
        {
            DataStorage.Iterator iterator = this.sourceAndDestination.iterator(DataStorage.READ_WRITE, this.offset, this.offset + this.length);

            while (this.length > 0)
            {
                double value = iterator.getDouble();
                iterator.setDouble(modMultiply(value, value));

                iterator.next();
                this.length--;
            }
        }

        private DataStorage sourceAndDestination;
        private long offset,
                     length;
    }

    /**
     * Creates a new convoluter that uses the specified
     * transform for transforming the data.
     *
     * @param radix The radix that will be used.
     * @param transform The transform that will be used.
     */

    public Double3NTTConvolutionStrategy(int radix, NTTStrategy transform)
    {
        this.transform = transform;
        this.carryCRT = new DoubleCarryCRT(radix);
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
                    return new MultiplyInPlaceRunnable(sourceAndDestination, source, offset, length);
                }
            };

            this.parallelRunner.runParallel(parallelRunnable);
        }
        else
        {
            new MultiplyInPlaceRunnable(sourceAndDestination, source, 0, size).run();   // Just run in current thread without parallelization
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
                    return new SquareInPlaceRunnable(sourceAndDestination, offset, length);
                }
            };

            this.parallelRunner.runParallel(parallelRunnable);
        }
        else
        {
            new SquareInPlaceRunnable(sourceAndDestination, 0, size).run(); // Just run in current thread without parallelization
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

            if (length > ctx.getSharedMemoryTreshold() / 8)
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
        return dataStorageBuilder.createCachedDataStorage(size * 8);
    }

    private static DataStorage createDataStorage(DataStorage dataStorage)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        DataStorageBuilder dataStorageBuilder = ctx.getBuilderFactory().getDataStorageBuilder();
        return dataStorageBuilder.createDataStorage(dataStorage);
    }

    private NTTStrategy transform;
    private DoubleCarryCRT carryCRT;
    private ParallelRunner parallelRunner;
    private boolean locked;
}
