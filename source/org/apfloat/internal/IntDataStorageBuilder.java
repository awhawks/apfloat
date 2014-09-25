package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.DataStorage;

/**
 * Default data storage creation strategy for the <code>int</code> data type.
 *
 * @see IntMemoryDataStorage
 * @see IntDiskDataStorage
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class IntDataStorageBuilder
    implements DataStorageBuilder
{
    /**
     * Default constructor.
     */

    public IntDataStorageBuilder()
    {
    }

    public DataStorage createDataStorage(long size)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();

        // Sizes are in bytes
        if (size <= ctx.getMemoryTreshold())
        {
            return new IntMemoryDataStorage();
        }
        else
        {
            return new IntDiskDataStorage();
        }
    }
}
