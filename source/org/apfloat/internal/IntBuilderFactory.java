package org.apfloat.internal;

import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.BuilderFactory;
import org.apfloat.spi.ApfloatBuilder;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.ConvolutionBuilder;
import org.apfloat.spi.NTTBuilder;

/**
 * Factory class for getting instances of the various builder classes needed
 * to build an <code>ApfloatImpl</code> with the <code>int</code> data element type.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class IntBuilderFactory
    implements BuilderFactory
{
    /**
     * Default constructor.
     */

    public IntBuilderFactory()
    {
    }

    public ApfloatBuilder getApfloatBuilder()
    {
        return IntBuilderFactory.apfloatBuilder;
    }

    public DataStorageBuilder getDataStorageBuilder()
    {
        return IntBuilderFactory.dataStorageBuilder;
    }

    public ConvolutionBuilder getConvolutionBuilder()
    {
        return IntBuilderFactory.convolutionBuilder;
    }

    public NTTBuilder getNTTBuilder()
    {
        return IntBuilderFactory.nttBuilder;
    }

    private static ApfloatBuilder apfloatBuilder = new IntApfloatBuilder();
    private static DataStorageBuilder dataStorageBuilder = new IntDataStorageBuilder();
    private static ConvolutionBuilder convolutionBuilder = new IntConvolutionBuilder();
    private static NTTBuilder nttBuilder = new IntNTTBuilder();
}
