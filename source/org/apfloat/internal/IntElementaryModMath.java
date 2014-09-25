package org.apfloat.internal;

/**
 * Elementary modulo arithmetic functions for <code>int</code> data.<p>
 *
 * Modular addition and subtraction are trivial, when the modulus is less
 * than 2<sup>31</sup> and overflow can be detected easily.<p>
 *
 * Modular multiplication is more complicated, and since it is usually
 * the single most time consuming operation in the whole program execution,
 * the very core of the Number Theoretic Transform (NTT), it should be
 * carefully optimized.<p>
 *
 * The obvious (but not very efficient) algorithm for multiplying two
 * <code>int</code>s and taking the remainder is<p>
 *
 * <code>(int) ((long) a * b % modulus)</code><p>
 *
 * The first observation is that since the modulus is practically
 * constant, it should be more efficient to calculate (once) the inverse
 * of the modulus, and then subsequently multiply by the inverse modulus
 * instead of dividing by the modulus.<p>
 *
 * The second observation is that to get the remainder of the division,
 * we don't necessarily need the actual result of the division (we just
 * want the remainder). So, we should discard the topmost 32 bits of the
 * full 64-bit result whenever possible, to save a few operations.<p>
 *
 * The basic approach is to get some approximation of <code>a * b / modulus</code>.
 * The approximation should be within +1 or -1 of the correct result. Then
 * calculate <code>a * b - approximateDivision * modulus</code> to get
 * the remainder. This calculation needs to use only the lowest 32 bits. As
 * the modulus is less than 2<sup>31</sup> it is easy to detect the case
 * when the approximate division was off by one (and the remainder is
 * <code>&#177;modulus</code> off).<p>
 *
 * There are different algorithms to calculate the approximate division
 * <code>a * b / modulus</code>. One would be to convert all the operands
 * to <code>double</code>. But, on most platforms converting between integer
 * types and floating point types is not very efficient. Another option is
 * to use fixed-point multiplication using <code>long</code> operands.
 * This is the approach used in this implementation.<p>
 *
 * As the modulus is slightly less than 2<sup>31</sup>, we can divide
 * 2<sup>63</sup> by the modulus, to get a fixed-point approximation
 * of the inverse modulus (rounded towards zero) to a precision of 33 bits.
 * The result of <code>a * b</code> takes 62 bits; shifting that right by
 * 30 bits and multiplying by the fixed-point inverse modulus will always
 * fit to 64 bits (because <code>a</code> and <code>b</code> are less than
 * the modulus and the inverse modulus was rounded towards zero). This
 * result can be further right-shifted by 33 to get the approximate result
 * of <code>a * b / modulus</code>.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class IntElementaryModMath
{
    /**
     * Default constructor.
     */

    public IntElementaryModMath()
    {
    }

    /**
     * Modular multiplication.
     *
     * @param a First operand.
     * @param b Second operand.
     *
     * @return <code>a * b % modulus</code>
     */

    public final int modMultiply(int a, int b)
    {
        long t = (long) a * (long) b;
        int r1 = (int) t - (int) ((t >>> 30) * this.inverseModulus >>> 33) * this.modulus,
            r2 = r1 - this.modulus;

        return (r2 < 0? r1 : r2);
    }

    /**
     * Modular addition.
     *
     * @param a First operand.
     * @param b Second operand.
     *
     * @return <code>(a + b) % modulus</code>
     */

    public final int modAdd(int a, int b)
    {
        int r1 = a + b,
            r2 = r1 - this.modulus;

        return (r2 < 0? r1 : r2);
    }

    /**
     * Modular subtraction. The result is always >= 0.
     *
     * @param a First operand.
     * @param b Second operand.
     *
     * @return <code>(a - b + modulus) % modulus</code>
     */

    public final int modSubtract(int a, int b)
    {
        int r1 = a - b,
            r2 = r1 + this.modulus;

        return (r1 < 0? r2 : r1);
    }

    /**
     * Get the modulus.
     *
     * @return The modulus.
     */

    public final int getModulus()
    {
        return this.modulus;
    }

    /**
     * Set the modulus.
     *
     * @param modulus The modulus.
     */

    public final void setModulus(int modulus)
    {
        this.inverseModulus = 0x8000000000000000L / -modulus;
        this.modulus = modulus;
    }

    private int modulus;
    private long inverseModulus;
}
