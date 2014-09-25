package org.apfloat.internal;

import org.apfloat.ApfloatRuntimeException;
import org.apfloat.ApfloatContext;
import org.apfloat.spi.DataStorage;
import org.apfloat.spi.ArrayAccess;
import org.apfloat.spi.Util;
import static org.apfloat.internal.FloatModConstants.*;

/**
 * Fast Number Theoretic Transform that uses a "six-step"
 * algorithm to calculate a long transform more efficiently on
 * cache-based memory architectures.<p>
 *
 * When the data to be transformed is considered to be an
 * n<sub>1</sub> x n<sub>2</sub> matrix of data, instead of a linear array,
 * the six steps are as follows:<p>
 *
 * <ol>
 *   <li>Transpose the matrix.</li>
 *   <li>Transform the rows.</li>
 *   <li>Transpose the matrix.</li>
 *   <li>Multiply each matrix element by w<sup>i j</sup> (where w is the n:th root of unity).</li>
 *   <li>Transform the rows.</li>
 *   <li>Transpose the matrix.</li>
 * </ol>
 *
 * In a convolution algorithm the last transposition step can be omitted
 * to increase performance, as well as the first transposition step in
 * the inverse transform. The convolution's element-by-element multiplication
 * is not sensitive to the order in which the elements are, of course.
 * Also scrambling the data can be omitted.<p>
 *
 * This algorithm is parallelized so that the row transforms are done in parallel
 * using multiple threads, if the number of processors is greater than one
 * in {@link ApfloatContext#getNumberOfProcessors() }.<p>
 *
 * If the data block to be transformed is larger than the shared memory treshold setting
 * in the current ApfloatContext, this class will synchronize all data access on
 * the shared memory lock retrieved from {@link ApfloatContext#getSharedMemoryLock() }.<p>
 *
 * All access to this class must be externally synchronized.
 *
 * @version 1.5.1
 * @author Mikko Tommila
 */

public class FloatSixStepFNTStrategy
    extends FloatParallelFNTStrategy
{
    /**
     * Default constructor.
     */

    public FloatSixStepFNTStrategy()
    {
    }

    public void transform(DataStorage dataStorage, int modulus)
        throws ApfloatRuntimeException
    {
        long length = dataStorage.getSize();            // Transform length n

        if (length > MAX_TRANSFORM_LENGTH)
        {
            throw new TransformLengthExceededException("Maximum transform length exceeded: " + length + " > " + MAX_TRANSFORM_LENGTH);
        }
        else if (length > Integer.MAX_VALUE)
        {
            throw new ApfloatInternalException("Maximum array length exceeded: " + length);
        }

        if (length < 2)
        {
            return;
        }

        assert (length == (length & -length));          // Must be a power of two

        ArrayAccess arrayAccess = dataStorage.getArray(DataStorage.READ_WRITE, 0, (int) length);

        transform(arrayAccess, modulus);

        arrayAccess.close();
    }

    public void inverseTransform(DataStorage dataStorage, int modulus, long totalTransformLength)
        throws ApfloatRuntimeException
    {
        long length = dataStorage.getSize();            // Transform length n

        if (Math.max(length, totalTransformLength) > MAX_TRANSFORM_LENGTH)
        {
            throw new TransformLengthExceededException("Maximum transform length exceeded: " + Math.max(length, totalTransformLength) + " > " + MAX_TRANSFORM_LENGTH);
        }
        else if (length > Integer.MAX_VALUE)
        {
            throw new ApfloatInternalException("Maximum array length exceeded: " + length);
        }

        if (length < 2)
        {
            return;
        }

        assert (length == (length & -length));          // Must be a power of two

        ArrayAccess arrayAccess = dataStorage.getArray(DataStorage.READ_WRITE, 0, (int) length);

        inverseTransform(arrayAccess, modulus, totalTransformLength);

        arrayAccess.close();
    }

    void transform(ArrayAccess arrayAccess, int modulus)
        throws ApfloatRuntimeException
    {
        int length = arrayAccess.getLength();

        if (length < 2)
        {
            return;
        }

        assert (length == (length & -length));          // Must be a power of two

        // Treat the input data as a n1 x n2 matrix

        int logLength = Util.log2down(length),
            n1 = logLength >> 1,
            n2 = logLength - n1;

        n1 = 1 << n1;
        n2 = 1 << n2;

        // Now n2 >= n1

        setModulus(MODULUS[modulus]);                                           // Modulus
        float w = getForwardNthRoot(PRIMITIVE_ROOT[modulus], length),         // Forward n:th root
                w1 = modPow(w, (float) n2);                                   // Forward n1:th root
        float[] wTable = createWTable(w1, n1);
        int[] permutationTable = Scramble.createScrambleTable(n1);

        FloatMatrix.transpose(arrayAccess, n1, n2);

        // Do n2 transforms of length n1
        transformRows(n1, n2, false, arrayAccess, wTable, permutationTable);

        FloatMatrix.transpose(arrayAccess, n2, n1);

        // Multiply each matrix element by w^(i*j)
        multiplyElements(arrayAccess, 0, n1, n2, w, (float) 1);

        // Do n1 transforms of length n2
        if (n1 != n2)
        {
            float w2 = modPow(w, (float) n1);             // Forward n2:th root
            wTable = createWTable(w2, n2);
        }
        transformRows(n2, n1, false, arrayAccess, wTable, null);
    }

    void inverseTransform(ArrayAccess arrayAccess, int modulus, long totalTransformLength)
        throws ApfloatRuntimeException
    {
        int length = arrayAccess.getLength();

        if (length < 2)
        {
            return;
        }

        assert (length == (length & -length));          // Must be a power of two

        // Treat the input data as a n1 x n2 matrix

        int logLength = Util.log2down(length),
            n1 = logLength >> 1,
            n2 = logLength - n1;

        n1 = 1 << n1;
        n2 = 1 << n2;

        // Now n2 >= n1

        setModulus(MODULUS[modulus]);                                           // Modulus
        float w = getInverseNthRoot(PRIMITIVE_ROOT[modulus], length),         // Inverse n:th root
                w2 = modPow(w, (float) n1),                                   // Inverse n2:th root
                inverseTotalTransformLength = modDivide((float) 1, (float) totalTransformLength);
        float[] wTable = createWTable(w2, n2);
        int[] permutationTable = Scramble.createScrambleTable(n1);

        // Do n1 transforms of length n2
        transformRows(n2, n1, true, arrayAccess, wTable, null);

        // Multiply each matrix element by w^(i*j) / totalTransformLength
        multiplyElements(arrayAccess, 0, n1, n2, w, inverseTotalTransformLength);

        FloatMatrix.transpose(arrayAccess, n1, n2);

        // Do n2 transforms of length n1
        if (n1 != n2)
        {
            // n2 = 2 * n1
            for (int i = 1; i < n1; i++)
            {
                wTable[i] = wTable[2 * i];
            }
        }
        transformRows(n1, n2, true, arrayAccess, wTable, permutationTable);

        FloatMatrix.transpose(arrayAccess, n2, n1);
    }
}
