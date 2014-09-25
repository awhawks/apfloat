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
 * @version 1.0
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
}
