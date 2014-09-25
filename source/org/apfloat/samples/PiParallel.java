package org.apfloat.samples;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import org.apfloat.Apfloat;
import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatMath;
import org.apfloat.ApfloatRuntimeException;

/**
 * Calculates pi using multiple threads in parallel.<p>
 *
 * Note that to get any performance gain from running many
 * threads in parallel, the JVM must be executing native threads.
 * If the JVM is running in green threads mode, there is no
 * advantage of having multiple threads, as the JVM will in fact
 * execute just one thread and divide its time to multiple
 * simulated threads.
 *
 * @version 1.5
 * @author Mikko Tommila
 */

public class PiParallel
    extends Pi
{
    /**
     * Parallel version of the Chudnovskys'
     * binary splitting algorithm.
     * Uses multiple threads to calculate pi in parallel.
     */

    protected static class ParallelChudnovskyPiCalculator
        extends ChudnovskyPiCalculator
    {
        /**
         * Construct a parallel pi calculator with the specified precision and radix.
         *
         * @param precision The target precision.
         * @param radix The radix to be used.
         */

        public ParallelChudnovskyPiCalculator(long precision, int radix)
            throws ApfloatRuntimeException
        {
            super(precision, radix);
        }

        /**
         * Entry point for the parallel binary splitting algorithm.
         *
         * @param n1 Start term.
         * @param n2 End term.
         * @param T Algorithm parameter.
         * @param Q Algorithm parameter.
         * @param P Algorithm parameter.
         * @param F Pointer to inverse square root parameter.
         * @param nodes The operation executors to be used for the calculation.
         * @param progressIndicator Class to print out the progress of the calculation.
         */

        protected void r(final long n1, final long n2, final ApfloatHolder T, final ApfloatHolder Q, final ApfloatHolder P, final ApfloatHolder F, OperationExecutor[] nodes, final BinarySplittingProgressIndicator progressIndicator)
            throws ApfloatRuntimeException
        {
            checkAlive();

            if (n1 == n2)
            {
                // Pathological case where available nodes > terms needed

                T.setApfloat(Apfloat.ZERO);
                Q.setApfloat(Apfloat.ONE);
                if (P != null) P.setApfloat(Apfloat.ONE);
            }
            else if (nodes.length == 1)
            {
                // End of splitting work between nodes/threads
                // calculate remaining terms on the node/thread

                if (DEBUG) Pi.err.println("PiParallel.r(" + n1 + ", " + n2 + ") executing all on node " + nodes[0]);

                ApfloatHolder[] TQP = nodes[0].execute(new Operation<ApfloatHolder[]>()
                {
                    public ApfloatHolder[] execute()
                    {
                        r(n1, n2, T, Q, P, progressIndicator);

                        return new ApfloatHolder[] { T, Q, P };
                    }
                });

                T.setApfloat(TQP[0].getApfloat());
                Q.setApfloat(TQP[1].getApfloat());
                if (P != null) P.setApfloat(TQP[2].getApfloat());
            }
            else
            {
                // Multiple nodes/threads available
                // Split work in ratio of node weights and execute in parallel

                Object[] objs = splitNodes(nodes);

                final OperationExecutor[] nodes1 = (OperationExecutor[]) objs[0],
                                          nodes2 = (OperationExecutor[]) objs[2];
                long weight1 = ((Long) objs[1]).longValue(),
                     weight2 = ((Long) objs[3]).longValue();

                final long nMiddle = n1 + (n2 - n1) * weight1 / (weight1 + weight2);
                final ApfloatHolder LT = new ApfloatHolder(),
                                    LQ = new ApfloatHolder(),
                                    LP = new ApfloatHolder();

                if (DEBUG) Pi.err.println("PiParallel.r(" + n1 + ", " + n2 + ") splitting " + formatArray(nodes) + " to r(" + n1 + ", " + nMiddle + ") " + formatArray(nodes1) + ", r(" + nMiddle + ", " + n2 + ") " + formatArray(nodes2));

                BackgroundOperation<Object> operation;

                // Call recursively this r() method to further split the term calculation
                operation = new BackgroundOperation<Object>(new Operation<Object>()
                {
                    public Object execute()
                    {
                        r(n1, nMiddle, LT, LQ, LP, null, nodes1, progressIndicator);
                        return null;
                    }
                });
                r(nMiddle, n2, T, Q, P, null, nodes2, progressIndicator);
                operation.getResult();                          // Waits for operation to complete

                // Calculate the combining multiplies using available nodes in parallel

                // Up to 4 calculations will be executed in parallel
                // If more than 4 nodes (threads) are available, each calculation can use multiple nodes (threads)
                assert (P == null || F == null);
                int numberNeeded = (P != null || F != null ? 1 : 0) + 3;
                nodes = recombineNodes(nodes, numberNeeded);

                final Operation<Apfloat> sqrtOperation = new Operation<Apfloat>()
                {
                    public Apfloat execute()
                    {
                        return ApfloatMath.inverseRoot(new Apfloat(640320, ParallelChudnovskyPiCalculator.this.precision, ParallelChudnovskyPiCalculator.this.radix), 2);
                    }
                }, T1operation = new Operation<Apfloat>()
                {
                    public Apfloat execute()
                    {
                        return Q.getApfloat().multiply(LT.getApfloat());
                    }
                }, T2operation = new Operation<Apfloat>()
                {
                    public Apfloat execute()
                    {
                        return LP.getApfloat().multiply(T.getApfloat());
                    }
                }, Toperation = new Operation<Apfloat>()
                {
                    public Apfloat execute()
                    {
                        return T1operation.execute().add(T2operation.execute());
                    }
                }, Qoperation = new Operation<Apfloat>()
                {
                    public Apfloat execute()
                    {
                        return LQ.getApfloat().multiply(Q.getApfloat());
                    }
                }, Poperation = new Operation<Apfloat>()
                {
                    public Apfloat execute()
                    {
                        return LP.getApfloat().multiply(P.getApfloat());
                    }
                };
                final Operation<Apfloat[]> QPoperation = new Operation<Apfloat[]>()
                {
                    public Apfloat[] execute()
                    {
                        return new Apfloat[] { Qoperation.execute(),
                                               P == null ? null : Poperation.execute() };
                    }
                };

                int availableNodes = nodes.length;

                BackgroundOperation<Apfloat> sqrtBackgroundOperation = null,
                                             operation1,
                                             operation2,
                                             operation3 = null;
                if (F != null && availableNodes > 1)
                {
                    if (DEBUG) Pi.err.println("PiParallel.r(" + n1 + ", " + n2 + ") calculating isqrt on node " + nodes[availableNodes - 1]);

                    sqrtBackgroundOperation = nodes[availableNodes - 1].executeBackground(sqrtOperation);
                    availableNodes--;
                }

                Apfloat t = null,
                        q = null,
                        p = null;

                switch (availableNodes)
                {
                    case 1:
                    {
                        t = nodes[0].execute(Toperation);
                        q = nodes[0].execute(Qoperation);
                        if (P != null) p = nodes[0].execute(Poperation);
                        break;
                    }
                    case 2:
                    {
                        operation1 = nodes[1].executeBackground(T1operation);
                        Apfloat tmp1 = nodes[0].execute(T2operation),
                                tmp2 = operation1.getResult();
                        operation1 = nodes[1].executeBackground(Qoperation);
                        t = executeAdd(nodes[0], tmp1, tmp2);
                        if (P != null) p = nodes[0].execute(Poperation);
                        q = operation1.getResult();
                        break;
                    }
                    case 3:
                    {
                        BackgroundOperation<Apfloat[]> operation1a;
                        operation1a = nodes[2].executeBackground(QPoperation);
                        operation2 = nodes[1].executeBackground(T1operation);
                        Apfloat tmp1 = nodes[0].execute(T2operation),
                                tmp2 = operation2.getResult();
                        t = executeAdd(nodes[1], tmp1, tmp2);
                        Apfloat[] QP = operation1a.getResult();
                        q = QP[0];
                        if (P != null) p = QP[1];
                        break;
                    }
                    default:
                    {
                        operation1 = nodes[availableNodes - 1].executeBackground(T1operation);
                        operation2 = nodes[availableNodes - 3].executeBackground(Qoperation);
                        if (P != null) operation3 = nodes[availableNodes - 4].executeBackground(Poperation);
                        Apfloat tmp1 = nodes[availableNodes - 2].execute(T2operation),
                                tmp2 = operation1.getResult();
                        t = executeAdd(nodes[availableNodes - 1], tmp1, tmp2);
                        q = operation2.getResult();
                        if (P != null) p = operation3.getResult();
                        break;
                    }
                }

                T.setApfloat(t);
                Q.setApfloat(q);
                if (P != null) P.setApfloat(p);

                if (sqrtBackgroundOperation != null)
                {
                    F.setApfloat(sqrtBackgroundOperation.getResult());
                }

                if (progressIndicator != null)
                {
                    progressIndicator.progress(n1, n2);
                }
            }
        }

        // Split nodes to two sets that have roughly the same total weights
        private Object[] splitNodes(OperationExecutor[] nodes)
        {
            List<OperationExecutor> list1 = new LinkedList<OperationExecutor>(),
                                    list2 = new LinkedList<OperationExecutor>();
            long weight1 = 0,
                 weight2 = 0;

            // Start from heaviest node to make maximally equal split
            for (int i = nodes.length; --i >= 0;)
            {
                if (weight1 < weight2)
                {
                    list1.add(0, nodes[i]);
                    weight1 += nodes[i].getWeight();
                }
                else
                {
                    list2.add(0, nodes[i]);
                    weight2 += nodes[i].getWeight();
                }
            }

            return new Object[] { list1.toArray(new OperationExecutor[list1.size()]), weight1,
                                  list2.toArray(new OperationExecutor[list2.size()]), weight2 };
        }

        private Apfloat executeAdd(OperationExecutor node, final Apfloat x, final Apfloat y)
        {
            return node.execute(new Operation<Apfloat>()
            {
                public Apfloat execute()
                {
                    return x.add(y);
                }
            });
        }

        /**
         * Get the available set of operation executor nodes.
         * This implementation returns {@link LocalOperationExecutor}s
         * but subclasses may return other operation executors.
         *
         * @return The set of available operation executors.
         */

        protected OperationExecutor[] getNodes()
        {
            ApfloatContext ctx = ApfloatContext.getGlobalContext();
            int numberOfProcessors = ctx.getNumberOfProcessors();
            OperationExecutor[] threads = new OperationExecutor[numberOfProcessors];

            for (int i = 0; i < numberOfProcessors; i++)
            {
                threads[i] = new ThreadLimitedOperationExecutor(1);
            }

            if (DEBUG) Pi.err.println("PiParallel.getNodes " + formatArray(threads));

            return threads;
        }

        /**
         * Attempt to combine or split nodes to form the needed number
         * of nodes. The returned number of nodes is something between
         * the number of nodes input and the number of nodes requested.
         * The requested number of nodes can be less than or greater than
         * the number of input nodes.
         *
         * @param nodes The operation executors to recombine.
         * @param numberNeeded The requested number of operation executors.
         *
         * @return The set of recombined operation executors.
         */

        protected OperationExecutor[] recombineNodes(OperationExecutor[] nodes, int numberNeeded)
        {
            if (numberNeeded >= nodes.length)
            {
                // LocalOperationExecutors can't be split since each corresponds to one thread

                if (DEBUG) Pi.err.println("PiParallel.recombineNodes unable to recombine nodes " + formatArray(nodes) + " (" + numberNeeded + " >= " + nodes.length + ")");

                return nodes;
            }
            else
            {
                // Combine LocalOperationExecutors to executors that can use more than one thread in calculations

                OperationExecutor[] newNodes = new OperationExecutor[numberNeeded];

                for (int i = 0; i < numberNeeded; i++)
                {
                    int numberOfProcessors = (nodes.length + i) / numberNeeded;

                    newNodes[i] = new ThreadLimitedOperationExecutor(numberOfProcessors);
                }

                if (DEBUG) Pi.err.println("PiParallel.recombineNodes recombined " + formatArray(nodes) + " to " + formatArray(newNodes));

                return newNodes;
            }
        }

        /**
         * Calculate pi using the Chudnovskys' binary splitting algorithm.
         */

        public Apfloat execute()
        {
            Pi.err.println("Using the Chudnovsky brothers' binary splitting algorithm");

            OperationExecutor[] nodes = getNodes();

            if (nodes.length > 1)
            {
                Pi.err.println("Using up to " + nodes.length + " parallel operations for calculation");
            }

            final ApfloatHolder T = new ApfloatHolder(),
                                Q = new ApfloatHolder(),
                                F = new ApfloatHolder();

            // Perform the calculation of T, Q and P to requested precision only, to improve performance

            long terms = (long) ((double) this.precision * Math.log((double) this.radix) / 32.65445004177);

            long time = System.currentTimeMillis();
            r(0, terms + 1, T, Q, null, F, nodes, new BinarySplittingProgressIndicator(terms));
            time = System.currentTimeMillis() - time;

            Pi.err.println("Series terms calculation complete, elapsed time " + time / 1000.0 + " seconds");
            Pi.err.print("Final value ");
            Pi.err.flush();

            nodes = recombineNodes(nodes, 1);

            time = System.currentTimeMillis();
            Apfloat pi = nodes[nodes.length - 1].execute(new Operation<Apfloat>()
            {
                public Apfloat execute()
                {
                    Apfloat t = T.getApfloat(),
                            q = Q.getApfloat(),
                            factor = F.getApfloat();

                    if (factor == null)
                    {
                        factor = ApfloatMath.inverseRoot(new Apfloat(640320, ParallelChudnovskyPiCalculator.this.precision, ParallelChudnovskyPiCalculator.this.radix), 2);
                    }

                    return ApfloatMath.inverseRoot(factor.multiply(t), 1).multiply(new Apfloat(53360, Apfloat.INFINITE, ParallelChudnovskyPiCalculator.this.radix)).multiply(q);
                }
            });
            time = System.currentTimeMillis() - time;

            Pi.err.println("took " + time / 1000.0 + " seconds");

            return pi;
        }
    }

    /**
     * Class to execute operations while setting {@link ApfloatContext#setNumberOfProcessors(int)}
     * to some value.
     */

    protected static class ThreadLimitedOperation<T>
        implements Operation<T>
    {
        /**
         * Wrap an existing operation to a thread limited context.
         *
         * @param operation The operation whose execution will have a limited number of threads available.
         * @param numberOfProcessors The maximum number of threads that can be used in the execution.
         */

        public ThreadLimitedOperation(Operation<T> operation, int numberOfProcessors)
        {
            this.operation = operation;
            this.numberOfProcessors = numberOfProcessors;
        }

        /**
         * Execute the operation.
         *
         * @return Result of the operation.
         */

        public T execute()
        {
            checkAlive();

            ApfloatContext ctx = (ApfloatContext) ApfloatContext.getContext().clone();
            ctx.setNumberOfProcessors(this.numberOfProcessors);
            ApfloatContext.setThreadContext(ctx);

            T result = this.operation.execute();

            ApfloatContext.removeThreadContext();

            return result;
        }

        private Operation<T> operation;
        private int numberOfProcessors;
    }

    private static class ThreadLimitedOperationExecutor
        extends LocalOperationExecutor
    {
        public ThreadLimitedOperationExecutor(int numberOfProcessors)
        {
            this.numberOfProcessors = numberOfProcessors;
        }

        public <T> T execute(Operation<T> operation)
        {
            return super.execute(new ThreadLimitedOperation<T>(operation, this.numberOfProcessors));
        }

        public <T> BackgroundOperation<T> executeBackground(Operation<T> operation)
        {
            return super.executeBackground(new ThreadLimitedOperation<T>(operation, this.numberOfProcessors));
        }

        public int getWeight()
        {
            return this.numberOfProcessors;
        }

        public String toString()
        {
            return String.valueOf(this.numberOfProcessors);
        }

        private int numberOfProcessors;
    }

    PiParallel() {}

    /**
     * Command-line entry point.
     *
     * @param args Command-line parameters.
     *
     * @exception IOException In case writing the output fails.
     */

    public static void main(String[] args)
        throws IOException, ApfloatRuntimeException
    {
        if (args.length < 1)
        {
            System.err.println("USAGE: PiParallel digits [threads] [radix]");
            System.err.println("    radix must be 2...36");

            return;
        }

        long precision = getPrecision(args[0]);
        int numberOfProcessors = (args.length > 1 ? getInt(args[1], "threads", 1, Integer.MAX_VALUE) : ApfloatContext.getContext().getNumberOfProcessors()),
            radix = (args.length > 2 ? getRadix(args[2]) : ApfloatContext.getContext().getDefaultRadix());

        ApfloatContext.getContext().setNumberOfProcessors(numberOfProcessors);

        Operation<Apfloat> operation = new ParallelChudnovskyPiCalculator(precision, radix);

        setOut(new PrintWriter(System.out, true));
        setErr(new PrintWriter(System.err, true));

        run(precision, radix, operation);
    }

    private static String formatArray(Object[] array)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("{ ");
        for (int i = 0; i < array.length; i++)
        {
            buffer.append(i == 0 ? "" : ", ");
            buffer.append(array[i]);
        }
        buffer.append(" }");
        return buffer.toString();
    }

    private static final boolean DEBUG = false;
}
