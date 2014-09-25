package org.apfloat.samples;

/**
 * Interface for implementing objects that can execute {@link Operation}s.
 * An operation can e.g. be executed locally or remotely.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public interface OperationExecutor
{
    /**
     * Executes some code, returning a value.
     *
     * @param operation The operation to execute.
     *
     * @return Return value of the operation.
     */

    public Object execute(Operation operation);

    /**
     * Starts executing some code in the background.
     *
     * @param operation The operation to execute in the background.
     *
     * @return An object for retrieving the result of the operation later.
     */

    public BackgroundOperation executeBackground(Operation operation);

    /**
     * Returns the relative weight of this executor.
     * The weights of different operation executors can be used
     * to distribute work more equally.
     *
     * @return The relative weight of this operation executor.
     */

    public int getWeight();
}
