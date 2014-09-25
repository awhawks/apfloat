package org.apfloat.internal;

import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.BuilderFactory;
import org.apfloat.spi.ApfloatBuilder;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.ConvolutionBuilder;
import org.apfloat.spi.NTTBuilder;

/**
 * Factory class for getting instances of the various builder classes needed
 * to build an <code>ApfloatImpl</code> with the <code>float</code> data element type.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class FloatBuilderFactory
    implements BuilderFactory
{
    /**
     * Default constructor.
     */

    public FloatBuilderFactory()
    {
    }

    public ApfloatBuilder getApfloatBuilder()
    {
        return FloatBuilderFactory.apfloatBuilder;
    }

    public DataStorageBuilder getDataStorageBuilder()
    {
        return FloatBuilderFactory.dataStorageBuilder;
    }

    public ConvolutionBuilder getConvolutionBuilder()
    {
        return FloatBuilderFactory.convolutionBuilder;
    }

    public NTTBuilder getNTTBuilder()
    {
        return FloatBuilderFactory.nttBuilder;
    }

    private static ApfloatBuilder apfloatBuilder = new FloatApfloatBuilder();
    private static DataStorageBuilder dataStorageBuilder = new FloatDataStorageBuilder();
    private static ConvolutionBuilder convolutionBuilder = new FloatConvolutionBuilder();
    private static NTTBuilder nttBuilder = new FloatNTTBuilder();
}
