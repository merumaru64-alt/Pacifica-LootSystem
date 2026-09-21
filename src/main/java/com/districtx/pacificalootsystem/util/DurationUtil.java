package com.districtx.pacificalootsystem.util;

public final class DurationUtil {
    private DurationUtil() { }

    public static String format(long totalSeconds) {
        long seconds = Math.max(0, totalSeconds);
        if (seconds < 60) return unit(seconds, "second");
        long minutes = seconds / 60;
        long remainder = seconds % 60;
        if (remainder == 0) return unit(minutes, "minute");
        return unit(minutes, "minute") + " and " + unit(remainder, "second");
    }

    private static String unit(long value, String name) {
        return value + " " + name + (value == 1 ? "" : "s");
    }
}