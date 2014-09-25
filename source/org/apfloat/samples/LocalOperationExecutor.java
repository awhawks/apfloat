package org.apfloat.samples;

/**
 * Class to execute {@link Operation}s locally.
 * The execution is done in the current JVM.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class LocalOperationExecutor
    implements OperationExecutor
{
    /**
     * Default constructor.
     */

    public LocalOperationExecutor()
    {
    }

    /**
     * Execute an operation immediately.
     * This method will block until the operation is complete.
     *
     * @param operation The operation to execute.
     *
     * @return The result of the operation.
     */

    public Object execute(Operation operation)
    {
        return operation.execute();
    }

    /**
     * Execute an operation in the background.
     * This method starts a new thread executing the operation and returns immediately.
     *
     * @param operation The operation to execute in the background.
     *
     * @return A {@link BackgroundOperation} for retrieving the result of the operation later.
     */

    public BackgroundOperation executeBackground(Operation operation)
    {
        return new BackgroundOperation(operation);
    }

    public int getWeight()
    {
        return 1;
    }
}
