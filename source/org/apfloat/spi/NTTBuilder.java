package org.apfloat.spi;

/**
 * Interface of a factory for creating Number Theoretic Transforms.
 * The factory method pattern is used.
 *
 * @see NTTStrategy
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public interface NTTBuilder
{
    /**
     * Creates a Number Theoretic Transform of suitable
     * type for the specified length.
     *
     * @param size The transform length that will be used.
     *
     * @return A suitable NTT object for performing the transform.
     */

    public NTTStrategy createNTT(long size);
}
