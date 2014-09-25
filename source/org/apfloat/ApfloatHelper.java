package org.apfloat;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.io.PushbackReader;
import java.io.IOException;

import org.apfloat.spi.ApfloatBuilder;
import org.apfloat.spi.ApfloatImpl;
import org.apfloat.spi.Util;
import static org.apfloat.spi.RadixConstants.*;

/**
 * Various utility methods related to apfloats.
 *
 * @version 1.3
 * @author Mikko Tommila
 */

class ApfloatHelper
{
    private ApfloatHelper()
    {
    }

    public static ApfloatImpl createApfloat(String value, boolean isInteger)
        throws NumberFormatException, ApfloatRuntimeException
    {
        long precision = (isInteger ? Apfloat.INFINITE : Apfloat.DEFAULT);
        int radix = getDefaultRadix();
        return implCreateApfloat(value, precision, radix, isInteger);
    }

    public static ApfloatImpl createApfloat(String value, long precision, boolean isInteger)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        return createApfloat(value, precision, radix, isInteger);
    }

    public static ApfloatImpl createApfloat(String value, long precision, int radix, boolean isInteger)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        if (precision != Apfloat.DEFAULT)
        {
            checkPrecision(precision);
        }
        return implCreateApfloat(value, precision, radix, isInteger);
    }

    private static ApfloatImpl implCreateApfloat(String value, long precision, int radix, boolean isInteger)
        throws NumberFormatException, ApfloatRuntimeException
    {
        ApfloatBuilder factory = getApfloatBuilder();
        return factory.createApfloat(value, precision, radix, isInteger);
    }

    public static ApfloatImpl createApfloat(long value)
        throws NumberFormatException, ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        return implCreateApfloat(value, Apfloat.INFINITE, radix);
    }

    public static ApfloatImpl createApfloat(long value, long precision)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        return createApfloat(value, precision, radix);
    }

    public static ApfloatImpl createApfloat(long value, long precision, int radix)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        precision = (precision == Apfloat.DEFAULT ? Apfloat.INFINITE : precision);
        checkPrecision(precision);
        return implCreateApfloat(value, precision, radix);
    }

    private static ApfloatImpl implCreateApfloat(long value, long precision, int radix)
        throws NumberFormatException, ApfloatRuntimeException
    {
        ApfloatBuilder factory = getApfloatBuilder();
        return factory.createApfloat(value, precision, radix);
    }

    public static ApfloatImpl createApfloat(float value)
        throws NumberFormatException, ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        int precision = getFloatPrecision(radix);
        return implCreateApfloat(value, precision, radix);
    }

    public static ApfloatImpl createApfloat(float value, long precision)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        return createApfloat(value, precision, radix);
    }

    public static ApfloatImpl createApfloat(float value, long precision, int radix)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        precision = (precision == Apfloat.DEFAULT ? getFloatPrecision(radix) : precision);
        checkPrecision(precision);
        return implCreateApfloat(value, precision, radix);
    }

    public static ApfloatImpl createApfloat(double value)
        throws NumberFormatException, ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        int precision = getDoublePrecision(radix);
        return implCreateApfloat(value, precision, radix);
    }

    public static ApfloatImpl createApfloat(double value, long precision)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        return createApfloat(value, precision, radix);
    }

    public static ApfloatImpl createApfloat(double value, long precision, int radix)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        precision = (precision == Apfloat.DEFAULT ? getDoublePrecision(radix) : precision);
        checkPrecision(precision);
        return implCreateApfloat(value, precision, radix);
    }

    private static ApfloatImpl implCreateApfloat(double value, long precision, int radix)
        throws NumberFormatException, ApfloatRuntimeException
    {
        ApfloatBuilder factory = getApfloatBuilder();
        return factory.createApfloat(value, precision, radix);
    }

    public static ApfloatImpl createApfloat(PushbackReader in, boolean isInteger)
        throws IOException, NumberFormatException, ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        return implCreateApfloat(in, Apfloat.DEFAULT, radix, isInteger);
    }

    public static ApfloatImpl createApfloat(PushbackReader in, long precision, boolean isInteger)
        throws IOException, NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        return createApfloat(in, precision, radix, isInteger);
    }

    public static ApfloatImpl createApfloat(PushbackReader in, long precision, int radix, boolean isInteger)
        throws IOException, NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        if (precision != Apfloat.DEFAULT)
        {
            checkPrecision(precision);
        }
        return implCreateApfloat(in, precision, radix, isInteger);
    }

    private static ApfloatImpl implCreateApfloat(PushbackReader in, long precision, int radix, boolean isInteger)
        throws IOException, NumberFormatException, ApfloatRuntimeException
    {
        ApfloatBuilder factory = getApfloatBuilder();
        return factory.createApfloat(in, precision, radix, isInteger);
    }

    public static ApfloatImpl createApfloat(BigInteger value)
        throws ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        return createApfloat(value.toString(radix), Apfloat.INFINITE, radix, true);
    }

    public static ApfloatImpl createApfloat(BigInteger value, long precision)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        int radix = getDefaultRadix();
        return createApfloat(value, precision, radix);
    }

    public static ApfloatImpl createApfloat(BigInteger value, long precision, int radix)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        checkRadix(radix);
        precision = (precision == Apfloat.DEFAULT ? Apfloat.INFINITE : precision);
        return createApfloat(value.toString(radix), precision, radix, true);
    }

    public static ApfloatImpl createApfloat(BigDecimal value)
        throws ApfloatRuntimeException
    {
        return implCreateApfloat(value.toString(), Apfloat.DEFAULT, 10, false);
    }

    public static ApfloatImpl createApfloat(BigDecimal value, long precision)
        throws IllegalArgumentException, ApfloatRuntimeException
    {
        return createApfloat(value.toString(), precision, 10, false);
    }

    /**
     * Extracts matching character from stream.
     * A non-matching character is pushed back to the stream.
     *
     * @param in The input.
     * @param c The character to expect from the stream.
     *
     * @return <code>true</code> if the specified character was extracted from the stream, <code>false</code> otherwise.
     *
     * @exception java.io.IOException In case of read error in the stream.
     */

    public static boolean readMatch(PushbackReader in, int c)
        throws IOException
    {
        int i = in.read();

        if (i != c)
        {
            if (i != -1)
            {
                in.unread(i);
            }
            return false;
        }

        return true;
    }

    /**
     * Extracts whitespace from stream.
     *
     * @param in The input.
     *
     * @exception java.io.IOException In case of read error in the stream.
     */

    public static void extractWhitespace(PushbackReader in)
        throws IOException
    {
        int c;

        while (Character.isWhitespace((char) (c = in.read())))
        {
            // Extracts any whitespace
        }
        if (c != -1)
        {
            in.unread(c);
        }
    }

    /**
     * Get working precisions for the arguments of e.g. an add, subtract or compare operation.<p>
     *
     * Note that the returned precision can be zero to indicate that the number is insignificant
     * in the calculation. This is the case if either operand is zero, or if one number lies
     * completely outside the significant range of the other number. Consider e.g. the case<p>
     *
     * x.scale() = 100<br>
     * x.precision() = 50<br>
     * y.scale() = 10<br>
     * y.precision() = 5<p>
     *
     * In e.g. the sum of x and y, the operand y would now be insignificant.
     *
     * @param x First argument.
     * @param y Second argument.
     *
     * @return Array of two longs containing the working precisions for <code>x</code> and <code>y</code>, correspondingly.
     */

    public static long[] getMatchingPrecisions(Apfloat x, Apfloat y)
        throws ApfloatRuntimeException
    {
        if (x.signum() == 0 || y.signum() == 0)
        {
            return new long[] { 0, 0 };
        }

        long xPrec  = x.precision(),
             yPrec  = y.precision(),
             xScale = x.scale(),
             yScale = y.scale(),
             maxScale = Math.max(xScale, yScale),
             xScaleDiff = (maxScale - xScale < 0 ? Apfloat.INFINITE : maxScale - xScale),
             yScaleDiff = (maxScale - yScale < 0 ? Apfloat.INFINITE : maxScale - yScale),
             maxPrec = Math.min(Util.ifFinite(xPrec, xPrec + xScaleDiff),
                                Util.ifFinite(yPrec, yPrec + yScaleDiff)),
             destXPrec = (maxPrec - xScaleDiff <= 0 ? 0 : Util.ifFinite(maxPrec, maxPrec - xScaleDiff)),
             destYPrec = (maxPrec - yScaleDiff <= 0 ? 0 : Util.ifFinite(maxPrec, maxPrec - yScaleDiff));

        return new long[] { destXPrec, destYPrec };
    }

    /**
     * Get working precisions for the arguments of an multiply-add operation
     * a * b + c * d. Works as well for a multiply-subtract operation, of course.<p>
     *
     * The returned array contains three longs:<p>
     *
     * [0] Working precisions for <code>a</code> and <code>b</code><br>
     * [1] Working precisions for <code>c</code> and <code>d</code><br>
     * [2] Maximum precision of the final result <code>a * b + c * d</code><p>
     *
     * Note that the precisions can be zero. See {@link #getMatchingPrecisions(Apfloat,Apfloat)}
     * for details.
     *
     * @param a First argument.
     * @param b Second argument.
     * @param c Third argument.
     * @param d Fourth argument.
     *
     * @return Array of three longs containing the precisions.
     */

    public static long[] getMatchingPrecisions(Apfloat a, Apfloat b, Apfloat c, Apfloat d)
        throws ApfloatRuntimeException
    {
        long abPrec  = (a.signum() == 0 || b.signum() == 0 ? 0 : Math.min(a.precision(), b.precision())),
             cdPrec  = (c.signum() == 0 || d.signum() == 0 ? 0 : Math.min(c.precision(), d.precision()));

        if (abPrec == 0 || cdPrec == 0)
        {
            return new long[] { abPrec, cdPrec, Math.max(abPrec, cdPrec) };
        }

        long abScale = a.scale() + b.scale(),
             cdScale = c.scale() + d.scale(),
             maxScale = Math.max(abScale, cdScale),
             abScaleDiff = (maxScale - abScale < 0 ? Apfloat.INFINITE : maxScale - abScale),
             cdScaleDiff = (maxScale - cdScale < 0 ? Apfloat.INFINITE : maxScale - cdScale),
             maxPrec = Math.min(Util.ifFinite(abPrec, abPrec + abScaleDiff),
                                Util.ifFinite(cdPrec, cdPrec + cdScaleDiff)),
             destAbPrec = (maxPrec - abScaleDiff <= 0 ? 0 : Util.ifFinite(maxPrec, maxPrec - abScaleDiff + 1)), // Add 1 since the scale may be 1 less
             destCdPrec = (maxPrec - cdScaleDiff <= 0 ? 0 : Util.ifFinite(maxPrec, maxPrec - cdScaleDiff + 1));

        return new long[] { destAbPrec, destCdPrec, maxPrec };
    }

    public static void checkPrecision(long precision)
        throws IllegalArgumentException
    {
        if (precision <= 0)
        {
            throw new IllegalArgumentException("Precision " + precision + " is not positive");
        }
    }

    public static void checkRadix(int radix)
        throws NumberFormatException
    {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
        {
            throw new NumberFormatException("Invalid radix " + radix + "; radix must be between " + Character.MIN_RADIX + " and " + Character.MAX_RADIX);
        }
    }

    public static int getFloatPrecision(int radix)
    {
        assert (radix > 0);
        return FLOAT_PRECISION[radix];
    }

    public static int getDoublePrecision(int radix)
    {
        assert (radix > 0);
        return DOUBLE_PRECISION[radix];
    }

    public static int getLongPrecision(int radix)
    {
        assert (radix > 0);
        return LONG_PRECISION[radix];
    }

    // Returns x with precision at most as specified
    public static Apfloat limitPrecision(Apfloat x, long precision)
        throws ApfloatRuntimeException
    {
        return x.precision(Math.min(x.precision(), precision));
    }

    // Returns x with precision at least as specified
    public static Apfloat ensurePrecision(Apfloat x, long precision)
        throws ApfloatRuntimeException
    {
        return x.precision(Math.max(x.precision(), precision));
    }

    // Returns given precision extended by specified amount
    public static long extendPrecision(long precision, long extraPrecision)
    {
        return Util.ifFinite(precision, precision + extraPrecision);
    }

    // Returns given precision extended by Apfloat.EXTRA_PRECISION
    public static long extendPrecision(long precision)
    {
        return extendPrecision(precision, Apfloat.EXTRA_PRECISION);
    }

    // Returns x with precision extended by Apfloat.EXTRA_PRECISION
    public static Apfloat extendPrecision(Apfloat x)
        throws ApfloatRuntimeException
    {
        return x.precision(extendPrecision(x.precision()));
    }

    // Returns x with precision extended by specified amount
    public static Apfloat extendPrecision(Apfloat x, long extraPrecision)
        throws ApfloatRuntimeException
    {
        return x.precision(extendPrecision(x.precision(), extraPrecision));
    }

    // Returns z with precision as specified
    public static Apcomplex setPrecision(Apcomplex z, long precision)
        throws ApfloatRuntimeException
    {
        if (z.real().signum() == 0)
        {
            return new Apcomplex(z.real(),
                                 z.imag().precision(precision));
        }
        else if (z.imag().signum() == 0)
        {
            return new Apcomplex(z.real().precision(precision),
                                 z.imag());
        }

        long precisionChange = precision - z.precision(),
             realPrecision = z.real().precision(),
             imagPrecision = z.imag().precision(),
             newRealPrecision = Util.ifFinite(realPrecision, realPrecision + precisionChange),
             newImagPrecision = Util.ifFinite(imagPrecision, imagPrecision + precisionChange);

        if (precisionChange < 0)
        {
            if (realPrecision + precisionChange <= 0)
            {
                return new Apcomplex(Apfloat.ZERO,
                                     z.imag().precision(precision));
            }
            else if (imagPrecision + precisionChange <= 0)
            {
                return new Apcomplex(z.real().precision(precision),
                                     Apfloat.ZERO);
            }
        }

        return new Apcomplex(z.real().precision(newRealPrecision),
                             z.imag().precision(newImagPrecision));
    }

    // Returns z with precision at most as specified
    public static Apcomplex limitPrecision(Apcomplex z, long precision)
        throws ApfloatRuntimeException
    {
        return new Apcomplex(z.real().precision(Math.min(z.real().precision(), precision)),
                             z.imag().precision(Math.min(z.imag().precision(), precision)));
    }

    // Returns z with precision at least as specified
    public static Apcomplex ensurePrecision(Apcomplex z, long precision)
        throws ApfloatRuntimeException
    {
        return new Apcomplex(z.real().precision(Math.max(z.real().precision(), precision)),
                             z.imag().precision(Math.max(z.imag().precision(), precision)));
    }

    // Returns z with precision extended by Apfloat.EXTRA_PRECISION
    public static Apcomplex extendPrecision(Apcomplex z)
        throws ApfloatRuntimeException
    {
        return new Apcomplex(z.real().precision(extendPrecision(z.real().precision())),
                             z.imag().precision(extendPrecision(z.imag().precision())));
    }

    // Returns z with precision extended by specified precision
    public static Apcomplex extendPrecision(Apcomplex z, long extraPrecision)
        throws ApfloatRuntimeException
    {
        return new Apcomplex(z.real().precision(extendPrecision(z.real().precision(), extraPrecision)),
                             z.imag().precision(extendPrecision(z.imag().precision(), extraPrecision)));
    }

    public static long size(Apfloat x)
        throws ApfloatRuntimeException
    {
        return (x.signum() == 0 ? 0 : x.getImpl(x.precision()).size());
    }

    public static long size(Apcomplex z)
        throws ApfloatRuntimeException
    {
        return Math.max(size(z.real()), size(z.imag()));
    }

    public static long size(Aprational x)
        throws ApfloatRuntimeException
    {
        return Math.max(size((Apfloat) x.numerator()), size((Apfloat) x.denominator()));
    }

    private static int getDefaultRadix()
        throws NumberFormatException
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        return ctx.getDefaultRadix();
    }

    private static ApfloatBuilder getApfloatBuilder()
    {
        ApfloatContext ctx = ApfloatContext.getContext();
        return ctx.getBuilderFactory().getApfloatBuilder();
    }
}
