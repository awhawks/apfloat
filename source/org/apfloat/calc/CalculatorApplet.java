package org.apfloat.calc;

import java.applet.Applet;
import java.awt.Container;
import java.awt.Label;

/**
 * Calculator applet.
 *
 * @version 1.2
 * @author Mikko Tommila
 */

public class CalculatorApplet
    extends Applet
{
    // Workaround to make this applet run with Microsoft VM and Java 1.4 VMs
    private class Handler
    {
        public Container getContents()
        {
            return new CalculatorAWT();
        }
    }

    /**
     * Default constructor.
     */

    public CalculatorApplet()
    {
    }

    /**
     * Initialize this applet.
     */

    public void init()
    {
        if (System.getProperty("java.version").compareTo("1.5") < 0)
        {
            add(new Label("This applet requires Java 5.0 or later. Download it from http://www.java.com"));
        }
        else
        {
            add(new Handler().getContents());
        }
    }

    /**
     * Called when this applet is destroyed.
     */

    public void destroy()
    {
        removeAll();
    }

    /**
     * Get information about this applet.
     *
     * @return Information about this applet.
     */

    public String getAppletInfo()
    {
        return "Calculator applet\n" +
               "Written by Mikko Tommila 2004\n" +
               "Java version: "           + System.getProperty("java.version") + "\n" +
               "Java Virtual Machine: "   + System.getProperty("java.vm.name");
    }
}
