package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;

/**
 * Memory based data storage implementation for the <code>double</code>
 * element type.
 *
 * @version 1.1
 * @author Mikko Tommila
 */

public final class DoubleMemoryDataStorage
    extends DataStorage
{
    /**
     * Default constructor.
     */

    public DoubleMemoryDataStorage()
    {
        this.data = new double[0];
    }

    /**
     * Subsequence constructor.
     *
     * @param doubleMemoryDataStorage The originating data storage.
     * @param offset The subsequence starting position.
     * @param length The subsequence length.
     */

    protected DoubleMemoryDataStorage(DoubleMemoryDataStorage doubleMemoryDataStorage, long offset, long length)
    {
        super(doubleMemoryDataStorage, offset, length);
        this.data = doubleMemoryDataStorage.data;
    }

    protected DataStorage implSubsequence(long offset, long length)
        throws ApfloatRuntimeException
    {
        return new DoubleMemoryDataStorage(this, offset + getOffset(), length);
    }

    protected void implCopyFrom(DataStorage dataStorage, long size)
        throws ApfloatRuntimeException
    {
        assert (size > 0);
        assert (!isReadOnly());
        assert (!isSubsequenced());

        if (size > Integer.MAX_VALUE)
        {
            throw new ApfloatRuntimeException("Size too big for memory array: " + size);
        }

        this.data = new double[(int) size];

        ApfloatContext ctx = ApfloatContext.getContext();
        int readSize = (int) Math.min(size, dataStorage.getSize()),
            position = 0,
            bufferSize = ctx.getBlockSize() / 8;

        while (readSize > 0)
        {
            int length = (int) Math.min(bufferSize, readSize);

            ArrayAccess arrayAccess = dataStorage.getArray(READ, position, length);
            System.arraycopy(arrayAccess.getDoubleData(), arrayAccess.getOffset(), this.data, position, length);
            arrayAccess.close();

            readSize -= length;
            position += length;
       }
    }

    protected long implGetSize()
    {
        return this.data.length;
    }

    protected void implSetSize(long size)
        throws ApfloatRuntimeException
    {
        assert (size > 0);
        assert (!isReadOnly());
        assert (!isSubsequenced());

        if (size == this.data.length)
        {
            return;
        }

        if (size > Integer.MAX_VALUE)
        {
            throw new ApfloatRuntimeException("Size too big for memory array: " + size);
        }

        int newSize = (int) size;

        double[] newData = new double[newSize];
        System.arraycopy(this.data, 0, newData, 0, Math.min(this.data.length, newSize));
        this.data = newData;
    }

    protected ArrayAccess implGetArray(int mode, long offset, int length)
        throws ApfloatRuntimeException
    {
        return new DoubleMemoryArrayAccess(this.data, (int) (offset + getOffset()), length);
    }

    protected ArrayAccess implGetTransposedArray(int mode, int startColumn, int columns, int rows)
        throws ApfloatRuntimeException
    {
        throw new ApfloatRuntimeException("Method not implemented - would be sub-optimal; change the apfloat configuration settings");
    }

    public Iterator iterator(int mode, long startPosition, long endPosition)
        throws IllegalArgumentException, IllegalStateException, ApfloatRuntimeException
    {
        return new AbstractIterator(mode, startPosition, endPosition)
        {
            public double getDouble()
                throws IllegalStateException
            {
                checkGet();
                return DoubleMemoryDataStorage.this.data[(int) getOffset() + (int) getPosition()];
            }

            public void setDouble(double value)
                throws IllegalStateException
            {
                checkSet();
                DoubleMemoryDataStorage.this.data[(int) getOffset() + (int) getPosition()] = value;
            }
        };
    }

    private static final long serialVersionUID = 5093781604796636929L;

    private double[] data;
}
