package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;

/**
 * Memory based data storage implementation for the <code>int</code>
 * element type.
 *
 * @version 1.8.0
 * @author Mikko Tommila
 */

public class IntMemoryDataStorage
    extends DataStorage
{
    /**
     * Default constructor.
     */

    public IntMemoryDataStorage()
    {
        this.data = new int[0];
    }

    /**
     * Subsequence constructor.
     *
     * @param intMemoryDataStorage The originating data storage.
     * @param offset The subsequence starting position.
     * @param length The subsequence length.
     */

    protected IntMemoryDataStorage(IntMemoryDataStorage intMemoryDataStorage, long offset, long length)
    {
        super(intMemoryDataStorage, offset, length);
        this.data = intMemoryDataStorage.data;
    }

    public boolean isCached()
    {
        return true;
    }

    protected DataStorage implSubsequence(long offset, long length)
        throws ApfloatRuntimeException
    {
        return new IntMemoryDataStorage(this, offset + getOffset(), length);
    }

    protected void implCopyFrom(DataStorage dataStorage, long size)
        throws ApfloatRuntimeException
    {
        assert (size > 0);
        assert (!isReadOnly());
        assert (!isSubsequenced());

        if (size > Integer.MAX_VALUE)
        {
            throw new ApfloatInternalException("Size too big for memory array: " + size);
        }

        if (dataStorage == this)
        {
            setSize(size);
            return;
        }

        this.data = new int[(int) size];

        ApfloatContext ctx = ApfloatContext.getContext();
        int readSize = (int) Math.min(size, dataStorage.getSize()),
            position = 0,
            bufferSize = ctx.getBlockSize() / 4;

        while (readSize > 0)
        {
            int length = (int) Math.min(bufferSize, readSize);

            ArrayAccess arrayAccess = dataStorage.getArray(READ, position, length);
            System.arraycopy(arrayAccess.getIntData(), arrayAccess.getOffset(), this.data, position, length);
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
            throw new ApfloatInternalException("Size too big for memory array: " + size);
        }

        int newSize = (int) size;

        int[] newData = new int[newSize];
        System.arraycopy(this.data, 0, newData, 0, Math.min(this.data.length, newSize));
        this.data = newData;
    }

    protected ArrayAccess implGetArray(int mode, long offset, int length)
        throws ApfloatRuntimeException
    {
        return new IntMemoryArrayAccess(this.data, (int) (offset + getOffset()), length);
    }

    protected ArrayAccess implGetArray(int mode, int startColumn, int columns, int rows)
        throws ApfloatRuntimeException
    {
        throw new ApfloatInternalException("Method not implemented - would be sub-optimal; change the apfloat configuration settings");
    }

    protected ArrayAccess implGetTransposedArray(int mode, int startColumn, int columns, int rows)
        throws ApfloatRuntimeException
    {
        throw new ApfloatInternalException("Method not implemented - would be sub-optimal; change the apfloat configuration settings");
    }

    private class ReadWriteIterator
        extends AbstractIterator
    {
        public ReadWriteIterator(long startPosition, long endPosition)
            throws IllegalArgumentException, IllegalStateException, ApfloatRuntimeException
        {
            this(READ_WRITE, startPosition, endPosition);
        }

        protected ReadWriteIterator(int mode, long startPosition, long endPosition)
            throws IllegalArgumentException, IllegalStateException, ApfloatRuntimeException
        {
            super(mode, startPosition, endPosition);

            this.data = IntMemoryDataStorage.this.data;

            this.position = (int) getPosition() + (int) getOffset();
            this.length = (int) getLength();
        }

        public boolean hasNext()
        {
            return (this.length > 0);
        }

        public void next()
            throws IllegalStateException
        {
            checkLength();
            this.position += getIncrement();
            this.length--;
        }

        public int getInt()
            throws IllegalStateException
        {
            checkLength();
            return this.data[this.position];
        }

        public void setInt(int value)
            throws IllegalStateException
        {
            checkLength();
            this.data[this.position] = value;
        }

        public <T> T get(Class<T> type)
            throws UnsupportedOperationException, IllegalStateException
        {
            if (!(type.equals(Integer.TYPE)))
            {
                throw new UnsupportedOperationException("Unsupported data type " + type.getCanonicalName() + ", the only supported type is int");
            }
            @SuppressWarnings("unchecked")
            T value = (T) (Integer) getInt();
            return value;
        }

        public <T> void set(Class<T> type, T value)
            throws UnsupportedOperationException, IllegalArgumentException, IllegalStateException
        {
            if (!(type.equals(Integer.TYPE)))
            {
                throw new UnsupportedOperationException("Unsupported data type " + type.getCanonicalName() + ", the only supported type is int");
            }
            if (!(value instanceof Integer))
            {
                throw new IllegalArgumentException("Unsupported value type " + value.getClass().getCanonicalName() + ", the only supported type is Integer");
            }
            setInt((Integer) value);
        }

        protected void checkLength()
            throws IllegalStateException
        {
            if (this.length == 0)
            {
                throw new IllegalStateException("At the end of iterator");
            }
        }

        private static final long serialVersionUID = 6881950853858664947L;

        private int[] data;
        private int position,
                    length;
    }

    private class ReadOnlyIterator
        extends ReadWriteIterator
    {
        public ReadOnlyIterator(long startPosition, long endPosition)
            throws IllegalArgumentException, IllegalStateException, ApfloatRuntimeException
        {
            super(READ, startPosition, endPosition);
        }

        public void setInt(int value)
            throws IllegalStateException
        {
            throw new IllegalStateException("Not a writable iterator");
        }

        private static final long serialVersionUID = -6709295918596292159L;
    }

    private class WriteOnlyIterator
        extends ReadWriteIterator
    {
        public WriteOnlyIterator(long startPosition, long endPosition)
            throws IllegalArgumentException, IllegalStateException, ApfloatRuntimeException
        {
            super(WRITE, startPosition, endPosition);
        }

        public int getInt()
            throws IllegalStateException
        {
            throw new IllegalStateException("Not a readable iterator");
        }

        private static final long serialVersionUID = 2753806793669098570L;
    }

    public Iterator iterator(int mode, long startPosition, long endPosition)
        throws IllegalArgumentException, IllegalStateException, ApfloatRuntimeException
    {
        Iterator iterator;
        switch (mode & READ_WRITE)
        {
            case READ:
                iterator = new ReadOnlyIterator(startPosition, endPosition);
                break;
            case WRITE:
                iterator = new WriteOnlyIterator(startPosition, endPosition);
                break;
            case READ_WRITE:
                iterator = new ReadWriteIterator(startPosition, endPosition);
                break;
            default:
                throw new IllegalArgumentException("Illegal mode: " + mode);
        }
        return iterator;
    }

    private static final long serialVersionUID = 4034264499027294039L;

    private int[] data;
}
