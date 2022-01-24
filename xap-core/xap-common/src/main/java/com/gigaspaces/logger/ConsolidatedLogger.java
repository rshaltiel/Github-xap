package com.gigaspaces.logger;

import com.gigaspaces.internal.utils.GsEnv;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * <p>Logs given message to given logger. Uses {@link LoadingCache} to ignore messages.
 * <br/>Messages are logged 'logs_number_before_suppress' times before they are ignored.
 * When the 'expire_after_write_to_cache' has passed or the cache size reach to
 * 'max_cache_size' - the messages will be logged again.
 * <br/>The following parameters can be configured:</p>
 * <ul>
 * <li>Cache size - "com.gs.logger.consolidate.max_cache_size"</li>
 * <li>Eviction timeout after write to cache (in seconds) - "com.gs.logger.consolidate.expire_after_write_to_cache"</li>
 * <li>Number of unique logs before suppressing - "com.gs.logger.consolidate.logs_number_before_suppress"</li>
 * </ul>
 **/
public class ConsolidatedLogger implements Logger {
    /* --- System Properties --- */
    private static final String CONSOLIDATE_LOGGER_MAX_CACHE_SIZE = "com.gs.logger.consolidate.max_cache_size";
    private static final String CONSOLIDATE_LOGGER_EXPIRE_AFTER_WRITE_TO_CACHE = "com.gs.logger.consolidate.expire_after_write_to_cache";
    private static final String CONSOLIDATE_LOGGER_LOGS_BEFORE_SUPPRESS = "com.gs.logger.consolidate.logs_number_before_suppress";

    /* Default Values */
    private static final int MAX_CACHE_SIZE = 100;
    private static final int EXPIRE_AFTER_WRITE = 5;
    private static final int LOGS_BEFORE_SUPPRESS = 3;

    private final Logger logger;
    private final LoadingCache<String, AtomicInteger> cache;
    private final String SUPPRESS_MSG;

    private ConsolidatedLogger(Logger logger, int maximumSize, int expireAfterWrite, int numberOfLogsBeforeSuppress) {
        this.logger = logger;
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterWrite, TimeUnit.SECONDS)
                .build(new CacheLoader<String, AtomicInteger>() {
                    @Override
                    public AtomicInteger load(String s) throws Exception {
                        return new AtomicInteger(0);
                    }
                });
        this.SUPPRESS_MSG = System.lineSeparator() +
                "- This log was logged " + numberOfLogsBeforeSuppress + " times in" +
                " less than " + expireAfterWrite + " seconds, Suppressing it.";
    }

    public static ConsolidatedLogger getLogger(String name) {
        Logger logger = LoggerFactory.getLogger(name);
        return getLogger(logger);
    }

    public static ConsolidatedLogger getLogger(Logger logger) {
        int maximumSize = GsEnv.propertyInt(CONSOLIDATE_LOGGER_MAX_CACHE_SIZE).get(MAX_CACHE_SIZE);
        int expireAfterWrite = GsEnv.propertyInt(CONSOLIDATE_LOGGER_EXPIRE_AFTER_WRITE_TO_CACHE).get(EXPIRE_AFTER_WRITE);
        int numberOfLogsBeforeSuppress = GsEnv.propertyInt(CONSOLIDATE_LOGGER_LOGS_BEFORE_SUPPRESS).get(LOGS_BEFORE_SUPPRESS);
        return new ConsolidatedLogger(logger, maximumSize, expireAfterWrite, numberOfLogsBeforeSuppress);
    }

    public Logger getSlfLogger() {
        return logger;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        final AtomicInteger count = cache.getUnchecked("t" + s);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.trace(s);
        }
        if (value == 10) {
            logger.trace(s + SUPPRESS_MSG);
        }
    }

    @Override
    public void trace(String s, Object o) {
        final AtomicInteger count = cache.getUnchecked("t" + s + o);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.trace(s, o);
        }
        if (value == 10) {
            logger.trace(s + SUPPRESS_MSG, o);
        }
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        final AtomicInteger count = cache.getUnchecked("t" + s + o + o1);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.trace(s, o, o1);
        }
        if (value == 10) {
            logger.trace(s + SUPPRESS_MSG, o, o1);
        }
    }

    @Override
    public void trace(String s, Object... objects) {
        final AtomicInteger count = cache.getUnchecked("t" + s + Arrays.toString(objects));
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.trace(s, objects);
        }
        if (value == 10) {
            logger.trace(s + SUPPRESS_MSG, objects);
        }
    }

    @Override
    public void trace(String s, Throwable throwable) {
        final AtomicInteger count = cache.getUnchecked("t" + s + throwable);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.trace(s, throwable);
        }
        if (value == 10) {
            logger.trace(s + SUPPRESS_MSG, throwable);
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        final AtomicInteger count = cache.getUnchecked("d" + s);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.debug(s);
        }
        if (value == 10) {
            logger.debug(s + SUPPRESS_MSG);
        }
    }

    @Override
    public void debug(String s, Object o) {
        final AtomicInteger count = cache.getUnchecked("d" + s + o);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.debug(s, o);
        }
        if (value == 10) {
            logger.debug(s + SUPPRESS_MSG, o);
        }
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        final AtomicInteger count = cache.getUnchecked("d" + s + o + o1);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.debug(s, o, o1);
        }
        if (value == 10) {
            logger.debug(s + SUPPRESS_MSG, o, o1);
        }
    }

    @Override
    public void debug(String s, Object... objects) {
        final AtomicInteger count = cache.getUnchecked("d" + s + Arrays.toString(objects));
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.debug(s, objects);
        }
        if (value == 10) {
            logger.debug(s + SUPPRESS_MSG, objects);
        }
    }

    @Override
    public void debug(String s, Throwable throwable) {
        final AtomicInteger count = cache.getUnchecked("d" + s + throwable);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.debug(s, throwable);
        }
        if (value == 10) {
            logger.debug(s + SUPPRESS_MSG, throwable);
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        final AtomicInteger count = cache.getUnchecked("i" + s);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.info(s);
        }
        if (value == 10) {
            logger.info(s + SUPPRESS_MSG);
        }
    }

    @Override
    public void info(String s, Object o) {
        final AtomicInteger count = cache.getUnchecked("i" + s + o);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.info(s, o);
        }
        if (value == 10) {
            logger.info(s + SUPPRESS_MSG, o);
        }
    }

    @Override
    public void info(String s, Object o, Object o1) {
        final AtomicInteger count = cache.getUnchecked("i" + s + o + o1);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.info(s, o, o1);
        }
        if (value == 10) {
            logger.info(s + SUPPRESS_MSG, o, o1);
        }
    }

    @Override
    public void info(String s, Object... objects) {
        final AtomicInteger count = cache.getUnchecked("i" + s + Arrays.toString(objects));
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.info(s, objects);
        }
        if (value == 10) {
            logger.info(s + SUPPRESS_MSG, objects);
        }
    }

    @Override
    public void info(String s, Throwable throwable) {
        final AtomicInteger count = cache.getUnchecked("i" + s + throwable);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.info(s, throwable);
        }
        if (value == 10) {
            logger.info(s + SUPPRESS_MSG, throwable);
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        final AtomicInteger count = cache.getUnchecked("w" + s);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.warn(s);
        }
        if (value == 10) {
            logger.warn(s + SUPPRESS_MSG);
        }
    }

    @Override
    public void warn(String s, Object o) {
        final AtomicInteger count = cache.getUnchecked("w" + s + o);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.warn(s, o);
        }
        if (value == 10) {
            logger.warn(s + SUPPRESS_MSG, o);
        }
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        final AtomicInteger count = cache.getUnchecked("w" + s + o + o1);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.warn(s, o, o1);
        }
        if (value == 10) {
            logger.warn(s + SUPPRESS_MSG, o, o1);
        }
    }

    @Override
    public void warn(String s, Object... objects) {
        final AtomicInteger count = cache.getUnchecked("w" + s + Arrays.toString(objects));
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.warn(s, objects);
        }
        if (value == 10) {
            logger.warn(s + SUPPRESS_MSG, objects);
        }
    }

    @Override
    public void warn(String s, Throwable throwable) {
        final AtomicInteger count = cache.getUnchecked("w" + s + throwable);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.warn(s, throwable);
        }
        if (value == 10) {
            logger.warn(s + SUPPRESS_MSG, throwable);
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        final AtomicInteger count = cache.getUnchecked("e" + s);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.error(s);
        }
        if (value == 10) {
            logger.error(s + SUPPRESS_MSG);
        }
    }

    @Override
    public void error(String s, Object o) {
        final AtomicInteger count = cache.getUnchecked("e" + s + o);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.error(s, o);
        }
        if (value == 10) {
            logger.error(s + SUPPRESS_MSG, o);
        }
    }

    @Override
    public void error(String s, Object o, Object o1) {
        final AtomicInteger count = cache.getUnchecked("e" + s + o + o1);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.error(s, o, o1);
        }
        if (value == 10) {
            logger.error(s + SUPPRESS_MSG, o, o1);
        }
    }

    @Override
    public void error(String s, Object... objects) {
        final AtomicInteger count = cache.getUnchecked("e" + s + Arrays.toString(objects));
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.error(s, objects);
        }
        if (value == 10) {
            logger.error(s + SUPPRESS_MSG, objects);
        }
    }

    @Override
    public void error(String s, Throwable throwable) {
        final AtomicInteger count = cache.getUnchecked("e" + s + throwable);
        final int value = count.getAndIncrement();
        if (value < 10) {
            logger.error(s, throwable);
        }
        if (value == 10) {
            logger.error(s + SUPPRESS_MSG, throwable);
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        throw new UnsupportedOperationException("This function is unsupported");
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        throw new UnsupportedOperationException("This function is unsupported");
    }
}
