package org.apfloat.samples;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Container;
import java.awt.Panel;
import java.awt.Label;
import java.awt.Choice;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.TextField;
import java.awt.TextArea;
import java.awt.Button;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import javax.imageio.spi.ServiceRegistry;       // Silly that this is under javax.imageio

import org.apfloat.ApfloatContext;
import org.apfloat.spi.BuilderFactory;

/**
 * Graphical AWT elements for calculating pi using three different algorithms.
 *
 * @version 1.0.2
 * @author Mikko Tommila
 */

public class PiAWT
    extends Panel
{
    /**
     * Interface to indicate an error status in the application.
     */

    public static interface StatusIndicator
    {
        /**
         * Show the specified error status.
         *
         * @param status The status.
         */

        public void showStatus(String status);
    }

    /**
     * Construct a panel with graphical elements.
     *
     * @param statusIndicator Handler for showing error messages in the application.
     */

    public PiAWT(StatusIndicator statusIndicator)
    {
        this.statusIndicator = statusIndicator;
        initGUI();
    }

    // Initialize the container and add the graphical elements to it
    private void initGUI()
    {
        ApfloatContext ctx = ApfloatContext.getContext();

        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.weightx = 1;
        constraints.weighty = 1;

        this.precisionLabel = new Label("Precision:");
        add(this.precisionLabel, constraints);

        this.precisionField = new TextField("133000", 15);
        add(this.precisionField, constraints);

        this.radixLabel = new Label("Radix:");
        add(this.radixLabel, constraints);

        this.radixChoice = new Choice();
        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++)
        {
            this.radixChoice.add(String.valueOf(i));
        }
        this.radixChoice.select(ctx.getProperty(ApfloatContext.DEFAULT_RADIX));
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(this.radixChoice, constraints);

        constraints.gridwidth = 1;
        constraints.gridheight = 2;
        initMethod(this, constraints);

        this.implementationLabel = new Label("Implementation:");
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        add(this.implementationLabel, constraints);

        this.implementationChoice = new Choice();
        this.builderFactories = new ArrayList();
        Iterator providers = ServiceRegistry.lookupProviders(org.apfloat.spi.BuilderFactory.class);
        while (providers.hasNext())
        {
            Object builderFactory = providers.next();
            this.builderFactories.add(builderFactory);
            this.implementationChoice.add(builderFactory.getClass().getName());
        }
        this.implementationChoice.select(0);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(this.implementationChoice, constraints);

        this.goButton = new Button("Go!");
        constraints.gridwidth = 1;
        add(this.goButton, constraints);

        abortButton = new Button("Abort!");
        this.abortButton.setEnabled(false);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(this.abortButton, constraints);

        this.statusLabel = new Label("Status:");
        add(this.statusLabel, constraints);

        this.statusArea = new TextArea(null, 5, 80, TextArea.SCROLLBARS_VERTICAL_ONLY);
        this.statusArea.setEditable(false);
        add(this.statusArea, constraints);

        this.resultLabel = new Label("Result:");
        add(this.resultLabel, constraints);

        this.resultArea = new TextArea(null, 5, 80, TextArea.SCROLLBARS_VERTICAL_ONLY);
        this.resultArea.setEditable(false);
        add(this.resultArea, constraints);

        this.goButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (isInputValid())
                {
                    PiAWT.this.statusArea.setText(null);
                    PiAWT.this.resultArea.setText(null);
                    PiAWT.this.goButton.setEnabled(false);
                    startThread();
                }
            }
        });

        this.abortButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                PiAWT.this.abortButton.setEnabled(false);
                stopThread();
            }
        });
    }

    /**
     * Initialize the "method" section GUI elements.
     * Two elements should be added to the <code>container</code>.
     *
     * @param container The container where the elements are to be added.
     * @param constraints The constraints with which the elements are to be added to the <code>container</code>.
     */

    protected void initMethod(Container container, Object constraints)
    {
        this.methodLabel = new Label("Method:");
        container.add(this.methodLabel, constraints);

        Panel panel = new Panel(new GridBagLayout());
        GridBagConstraints panelConstraints = new GridBagConstraints();
        panelConstraints.gridwidth = GridBagConstraints.REMAINDER;
        panelConstraints.anchor = GridBagConstraints.NORTHWEST;
        panelConstraints.weightx = 1;
        panelConstraints.weighty = 1;

        this.methods = new CheckboxGroup();
        this.chudnovsky = new Checkbox("Chudnovsky", true, this.methods);
        panel.add(this.chudnovsky, panelConstraints);

        this.gaussLegendre = new Checkbox("Gauss-Legendre", false, this.methods);
        panel.add(this.gaussLegendre, panelConstraints);

        this.borwein = new Checkbox("Borwein", false, this.methods);
        panel.add(this.borwein, panelConstraints);

        container.add(panel, constraints);
    }

    /**
     * Validates the input fields.
     *
     * @return <code>true</code> if all input fields contain valid values, otherwise <code>false</code>.
     */

    protected boolean isInputValid()
    {
        String precisionString = this.precisionField.getText();
        try
        {
            long precision = Long.parseLong(precisionString);
            if (precision <= 0)
            {
                throw new NumberFormatException();
            }
            showStatus(null);
            return true;
        }
        catch (NumberFormatException nfe)
        {
            showStatus("Invalid precision: " + precisionString);
            this.precisionField.requestFocus();
            return false;
        }
    }

    /**
     * Show the specified error status.
     *
     * @param status The status.
     */

    protected void showStatus(String status)
    {
        this.statusIndicator.showStatus(status);
    }

    // Prints output to a text area
    private class FlushStringWriter
        extends StringWriter
    {
        public FlushStringWriter(TextArea dst)
        {
            this.dst = dst;
            this.length = 0;
            this.position = 0;
            this.lastLinefeedPosition = 0;
        }

        public void flush()
        {
            super.flush();

            StringBuffer buffer = getBuffer();
            String text = buffer.toString();
            this.dst.replaceRange(text, this.position, this.length);
            this.position += text.length();
            this.length = this.position;
            if (text.endsWith(System.getProperty("line.separator")))
            {
                this.lastLinefeedPosition = this.position;
            }
            else if (text.endsWith("\r"))
            {
                this.position = this.lastLinefeedPosition;
            }

            buffer.setLength(0);
        }

        private TextArea dst;
        private int length,
                    position,
                    lastLinefeedPosition;
    }

    /**
     * Get the calculation operation to execute.
     *
     * @param precision The precision to be used.
     * @param radix The radix to be used.
     *
     * @return The calculation operation to execute.
     */

    protected Operation getOperation(long precision, int radix)
    {
        if (this.chudnovsky.getState())
        {
            return new Pi.ChudnovskyPiCalculator(precision, radix);
        }
        else if (this.gaussLegendre.getState())
        {
            return new Pi.GaussLegendrePiCalculator(precision, radix);
        }
        else
        {
            return new Pi.BorweinPiCalculator(precision, radix);
        }
    }

    private void startThread()
    {
        // Writer for writing standard output to the result area
        Pi.setOut(new PrintWriter(new FlushStringWriter(PiAWT.this.resultArea), true));

        // Writer for writing standard error output to the status area
        Pi.setErr(new PrintWriter(new FlushStringWriter(PiAWT.this.statusArea), true));

        // Set the selected builder factory
        ApfloatContext ctx = ApfloatContext.getContext();
        BuilderFactory builderFactory = (BuilderFactory) this.builderFactories.get(this.implementationChoice.getSelectedIndex());
        ctx.setBuilderFactory(builderFactory);

        // Thread for calculating pi and showing the result
        this.calculatorThread = new Thread()
        {
            public void run()
            {
                long precision = Long.parseLong(PiAWT.this.precisionField.getText());
                int radix = Integer.parseInt(PiAWT.this.radixChoice.getSelectedItem());
                Operation operation = getOperation(precision, radix);

                try
                {
                    Pi.run(precision, radix, operation);
                }
                catch (ThreadDeath td)
                {
                    aborted();
                }
                catch (AssertionError ae)
                {
                    crashed(ae);
                }
                catch (Exception e)
                {
                    crashed(e);
                }
                finally
                {
                    end();
                }
            }
        };

        Pi.setAlive(true);
        this.calculatorThread.start();
        this.abortButton.setEnabled(true);
    }

    private void stopThread()
    {
        Pi.setAlive(false);
    }

    private void aborted()
    {
        Pi.getErr().println("Aborted");
    }

    private void crashed(Throwable cause)
    {
        Pi.getErr().println("Crashed with " + cause);
    }

    private void end()
    {
        this.abortButton.setEnabled(false);
        this.goButton.setEnabled(true);
        System.gc();                            // Garbage collection may not have run perfectly by this point
    }

    private StatusIndicator statusIndicator;

    private Label precisionLabel;
    private TextField precisionField;
    private Label radixLabel;
    private Choice radixChoice;
    private Label methodLabel;
    private Checkbox chudnovsky;
    private CheckboxGroup methods;
    private Checkbox gaussLegendre;
    private Checkbox borwein;
    private Label implementationLabel;
    private Choice implementationChoice;
    private Button goButton;
    private Button abortButton;
    private Label statusLabel;
    private TextArea statusArea;
    private Label resultLabel;
    private TextArea resultArea;

    private List builderFactories;
    private Thread calculatorThread;
}
