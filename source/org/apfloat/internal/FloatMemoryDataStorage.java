package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;

/**
 * Memory based data storage implementation for the <code>float</code>
 * element type.
 *
 * @version 1.1
 * @author Mikko Tommila
 */

public final class FloatMemoryDataStorage
    extends DataStorage
{
    /**
     * Default constructor.
     */

    public FloatMemoryDataStorage()
    {
        this.data = new float[0];
    }

    /**
     * Subsequence constructor.
     *
     * @param floatMemoryDataStorage The originating data storage.
     * @param offset The subsequence starting position.
     * @param length The subsequence length.
     */

    protected FloatMemoryDataStorage(FloatMemoryDataStorage floatMemoryDataStorage, long offset, long length)
    {
        super(floatMemoryDataStorage, offset, length);
        this.data = floatMemoryDataStorage.data;
    }

    protected DataStorage implSubsequence(long offset, long length)
        throws ApfloatRuntimeException
    {
        return new FloatMemoryDataStorage(this, offset + getOffset(), length);
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

        this.data = new float[(int) size];

        ApfloatContext ctx = ApfloatContext.getContext();
        int readSize = (int) Math.min(size, dataStorage.getSize()),
            position = 0,
            bufferSize = ctx.getBlockSize() / 4;

        while (readSize > 0)
        {
            int length = (int) Math.min(bufferSize, readSize);

            ArrayAccess arrayAccess = dataStorage.getArray(READ, position, length);
            System.arraycopy(arrayAccess.getFloatData(), arrayAccess.getOffset(), this.data, position, length);
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

        float[] newData = new float[newSize];
        System.arraycopy(this.data, 0, newData, 0, Math.min(this.data.length, newSize));
        this.data = newData;
    }

    protected ArrayAccess implGetArray(int mode, long offset, int length)
        throws ApfloatRuntimeException
    {
        return new FloatMemoryArrayAccess(this.data, (int) (offset + getOffset()), length);
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
            public float getFloat()
                throws IllegalStateException
            {
                checkGet();
                return FloatMemoryDataStorage.this.data[(int) getOffset() + (int) getPosition()];
            }

            public void setFloat(float value)
                throws IllegalStateException
            {
                checkSet();
                FloatMemoryDataStorage.this.data[(int) getOffset() + (int) getPosition()] = value;
            }
        };
    }

    private static final long serialVersionUID = -862001153825924236L;

    private float[] data;
}
