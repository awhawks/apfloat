package org.apfloat;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Enumeration;
import java.util.Collections;
import java.util.Map;
import java.util.Hashtable;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import org.apfloat.spi.BuilderFactory;
import org.apfloat.spi.FilenameGenerator;
import org.apfloat.spi.Util;

/**
 * This class encapsulates the information needed by the apfloat implementation
 * to perform computations.<p>
 *
 * All environment related settings of an apfloat implementation are accessed
 * through an <code>ApfloatContext</code>. Such settings include for example
 * the implementation provider class, maximum memory size to be used, and
 * the file names that are used for temporary files.<p>
 *
 * For performance reasons, access to an <code>ApfloatContext</code> is not
 * synchronized. Presumably, this won't be a problem in most cases. But, if
 * the code needs to concurrently modify and access an ApfloatContext, all
 * access to it should be externally synchronized.<p>
 *
 * At simplest, there is just one <code>ApfloatContext</code>, the global
 * apfloat context. All settings in your application are retrieved through
 * it. The global context is created when the <code>ApfloatContext</code>
 * class is loaded, and it's thus always available.<p>
 *
 * Values for the different settings in the global apfloat context are specified
 * in the <code>apfloat.properties</code> file, found in the class path. Since
 * they are loaded via a <code>ResourceBundle</code> named "apfloat", you can
 * alternatively deploy a <code>ResourceBundle</code> class named "apfloat" in
 * your class path to avoid having a .properties file, or to define properties
 * dynamically at run time.<p>
 *
 * The different settings that can be specified in the <code>apfloat.properties</code>
 * file are as follows:<p>
 *
 * <ul>
 *   <li><code>builderFactory</code>, name of the class set as in {@link #setBuilderFactory(BuilderFactory)}</li>
 *   <li><code>defaultRadix</code>, set as in {@link #setDefaultRadix(int)}</li>
 *   <li><code>maxMemoryBlockSize</code>, set as in {@link #setMaxMemoryBlockSize(long)}</li>
 *   <li><code>cacheL1Size</code>, set as in {@link #setCacheL1Size(int)}</li>
 *   <li><code>cacheL2Size</code>, set as in {@link #setCacheL2Size(int)}</li>
 *   <li><code>cacheBurst</code>, set as in {@link #setCacheBurst(int)}</li>
 *   <li><code>memoryTreshold</code>, set as in {@link #setMemoryTreshold(int)}</li>
 *   <li><code>blockSize</code>, set as in {@link #setBlockSize(int)}</li>
 *   <li><code>numberOfProcessors</code>, set as in {@link #setNumberOfProcessors(int)}</li>
 *   <li><code>filePath</code>, set as in {@link #setProperty(String,String)} with property name {@link #FILE_PATH}</li>
 *   <li><code>fileInitialValue</code>, set as in {@link #setProperty(String,String)} with property name {@link #FILE_INITIAL_VALUE}</li>
 *   <li><code>fileSuffix</code>, set as in {@link #setProperty(String,String)} with property name {@link #FILE_SUFFIX}</li>
 *   <li><code>cleanupAtExit</code>, set as in {@link #setCleanupAtExit(boolean)}</li>
 * </ul>
 *
 * An example <code>apfloat.properties</code> file could contain the following:<p>
 *
 * <pre>
 * builderFactory=org.apfloat.internal.IntBuilderFactory
 * defaultRadix=10
 * maxMemoryBlockSize=50331648
 * cacheL1Size=8192
 * cacheL2Size=262144
 * cacheBurst=32
 * memoryTreshold=65536
 * blockSize=65536
 * numberOfProcessors=1
 * filePath=
 * fileInitialValue=0
 * fileSuffix=.ap
 * cleanupAtExit=true
 * </pre>
 *
 * The total memory size and the number of processors are detected automatically,
 * as reported by the Java runtime, if they are not specified in the configuration
 * bundle.<p>
 *
 * If you need to create a complex multithreaded application that performs
 * apfloat calculations in parallel using multiple threads, you may need to
 * change the ApfloatContext settings for the different working threads.<p>
 *
 * If thread-specific apfloat contexts are not specified, all threads will use
 * the global context. To set a thread specific context, you would typically
 * create a {@link #clone()} of the global (or other parent) context, and then
 * set that context to the thread using {@link #setThreadContext(ApfloatContext,Thread)}.
 * Note that if you do not create a clone of the context, the same context will still
 * be used, since it's passed by reference.<p>
 *
 * Typically you may need to set the following properties for each thread:
 * <ul>
 *   <li>{@link #setNumberOfProcessors(int)}: Since the number of physical
 *       processors available is fixed, you may want to limit the amount
 *       of processors each thread can use. In many cases you will want each
 *       thread to use exactly one processor, and create as many threads as
 *       there are processors.</li>
 *   <li>{@link #setMaxMemoryBlockSize(long)}: The physical memory is global
 *       and its amount is fixed as well. Since all threads share the global
 *       memory, you may want to limit the maximum amount of memory each thread
 *       can use. If you do this, you will probably just split the amount of
 *       memory between the threads, e.g. by dividing it equally. In this case
 *       you should set each thread to have a separate shared memory lock with
 *       {@link #setSharedMemoryLock(Object)}. In this solution all threads can
 *       allocate their maximum allowed memory block at the same time, and still
 *       the VM won't run out of memory.<br>
 *       Another possibility is to set the whole global memory size as the maximum
 *       available for each thread, and use the same shared memory lock for every
 *       thread. This is actually the default behavior, if you don't call
 *       {@link #setMaxMemoryBlockSize(long)} nor {@link #setSharedMemoryLock(Object)}.
 *       This way all threads can access the maximum amount of physical memory
 *       available. The drawback is that the threads will synchronize on the
 *       same memory block, so only one thread can use it at a time. This can
 *       obviously have a huge effect on performance, since you can end up in a
 *       situation where all but one thread are idle most of the time. Note that
 *       the shared memory will be used for all data blocks larger than the
 *       memory treshold (see {@link #getMemoryTreshold()}). </li>
 *   <li>{@link #setFilenameGenerator(FilenameGenerator)}: When you clone an
 *       ApfloatContext, the filename generator is by default shared. For most
 *       situations this is fine. If you for some reason want to separate
 *       the files generated in each thread, you can just set a new
 *       FilenameGenerator for each thread. In this case it's essential to
 *       configure the FilenameGenerators not to generate conflicting file
 *       names. You can do this easily by specifying a different directory or
 *       file suffix for each filename generator, or by specifying a different
 *       starting value for the file names (e.g. 1000000, 2000000, 3000000, ...).<br>
 *       Setting the filename generator may also be relevant, if you use a
 *       distributed computing platform where separate physical machines (or
 *       at least separate VM processes) create temporary files in the same
 *       shared disk storage. In this case it's also essential to configure the
 *       different processes so that they do not generate conflicting file names.</li>
 * </ul>
 *
 * The other settings are generally global and do not typically need to
 * be set differently for each thread.<p>
 *
 * Unfortunately, Java doesn't allow detecting automatically many of the
 * settings, such as cache sizes. Also, for optimal performance, it would
 * usually be desirable to set each thread's processor affinity (which
 * physical processor runs which thread), which is also not possible.
 * If these features are added to the Java platform in the future, they
 * may be added to the <code>ApfloatContext</code> API as well.
 *
 * @version 1.3
 * @author Mikko Tommila
 */

public class ApfloatContext
    implements Cloneable
{
    /**
     * Property name for specifying the apfloat builder factory class.
     */

    public static final String BUILDER_FACTORY = "builderFactory";

    /**
     * Property name for specifying the default radix.
     */

    public static final String DEFAULT_RADIX = "defaultRadix";

    /**
     * Property name for specifying the maximum memory block size.
     */

    public static final String MAX_MEMORY_BLOCK_SIZE = "maxMemoryBlockSize";

    /**
     * Property name for specifying the level 1 cache size.
     */

    public static final String CACHE_L1_SIZE = "cacheL1Size";

    /**
     * Property name for specifying the level 2 cache size.
     */

    public static final String CACHE_L2_SIZE = "cacheL2Size";

    /**
     * Property name for specifying the level 1 cache burst size.
     */

    public static final String CACHE_BURST = "cacheBurst";

    /**
     * Property name for specifying the apfloat memory treshold.
     */

    public static final String MEMORY_TRESHOLD = "memoryTreshold";

    /**
     * Property name for specifying the I/O block size.
     */

    public static final String BLOCK_SIZE = "blockSize";

    /**
     * Property name for specifying the number of processors available.
     */

    public static final String NUMBER_OF_PROCESSORS = "numberOfProcessors";

    /**
     * Property name for specifying the temporary file path.
     */

    public static final String FILE_PATH = "filePath";

    /**
     * Property name for specifying the temporary file initial value.
     */

    public static final String FILE_INITIAL_VALUE = "fileInitialValue";

    /**
     * Property name for specifying the temporary file suffix.
     */

    public static final String FILE_SUFFIX = "fileSuffix";

    /**
     * Property name for specifying if clean-up should be done at program exit.
     */

    public static final String CLEANUP_AT_EXIT = "cleanupAtExit";

    // At system exit, run garbage collection and finalization to clean up temporary files
    private static class CleanupThread
        extends Thread
    {
        public CleanupThread()
        {
            super("apfloat shutdown clean-up thread");
        }

        public void run()
        {
            ApfloatMath.cleanUp();      // Clear references to static cached apfloats
            System.gc();
            System.gc();
            System.runFinalization();
        }
    }

    /**
     * Create a new ApfloatContext using the specified properties.
     *
     * @param properties The properties for the ApfloatContext.
     */

    public ApfloatContext(Properties properties)
        throws ApfloatRuntimeException
    {
        this.properties = (Properties) ApfloatContext.defaultProperties.clone();
        this.properties.putAll(properties);

        setProperties(this.properties);
    }

    /**
     * Get the ApfloatContext for the calling thread. If a thread-specific
     * context has not been specified, the global context is returned.
     *
     * @return The ApfloatContext for the calling thread.
     */

    public static ApfloatContext getContext()
    {
        synchronized (ApfloatContext.threadContexts)    // Avoid two monitor entry/exit blocks in multi-thread environments
        {
            if (ApfloatContext.threadContexts.isEmpty())
            {
                return getGlobalContext();
            }
            else
            {
                ApfloatContext ctx = getThreadContext();

                if (ctx == null)
                {
                    ctx = getGlobalContext();
                }

                return ctx;
            }
        }
    }

    /**
     * Get the global ApfloatContext.
     *
     * @return The global ApfloatContext.
     */

    public static ApfloatContext getGlobalContext()
    {
        return ApfloatContext.globalContext;
    }

    /**
     * Get the thread-specific ApfloatContext for the calling thread.
     *
     * @return The ApfloatContext for the calling thread, or <code>null</code> if one has not been specified.
     */

    public static ApfloatContext getThreadContext()
    {
        return getThreadContext(Thread.currentThread());
    }

    /**
     * Get the thread-specific ApfloatContext for the specified thread.
     *
     * @param thread The thread whose ApfloatContext is to be returned.
     *
     * @return The ApfloatContext for the specified thread, or <code>null</code> if one has not been specified.
     */

    public static ApfloatContext getThreadContext(Thread thread)
    {
        return ApfloatContext.threadContexts.get(thread);
    }

    /**
     * Set the thread-specific ApfloatContext for the calling thread.
     *
     * @param threadContext The ApfloatContext for the calling thread.
     */

    public static void setThreadContext(ApfloatContext threadContext)
    {
        setThreadContext(threadContext, Thread.currentThread());
    }

    /**
     * Set the thread-specific ApfloatContext for the specified thread.
     *
     * @param threadContext The ApfloatContext for the specified thread.
     * @param thread The thread whose ApfloatContext is to be set.
     */

    public static void setThreadContext(ApfloatContext threadContext, Thread thread)
    {
        ApfloatContext.threadContexts.put(thread, threadContext);
    }

    /**
     * Removes the thread-specific context for the current thread.
     */

    public static void removeThreadContext()
    {
        removeThreadContext(Thread.currentThread());
    }

    /**
     * Removes the thread-specific context for the specified thread.
     *
     * @param thread The thread whose ApfloatContext is to be removed.
     */

    public static void removeThreadContext(Thread thread)
    {
        ApfloatContext.threadContexts.remove(thread);
    }

    /**
     * Removes all thread-specific ApfloatContexts.
     */

    public static void clearThreadContexts()
    {
        ApfloatContext.threadContexts.clear();
    }

    /**
     * Get the BuilderFactory.
     *
     * @return The BuilderFactory for this ApfloatContext.
     */

    public BuilderFactory getBuilderFactory()
    {
        return this.builderFactory;
    }

    /**
     * Set the BuilderFactory.
     *
     * @param builderFactory The BuilderFactory for this ApfloatContext.
     */

    public void setBuilderFactory(BuilderFactory builderFactory)
    {
        this.properties.setProperty(BUILDER_FACTORY, builderFactory.getClass().getName());
        this.builderFactory = builderFactory;
    }

    /**
     * Get the FilenameGenerator.
     *
     * @return The FilenameGenerator for this ApfloatContext.
     */

    public FilenameGenerator getFilenameGenerator()
    {
        return this.filenameGenerator;
    }

    /**
     * Set the FilenameGenerator.
     *
     * @param filenameGenerator The FilenameGenerator for this ApfloatContext.
     */

    public void setFilenameGenerator(FilenameGenerator filenameGenerator)
    {
        this.properties.setProperty(FILE_PATH, filenameGenerator.getPath());
        this.properties.setProperty(FILE_INITIAL_VALUE, String.valueOf(filenameGenerator.getInitialValue()));
        this.properties.setProperty(FILE_SUFFIX, filenameGenerator.getSuffix());
        this.filenameGenerator = filenameGenerator;
    }

    /**
     * Get the default radix.
     *
     * @return The default radix for this ApfloatContext.
     */

    public int getDefaultRadix()
    {
        return this.defaultRadix;
    }

    /**
     * Set the default radix.
     * The default value is 10.
     *
     * @param radix The default radix for this ApfloatContext.
     */

    public void setDefaultRadix(int radix)
    {
        radix = Math.min(Math.max(radix, Character.MIN_RADIX), Character.MAX_RADIX);
        this.properties.setProperty(DEFAULT_RADIX, String.valueOf(radix));
        this.defaultRadix = radix;
    }

    /**
     * Get the maximum memory block size.
     *
     * @return The maximum memory block size.
     *
     * @see #setMaxMemoryBlockSize(long)
     */

    public long getMaxMemoryBlockSize()
    {
        return this.maxMemoryBlockSize;
    }

    /**
     * Set the maximum allowed memory block size in bytes.
     * Apfloat will allocate an array at most of this size
     * for calculations using this context.
     * The minimum value for this setting is 65536.<p>
     *
     * If you set the value of this parameter too low,
     * performance will suffer greatly as data is unnecessarily
     * paged to disk. If you set this value too high, your
     * application can crash with an <code>OutOfMemoryError</code>.<p>
     *
     * The default value for this setting is 80% of the total memory
     * available to the VM at application startup, as reported by
     * <code>Runtime.totalMemory()</code>, rounded down to the nearest
     * power of two or three times a power of two.
     *
     * @param maxMemoryBlockSize Maximum allocated memory block size in bytes.
     */

    public void setMaxMemoryBlockSize(long maxMemoryBlockSize)
    {
        // Note that setting the 64-bit long is not guaranteed to be atomic
        // However on 32-bit JVMs the upper word is always zero, and on 64-bit JVMs the update is probably atomic
        maxMemoryBlockSize = Util.round23down(Math.max(maxMemoryBlockSize, 65536));
        this.properties.setProperty(MAX_MEMORY_BLOCK_SIZE, String.valueOf(maxMemoryBlockSize));
        this.maxMemoryBlockSize = maxMemoryBlockSize;
    }

    /**
     * Get the level 1 cache size.
     *
     * @return The level 1 cache size.
     *
     * @see #setCacheL1Size(int)
     */

    public int getCacheL1Size()
    {
        return this.cacheL1Size;
    }

    /**
     * Set the L1 cache size in bytes. The minimum value for this setting is 512.<p>
     *
     * This setting has a minor performance impact on some memory
     * intensive operations. Unless you really want to tweak the performance,
     * it's better to not touch this setting.<p>
     *
     * The default value for this setting is 8kB.
     *
     * @param cacheL1Size The level 1 cache size in bytes.
     */

    public void setCacheL1Size(int cacheL1Size)
    {
        cacheL1Size = Util.round2down(Math.max(cacheL1Size, 512));
        this.properties.setProperty(CACHE_L1_SIZE, String.valueOf(cacheL1Size));
        this.cacheL1Size = cacheL1Size;
    }

    /**
     * Get the level 2 cache size.
     *
     * @return The level 2 cache size.
     *
     * @see #setCacheL2Size(int)
     */

    public int getCacheL2Size()
    {
        return this.cacheL2Size;
    }

    /**
     * Set the L2 cache size in bytes. The minimum value for this setting is 2048.
     *
     * This setting has a minor performance impact on some memory
     * intensive operations. Unless you really want to tweak the performance,
     * it's better to not touch this setting.<p>
     *
     * The default value for this setting is 256kB.
     *
     * @param cacheL2Size The level 2 cache size in bytes.
     */

    public void setCacheL2Size(int cacheL2Size)
    {
        cacheL2Size = Util.round2down(Math.max(cacheL2Size, 2048));
        this.properties.setProperty(CACHE_L2_SIZE, String.valueOf(cacheL2Size));
        this.cacheL2Size = cacheL2Size;
    }

    /**
     * Get the level 1 cache burst size.
     *
     * @return The cache burst size.
     *
     * @see #setCacheBurst(int)
     */

    public int getCacheBurst()
    {
        return this.cacheBurst;
    }

    /**
     * Set the L1 cache burst block size in bytes.
     * This value is also known as "L1 cache line size".<p>
     *
     * Some common values are:
     * <ul>
     *   <li>16 for 486 processors</li>
     *   <li>32 for Pentium MMX/II/III/Celeron series and Itanium processors</li>
     *   <li>64 for Pentium 4 and Itanium 2 processors</li>
     * </ul>
     * The processor will move at least this amount of bytes
     * whenever data is moved between the level 1 cache and
     * other memory (lower level cache or main memory).
     * Note that other cache levels than L1 may have a different
     * line size. The minimum value for this setting is 8.<p>
     *
     * This setting has a minor performance impact on some memory
     * intensive operations. Unless you really want to tweak the performance,
     * it's usually better to not touch this setting. Though, if you have e.g.
     * a Pentium 4 processor, you may want to increase the value
     * of this setting to 64 from the default value of 32.
     *
     * @param cacheBurst The number of bytes in a L1 cache line.
     */

    public void setCacheBurst(int cacheBurst)
    {
        cacheBurst = Util.round2down(Math.max(cacheBurst, 8));
        this.properties.setProperty(CACHE_BURST, String.valueOf(cacheBurst));
        this.cacheBurst = cacheBurst;
    }

    /**
     * Get the memory treshold.
     *
     * @return The memory treshold.
     *
     * @see #setMemoryTreshold(int)
     */

    public int getMemoryTreshold()
    {
        return this.memoryTreshold;
    }

    /**
     * Set the maximum size of apfloats in bytes that are
     * stored in memory within this context. The minimum value for this setting is 128.<p>
     *
     * If the memory treshold is too small, performance will suffer as
     * small numbers are stored to disk, and the amount of disk I/O
     * overhead becomes significant. On the other hand, if the memory
     * treshold is too big, you can get an <code>OutOfMemoryError</code>.<p>
     *
     * The optimal value depends greatly on each application. Obviously,
     * if you have plenty of heap space and don't create too many too big
     * numbers you are not likely to have problems. The default value of
     * this setting is 64kB.
     *
     * @param memoryTreshold The number of bytes that apfloats that are stored in memory will at most have within this context.
     */

    public void setMemoryTreshold(int memoryTreshold)
    {
        memoryTreshold = Math.max(memoryTreshold, 128);
        this.properties.setProperty(MEMORY_TRESHOLD, String.valueOf(memoryTreshold));
        this.memoryTreshold = memoryTreshold;
    }

    /**
     * Get the I/O block size.
     *
     * @return The I/O block size.
     *
     * @see #setBlockSize(int)
     */

    public int getBlockSize()
    {
        return this.blockSize;
    }

    /**
     * Set the efficient I/O block size in bytes for
     * this context. The minimum value for this setting is 128.<p>
     *
     * If the block size is too small, the overhead of each I/O call will
     * definetely have an adverse effect on performance. Setting the block
     * size very big will not affect performance significantly, but can
     * increase intermediate memory consumption a lot, possibly resulting
     * in running out of memory with an <code>OutOfMemoryError</code>. A
     * recommended minimum value is at least a few kilobytes.<p>
     *
     * In many places, data in files is accessed in reverse order, fetching
     * blocks of this size. Probably the optimal value of this setting is
     * roughly half of the read-ahead buffer size of you hard disk.
     * The default value is 64kB.
     *
     * @param blockSize The I/O block size in bytes to be used in calculations using this context.
     */

    public void setBlockSize(int blockSize)
    {
        blockSize = Util.round2down(Math.max(blockSize, 128));
        this.properties.setProperty(BLOCK_SIZE, String.valueOf(blockSize));
        this.blockSize = blockSize;
    }

    /**
     * Get the number of processors.
     *
     * @return The number of processors.
     *
     * @see #setNumberOfProcessors(int)
     */

    public int getNumberOfProcessors()
    {
        return this.numberOfProcessors;
    }

    /**
     * Set the number of processors available to calculations using
     * this context. The minimum value for this setting is 1.
     * The default is to use all processors available.
     *
     * @param numberOfProcessors Number of processors available to calculations using this context.
     */

    public void setNumberOfProcessors(int numberOfProcessors)
    {
        numberOfProcessors = Math.max(numberOfProcessors, 1);
        this.properties.setProperty(NUMBER_OF_PROCESSORS, String.valueOf(numberOfProcessors));
        this.numberOfProcessors = numberOfProcessors;
    }

    /**
     * Get if clean-up should be performed at the time the program exits.
     *
     * @return <code>true</code> if clean-up will be done at JVM exit, or <code>false</code> if not.
     *
     * @see #setCleanupAtExit(boolean)
     */

    public boolean getCleanupAtExit()
    {
        return (this.cleanupThread != null);
    }

    /**
     * Set if clean-up should be performed at the time the program exits.
     * The clean-up runs garbage collection and finalization to remove any
     * remaining temporary files that may have been created.
     * The default behavior is <code>true</code>.<p>
     *
     * For example unsigned applets must have this property set to <code>false</code>,
     * since they do not have access to setting shutdown hooks.<p>
     *
     * Note that if this setting is ever set to <code>true</code> in any
     * <code>ApfloatContext</code> (and never set to <code>false</code>
     * subsequently in that context), then clean-up will be performed.
     *
     * @param cleanupAtExit <code>true</code> if clean-up should be done at JVM exit, or <code>false</code> if not.
     */

    public void setCleanupAtExit(boolean cleanupAtExit)
    {
        this.properties.setProperty(CLEANUP_AT_EXIT, String.valueOf(cleanupAtExit));

        if (cleanupAtExit && this.cleanupThread == null)
        {
            this.cleanupThread = new CleanupThread();
            Runtime.getRuntime().addShutdownHook(this.cleanupThread);
        }
        else if (!cleanupAtExit && this.cleanupThread != null)
        {
            Runtime.getRuntime().removeShutdownHook(this.cleanupThread);
            this.cleanupThread = null;
        }
    }

    /**
     * Get the value of a property as string.
     * The name of the property can be any of the constants defined above.
     *
     * @param propertyName The name of the property.
     *
     * @return The value of the property as a <code>String</code>.
     */

    public String getProperty(String propertyName)
    {
        return this.properties.getProperty(propertyName);
    }

    /**
     * Set the value of a property as string.
     * The name of the property can be any of the constants defined above.
     *
     * @param propertyName The name of the property.
     * @param propertyValue The value of the property as a <code>String</code>.
     */

    public void setProperty(String propertyName, String propertyValue)
        throws ApfloatRuntimeException
    {
        try
        {
            if (propertyName.equals(BUILDER_FACTORY))
            {
                setBuilderFactory((BuilderFactory) Class.forName(propertyValue).newInstance());
            }
            else if (propertyName.equals(DEFAULT_RADIX))
            {
                setDefaultRadix(Integer.parseInt(propertyValue));
            }
            else if (propertyName.equals(MAX_MEMORY_BLOCK_SIZE))
            {
                setMaxMemoryBlockSize(Long.parseLong(propertyValue));
            }
            else if (propertyName.equals(CACHE_L1_SIZE))
            {
                setCacheL1Size(Integer.parseInt(propertyValue));
            }
            if (propertyName.equals(CACHE_L2_SIZE))
            {
                setCacheL2Size(Integer.parseInt(propertyValue));
            }
            else if (propertyName.equals(CACHE_BURST))
            {
                setCacheBurst(Integer.parseInt(propertyValue));
            }
            if (propertyName.equals(MEMORY_TRESHOLD))
            {
                setMemoryTreshold(Integer.parseInt(propertyValue));
            }
            else if (propertyName.equals(BLOCK_SIZE))
            {
                setBlockSize(Integer.parseInt(propertyValue));
            }
            else if (propertyName.equals(NUMBER_OF_PROCESSORS))
            {
                setNumberOfProcessors(Integer.parseInt(propertyValue));
            }
            else if (propertyName.equals(FILE_PATH))
            {
                setFilenameGenerator(new FilenameGenerator(propertyValue,
                                                           getProperty(FILE_INITIAL_VALUE),
                                                           getProperty(FILE_SUFFIX)));
            }
            else if (propertyName.equals(FILE_INITIAL_VALUE))
            {
                setFilenameGenerator(new FilenameGenerator(getProperty(FILE_PATH),
                                                           propertyValue,
                                                           getProperty(FILE_SUFFIX)));
            }
            else if (propertyName.equals(FILE_SUFFIX))
            {
                setFilenameGenerator(new FilenameGenerator(getProperty(FILE_PATH),
                                                           getProperty(FILE_INITIAL_VALUE),
                                                           propertyValue));
            }
            else if (propertyName.equals(CLEANUP_AT_EXIT))
            {
                setCleanupAtExit(Boolean.parseBoolean(propertyValue));
            }
            else
            {
                this.properties.setProperty(propertyName, propertyValue);
            }
        }
        catch (Exception e)
        {
            throw new ApfloatRuntimeException("Error setting property \"" + propertyName + "\" to value \"" + propertyValue + '\"', e);
        }
    }

    /**
     * Get the values of all properties as strings.
     * The names of the properties are all of the constants defined above.
     *
     * @return The properties.
     */

    public Properties getProperties()
    {
        return (Properties) this.properties.clone();
    }

    /**
     * Get the shared memory lock object.
     * All internal functions that allocate a memory block larger than the
     * memory treshold should synchronize the allocation and memory access
     * on the object returned by this method.
     *
     * @return The object on which large memory block allocation and access should be synchronized.
     */

    public Object getSharedMemoryLock()
    {
        return this.sharedMemoryLock;
    }

    /**
     * Set the shared memory lock object.
     * All internal functions that allocate a memory block larger than the
     * memory treshold should synchronize the allocation and memory access
     * on the object passed to this method.<p>
     *
     * The object is not used for anything else than synchronization, so the
     * class of the object should really be <code>java.lang.Object</code>. One
     * would typically call this method e.g. as
     * <code>ctx.setSharedMemoryLock(new Object())</code>.
     *
     * @param lock The object on which large memory block allocation and access should be synchronized.
     */

    public void setSharedMemoryLock(Object lock)
    {
        this.sharedMemoryLock = lock;
    }

    /**
     * Get the ExecutorService.
     * It can be used for executing operations in parallel.<p>
     *
     * By default the executor service is a thread pool that is
     * shared by all the ApfloatContexts. The threads in the pool
     * are daemon threads so the thread pool requires no clean-up
     * at shutdown time.
     *
     * @return The ExecutorService.
     *
     * @since 1.1
     */

    public ExecutorService getExecutorService()
    {
        return this.executorService;
    }

    /**
     * Set the ExecutorService.<p>
     *
     * Note that if a custom ExecutorService is used, e.g. a thread pool,
     * it is the caller's responsibility to clean up the ExecutorService
     * at shutdown.
     *
     * @param executorService The ExecutorService.
     *
     * @since 1.1
     */

    public void setExecutorService(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    /**
     * Get an arbitrary object as an attribute for this ApfloatContext.
     *
     * @param name Name of the attribute.
     *
     * @return Value of the attribute or <code>null</code> if the attribute doesn't exist.
     */

    public Object getAttribute(String name)
    {
        return this.attributes.get(name);
    }

    /**
     * Set an arbitrary object as an attribute for this ApfloatContext.
     *
     * @param name Name of the attribute.
     * @param value Value of the attribute.
     *
     * @return Previous value of the attribute or <code>null</code> if the attribute didn't exist.
     */

    public Object setAttribute(String name, Object value)
    {
        return this.attributes.put(name, value);
    }

    /**
     * Remove an attribute from this ApfloatContext.
     *
     * @param name Name of the attribute.
     *
     * @return Value of the attribute or <code>null</code> if the attribute didn't exist.
     */

    public Object removeAttribute(String name)
    {
        return this.attributes.remove(name);
    }

    /**
     * Get names of all attributes for this ApfloatContext.
     *
     * @return Names of all attributes as strings.
     */

    public Enumeration<String> getAttributeNames()
    {
        return this.attributes.keys();
    }

    /**
     * Loads properties from a properties file or resource bundle.
     * First the <code>ResourceBundle</code> by the name "apfloat" is
     * located, then all properties found from that resource bundle
     * are put to a <code>Properties</code> object.<p>
     *
     * The resource bundle is found basically using the following logic
     * (note that this is standard Java <code>ResourceBundle</code>
     * functionality), in this order whichever is found first:<p>
     *
     * <ol>
     *   <li>From the class named <code>apfloat</code> (that should be a subclass of <code>ResourceBundle</code>), in the current class path</li>
     *   <li>From the file "apfloat.properties" in the current class path</li>
     * </ol>
     *
     * @return Properties found in the "apfloat" resource bundle, or an empty <code>Properties</code> object, if the resource bundle is not found.
     */

    public static Properties loadProperties()
        throws ApfloatRuntimeException
    {
        Properties properties = new Properties();

        try
        {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("apfloat");

            Enumeration<String> keys = resourceBundle.getKeys();
            while (keys.hasMoreElements())
            {
                String key = keys.nextElement();
                properties.setProperty(key, resourceBundle.getString(key));
            }
        }
        catch (MissingResourceException mre)
        {
            // Ignore - properties file or class is not found or can't be read
        }

        return properties;
    }

    /**
     * Returns a new instance of a default ExecutorService.
     *
     * @return A new instance of a default ExecutorService.
     *
     * @since 1.3
     */

    public static ExecutorService getDefaultExecutorService()
    {
        // Executor service with all daemon threads, to avoid clean-up
        ThreadFactory threadFactory = new ThreadFactory()
        {
            public Thread newThread(Runnable runnable)
            {
                Thread thread = this.defaultThreadFactory.newThread(runnable);
                thread.setDaemon(true);

                return thread;
            }

            private ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
        };

        return Executors.newCachedThreadPool(threadFactory);
    }

    /**
     * Set the values of all properties as strings.
     * The names of the properties can be all of the constants defined above.
     *
     * @param properties The properties.
     */

    public void setProperties(Properties properties)
        throws ApfloatRuntimeException
    {
        Enumeration keys = properties.propertyNames();
        while (keys.hasMoreElements())
        {
            String key = (String) keys.nextElement();
            setProperty(key, properties.getProperty(key));
        }
    }

    /**
     * Creates a copy of this object.<p>
     *
     * The clone has the same BuilderFactory and FilenameGenerator members
     * and the same shared memory lock and ExecutorService as the original
     * ApfloatContext.<p>
     *
     * A shallow copy of the property set and the attribute set is created.
     * Thus setting a property or attribute on the clone will not set it
     * in the original object. Since the actual attributes (values) are shared,
     * if an attribute is mutable and is modified in the clone, the modified
     * value will appear in the original also.<p>
     *
     * @return A mostly shallow copy of this object.
     */

    public Object clone()
    {
        try
        {
            ApfloatContext ctx = (ApfloatContext) super.clone();    // Copy all attributes by reference
            ctx.properties = (Properties) ctx.properties.clone();   // Create shallow copies
            ctx.attributes = (Hashtable<String, Object>) ctx.attributes.clone();

            return ctx;
        }
        catch (CloneNotSupportedException cnse)
        {
            // Should not occur
            throw new InternalError();
        }
    }

    private static ApfloatContext globalContext;
    private static Map<Thread, ApfloatContext> threadContexts = Collections.synchronizedMap(new WeakHashMap<Thread, ApfloatContext>()); // Use WeakHashMap to automatically remove completed threads
    private static Properties defaultProperties;
    private static ExecutorService defaultExecutorService;

    private volatile BuilderFactory builderFactory;
    private volatile FilenameGenerator filenameGenerator;
    private volatile int defaultRadix;
    private volatile long maxMemoryBlockSize;
    private volatile int cacheL1Size;
    private volatile int cacheL2Size;
    private volatile int cacheBurst;
    private volatile int memoryTreshold;
    private volatile int blockSize;
    private volatile int numberOfProcessors;
    private volatile Thread cleanupThread;
    private volatile Properties properties;
    private volatile Object sharedMemoryLock = new Object();
    private volatile ExecutorService executorService = ApfloatContext.defaultExecutorService;
    private volatile Hashtable<String, Object> attributes = new Hashtable<String, Object>();

    static
    {
        ApfloatContext.defaultProperties = new Properties();

        // Try to use up to 80% of total memory and all processors
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memoryUsage = memoryBean.getHeapMemoryUsage();
        long totalMemory = Math.max(memoryUsage.getCommitted(), memoryUsage.getMax());
        long maxMemoryBlockSize = Util.round23down(totalMemory / 5 * 4);
        int numberOfProcessors = Runtime.getRuntime().availableProcessors();

        ApfloatContext.defaultProperties.setProperty(BUILDER_FACTORY, "org.apfloat.internal.IntBuilderFactory");
        ApfloatContext.defaultProperties.setProperty(DEFAULT_RADIX, "10");
        ApfloatContext.defaultProperties.setProperty(MAX_MEMORY_BLOCK_SIZE, String.valueOf(maxMemoryBlockSize));
        ApfloatContext.defaultProperties.setProperty(CACHE_L1_SIZE, "8192");
        ApfloatContext.defaultProperties.setProperty(CACHE_L2_SIZE, "262144");
        ApfloatContext.defaultProperties.setProperty(CACHE_BURST, "32");
        ApfloatContext.defaultProperties.setProperty(MEMORY_TRESHOLD, "65536");
        ApfloatContext.defaultProperties.setProperty(BLOCK_SIZE, "65536");
        ApfloatContext.defaultProperties.setProperty(NUMBER_OF_PROCESSORS, String.valueOf(numberOfProcessors));
        ApfloatContext.defaultProperties.setProperty(FILE_PATH, "");
        ApfloatContext.defaultProperties.setProperty(FILE_INITIAL_VALUE, "0");
        ApfloatContext.defaultProperties.setProperty(FILE_SUFFIX, ".ap");
        ApfloatContext.defaultProperties.setProperty(CLEANUP_AT_EXIT, "true");

        ApfloatContext.defaultExecutorService = getDefaultExecutorService();

        // Set combination of default properties and properties specified in the resource bundle
        ApfloatContext.globalContext = new ApfloatContext(loadProperties());
    }
}
