package org.apfloat.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;

/**
 * Disk-based data storage for the <code>int</code> element type.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public final class IntDiskDataStorage
    extends DiskDataStorage
{
    /**
     * Default constructor.
     */

    public IntDiskDataStorage()
        throws ApfloatRuntimeException
    {
    }

    /**
     * Subsequence constructor.
     *
     * @param intDiskDataStorage The originating data storage.
     * @param offset The subsequence starting position.
     * @param length The subsequence length.
     */

    protected IntDiskDataStorage(IntDiskDataStorage intDiskDataStorage, long offset, long length)
    {
        super(intDiskDataStorage, offset, length);
    }

    protected DataStorage implSubsequence(long offset, long length)
        throws ApfloatRuntimeException
    {
        return new IntDiskDataStorage(this, offset + getOffset(), length);
    }

    private class IntDiskArrayAccess
        extends IntMemoryArrayAccess
    {
        // fileOffset is absolute position in file
        public IntDiskArrayAccess(int mode, long fileOffset, int length)
            throws ApfloatRuntimeException
        {
            super(new int[length], 0, length);
            this.mode = mode;
            this.fileOffset = fileOffset;

            if ((mode & READ) != 0)
            {
                final int[] array = getIntData();
                WritableByteChannel out = new WritableByteChannel()
                {
                    public int write(ByteBuffer buffer)
                    {
                        IntBuffer src = buffer.asIntBuffer();
                        //int readLength = src.remaining();
                        int readLength = Math.min(src.remaining(), array.length - readPosition);

                        src.get(array, readPosition, readLength);

                        readPosition += readLength;
                        buffer.position(readLength * 4);

                        return readLength * 4;
                    }

                    public void close() {}
                    public boolean isOpen() { return true; }

                    private int readPosition = 0;
                };

                transferTo(out, fileOffset * 4, (long) length * 4);
            }
        }

        public void close()
            throws ApfloatRuntimeException
        {
            if ((this.mode & WRITE) != 0 && getData() != null)
            {
                final int[] array = getIntData();
                ReadableByteChannel in = new ReadableByteChannel()
                {
                    public int read(ByteBuffer buffer)
                    {
                        IntBuffer dst = buffer.asIntBuffer();
                        //int writeLength = dst.remaining();
                        int writeLength = Math.min(dst.remaining(), array.length - writePosition);

                        dst.put(array, writePosition, writeLength);

                        writePosition += writeLength;
                        buffer.position(writeLength * 4);

                        return writeLength * 4;
                    }

                    public void close() {}
                    public boolean isOpen() { return true; }

                    private int writePosition = 0;
                };

                transferFrom(in, this.fileOffset * 4, (long) array.length * 4);
            }

            super.close();
        }

        private int mode;
        private long fileOffset;
    }

    protected ArrayAccess implGetArray(int mode, long offset, int length)
        throws ApfloatRuntimeException
    {
        return new IntDiskArrayAccess(mode, getOffset() + offset, length);
    }

    private class TransposedMemoryArrayAccess
        extends IntMemoryArrayAccess
    {
        public TransposedMemoryArrayAccess(int mode, int[] data, int startColumn, int columns, int rows)
        {
            super(data, 0, data.length);
            this.mode = mode;
            this.startColumn = startColumn;
            this.columns = columns;
            this.rows = rows;
        }

        public void close()
            throws ApfloatRuntimeException
        {
            if ((this.mode & WRITE) != 0 && getData() != null)
            {
                setTransposedArray(this, this.startColumn, this.columns, this.rows);
            }
            super.close();
        }

        private int mode,
                    startColumn,
                    columns,
                    rows;
    }

    protected synchronized ArrayAccess implGetTransposedArray(int mode, int startColumn, int columns, int rows)
        throws ApfloatRuntimeException
    {
        int width = (int) (getSize() / rows);

        if (columns != (columns & -columns) || rows != (rows & -rows) || startColumn + columns > width)
        {
            throw new ApfloatRuntimeException("Invalid size");
        }

        int blockSize = columns * rows,
            b = columns;
        ArrayAccess arrayAccess = new TransposedMemoryArrayAccess(mode, new int[blockSize], startColumn, columns, rows);

        if ((mode & READ) != 0)
        {
            // Read the data from the input file in b x b blocks

            long readPosition = startColumn;
            for (int i = 0; i < rows; i += b)
            {
                int writePosition = i;

                for (int j = 0; j < b; j++)
                {
                    readToArray(readPosition, arrayAccess, writePosition, b);

                    readPosition += width;
                    writePosition += rows;
                }

                // Transpose the b x b block

                ArrayAccess subArrayAccess = arrayAccess.subsequence(i, blockSize - i);
                IntMatrix.transposeSquare(subArrayAccess, b, rows);
            }
        }

        return arrayAccess;
    }

    // Write the data back to the same location in file as retrieved with getTransposedArray()
    private synchronized void setTransposedArray(ArrayAccess arrayAccess, int startColumn, int columns, int rows)
        throws ApfloatRuntimeException
    {
        int width = (int) (getSize() / rows);

        int blockSize = arrayAccess.getLength(),
            b = columns;

        long writePosition = startColumn;
        for (int i = 0; i < rows; i += b)
        {
            int readPosition = i;

            // Transpose the b x b block

            ArrayAccess subArrayAccess = arrayAccess.subsequence(i, blockSize - i);
            IntMatrix.transposeSquare(subArrayAccess, b, rows);

            for (int j = 0; j < b; j++)
            {
                writeFromArray(arrayAccess, readPosition, writePosition, b);

                readPosition += rows;
                writePosition += width;
            }
        }
    }

    private void readToArray(long readPosition, ArrayAccess arrayAccess, int writePosition, int length)
        throws ApfloatRuntimeException
    {
        ArrayAccess readArrayAccess = getArray(READ, readPosition, length);
        System.arraycopy(readArrayAccess.getData(), readArrayAccess.getOffset(), arrayAccess.getData(), arrayAccess.getOffset() + writePosition, length);
        readArrayAccess.close();
    }

    private void writeFromArray(ArrayAccess arrayAccess, int readPosition, long writePosition, int length)
        throws ApfloatRuntimeException
    {
        ArrayAccess writeArrayAccess = getArray(WRITE, writePosition, length);
        System.arraycopy(arrayAccess.getData(), arrayAccess.getOffset() + readPosition, writeArrayAccess.getData(), writeArrayAccess.getOffset(), length);
        writeArrayAccess.close();
    }

    private class BlockIterator
        extends AbstractIterator
    {
        public BlockIterator(int mode, long startPosition, long endPosition)
            throws IllegalArgumentException, IllegalStateException, ApfloatRuntimeException
        {
            super(mode, startPosition, endPosition);
            this.arrayAccess = null;
            this.remaining = 0;
        }

        public void next()
            throws IllegalStateException, ApfloatRuntimeException
        {
            checkLength();

            assert (this.remaining > 0);

            checkAvailable();

            this.offset += getIncrement();
            this.remaining--;

            if (this.remaining == 0)
            {
                close();
            }

            super.next();
        }

        public int getInt()
            throws IllegalStateException, ApfloatRuntimeException
        {
            checkGet();
            checkAvailable();
            return this.data[this.offset];
        }

        public void setInt(int value)
            throws IllegalStateException, ApfloatRuntimeException
        {
            checkSet();
            checkAvailable();
            this.data[this.offset] = value;
        }

        /**
         * Closes the iterator. This needs to be called only if the
         * iterator is not iterated to the end.
         */

        public void close()
            throws ApfloatRuntimeException
        {
            if (this.arrayAccess != null)
            {
                this.data = null;
                this.arrayAccess.close();
                this.arrayAccess = null;
            }
        }

        private void checkAvailable()
            throws ApfloatRuntimeException
        {
            if (this.arrayAccess == null)
            {
                boolean isForward = (getIncrement() > 0);
                int length = (int) Math.min(getLength(), getBlockSize() / 4);
                long offset = (isForward ? getPosition() : getPosition() - length + 1);

                this.arrayAccess = getArray(getMode(), offset, length);
                this.data = this.arrayAccess.getIntData();
                this.offset = this.arrayAccess.getOffset() + (isForward ? 0 : length - 1);
                this.remaining = length;
            }
        }

        private ArrayAccess arrayAccess;
        private int[] data;
        private int offset,
                    remaining;
    }

    public Iterator iterator(int mode, long startPosition, long endPosition)
        throws IllegalArgumentException, IllegalStateException, ApfloatRuntimeException
    {
        return new BlockIterator(mode, startPosition, endPosition);
    }

    protected int getUnitSize()
    {
        return 4;
    }
}
