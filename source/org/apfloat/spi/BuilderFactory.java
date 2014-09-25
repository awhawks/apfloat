package org.apfloat.spi;

/**
 * A <code>BuilderFactory</code> object contains factory methods for building
 * the various parts of an apfloat using the Builder pattern. There
 * is no separate "director" object in the apfloat SPI; it is suggested
 * that the <code>ApfloatImpl</code> implementation itself acts as the director,
 * calling the different builders directly.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public interface BuilderFactory
{
    /**
     * Returns an <code>ApfloatBuilder</code> object.
     *
     * @return An <code>ApfloatBuilder</code> object.
     */

    public ApfloatBuilder getApfloatBuilder();

    /**
     * Returns a <code>DataStorageBuilder</code> object.
     *
     * @return A <code>DataStorageBuilder</code> object.
     */

    public DataStorageBuilder getDataStorageBuilder();

    /**
     * Returns a <code>ConvolutionBuilder</code> object.
     *
     * @return A <code>ConvolutionBuilder</code> object.
     */

    public ConvolutionBuilder getConvolutionBuilder();

    /**
     * Returns an <code>NTTBuilder</code> object.
     *
     * @return An <code>NTTBuilder</code> object.
     */

    public NTTBuilder getNTTBuilder();
}
