package com.altiran.dropstop.utils;

import jakarta.annotation.Nonnull;

/**
 * Utility class for startup.
 */
public final class ProcessUtils {
    /**
     * Calculate and format the execution time of a process.
     */
    public static @Nonnull String getTimeTaken(long timestamp) {
        long ms = (System.nanoTime() - timestamp) / 1000000;

        if (ms > 1000) {
            return NumberUtils.roundDecimalNumber(ms / 1000.0) + "s";
        } else {
            return NumberUtils.roundDecimalNumber(ms) + "ms";
        }
    }
}
