package org.apfloat.samples;

/**
 * Class for running an {@link Operation} in the background in a separate thread.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class BackgroundOperation
{
    /**
     * Runs an operation in the background in a separate thread.
     * The thread is started immediately.
     *
     * @param operation The operation to execute.
     */

    public BackgroundOperation(final Operation operation)
    {
        this.thread = new Thread()
        {
            public void run()
            {
                try
                {
                    BackgroundOperation.this.result = operation.execute();
                }
                catch (RuntimeException re)
                {
                    BackgroundOperation.this.exception = re;
                }
            }
        };

        this.thread.start();
    }

    /**
     * Check if the operation has been completed.
     *
     * @return <code>true</code> if the execution of the operation has been completed, otherwise <code>false</code>.
     */

    public boolean isFinished()
    {
        return !this.thread.isAlive();
    }

    /**
     * Get the result of the operation.
     * This method blocks until the operation has been completed.
     *
     * @return Result of the operation.
     *
     * @exception RuntimeException If a RuntimeException was thrown by the executed operation, it's thrown by this method.
     */

    public Object getResult()
    {
        while (!isFinished())
        {
            try
            {
                this.thread.join();
            }
            catch (InterruptedException ie)
            {
                // Propagate to exit thread
                throw new RuntimeException(ie);
            }
        }

        if (this.exception != null)
        {
            throw this.exception;
        }
        else
        {
            return this.result;
        }
    }

    private Thread thread;
    private Object result;
    private RuntimeException exception;
}
