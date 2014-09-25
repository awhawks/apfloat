package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.ConvolutionStrategy;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.DataStorage;

/**
 * Medium-length convolution strategy.
 * Performs a simple O(n<sup>2</sup>) multiplication when the size of one operand is relatively short.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class LongMediumConvolutionStrategy
    extends LongBaseMath
    implements ConvolutionStrategy, LongRadixConstants
{
    /**
     * Creates a convolution strategy using the specified radix.
     *
     * @param radix The radix that will be used.
     */

    public LongMediumConvolutionStrategy(int radix)
    {
        super(radix);
    }

    public DataStorage convolute(DataStorage x, DataStorage y, long resultSize)
        throws ApfloatRuntimeException
    {
        DataStorage shortStorage, longStorage;

        if (x.getSize() > y.getSize())
        {
            shortStorage = y;
            longStorage = x;
        }
        else
        {
            shortStorage = x;
            longStorage = y;
        }

        long shortSize = shortStorage.getSize(),
             longSize = longStorage.getSize(),
             size = shortSize + longSize;

        if (shortSize > Integer.MAX_VALUE)
        {
            throw new ApfloatRuntimeException("Too long shorter number, size = " + shortSize);
        }

        final int bufferSize = (int) shortSize;

        ApfloatContext ctx = ApfloatContext.getContext();
        DataStorageBuilder dataStorageBuilder = ctx.getBuilderFactory().getDataStorageBuilder();
        DataStorage resultStorage = dataStorageBuilder.createDataStorage(size * 8);
        resultStorage.setSize(size);

        DataStorage.Iterator src = longStorage.iterator(DataStorage.READ, longSize, 0),
                             dst = resultStorage.iterator(DataStorage.WRITE, size, 0),
                             tmpDst = new DataStorage.Iterator()                        // Cyclic iterator
                             {
                                 public void next()
                                 {
                                     position++;
                                     position = (position == bufferSize ? 0 : position);
                                 }

                                 public long getLong()
                                 {
                                     return buffer[position];
                                 }

                                 public void setLong(long value)
                                 {
                                     buffer[position] = value;
                                 }

                                 private long[] buffer = new long[bufferSize];
                                 private int position = 0;
                             };

        for (long i = 0; i < longSize; i++)
        {
            DataStorage.Iterator tmpSrc = shortStorage.iterator(DataStorage.READ, shortSize, 0);        // Sub-optimal: this could be cyclic also

            long factor = src.getLong(),          // Get one word of source data
                    carry = baseMultiplyAdd(tmpSrc, tmpDst, factor, 0, tmpDst, shortSize),
                    result = tmpDst.getLong();       // Least significant word of the result

            dst.setLong(result);     // Store one word of result

            tmpDst.setLong(carry);   // Set carry from calculation as new last word in cyclic buffer

            tmpDst.next();              // Cycle buffer; current first word becomes last
            src.next();
            dst.next();
        }

        // Exhaust last words from temporary cyclic buffer and store them to result data
        for (int i = 0; i < bufferSize; i++)
        {
            long result = tmpDst.getLong();
            dst.setLong(result);

            tmpDst.next();
            dst.next();
        }

        return resultStorage;
    }
}