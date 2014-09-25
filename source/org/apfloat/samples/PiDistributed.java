package org.apfloat.samples;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

import org.apfloat.Apfloat;
import org.apfloat.ApfloatContext;
import org.apfloat.ApfloatRuntimeException;

/**
 * Calculates pi using a cluster of servers.
 * The servers should be running {@link OperationServer}.
 *
 * The names and ports of the cluster nodes are read from the file
 * <code>cluster.properties</code>, or a <code>ResourceBundle</code>
 * by the name "cluster". The format of the property file is as
 * follows:<p>
 *
 * <pre>
 * server1=hostname.company.com:1234
 * server2=hostname2.company.com:2345
 * server3=hostname3.company.com:3456
 * weight1=100
 * weight2=110
 * weight3=50
 * </pre>
 *
 * The server addresses are specified as hostname:port. Weights can
 * (but don't have to) be assigned to nodes to indicate the relative
 * performance of each node, to allow distributing a suitable amount
 * of work for each node. For example, <code>weight2</code> is the
 * relative performance of <code>server2</code> etc. The weights must
 * be integers in the range 1...1000.<p>
 *
 * Guidelines for configuring the servers:
 *
 * <ul>
 *   <li>If the machines are not identical, give proper weights to every
 *       machine. This can improve performance greatly.</li>
 *   <li>If the machines are somewhat similar (e.g. same processor but
 *       different clock frequency), you can calculate the weight roughly
 *       as <code>clockFrequency * numberOfProcessors</code>. For example,
 *       a machine with two 1600MHz processors is four times as fast as
 *       a machine with one 800MHz processor.
 *       </li>
 *   <li>If the machines are very heterogenous, you can benchmark their
 *       performance by running e.g. {@link PiParallel} with one
 *       million digits. Remember to specify the correct number of
 *       CPUs on each machine.</li>
 *   <li>Different JVMs can have different performance. For example,
 *       Sun's Java client VM achieves roughly two thirds of the
 *       performance of the server VM when running this application.</li>
 *   <li>When running {@link OperationServer} on the cluster nodes,
 *       specify the number of worker threads for each server to be
 *       the same as the number of CPUs of the machine.</li>
 *   <li>Additionally, you should specify the number of processors
 *       correctly in the <code>apfloat.properties</code> file
 *       for each cluster server.</li>
 * </ul>
 *
 * Similarly as with {@link PiParallel}, if some nodes have multiple
 * CPUs, to get any performance gain from running many
 * threads in parallel, the JVM must be executing native threads.
 * If the JVM is running in green threads mode, there is no
 * advantage of having multiple threads, as the JVM will in fact
 * execute just one thread and divide its time to multiple
 * simulated threads.
 *
 * @version 1.5.1
 * @author Mikko Tommila
 */

public class PiDistributed
    extends PiParallel
{
    /**
     * Distributed version of the binary splitting algorithm.
     * Uses multiple computers to calculate pi in parallel.
     */

    protected static class DistributedBinarySplittingPiCalculator
        extends ParallelBinarySplittingPiCalculator
    {
        /**
         * Construct a distributed pi calculator with the specified precision and radix.
         *
         * @param series The binary splitting series to be used.
         */

        public DistributedBinarySplittingPiCalculator(BinarySplittingSeries series)
        {
            super(series);
        }

        public void r(final long n1, final long n2, final ApfloatHolder T, final ApfloatHolder Q, final ApfloatHolder P, final ApfloatHolder F, OperationExecutor[] nodes, BinarySplittingProgressIndicator progressIndicator)
            throws ApfloatRuntimeException
        {
            if (!(nodes[0] instanceof Node))
            {
                // Method is running on server side
                // Perform actual calculation or further split to threads

                super.r(n1, n2, T, Q, P, F, nodes, null);
            }
            else if (nodes.length == 1)
            {
                // End of splitting work between nodes
                // Calculate remaining terms on the node
                // Splitting of work continues on the server node using multiple threads

                if (DEBUG) Pi.err.println("PiDistributed.r(" + n1 + ", " + n2 + ") transferring to server side node " + nodes[0]);

                ApfloatHolder[] TQPF = nodes[0].execute(new Operation<ApfloatHolder[]>()
                {
                    public ApfloatHolder[] execute()
                    {
                        // Now get all threads available local on the server
                        OperationExecutor[] threads = DistributedBinarySplittingPiCalculator.super.getNodes();

                        // Continue splitting by threads on server side
                        r(n1, n2, T, Q, P, F, threads, null);

                        return new ApfloatHolder[] { T, Q, P, F };
                    }
                });

                T.setApfloat(TQPF[0].getApfloat());
                Q.setApfloat(TQPF[1].getApfloat());
                if (P != null) P.setApfloat(TQPF[2].getApfloat());
                if (F != null) F.setApfloat(TQPF[3].getApfloat());
            }
            else
            {
                // Multiple nodes available; split work in ratio of node weights and execute in parallel
                // This split is done on the client side

                if (DEBUG) Pi.err.println("PiDistributed.r(" + n1 + ", " + n2 + ") splitting " + formatArray(nodes));

                super.r(n1, n2, T, Q, P, F, nodes, null);
            }
        }

        /**
         * Get the available set of operation executor nodes.
         * This implementation returns {@link RemoteOperationExecutor}s,
         * which execute operations on the cluster's nodes.
         *
         * @return The nodes of the cluster.
         */

        public OperationExecutor[] getNodes()
        {
            ResourceBundle resourceBundle = null;

            try
            {
                resourceBundle = ResourceBundle.getBundle("cluster");
            }
            catch (MissingResourceException mre)
            {
                System.err.println("ResourceBundle \"cluster\" not found");

                System.exit(1);
            }

            Node[] nodes = null;
            List<Node> list = new ArrayList<Node>();
            long totalWeight = 0;
            int weightedNodes = 0;

            // Loop through all properties in the file
            Enumeration<String> keys = resourceBundle.getKeys();
            while (keys.hasMoreElements())
            {
                String key = keys.nextElement();
                // Only process the server properties
                if (key.startsWith("server"))
                {
                    int weight = -1;                    // -1 means unspecified here

                    // Check if a weight is specified for this server
                    try
                    {
                        String weightString = resourceBundle.getString("weight" + key.substring(6));

                        try
                        {
                            weight = Integer.parseInt(weightString);

                            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT)
                            {
                                throw new NumberFormatException(weightString);
                            }

                            weightedNodes++;
                        }
                        catch (NumberFormatException nfe)
                        {
                            System.err.println("Invalid weight: " + nfe.getMessage());

                            System.exit(1);
                        }

                        totalWeight += weight;
                    }
                    catch (MissingResourceException mre)
                    {
                        // Weight not specified, OK
                    }

                    // Parse hostname and port
                    String server = resourceBundle.getString(key);
                    int index = server.indexOf(':');
                    if (index < 0)
                    {
                        System.err.println("No port specified for server: " + server);

                        System.exit(1);
                    }
                    String host = server.substring(0, index),
                           portString = server.substring(index + 1);
                    int port = 0;
                    try
                    {
                        port = Integer.parseInt(portString);
                    }
                    catch (NumberFormatException nfe)
                    {
                        System.err.println("Invalid port for host " + host + ": " + portString);

                        System.exit(1);
                    }

                    list.add(new Node(host, port, weight));
                }
            }

            if (list.size() == 0)
            {
                System.err.println("No nodes for cluster specified");

                System.exit(1);
            }

            nodes = list.toArray(new Node[list.size()]);

            // If no weights were specified at all, all nodes have same weight
            int averageWeight = (weightedNodes == 0 ? 1 : (int) (totalWeight / weightedNodes));

            // Loop through all nodes and set average weight for all nodes that don't have a weight specified
            for (Node node : nodes)
            {
                if (node.getWeight() == -1)
                {
                    node.setWeight(averageWeight);
                }
            }

            // Sort nodes in weight order (smallest first)
            Arrays.sort(nodes);

            // Get the available number of threads for each node
            for (Node node : nodes)
            {
                Integer numberOfProcessors = node.execute(new Operation<Integer>()
                {
                    public Integer execute()
                    {
                        return ApfloatContext.getGlobalContext().getNumberOfProcessors();
                    }
                });

                node.setNumberOfProcessors(numberOfProcessors);
            }

            if (DEBUG) Pi.err.println("PiDistributed.getNodes " + formatArray(nodes));

            return nodes;
        }

        public OperationExecutor[] recombineNodes(OperationExecutor[] nodes, int numberNeeded)
        {
            if (!(nodes[0] instanceof Node))
            {
                // Method is running on server side
                // Recombine threads if applicable

                return super.recombineNodes(nodes, numberNeeded);
            }
            else if (numberNeeded <= nodes.length)
            {
                // Method is running on client side
                // RemoteOperationExecutors can't be combined since they don't exist on the same machine like threads

                if (DEBUG) Pi.err.println("PiDistributed.recombineNodes unable to recombine nodes " + formatArray(nodes) + " (" + numberNeeded + " <= " + nodes.length + ")");

                return nodes;
            }
            else
            {
                // Split RemoteOperationExecutors to executors that don't use all threads available on the server

                SortedSet<Node> allNodes = new TreeSet<Node>(),
                                splittableNodes = new TreeSet<Node>();
                for (OperationExecutor operationExecutor : nodes)
                {
                    Node node = (Node) operationExecutor;
                    (node.getNumberOfProcessors() > 1 ? splittableNodes : allNodes).add(node);
                }

                // Continue splitting heaviest node until no more splits can be made or we have the needed number of nodes
                while (splittableNodes.size() > 0 && allNodes.size() + splittableNodes.size() < numberNeeded)
                {
                    // Get heaviest splittable node
                    Node node = splittableNodes.last();
                    int numberOfProcessors = node.getNumberOfProcessors(),
                        numberOfProcessors1 = numberOfProcessors / 2,
                        numberOfProcessors2 = (numberOfProcessors + 1) / 2;
                    Node node1 = new Node(node.getHost(),
                                          node.getPort(),
                                          node.getWeight() * numberOfProcessors1 / numberOfProcessors,
                                          numberOfProcessors1),
                         node2 = new Node(node.getHost(),
                                          node.getPort(),
                                          node.getWeight() * numberOfProcessors2 / numberOfProcessors,
                                          numberOfProcessors2);
                    splittableNodes.remove(node);
                    (node1.getNumberOfProcessors() > 1 ? splittableNodes : allNodes).add(node1);
                    (node2.getNumberOfProcessors() > 1 ? splittableNodes : allNodes).add(node2);
                }

                allNodes.addAll(splittableNodes);

                Node[] newNodes = allNodes.toArray(new Node[allNodes.size()]);

                if (DEBUG) Pi.err.println("PiDistributed.recombineNodes recombined " + formatArray(nodes) + " to " + formatArray(newNodes) + " (requested " + numberNeeded + ")");

                return newNodes;
            }
        }
    }

    /**
     * Class for calculating pi using the distributed Chudnovskys' binary splitting algorithm.
     */

    public static class DistributedChudnovskyPiCalculator
        extends ParallelChudnovskyPiCalculator
    {
        /**
         * Construct a pi calculator with the specified precision and radix.
         *
         * @param precision The target precision.
         * @param radix The radix to be used.
         */

        public DistributedChudnovskyPiCalculator(long precision, int radix)
            throws ApfloatRuntimeException
        {
            super(new DistributedBinarySplittingPiCalculator(new ChudnovskyBinarySplittingSeries(precision, radix)), precision, radix);
        }
    }

    /**
     * Class for calculating pi using the distributed Ramanujan's binary splitting algorithm.
     */

    public static class DistributedRamanujanPiCalculator
        extends ParallelRamanujanPiCalculator
    {
        /**
         * Construct a pi calculator with the specified precision and radix.
         *
         * @param precision The target precision.
         * @param radix The radix to be used.
         */

        public DistributedRamanujanPiCalculator(long precision, int radix)
            throws ApfloatRuntimeException
        {
            super(new DistributedBinarySplittingPiCalculator(new RamanujanBinarySplittingSeries(precision, radix)), precision, radix);
        }
    }

    // RemoteOperationExecutor that actually implements the weight property
    private static class Node
        extends RemoteOperationExecutor
        implements Comparable<Node>
    {
        public Node(String host, int port, int weight)
        {
            this(host, port, weight, 1);
        }

        public Node(String host, int port, int weight, int numberOfProcessors)
        {
            super(host, port);
            this.weight = weight;
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

        public void setWeight(int weight)
        {
            this.weight = weight;
        }

        public int getWeight()
        {
            return this.weight;
        }

        public void setNumberOfProcessors(int numberOfProcessors)
        {
            this.numberOfProcessors = numberOfProcessors;
        }

        public int getNumberOfProcessors()
        {
            return this.numberOfProcessors;
        }

        public int compareTo(Node that)
        {
            // Must differentiate objects with same weight but that are not the same
            int weightDifference = this.weight - that.weight;
            return (weightDifference != 0 ? weightDifference : this.hashCode() - that.hashCode());      // This is not rock solid...
        }

        public String toString()
        {
            return this.weight + "/" + this.numberOfProcessors;
        }

        private int weight;
        private int numberOfProcessors;
    }

    PiDistributed()
    {
    }

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
            System.err.println("USAGE: PiDistributed digits [method] [radix]");
            System.err.println("    radix must be 2...36");

            return;
        }

        long precision = getPrecision(args[0]);
        int method = (args.length > 1 ? getInt(args[1], "method", 0, 1) : 0),
            radix = (args.length > 2 ? getRadix(args[2]) : ApfloatContext.getContext().getDefaultRadix());

        Operation<Apfloat> operation;

        switch (method)
        {
            case 0:
                operation = new DistributedChudnovskyPiCalculator(precision, radix);
                break;
            default:
                operation = new DistributedRamanujanPiCalculator(precision, radix);
        }

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

    private static final int MIN_WEIGHT = 1,
                             MAX_WEIGHT = 1000;
    private static final boolean DEBUG = false;
}
