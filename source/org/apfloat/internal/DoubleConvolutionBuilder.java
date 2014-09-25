package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;
import org.apfloat.spi.ConvolutionBuilder;
import org.apfloat.spi.ConvolutionStrategy;
import org.apfloat.spi.NTTBuilder;
import org.apfloat.spi.NTTStrategy;
import org.apfloat.spi.Util;

/**
 * Creates convolutions of suitable type for the specified length for the <code>double</code> type.<p>
 *
 * Based on a work estimate, depending on the operand sizes, the O(n<sup>2</sup>)
 * "schoolboy" and the NTT algorithms are chosen e.g. as follows:<p>
 *
 * <table border="1">
 * <tr><th>size1</th><th>size2</th><th>Algorithm</th></tr>
 * <tr><td>16</td><td>16</td><td>Schoolboy</td></tr>
 * <tr><td>16</td><td>256</td><td>Schoolboy</td></tr>
 * <tr><td>32</td><td>32</td><td>Schoolboy</td></tr>
 * <tr><td>32</td><td>256</td><td>Schoolboy</td></tr>
 * <tr><td>64</td><td>64</td><td>NTT</td></tr>
 * <tr><td>64</td><td>256</td><td>NTT</td></tr>
 * <tr><td>64</td><td>65536</td><td>Schoolboy</td></tr>
 * <tr><td>128</td><td>128</td><td>NTT</td></tr>
 * <tr><td>128</td><td>65536</td><td>NTT</td></tr>
 * <tr><td>128</td><td>4294967296</td><td>Schoolboy</td></tr>
 * <tr><td>256</td><td>256</td><td>NTT</td></tr>
 * <tr><td>256</td><td>281474976710656</td><td>NTT</td></tr>
 * </table>
 *
 * @see DoubleShortConvolutionStrategy
 * @see DoubleMediumConvolutionStrategy
 * @see Double3NTTConvolutionStrategy
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class DoubleConvolutionBuilder
    implements ConvolutionBuilder
{
    /**
     * Default constructor.
     */

    public DoubleConvolutionBuilder()
    {
    }

    public ConvolutionStrategy createConvolution(int radix, long size1, long size2, long resultSize)
    {
        long minSize = Math.min(size1, size2),
             maxSize = Math.max(size1, size2),
             totalSize = size1 + size2;

        if (minSize == 1)
        {
            return new DoubleShortConvolutionStrategy(radix);
        }
        else if ((float) minSize * maxSize < 4.1f * totalSize * Util.log2down(totalSize))       // Optimize a*(n+m)*log(n+m) vs. b*n*m
        {
            return new DoubleMediumConvolutionStrategy(radix);
        }
        else
        {
            ApfloatContext ctx = ApfloatContext.getContext();
            NTTBuilder nttBuilder = ctx.getBuilderFactory().getNTTBuilder();
            NTTStrategy transform = nttBuilder.createNTT(totalSize);

            return new Double3NTTConvolutionStrategy(radix, transform);
        }
    }
}
