package org.apfloat.internal;

import org.apfloat.spi.BuilderFactory;
import org.apfloat.spi.ApfloatBuilder;
import org.apfloat.spi.DataStorageBuilder;
import org.apfloat.spi.ConvolutionBuilder;
import org.apfloat.spi.NTTBuilder;

/**
 * Factory class for getting instances of the various builder classes needed
 * to build an <code>ApfloatImpl</code> with the <code>long</code> data element type.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class LongBuilderFactory
    implements BuilderFactory
{
    /**
     * Default constructor.
     */

    public LongBuilderFactory()
    {
    }

    public ApfloatBuilder getApfloatBuilder()
    {
        return LongBuilderFactory.apfloatBuilder;
    }

    public DataStorageBuilder getDataStorageBuilder()
    {
        return LongBuilderFactory.dataStorageBuilder;
    }

    public ConvolutionBuilder getConvolutionBuilder()
    {
        return LongBuilderFactory.convolutionBuilder;
    }

    public NTTBuilder getNTTBuilder()
    {
        return LongBuilderFactory.nttBuilder;
    }

    private static ApfloatBuilder apfloatBuilder = new LongApfloatBuilder();
    private static DataStorageBuilder dataStorageBuilder = new LongDataStorageBuilder();
    private static ConvolutionBuilder convolutionBuilder = new LongConvolutionBuilder();
    private static NTTBuilder nttBuilder = new LongNTTBuilder();
}
