package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.DataStorage;

/**
 * Default data storage creation strategy for the <code>double</code> data type.
 *
 * @see DoubleMemoryDataStorage
 * @see DoubleDiskDataStorage
 *
 * @version 1.5.1
 * @author Mikko Tommila
 */

public class DoubleDataStorageBuilder
    implements DataStorageBuilder
{
    /**
     * Default constructor.
     */

    public DoubleDataStorageBuilder()
    {
    }

    public DataStorage createDataStorage(long size)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();

        // Sizes are in bytes
        if (size <= ctx.getMemoryTreshold())
        {
            return new DoubleMemoryDataStorage();
        }
        else
        {
            return new DoubleDiskDataStorage();
        }
    }

    public DataStorage createCachedDataStorage(long size)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();

        // Sizes are in bytes
        if (size <= ctx.getMaxMemoryBlockSize())
        {
            // Use memory data storage if it can fit in memory
            return new DoubleMemoryDataStorage();
        }
        else
        {
            // If it can't fit in memory then still have to use disk data storage
            return new DoubleDiskDataStorage();
        }
    }

    public DataStorage createDataStorage(DataStorage dataStorage)
        throws ApfloatRuntimeException
    {
        if (dataStorage instanceof DoubleMemoryDataStorage)
        {
            long size = dataStorage.getSize();
            ApfloatContext ctx = ApfloatContext.getContext();

            // Sizes are in bytes
            if (size > ctx.getMemoryTreshold())
            {
               // If it is a memory data storage and should be moved to disk then do so
                DataStorage tmp = new DoubleDiskDataStorage();
                tmp.copyFrom(dataStorage);
                dataStorage = tmp;
            }
        }
        return dataStorage;
    }
}
