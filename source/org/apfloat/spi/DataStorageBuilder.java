package org.apfloat.spi;

import org.apfloat.ApfloatRuntimeException;

/**
 * Interface for determining a suitable storage
 * type for data of some expected size. The factory method
 * pattern is used for creating the data storages.<p>
 *
 * The storage type can be different based on the size of the
 * data. For example, it may be beneficial to store small amounts
 * of data always in memory, for small overhead in access times,
 * and to store larger objects on disk files, to avoid running
 * out of memory.<p>
 *
 * Further, an implementing class may provide data storage objects
 * that store data in disk files, for Java client applications, or
 * e.g. in a relational database, for an EJB server environment
 * where files are not allowed to be used.
 *
 * @see DataStorage
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public interface DataStorageBuilder
{
    /**
     * Get an appropriate type of data storage for the requested size of data.<p>
     *
     * Note that the returned data storage object is not set to have the
     * requested size, so the client should call the object's {@link DataStorage#setSize(long)}
     * method before storing data to it.
     *
     * @param size The size of data to be stored in the storage, in bytes.
     *
     * @return An empty <code>DataStorage</code> object of an appropriate type for storing <code>size</code> bytes of data.
     */

    DataStorage createDataStorage(long size)
        throws ApfloatRuntimeException;
}
