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
 * @version 1.0
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
}
