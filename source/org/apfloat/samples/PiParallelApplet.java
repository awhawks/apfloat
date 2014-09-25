package org.apfloat.samples;

import java.awt.Container;

/**
 * Applet for calculating pi using multiple threads in parallel.
 *
 * @version 1.0.2
 * @author Mikko Tommila
 */

public class PiParallelApplet
    extends PiApplet
{
    /**
     * Default constructor.
     */

    public PiParallelApplet()
    {
    }

    protected Container getContents()
    {
        return new PiParallelAWT(this);
    }
}
