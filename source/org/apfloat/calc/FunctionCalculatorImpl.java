package org.apfloat.calc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculator implementation with function support.
 * Provides a mapping mechanism for functions.
 *
 * @version 1.2
 * @author Mikko Tommila
 */

public abstract class FunctionCalculatorImpl
    extends AbstractCalculatorImpl
{
    /**
     * Arbitrary function.
     */

    protected static interface Function
    {
        /**
         * Call the function.
         *
         * @param arguments The function's arguments.
         *
         * @return The function's value.
         *
         * @exception ParseException In case the arguments are invalid.
         */

        public Number call(List<Number> arguments)
            throws ParseException;
    }

    /**
     * Function taking a fixed number of arguments.
     */

    protected static abstract class AbstractFunction
        implements Function
    {
        /**
         * Constructor.
         *
         * @param name The function's name.
         * @param arguments Number of arguments that the function takes.
         */

        protected AbstractFunction(String name, int arguments)
        {
            this.name = name;
            this.arguments = arguments;
        }

        /**
         * Validate the number of arguments.
         *
         * @param arguments The function's arguments.
         *
         * @exception ParseException In case of incorrect number of arguments.
         */

        protected void validate(List<Number> arguments)
            throws ParseException 
        {
            if (arguments.size() != this.arguments)
            {
                throw new ParseException("Function " + this.name + " takes " + this.arguments + " argument" + (this.arguments == 1 ? "" : "s") + ", not " + arguments.size());
            }
        }

        private String name;
        private int arguments;
    }

    /**
     * Function taking one argument.
     */

    protected abstract class Function1
        extends AbstractFunction
    {
        /**
         * Constructor.
         *
         * @param name The function's name.
         */

        protected Function1(String name)
        {
            super(name, 1);
        }

        public final Number call(List<Number> arguments)
            throws ParseException 
        {
            validate (arguments);
            return promote(call(getFunctions(arguments), arguments.get(0)));
        }

        /**
         * Call the function.
         *
         * @param functions The function implementations.
         * @param argument The function's argument.
         *
         * @return The function's value.
         */

        protected abstract Number call(Functions functions, Number argument);
    }

    /**
     * Function taking two arguments.
     */

    protected abstract class Function2
        extends AbstractFunction
    {
        /**
         * Constructor.
         *
         * @param name The function's name.
         */

        protected Function2(String name)
        {
            super(name, 2);
        }

        public final Number call(List<Number> arguments)
            throws ParseException 
        {
            validate (arguments);
            return promote(call(getFunctions(arguments), arguments.get(0), arguments.get(1)));
        }

        /**
         * Call the function.
         *
         * @param functions The function implementations.
         * @param argument1 The function's first argument.
         * @param argument2 The function's second argument.
         *
         * @return The function's value.
         */

        protected abstract Number call(Functions functions, Number argument1, Number argument2);
    }

    /**
     * Function implementations.
     */

    protected static interface Functions
    {
        public Number negate(Number x);
        public Number add(Number x, Number y);
        public Number subtract(Number x, Number y);
        public Number multiply(Number x, Number y);
        public Number divide(Number x, Number y);
        public Number mod(Number x, Number y);
        public Number pow(Number x, Number y);

        public Number arg(Number x);
        public Number conj(Number x);
        public Number imag(Number x);
        public Number real(Number x);

        public Number abs(Number x);
        public Number acos(Number x);
        public Number acosh(Number x);
        public Number asin(Number x);
        public Number asinh(Number x);
        public Number atan(Number x);
        public Number atanh(Number x);
        public Number cbrt(Number x);
        public Number ceil(Number x);
        public Number cos(Number x);
        public Number cosh(Number x);
        public Number exp(Number x);
        public Number factorial(Number x);
        public Number floor(Number x);
        public Number log(Number x);
        public Number pi(Number x);
        public Number sin(Number x);
        public Number sinh(Number x);
        public Number sqrt(Number x);
        public Number tan(Number x);
        public Number tanh(Number x);
        public Number truncate(Number x);

        public Number agm(Number x, Number y);
        public Number atan2(Number x, Number y);
        public Number copySign(Number x, Number y);
        public Number fmod(Number x, Number y);
        public Number gcd(Number x, Number y);
        public Number hypot(Number x, Number y);
        public Number inverseRoot(Number x, Number y);
        public Number lcm(Number x, Number y);
        public Number root(Number x, Number y);
        public Number scale(Number x, Number y);
        public Number precision(Number x, Number y);
    }

    /**
     * Default constructor.
     */

    protected FunctionCalculatorImpl()
    {
        this.functions = new HashMap<String, Function>();

        setFunction("negate", new Function1("negate") { protected Number call(Functions functions, Number argument) { return functions.negate(argument); } });
        setFunction("add", new Function2("add") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.add(argument1, argument2); } });
        setFunction("subtract", new Function2("subtract") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.subtract(argument1, argument2); } });
        setFunction("multiply", new Function2("multiply") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.multiply(argument1, argument2); } });
        setFunction("divide", new Function2("divide") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.divide(argument1, argument2); } });
        setFunction("mod", new Function2("mod") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.mod(argument1, argument2); } });
        setFunction("pow", new Function2("pow") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.pow(argument1, argument2); } });

        setFunction("abs", new Function1("abs") { protected Number call(Functions functions, Number argument) { return functions.abs(argument); } });
        setFunction("acos", new Function1("acos") { protected Number call(Functions functions, Number argument) { return functions.acos(argument); } });
        setFunction("acosh", new Function1("acosh") { protected Number call(Functions functions, Number argument) { return functions.acosh(argument); } });
        setFunction("asin", new Function1("asin") { protected Number call(Functions functions, Number argument) { return functions.asin(argument); } });
        setFunction("asinh", new Function1("asinh") { protected Number call(Functions functions, Number argument) { return functions.asinh(argument); } });
        setFunction("atan", new Function1("atan") { protected Number call(Functions functions, Number argument) { return functions.atan(argument); } });
        setFunction("atanh", new Function1("atanh") { protected Number call(Functions functions, Number argument) { return functions.atanh(argument); } });
        setFunction("cbrt", new Function1("cbrt") { protected Number call(Functions functions, Number argument) { return functions.cbrt(argument); } });
        setFunction("ceil", new Function1("ceil") { protected Number call(Functions functions, Number argument) { return functions.ceil(argument); } });
        setFunction("cos", new Function1("cos") { protected Number call(Functions functions, Number argument) { return functions.cos(argument); } });
        setFunction("cosh", new Function1("cosh") { protected Number call(Functions functions, Number argument) { return functions.cosh(argument); } });
        setFunction("exp", new Function1("exp") { protected Number call(Functions functions, Number argument) { return functions.exp(argument); } });
        setFunction("factorial", new Function1("factorial") { protected Number call(Functions functions, Number argument) { return functions.factorial(argument); } });
        setFunction("floor", new Function1("floor") { protected Number call(Functions functions, Number argument) { return functions.floor(argument); } });
        setFunction("log", new Function1("log") { protected Number call(Functions functions, Number argument) { return functions.log(argument); } });
        setFunction("pi", new Function1("pi") { protected Number call(Functions functions, Number argument) { return functions.pi(argument); } });
        setFunction("sin", new Function1("sin") { protected Number call(Functions functions, Number argument) { return functions.sin(argument); } });
        setFunction("sinh", new Function1("sinh") { protected Number call(Functions functions, Number argument) { return functions.sinh(argument); } });
        setFunction("sqrt", new Function1("sqrt") { protected Number call(Functions functions, Number argument) { return functions.sqrt(argument); } });
        setFunction("tan", new Function1("tan") { protected Number call(Functions functions, Number argument) { return functions.tan(argument); } });
        setFunction("tanh", new Function1("tanh") { protected Number call(Functions functions, Number argument) { return functions.tanh(argument); } });
        setFunction("truncate", new Function1("truncate") { protected Number call(Functions functions, Number argument) { return functions.truncate(argument); } });

        setFunction("arg", new Function1("arg") { protected Number call(Functions functions, Number argument) { return functions.arg(argument); } });
        setFunction("conj", new Function1("conj") { protected Number call(Functions functions, Number argument) { return functions.conj(argument); } });
        setFunction("imag", new Function1("imag") { protected Number call(Functions functions, Number argument) { return functions.imag(argument); } });
        setFunction("real", new Function1("real") { protected Number call(Functions functions, Number argument) { return functions.real(argument); } });

        setFunction("agm", new Function2("agm") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.agm(argument1, argument2); } });
        setFunction("atan2", new Function2("atan2") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.atan2(argument1, argument2); } });
        setFunction("copySign", new Function2("copySign") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.copySign(argument1, argument2); } });
        setFunction("fmod", new Function2("fmod") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.fmod(argument1, argument2); } });
        setFunction("gcd", new Function2("gcd") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.gcd(argument1, argument2); } });
        setFunction("hypot", new Function2("hypot") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.hypot(argument1, argument2); } });
        setFunction("inverseRoot", new Function2("inverseRoot") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.inverseRoot(argument1, argument2); } });
        setFunction("lcm", new Function2("lcm") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.lcm(argument1, argument2); } });
        setFunction("root", new Function2("root") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.root(argument1, argument2); } });
        setFunction("scale", new Function2("scale") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.scale(argument1, argument2); } });
        setFunction("n", new Function2("precision") { protected Number call(Functions functions, Number argument1, Number argument2) { return functions.precision(argument1, argument2); } });
    }

    public Number function(String name, List<Number> arguments)
        throws ParseException
    {
        Function function = functions.get(name);
        if (function == null)
        {
            throw new ParseException("Invalid function: " + name);
        }
        return function.call(arguments);
    }

    private Functions getFunctions(List<Number> arguments)
    {
        Functions functions = null;
        for (Number argument : arguments)
        {
            Functions functions2 = getFunctions(argument);
            functions = (functions != null && functions.getClass().isAssignableFrom(functions2.getClass()) ? functions : functions2);
        }
        return functions;
    }

    /**
     * Define a function.
     *
     * @param name The function name.
     * @param function The function.
     */

    protected void setFunction(String name, Function function)
    {
        this.functions.put(name, function);
    }

    /**
     * Get the function implementations.
     *
     * @param x The number to use as the function argument.
     *
     * @return The function implementations.
     */

    protected abstract Functions getFunctions(Number x);

    /**
     * Promote a number to a more specific class.
     *
     * @param x The argument.
     *
     * @return The argument, possibly converted to a more specific subclass.
     */

    protected abstract Number promote(Number x);

    private Map<String, Function> functions;
}
