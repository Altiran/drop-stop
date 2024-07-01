package com.altiran.dropstop.utils;

import jakarta.annotation.Nonnull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility class for number operations.
 */
public final class NumberUtils {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    /**
     * Round a decimal number to two decimal places.
     */
    public static @Nonnull String roundDecimalNumber(double number) {
        return DECIMAL_FORMAT.format(number);
    }
}
