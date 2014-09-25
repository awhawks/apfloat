package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;

/**
 * Memory based data storage implementation for the <code>long</code>
 * element type.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public final class LongMemoryDataStorage
    extends DataStorage
{
    /**
     * Default constructor.
     */

    public LongMemoryDataStorage()
    {
        this.data = new long[0];
    }

    /**
     * Subsequence constructor.
     *
     * @param longMemoryDataStorage The originating data storage.
     * @param offset The subsequence starting position.
     * @param length The subsequence length.
     */

    protected LongMemoryDataStorage(LongMemoryDataStorage longMemoryDataStorage, long offset, long length)
    {
        super(longMemoryDataStorage, offset, length);
        this.data = longMemoryDataStorage.data;
    }

    protected DataStorage implSubsequence(long offset, long length)
        throws ApfloatRuntimeException
    {
        return new LongMemoryDataStorage(this, offset + getOffset(), length);
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

        this.data = new long[(int) size];

        ApfloatContext ctx = ApfloatContext.getContext();
        int readSize = (int) Math.min(size, dataStorage.getSize()),
            position = 0,
            bufferSize = ctx.getBlockSize() / 8;

        while (readSize > 0)
        {
            int length = (int) Math.min(bufferSize, readSize);

            ArrayAccess arrayAccess = dataStorage.getArray(READ, position, length);
            System.arraycopy(arrayAccess.getLongData(), arrayAccess.getOffset(), this.data, position, length);
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

        long[] newData = new long[newSize];
        System.arraycopy(this.data, 0, newData, 0, Math.min(this.data.length, newSize));
        this.data = newData;
    }

    protected ArrayAccess implGetArray(int mode, long offset, int length)
        throws ApfloatRuntimeException
    {
        return new LongMemoryArrayAccess(this.data, (int) (offset + getOffset()), length);
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
            public long getLong()
                throws IllegalStateException
            {
                checkGet();
                return LongMemoryDataStorage.this.data[(int) getOffset() + (int) getPosition()];
            }

            public void setLong(long value)
                throws IllegalStateException
            {
                checkSet();
                LongMemoryDataStorage.this.data[(int) getOffset() + (int) getPosition()] = value;
            }
        };
    }

    private long[] data;
}
