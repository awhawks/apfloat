package org.apfloat;

import java.math.BigInteger;
import java.io.PushbackReader;
import java.io.Writer;
import java.io.IOException;

import org.apfloat.spi.ApfloatImpl;

/**
 * Arbitrary precision rational number class. An aprational consists of
 * a numerator and a denominator of type {@link Apint}.<p>
 *
 * @see Apint
 *
 * @version 1.0.1
 * @author Mikko Tommila
 */

public class Aprational
    extends Apfloat
{
    /**
     * Default constructor. To be used only by subclasses that
     * overload all needed methods.
     */

    protected Aprational()
    {
    }

    /**
     * Construct an integer aprational whose denominator is one.
     *
     * @param value The numerator of the number.
     */

    public Aprational(Apint value)
        throws ApfloatRuntimeException
    {
        this(value, ONE);
    }

    /**
     * Construct an aprational with the specified numerator and denominator.
     *
     * @param numerator The numerator.
     * @param denominator The denominator.
     *
     * @exception java.lang.IllegalArgumentException In case the denominator is zero, or if the denominator is not one or the numerator is not zero, and the radix of the numerator and denominator are different.
     */

    public Aprational(Apint numerator, Apint denominator)
        throws IllegalArgumentException, ApfloatRuntimeException
    {
        this.numerator = numerator;
        this.denominator = denominator;

        checkDenominator();

        reduce();
    }

    /**
     * Constructs an aprational from a string. The default radix is used.<p>
     *
     * The input must be of one of the formats<p>
     *
     * <code>integer</code><br>
     * <code>numerator [whitespace] "/" [whitespace] denominator</code><br>
     *
     * @param value The input string.
     *
     * @exception java.lang.NumberFormatException In case the number is invalid.
     * @exception java.lang.IllegalArgumentException In case the denominator is zero.
     */

    public Aprational(String value)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        this(value, ApfloatContext.getContext().getDefaultRadix());
    }

    /**
     * Constructs an aprational from a string with the specified radix.<p>
     *
     * The input must be of one of the formats<p>
     *
     * <code>integer</code><br>
     * <code>numerator [whitespace] "/" [whitespace] denominator</code><br>
     *
     * @param value The input string.
     * @param radix The radix to be used.
     *
     * @exception java.lang.NumberFormatException In case the number is invalid.
     * @exception java.lang.IllegalArgumentException In case the denominator is zero.
     */

    public Aprational(String value, int radix)
        throws NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        int index = value.indexOf('/');
        if (index < 0)
        {
            this.numerator = new Apint(value, radix);
            this.denominator = ONE;
            return;
        }

        this.numerator = new Apint(value.substring(0, index).trim(), radix);
        this.denominator = new Apint(value.substring(index + 1).trim(), radix);

        checkDenominator();

        reduce();
    }

    /**
     * Reads an aprational from a reader. The default radix is used. The constructor
     * stops reading at the first character it doesn't understand. The reader must
     * thus be a <code>PushbackReader</code> so that the invalid character can be
     * returned back to the stream.<p>
     *
     * The input must be of one of the formats<p>
     *
     * <code>integer [whitespace]</code><br>
     * <code>numerator [whitespace] "/" [whitespace] denominator</code><br>
     *
     * @param in The input stream.
     *
     * @exception java.io.IOException In case of I/O error reading the stream.
     * @exception java.lang.NumberFormatException In case the number is invalid.
     * @exception java.lang.IllegalArgumentException In case the denominator is zero.
     */

    public Aprational(PushbackReader in)
        throws IOException, NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        this(in, ApfloatContext.getContext().getDefaultRadix());
    }

    /**
     * Reads an aprational from a reader. The specified radix is used.
     *
     * @param in The input stream.
     * @param radix The radix to be used.
     *
     * @exception java.io.IOException In case of I/O error reading the stream.
     * @exception java.lang.IllegalArgumentException In case the denominator is zero.
     * @exception java.lang.NumberFormatException In case the number is invalid.
     *
     * @see #Aprational(PushbackReader)
     */

    public Aprational(PushbackReader in, int radix)
        throws IOException, IllegalArgumentException, NumberFormatException, IllegalArgumentException, ApfloatRuntimeException
    {
        this.numerator = new Apint(in, radix);

        ApfloatHelper.extractWhitespace(in);

        if (!ApfloatHelper.readMatch(in, '/'))
        {
            this.denominator = ONE;
            return;
        }

        ApfloatHelper.extractWhitespace(in);
        this.denominator = new Apint(in, radix);

        checkDenominator();

        reduce();
    }

    /**
     * Constructs an aprational from a <code>BigInteger</code>.
     * The default radix is used.
     *
     * @param value The numerator of the number.
     */

    public Aprational(BigInteger value)
        throws ApfloatRuntimeException
    {
        this.numerator = new Apint(value);
        this.denominator = ONE;
    }

    /**
     * Constructs an aprational from a <code>BigInteger</code> using the specified radix.
     *
     * @param value The numerator of the number.
     * @param radix The radix of the number.
     */

    public Aprational(BigInteger value, int radix)
        throws ApfloatRuntimeException
    {
        this.numerator = new Apint(value, radix);
        this.denominator = ONE;
    }

    /**
     * Numerator of this aprational.
     *
     * @return <code>n</code> where <code>this = n / m</code>.
     */

    public Apint numerator()
    {
        return this.numerator;
    }

    /**
     * Denominator of this aprational.
     *
     * @return <code>m</code> where <code>this = n / m</code>.
     */

    public Apint denominator()
    {
        return this.denominator;
    }

    /**
     * Radix of this aprational.
     *
     * @return Radix of this aprational.
     */

    public int radix()
    {
        return (numerator() == ONE ? denominator().radix() : numerator().radix());
    }

    /**
     * Returns the precision of this aprational.
     *
     * @return <code>INFINITE</code>
     */

    public long precision()
        throws ApfloatRuntimeException
    {
        return INFINITE;
    }

    /**
     * Returns the scale of this aprational. Scale is equal to the number of digits in the aprational's truncated value.<p>
     *
     * Zero has a scale of <code>-INFINITE</code>.
     *
     * @return Number of digits in the truncated value of this aprational in the radix in which it's presented.
     */

    public synchronized long scale()
        throws ApfloatRuntimeException
    {
        if (signum() == 0)
        {
            return -INFINITE;
        }

        if (this.scale == UNDEFINED)
        {
            long scale = numerator().scale() - denominator().scale();

            if (scale > 0)
            {
                scale = truncate().scale();
            }
            else
            {
                scale = AprationalMath.scale(this, 1 - scale).truncate().scale() + scale - 1;
            }

            // Stores of longs are not guaranteed to be atomic, so this method must be synchronized
            this.scale = scale;
        }

        return this.scale;
    }

    /**
     * Returns the signum function of this aprational.
     *
     * @return -1, 0 or 1 as the value of this aprational is negative, zero or positive.
     */

    public int signum()
    {
        return numerator().signum();
    }

    /**
     * Returns if this aprational is "short".
     *
     * @return <code>true</code> if the aprational is "short", <code>false</code> if not.
     *
     * @see Apfloat#isShort()
     */

    public boolean isShort()
        throws ApfloatRuntimeException
    {
        return numerator().isShort() && denominator().equals(ONE);
    }

    /**
     * Adds two aprational numbers.
     *
     * @param x The number to be added to this number.
     *
     * @return <code>this + x</code>.
     */

    public Aprational add(Aprational x)
        throws ApfloatRuntimeException
    {
        return new Aprational(numerator().multiply(x.denominator()).add(denominator().multiply(x.numerator())),
                              denominator().multiply(x.denominator())).reduce();
    }

    /**
     * Subtracts two aprational numbers.
     *
     * @param x The number to be subtracted from this number.
     *
     * @return <code>this - x</code>.
     */

    public Aprational subtract(Aprational x)
        throws ApfloatRuntimeException
    {
        return new Aprational(numerator().multiply(x.denominator()).subtract(denominator().multiply(x.numerator())),
                              denominator().multiply(x.denominator())).reduce();
    }

    /**
     * Multiplies two aprational numbers.
     *
     * @param x The number to be multiplied by this number.
     *
     * @return <code>this * x</code>.
     */

    public Aprational multiply(Aprational x)
        throws ApfloatRuntimeException
    {
        Aprational result = new Aprational(numerator().multiply(x.numerator()),
                                           denominator().multiply(x.denominator()));

        if (this == x)
        {
            // When squaring we know that no reduction is needed
            return result;
        }
        else
        {
            return result.reduce();
        }
    }

    /**
     * Divides two aprational numbers.
     *
     * @param x The number by which this number is to be divided.
     *
     * @return <code>this / x</code>.
     *
     * @exception java.lang.ArithmeticException In case the divisor is zero.
     */

    public Aprational divide(Aprational x)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (x.signum() == 0)
        {
            throw new ArithmeticException("Division by zero");
        }
        else if (signum() == 0)
        {
            // 0 / x = 0
            return this;
        }
        // Comparison against one would be inefficient at this point

        return new Aprational(numerator().multiply(x.denominator()),
                              denominator().multiply(x.numerator())).reduce();
    }

    /**
     * Floor function. Returns the largest (closest to positive infinity) value
     * that is not greater than this aprational and is equal to a mathematical integer.
     *
     * @return This aprational rounded towards negative infinity.
     */

    public Apint floor()
        throws ApfloatRuntimeException
    {
        if (signum() >= 0)
        {
            return truncate();
        }
        else
        {
            return roundAway();
        }
    }

    /**
     * Ceiling function. Returns the smallest (closest to negative infinity) value
     * that is not less than this aprational and is equal to a mathematical integer.
     *
     * @return This aprational rounded towards positive infinity.
     */

    public Apint ceil()
        throws ApfloatRuntimeException
    {
        if (signum() <= 0)
        {
            return truncate();
        }
        else
        {
            return roundAway();
        }
    }

    /**
     * Truncates fractional part.
     *
     * @return This aprational rounded towards zero.
     */

    public Apint truncate()
        throws ApfloatRuntimeException
    {
        return numerator().divide(denominator());
    }

    // Round away from zero i.e. opposite direction of rounding than in truncate()
    private Apint roundAway()
        throws ApfloatRuntimeException
    {
        Apint[] div = ApintMath.div(numerator(), denominator());

        if (div[1].signum() == 0)
        {
            // No remainder from division; result is exact
            return div[0];
        }
        else
        {
            // Remainder from division; round away from zero
            return div[0].add(new Apint(signum(), div[0].radix()));
        }
    }

    /**
     * Compare this aprational to the specified aprational.<p>
     *
     * @param x Aprational to which this aprational is to be compared.
     *
     * @return -1, 0 or 1 as this aprational is numerically less than, equal to, or greater than <code>x</code>.
     */

    public int compareTo(Aprational x)
        throws ApfloatRuntimeException
    {
        Apint a = numerator().multiply(x.denominator()),
              b = x.numerator().multiply(denominator());

        return a.compareTo(b);
    }

    /**
     * Compare this aprational to the specified apfloat.<p>
     *
     * @param x Apfloat to which this aprational is to be compared.
     *
     * @return -1, 0 or 1 as this aprational is numerically less than, equal to, or greater than <code>x</code>.
     */

    public int compareTo(Apfloat x)
        throws ApfloatRuntimeException
    {
        if (x instanceof Aprational)
        {
            return compareTo((Aprational) x);
        }
        else
        {
            // Sub-optimal performance wise, but works
            Apfloat a = numerator().precision(INFINITE),                // Actual class must be Apfloat
                    b = x.multiply(denominator()).precision(INFINITE);  // Actual class must be Apfloat

            return a.compareTo(b);
        }
    }

    /**
     * Compare this aprational to the specified object.
     *
     * @param obj Object to which this aprational is to be compared.
     *
     * @return -1, 0 or 1 as this aprational is numerically less than, equal to, or greater than <code>obj</code>.
     *
     * @exception java.lang.ClassCastException If the specified object is not an apfloat.
     */

    public int compareTo(Object obj)
        throws ClassCastException
    {
        if (obj instanceof Aprational)
        {
            return compareTo((Aprational) obj);
        }
        else if (obj instanceof Apfloat)
        {
            return compareTo((Apfloat) obj);
        }
        else
        {
            return super.compareTo(obj);
        }
    }

    /**
     * Compares this object to the specified object.<p>
     *
     * Note: if two apfloats are compared where one number doesn't have enough
     * precise digits, the mantissa is assumed to contain zeros.
     * See {@link Apfloat#compareTo(Apfloat)}.
     *
     * @param obj The object to compare with.
     *
     * @return <code>true</code> if the objects are the same; <code>false</code> otherwise.
     */

    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        else if (obj instanceof Aprational)
        {
            Aprational that = (Aprational) obj;
            return numerator().equals(that.numerator()) &&
                   denominator().equals(that.denominator());
        }
        else if (obj instanceof Apfloat)
        {
            Apfloat that = (Apfloat) obj;

            // Sub-optimal performance wise, but works
            Apfloat a = numerator().precision(INFINITE),                    // Actual class must be Apfloat
                    b = that.multiply(denominator()).precision(INFINITE);   // Actual class must be Apfloat

            return a.equals(b);
        }
        else
        {
            return super.equals(obj);
        }
    }

    /**
     * Returns a hash code for this aprational.
     *
     * @return The hash code value for this object.
     */

    public int hashCode()
    {
        return numerator().hashCode() * 3 +
               denominator().hashCode();
    }

    /**
     * Returns a string representation of this aprational.
     *
     * @return A string representing this object.
     */

    public String toString()
    {
        return toString(true);
    }

    /**
     * Returns a string representation of this aprational.
     *
     * @param pretty <code>true</code> to use a fixed-point notation, <code>false</code> to use an exponential notation.
     *
     * @return A string representing this object.
     */

    public String toString(boolean pretty)
        throws ApfloatRuntimeException
    {
        return numerator().toString(pretty) +
               (denominator().equals(ONE) ? "" : '/' + denominator().toString(pretty));
    }

    /**
     * Write a string representation of this aprational to a <code>Writer</code>.
     *
     * @param out The output <code>Writer</code>.
     *
     * @exception java.io.IOException In case of I/O error writing to the stream.
     */

    public void writeTo(Writer out)
        throws IOException, ApfloatRuntimeException
    {
        writeTo(out, true);
    }

    /**
     * Write a string representation of this aprational to a <code>Writer</code>.
     *
     * @param out The output <code>Writer</code>.
     * @param pretty <code>true</code> to use a fixed-point notation, <code>false</code> to use an exponential notation.
     *
     * @exception java.io.IOException In case of I/O error writing to the stream.
     */

    public void writeTo(Writer out, boolean pretty)
        throws IOException, ApfloatRuntimeException
    {
        numerator().writeTo(out, pretty);
        if (!denominator().equals(ONE))
        {
            out.write('/');
            denominator().writeTo(out, pretty);
        }
    }

    /**
     * Returns an <code>ApfloatImpl</code> representing the approximation of this
     * aprational up to the requested precision.<p>
     *
     * @param precision Precision of the <code>ApfloatImpl</code> that is needed.
     *
     * @return An <code>ApfloatImpl</code> representing this object to the requested precision.
     */

    protected ApfloatImpl getImpl(long precision)
        throws ApfloatRuntimeException
    {
        ensureApprox(precision);

        return this.approx.getImpl(precision);
    }

    private void checkDenominator()
        throws IllegalArgumentException
    {
        if (this.denominator.signum() == 0)
        {
            throw new IllegalArgumentException("Denominator is zero");
        }
    }

    // Reduce the numerator and denominator to smallest possible terms and set the signs properly
    // NOTE: the method mutates this object, so it must only be called for newly constructed aprationals
    // Returns this, for convenience
    private Aprational reduce()
        throws IllegalArgumentException, ApfloatRuntimeException
    {
        if (this.numerator.signum() == 0)
        {
            this.denominator = ONE;
        }
        else
        {
            if (!this.numerator.equals(ONE) && !this.denominator.equals(ONE))
            {
                if (this.numerator.radix() != this.denominator.radix())
                {
                    throw new IllegalArgumentException("Numerator and denominator must have the same radix");
                }

                Apint gcd = ApintMath.gcd(this.numerator, this.denominator);
                this.numerator = this.numerator.divide(gcd);
                this.denominator = this.denominator.divide(gcd);
            }

            int sign = this.numerator.signum() * this.denominator.signum();

            this.denominator = ApintMath.abs(this.denominator);

            if (sign < 0 && numerator.signum() > 0)
            {
                this.numerator = ApintMath.negate(this.numerator);
            }
        }

        return this;
    }

    private synchronized void ensureApprox(long precision)
        throws ApfloatRuntimeException
    {
        if (!hasApprox(precision))
        {
            if (denominator().equals(ONE))
            {
                this.approx = numerator();
            }
            else
            {
                precision = Math.max(precision, 1);     // In case the requested precision would be zero

                if (denominator().isShort())
                {
                    this.approx = numerator().precision(precision).divide(denominator());
                }
                else
                {
                    this.inverseDen = ApfloatMath.inverseRoot(denominator(), 1, precision, this.inverseDen);
                    this.approx = numerator().multiply(this.inverseDen);
                }
            }
        }
    }

    private boolean hasApprox(long precision)
        throws ApfloatRuntimeException
    {
        return (this.approx != null && this.approx.precision() >= precision);
    }

    private static final long UNDEFINED = 0x8000000000000000L;

    private Apint numerator;
    private Apint denominator;
    private long scale = UNDEFINED;
    private transient Apfloat inverseDen = null;
    private transient Apfloat approx = null;
}
