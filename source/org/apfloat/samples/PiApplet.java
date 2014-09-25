package org.apfloat.samples;

import java.applet.Applet;
import java.awt.Container;
import java.awt.Label;

import org.apfloat.ApfloatContext;
import org.apfloat.spi.BuilderFactory;

/**
 * Applet for calculating pi using three different algorithms.
 *
 * @version 1.0.2
 * @author Mikko Tommila
 */

public class PiApplet
    extends Applet
    implements PiAWT.StatusIndicator
{
    /**
     * Default constructor.
     */

    public PiApplet()
    {
    }

    /**
     * Initialize this applet.
     */

    public void init()
    {
        if (System.getProperty("java.version").compareTo("1.4") < 0)
        {
            add(new Label("This applet requires Java 1.4 or later. Download it from http://www.java.com"));
        }
        else
        {
            add(getContents());
        }
    }

    /**
     * Get the graphical elements of this applet.
     *
     * @return The graphical elements of this applet.
     */

    protected Container getContents()
    {
        return new PiAWT(this);
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
        Object builderFactory = ApfloatContext.getContext().getBuilderFactory();
        Package specificationPackage = Package.getPackage("org.apfloat"),
                implementationPackage = builderFactory.getClass().getPackage();

        return "Pi calculation applet\n" +
               "Written by Mikko Tommila 2002 - 2003\n" +
               "Specification-Title: "    + specificationPackage.getSpecificationTitle() + "\n" +
               "Specification-Version: "  + specificationPackage.getSpecificationVersion() + "\n" +
               "Specification-Vendor: "   + specificationPackage.getSpecificationVendor() + "\n" +
               "Implementation-Title: "   + implementationPackage.getImplementationTitle() + "\n" +
               "Implementation-Version: " + implementationPackage.getImplementationVersion() + "\n" +
               "Implementation-Vendor: "  + implementationPackage.getImplementationVendor() + "\n" +
               "Java version: "           + System.getProperty("java.version") + "\n" +
               "Java Virtual Machine: "   + System.getProperty("java.vm.name");
    }
}
