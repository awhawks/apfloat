package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.DataStorage;

/**
 * Default data storage creation strategy for the <code>long</code> data type.
 *
 * @see LongMemoryDataStorage
 * @see LongDiskDataStorage
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class LongDataStorageBuilder
    implements DataStorageBuilder
{
    /**
     * Default constructor.
     */

    public LongDataStorageBuilder()
    {
    }

    public DataStorage createDataStorage(long size)
        throws ApfloatRuntimeException
    {
        ApfloatContext ctx = ApfloatContext.getContext();

        // Sizes are in bytes
        if (size <= ctx.getMemoryTreshold())
        {
            return new LongMemoryDataStorage();
        }
        else
        {
            return new LongDiskDataStorage();
        }
    }
}
