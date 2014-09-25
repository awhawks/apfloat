package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.DataStorage;

/**
 * Default data storage creation strategy for the <code>float</code> data type.
 *
 * @see FloatMemoryDataStorage
 * @see FloatDiskDataStorage
 *
 * @version 1.5.1
 * @author Mikko Tommila
 */

public class FloatDataStorageBuilder
    implements DataStorageBuilder
{
    /**
     * Default constructor.
     */

    public FloatDataStorageBuilder()
    {
    }

    public DataStorage createDataStorage(long size)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();

        // Sizes are in bytes
        if (size <= ctx.getMemoryTreshold())
        {
            return new FloatMemoryDataStorage();
        }
        else
        {
            return new FloatDiskDataStorage();
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
            return new FloatMemoryDataStorage();
        }
        else
        {
            // If it can't fit in memory then still have to use disk data storage
            return new FloatDiskDataStorage();
        }
    }

    public DataStorage createDataStorage(DataStorage dataStorage)
        throws ApfloatRuntimeException
    {
        if (dataStorage instanceof FloatMemoryDataStorage)
        {
            long size = dataStorage.getSize();
            ApfloatContext ctx = ApfloatContext.getContext();

            // Sizes are in bytes
            if (size > ctx.getMemoryTreshold())
            {
               // If it is a memory data storage and should be moved to disk then do so
                DataStorage tmp = new FloatDiskDataStorage();
                tmp.copyFrom(dataStorage);
                dataStorage = tmp;
            }
        }
        return dataStorage;
    }
}
