package org.apfloat.internal;

import org.apfloat.ApfloatRuntimeException;

import org.apfloat.spi.BuilderFactory;
import org.apfloat.spi.ApfloatBuilder;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.ConvolutionBuilder;
import org.apfloat.spi.NTTBuilder;

/**
 * Factory class for getting instances of the various builder classes needed
 * to build an <code>ApfloatImpl</code> with the <code>double</code> data element type.
 *
 * @version 1.6.2
 * @author Mikko Tommila
 */

public class DoubleBuilderFactory
    implements BuilderFactory
{
    /**
     * Default constructor.
     */

    public DoubleBuilderFactory()
    {
    }

    public ApfloatBuilder getApfloatBuilder()
    {
        return DoubleBuilderFactory.apfloatBuilder;
    }

    public DataStorageBuilder getDataStorageBuilder()
    {
        return DoubleBuilderFactory.dataStorageBuilder;
    }

    public ConvolutionBuilder getConvolutionBuilder()
    {
        return DoubleBuilderFactory.convolutionBuilder;
    }

    public NTTBuilder getNTTBuilder()
    {
        return DoubleBuilderFactory.nttBuilder;
    }

    public void shutdown()
        throws ApfloatRuntimeException
    {
        DiskDataStorage.cleanUp();
    }

    public void gc()
        throws ApfloatRuntimeException
    {
        System.gc();
        System.gc();
        System.runFinalization();
        DiskDataStorage.gc();
    }

    private static ApfloatBuilder apfloatBuilder = new DoubleApfloatBuilder();
    private static DataStorageBuilder dataStorageBuilder = new DoubleDataStorageBuilder();
    private static ConvolutionBuilder convolutionBuilder = new DoubleConvolutionBuilder();
    private static NTTBuilder nttBuilder = new DoubleNTTBuilder();
}
