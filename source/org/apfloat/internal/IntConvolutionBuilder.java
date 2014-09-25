package org.apfloat.internal;

import org.apfloat.ApfloatContext;
import org.apfloat.spi.ConvolutionBuilder;
import org.apfloat.spi.ConvolutionStrategy;
import org.apfloat.spi.NTTBuilder;
import org.apfloat.spi.NTTStrategy;
import org.apfloat.spi.Util;
import static org.apfloat.internal.IntConstants.*;

/**
 * Creates convolutions of suitable type for the specified length for the <code>int</code> type.<p>
 *
 * Based on a work estimate, depending on the operand sizes, the O(n<sup>2</sup>)
 * long multiplication, Karatsuba multiplication and the NTT algorithms are chosen e.g. as follows:<p>
 *
 * <table border="1">
 * <tr><th>size1</th><th>size2</th><th>Algorithm</th></tr>
 * <tr><td>16</td><td>16</td><td>Long</td></tr>
 * <tr><td>16</td><td>256</td><td>Long</td></tr>
 * <tr><td>32</td><td>32</td><td>Long</td></tr>
 * <tr><td>32</td><td>256</td><td>Long</td></tr>
 * <tr><td>64</td><td>64</td><td>Karatsuba</td></tr>
 * <tr><td>64</td><td>256</td><td>NTT</td></tr>
 * <tr><td>64</td><td>65536</td><td>Karatsuba</td></tr>
 * <tr><td>128</td><td>128</td><td>NTT</td></tr>
 * <tr><td>128</td><td>65536</td><td>NTT</td></tr>
 * <tr><td>128</td><td>4294967296</td><td>Karatsuba</td></tr>
 * <tr><td>256</td><td>256</td><td>NTT</td></tr>
 * <tr><td>256</td><td>4294967296</td><td>Karatsuba</td></tr>
 * <tr><td>512</td><td>512</td><td>NTT</td></tr>
 * <tr><td>512</td><td>4294967296</td><td>NTT</td></tr>
 * </table>
 *
 * @see IntShortConvolutionStrategy
 * @see IntMediumConvolutionStrategy
 * @see IntKaratsubaConvolutionStrategy
 * @see Int3NTTConvolutionStrategy
 *
 * @version 1.4
 * @author Mikko Tommila
 */

public class IntConvolutionBuilder
    implements ConvolutionBuilder
{
    /**
     * Default constructor.
     */

    public IntConvolutionBuilder()
    {
    }

    public ConvolutionStrategy createConvolution(int radix, long size1, long size2, long resultSize)
    {
        long minSize = Math.min(size1, size2),
             maxSize = Math.max(size1, size2),
             totalSize = size1 + size2;

        if (minSize == 1)
        {
            return new IntShortConvolutionStrategy(radix);
        }
        else if (minSize <= IntKaratsubaConvolutionStrategy.CUTOFF_POINT)
        {
            return new IntMediumConvolutionStrategy(radix);
        }
        else
        {
            float mediumCost = (float) minSize * maxSize,
                  karatsubaCost = KARATSUBA_COST_FACTOR * (float) Math.pow((double) minSize, LOG2_3) * maxSize / minSize,
                  nttCost = NTT_COST_FACTOR * totalSize * Util.log2down(totalSize);

            if (mediumCost <= Math.min(karatsubaCost, nttCost))
            {
                return new IntMediumConvolutionStrategy(radix);
            }
            else if (karatsubaCost <= nttCost)
            {
                return new IntKaratsubaConvolutionStrategy(radix);
            }
            else
            {
                ApfloatContext ctx = ApfloatContext.getContext();
                NTTBuilder nttBuilder = ctx.getBuilderFactory().getNTTBuilder();
                NTTStrategy transform = nttBuilder.createNTT(totalSize);

                return new Int3NTTConvolutionStrategy(radix, transform);
            }
        }
    }

    private static final double LOG2_3 = Math.log(3.0) / Math.log(2.0);
}
