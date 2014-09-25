package org.apfloat.samples;

import java.applet.Applet;
import java.awt.Container;
import java.awt.Label;
import java.security.AccessControlException;

import org.apfloat.ApfloatContext;
import org.apfloat.spi.FilenameGenerator;

/**
 * Applet for calculating pi using four different algorithms.
 *
 * @version 1.6
 * @author Mikko Tommila
 */

public class PiApplet
    extends Applet
{
    // Workaround to make this applet run with Microsoft VM and Java 1.4 VMs
    class Handler
        implements PiAWT.StatusIndicator
    {
        public void showStatus(String status)
        {
            PiApplet.this.showStatus(status);
        }

        public Container getContents()
        {
            return new PiAWT(this);
        }

        public void init()
        {
            // Recreate the executor service in case the old thread group was destroyed by reloading the applet
            ApfloatContext ctx = ApfloatContext.getContext();
            ctx.setExecutorService(ApfloatContext.getDefaultExecutorService());

            try
            {
                // The applet may not be able to write files to the current directory, but probably can write to the temp directory
                FilenameGenerator filenameGenerator = new FilenameGenerator(System.getProperty("java.io.tmpdir"), null, null);
                ctx.setFilenameGenerator(filenameGenerator);
            }
            catch (AccessControlException ace)
            {
                // Ignore - reading the system property may not be allowed in unsigned applets
            }
        }
    }

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
        if (System.getProperty("java.version").compareTo("1.5") < 0)
        {
            add(new Label("This applet requires Java 5.0 or later. Download it from http://www.java.com"));
        }
        else
        {
            add(getContents());
            new Handler().init();
        }
    }

    /**
     * Get the graphical elements of this applet.
     *
     * @return The graphical elements of this applet.
     */

    protected Container getContents()
    {
        return new Handler().getContents();
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
               "Written by Mikko Tommila 2002 - 2010\n" +
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
