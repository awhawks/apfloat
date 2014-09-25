package org.apfloat;

/**
 * Various mathematical functions for arbitrary precision rational numbers.
 *
 * @version 1.0.1
 * @author Mikko Tommila
 */

public class AprationalMath
{
    private AprationalMath()
    {
    }

    /**
     * Integer power.
     *
     * @param x Base of the power operator.
     * @param n Exponent of the power operator.
     *
     * @return <code>x</code> to the <code>n</code>:th power, that is <code>x<sup>n</sup></code>.
     *
     * @exception java.lang.ArithmeticException If both <code>x</code> and <code>n</code> are zero.
     */

    public static Aprational pow(Aprational x, long n)
        throws ArithmeticException, ApfloatRuntimeException
    {
        if (n == 0)
        {
            if (x.signum() == 0)
            {
                throw new ArithmeticException("Zero to power zero");
            }

            return new Apint(1, x.radix());
        }
        else if (n < 0)
        {
            x = Aprational.ONE.divide(x);
            n = -n;
        }

        // Algorithm improvements by Bernd Kellner
        int b2pow = 0;

        while ((n & 1) == 0)
        {
            b2pow++;
            n >>>= 1;
        }

        Aprational r = x;

        while ((n >>>= 1) > 0)
        {
            x = x.multiply(x);
            if ((n & 1) != 0)
            {
                r = r.multiply(x);
            }
        }

        while (b2pow-- > 0)
        {
            r = r.multiply(r);
        }

        return r;
    }

    /**
     * Returns an aprational whose value is <code>-x</code>.
     *
     * @param x The argument.
     *
     * @return <code>-x</code>.
     */

    public static Aprational negate(Aprational x)
        throws ApfloatRuntimeException
    {
        return new Aprational(ApintMath.negate(x.numerator()), x.denominator());
    }

    /**
     * Absolute value.
     *
     * @param x The argument.
     *
     * @return Absolute value of <code>x</code>.
     */

    public static Aprational abs(Aprational x)
        throws ApfloatRuntimeException
    {
        if (x.signum() >= 0)
        {
            return x;
        }
        else
        {
            return negate(x);
        }
    }

    /**
     * Multiply by a power of the radix.
     * Note that this method is prone to intermediate overflow errors.
     * Also, scaling by a very large negative number won't result in an
     * underflow and a zero result, but an overflow of the denominator
     * and an exception thrown.
     *
     * @param x The argument.
     * @param scale The scaling factor.
     *
     * @return <code>x * x.radix()<sup>scale</sup></code>.
     */

    public static Aprational scale(Aprational x, long scale)
        throws ApfloatRuntimeException
    {
        if (scale >= 0)
        {
            return new Aprational(ApintMath.scale(x.numerator(), scale), x.denominator());
        }
        else if (scale == 0x8000000000000000L)
        {
            Apint scaler = ApintMath.pow(new Apint(x.radix(), x.radix()), 0x4000000000000000L);
            return new Aprational(x.numerator(), x.denominator().multiply(scaler).multiply(scaler));
        }
        else
        {
            return new Aprational(x.numerator(), ApintMath.scale(x.denominator(), -scale));
        }
    }
}
