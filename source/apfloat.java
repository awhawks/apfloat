import java.util.ListResourceBundle;

import org.apfloat.ApfloatContext;
import org.apfloat.spi.Util;

/**
 * Default initial settings for the global {@link ApfloatContext}.
 *
 * @version 1.0
 * @author Mikko Tommila
 */

public class apfloat
    extends ListResourceBundle
{
    public Object[][] getContents()
    {
        return CONTENTS;
    }

    // Try to use up to 80% of total memory and all processors
    private static final Object[][] CONTENTS =
    {
        { ApfloatContext.BUILDER_FACTORY, "org.apfloat.internal.IntBuilderFactory" },
        { ApfloatContext.DEFAULT_RADIX, "10" },
        { ApfloatContext.MAX_MEMORY_BLOCK_SIZE, String.valueOf(Util.round23down(Runtime.getRuntime().totalMemory() / 5 * 4)) },
        { ApfloatContext.CACHE_L1_SIZE, "8192" },
        { ApfloatContext.CACHE_L2_SIZE, "262144" },
        { ApfloatContext.CACHE_BURST, "32" },
        { ApfloatContext.MEMORY_TRESHOLD, "65536" },
        { ApfloatContext.BLOCK_SIZE, "65536" },
        { ApfloatContext.NUMBER_OF_PROCESSORS, String.valueOf(Runtime.getRuntime().availableProcessors()) },
        { ApfloatContext.FILE_PATH, "" },
        { ApfloatContext.FILE_INITIAL_VALUE, "0" },
        { ApfloatContext.FILE_SUFFIX, ".ap" },
        { ApfloatContext.CLEANUP_AT_EXIT, "true" }
    };
}
