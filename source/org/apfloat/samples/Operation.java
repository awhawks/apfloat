package org.apfloat.samples;

import java.io.Serializable;

/**
 * Interface for implementing arbitrary operations to be executed.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public interface Operation
    extends Serializable
{
    /**
     * Executes some code, returning a value.
     *
     * @return Return value of the operation.
     */

    public Object execute();
}
