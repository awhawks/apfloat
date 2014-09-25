package org.apfloat;

import java.math.BigInteger;
import java.io.PushbackReader;
import java.io.Writer;
import java.io.IOException;

import org.apfloat.spi.ApfloatImpl;

/**
 * Arbitrary precision integer class.<p>
 *
 * In addition to the constructors, it is possible to create an apint
 * from an apfloat or aprational via the methods that round these
 * numbers to an integer value: {@link Apfloat#floor() }, {@link Apfloat#ceil() },
 * and {@link Apfloat#truncate() }.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class Apint
    extends Aprational
{
    /**
     * Default constructor. To be used only by subclasses that
     * overload all needed methods.
     */

    protected Apint()
    {
    }

    // Package private constructor that skips validating that the provided value actually is an integer
    Apint(Apfloat value)
    {
        this.value = value;
    }

    /**
     * Constructs an apfloat from the specified string.
     * The default radix will be used.<p>
     *
     * @param value The string representing the number.
     *
     * @exception java.lang.NumberFormatException In case the number is invalid.
     */

    public Apint(String value)
        throws NumberFormatException, ApfloatRuntimeException
    {
        this.value = new Apfloat(ApfloatHelper.createApfloat(value, true));
    }

    /**
     * Constructs an apfloat from the specified string and radix.
     *
     * @param value The string representing the number.
     * @param radix The radix of the number.
     *
     * @exception java.lang.NumberFormatException In case the number is invalid.
     */

    public Apint(String value, int radix)
        throws NumberFormatException, ApfloatRuntimeException
    {
        this.value = new Apfloat(ApfloatHelper.createApfloat(value, INFINITE, radix, true));
    }

    /**
     * Constructs an apfloat from the specified <code>long</code>.
     * The default radix will be used.
     *
     * @param value The value of the number.
     *
     * @exception java.lang.NumberFormatException In case the number is invalid.
     */

    public Apint(long value)
        throws NumberFormatException, ApfloatRuntimeException
    {
        this.value = new Apfloat(ApfloatHelper.createApfloat(value));
    }

    /**
     * Constructs an apfloat from the specified <code>long</code>
     * and radix.
     *
     * @param value The value of the number.
     * @param radix The radix of the number.
     *
     * @exception java.lang.NumberFormatException In case the number is invalid.
     */

    public Apint(long value, int radix)
        throws NumberFormatException, ApfloatRuntimeException
    {
        this.value = new Apfloat(ApfloatHelper.createApfloat(value, INFINITE, radix));
    }

    /**
     * Reads an apint from a stream using the default radix.
     *
     * @param in The stream to read from
     *
     * @exception java.io.IOException If an I/O error occurs accessing the stream.
     * @exception java.lang.NumberFormatException If the number is not valid.
     */

    public Apint(PushbackReader in)
        throws IOException, NumberFormatException, ApfloatRuntimeException
    {
        this.value = new Apfloat(ApfloatHelper.createApfloat(in, INFINITE, true));
    }

    /**
     * Reads an apint from a stream using the specified radix.
     *
     * @param in The stream to read from
     * @param radix The radix of the number.
     *
     * @exception java.io.IOException If an I/O error occurs accessing the stream.
     * @exception java.lang.NumberFormatException If the number is not valid.
     */

    public Apint(PushbackReader in, int radix)
        throws IOException, NumberFormatException, ApfloatRuntimeException
    {
        this.value = new Apfloat(ApfloatHelper.createApfloat(in, INFINITE, radix, true));
    }

    /**
     * Constructs an apint from a <code>BigInteger</code>. The default radix is used.
     *
     * @param value The value of the number.
     *
     * @exception java.lang.NumberFormatException If the default radix is not valid.
     */

    public Apint(BigInteger value)
        throws NumberFormatException, ApfloatRuntimeException
    {
        this.value = new Apfloat(value);
    }

    /**
     * Constructs an apint from a <code>BigInteger</code> using the specified radix.
     *
     * @param value The value of the number.
     * @param radix The radix of the number.
     *
     * @exception java.lang.NumberFormatException If the radix is not valid.
     */

    public Apint(BigInteger value, int radix)
        throws NumberFormatException, ApfloatRuntimeException
    {
        this.value = new Apfloat(value, INFINITE, radix);
    }

    /**
     * Numerator of this aprational.
     *
     * @return <code>this</code>.
     */

    public Apint numerator()
    {
        return this;
    }

    /**
     * Denominator of this aprational.
     *
     * @return {@link #ONE}.
     */

    public Apint denominator()
    {
        return ONE;
    }

    /**
     * Radix of this apint.
     *
     * @return Radix of this apint.
     */

    public int radix()
    {
        return this.value.radix();
    }

    /**
     * Returns the scale of this apint. Scale is equal to the number of digits in an apint.<p>
     *
     * Zero has a scale of <code>-INFINITE</code>.
     *
     * @return Number of digits in this apint in the radix in which it's presented.
     */

    public long scale()
        throws ApfloatRuntimeException
    {
        return this.value.scale();
    }

    /**
     * Returns the signum function of this apint.
     *
     * @return -1, 0 or 1 as the value of this apint is negative, zero or positive.
     */

    public int signum()
    {
        return this.value.signum();
    }

    /**
     * Returns if this apint is "short".
     *
     * @return <code>true</code> if the apint is "short", <code>false</code> if not.
     *
     * @see Apfloat#isShort()
     */

    public boolean isShort()
        throws ApfloatRuntimeException
    {
        return this.value.isShort();
    }

    /**
     * Adds two apints.
     *
     * @param x The number to be added to this number.
     *
     * @return <code>this + x</code>.
     */

    public Apint add(Apint x)
        throws ApfloatRuntimeException
    {
        return new Apint(this.value.add(x.value));
    }

    /**
     * Subtracts two apints.
     *
     * @param x The number to be subtracted from this number.
     *
     * @return <code>this - x</code>.
     */

    public Apint subtract(Apint x)
        throws ApfloatRuntimeException
    {
        return new Apint(this.value.subtract(x.value));
    }

    /**
     * Multiplies two apints.
     *
     * @param x The number to be multiplied by this number.
     *
     * @return <code>this * x</code>.
     */

    public Apint multiply(Apint x)
        throws ApfloatRuntimeException
    {
        return new Apint(this.value.multiply(x.value));
    }

    /**
     * Divides two apints.
     *
     * @param x The number by which this number is to be divided.
     *
     * @return <code>this / x</code>.
     *
     * @exception java.lang.ArithmeticException In case the divisor is zero.
     */

    public Apint divide(Apint x)
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
        else if (x.equals(ONE))
        {
            // x / 1 = x
            return this;
        }

        long precision;
        Apfloat a, b, tx, ty;
        Apint t;

        a = ApfloatMath.abs(this.value);
        b = ApfloatMath.abs(x.value);

        if (a.compareTo(b) < 0)
        {
            return ZERO;                        // abs(this) < abs(x)
        }
        else
        {
            precision = scale() - x.scale() + EXTRA_PRECISION;          // Some extra precision
        }

        tx = this.value.precision(precision);
        ty = x.value.precision(precision);

        t = tx.divide(ty).truncate();           // Approximate division

        a = ApfloatMath.abs(this.value.subtract(t.multiply(x.value)));

        if (a.compareTo(b) >= 0)                // Fix division round-off error
        {
            t = t.add(new Apint(signum() * x.signum(), x.radix()));
        }

        return t;
    }

    /**
     * Calculates the remainder when divided by an apint.
     * The result has the same sign as this number.
     * If <code>x</code> is zero, then zero is returned.
     *
     * @param x The number that is used as the divisor in the remainder calculation.
     *
     * @return <code>this % x</code>.
     *
     * @see ApfloatMath#fmod(Apfloat,Apfloat)
     */

    public Apint mod(Apint x)
        throws ApfloatRuntimeException
    {
        return new Apint(ApfloatMath.fmod(this.value, x.value));
    }

    /**
     * Floor function. Returns the largest (closest to positive infinity) value
     * that is not greater than this apfloat and is equal to a mathematical integer.
     *
     * @return This apint.
     */

    public Apint floor()
    {
        return this;
    }

    /**
     * Ceiling function. Returns the smallest (closest to negative infinity) value
     * that is not less than this apfloat and is equal to a mathematical integer.
     *
     * @return This apint.
     */

    public Apint ceil()
    {
        return this;
    }

    /**
     * Truncates fractional part.
     *
     * @return This apint.
     */

    public Apint truncate()
    {
        return this;
    }

    /**
     * Compare this apint to the specified apint.<p>
     *
     * @param x Apint to which this apint is to be compared.
     *
     * @return -1, 0 or 1 as this apint is numerically less than, equal to, or greater than <code>x</code>.
     */

    public int compareTo(Apint x)
        throws ApfloatRuntimeException
    {
        return this.value.compareTo(x.value);
    }

    /**
     * Compare this apint to the specified aprational.<p>
     *
     * @param x Aprational to which this apint is to be compared.
     *
     * @return -1, 0 or 1 as this apint is numerically less than, equal to, or greater than <code>x</code>.
     */

    public int compareTo(Aprational x)
        throws ApfloatRuntimeException
    {
        if (x instanceof Apint)
        {
            return compareTo((Apint) x);
        }
        else
        {
            return super.compareTo(x);
        }
    }

    /**
     * Compare this apint to the specified apfloat.<p>
     *
     * @param x Apfloat to which this apint is to be compared.
     *
     * @return -1, 0 or 1 as this apint is numerically less than, equal to, or greater than <code>x</code>.
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
            return this.value.compareTo(x);
        }
    }

    /**
     * Compare this apint to the specified object.
     *
     * @param obj Object to which this apint is to be compared.
     *
     * @return -1, 0 or 1 as this apint is numerically less than, equal to, or greater than <code>obj</code>.
     *
     * @exception java.lang.ClassCastException If the specified object is not an apfloat.
     */

    public int compareTo(Object obj)
        throws ClassCastException, ApfloatRuntimeException
    {
        if (obj instanceof Apint)
        {
            return compareTo((Apint) obj);
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
        throws ApfloatRuntimeException
    {
        if (obj == this)
        {
            return true;
        }
        else if (obj instanceof Apint)
        {
            Apint that = (Apint) obj;
            return this.value.equals(that.value);
        }
        else if (obj instanceof Apfloat && !(obj instanceof Aprational))
        {
            Apfloat that = (Apfloat) obj;
            return this.value.equals(that);
        }
        else
        {
            return super.equals(obj);
        }
    }

    /**
     * Returns a hash code for this apint.
     *
     * @return The hash code value for this object.
     */

    public int hashCode()
    {
        return this.value.hashCode();
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
        return this.value.toString(pretty);
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
        this.value.writeTo(out, pretty);
    }

    /**
     * Returns an <code>ApfloatImpl</code> representing this apint up to the requested precision.
     *
     * @param precision Precision of the <code>ApfloatImpl</code> that is needed.
     *
     * @return An <code>ApfloatImpl</code> representing this object to the requested precision.
     */

    protected ApfloatImpl getImpl(long precision)
        throws ApfloatRuntimeException
    {
        return this.value.getImpl(precision);
    }

    private Apfloat value;
}
