package com.ficn.panel.parser;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KubernetesQuantity {

    private static final Pattern QUANTITY_PATTERN =
            Pattern.compile("^([+-]?\\d+(?:\\.\\d+)?)([eE][+-]?\\d+)?([a-zA-Z]+)?$");

    private KubernetesQuantity() {
    }

    public static long toBytes(String quantity) {
        Parsed parsed = parse(quantity);
        if (parsed == null) {
            return 0L;
        }
        return Math.round(parsed.amount * memoryMultiplier(parsed.suffix));
    }

    public static double toCores(String quantity) {
        Parsed parsed = parse(quantity);
        if (parsed == null) {
            return 0D;
        }
        return parsed.amount * cpuMultiplier(parsed.suffix);
    }

    public static String milliCpu(double cores) {
        long milli = Math.round(cores * 1000D);
        return milli + "m";
    }

    public static String bytes(long value) {
        return Long.toString(Math.max(0L, value));
    }

    private static Parsed parse(String quantity) {
        if (!StringUtils.hasText(quantity)) {
            return null;
        }
        Matcher matcher = QUANTITY_PATTERN.matcher(quantity.trim());
        if (!matcher.matches()) {
            return null;
        }
        double amount = Double.parseDouble(matcher.group(1));
        if (matcher.group(2) != null) {
            amount = Double.parseDouble(matcher.group(1) + matcher.group(2));
        }
        String suffix = matcher.group(3) == null ? "" : matcher.group(3);
        return new Parsed(amount, suffix);
    }

    private static double memoryMultiplier(String suffix) {
        return switch (suffix) {
            case "Ki" -> 1024D;
            case "Mi" -> 1024D * 1024D;
            case "Gi" -> 1024D * 1024D * 1024D;
            case "Ti" -> 1024D * 1024D * 1024D * 1024D;
            case "Pi" -> 1024D * 1024D * 1024D * 1024D * 1024D;
            case "n" -> 1e-9;
            case "u" -> 1e-6;
            case "m" -> 1e-3;
            case "k", "K" -> 1e3;
            case "M" -> 1e6;
            case "G" -> 1e9;
            case "T" -> 1e12;
            case "" -> 1D;
            default -> 1D;
        };
    }

    private static double cpuMultiplier(String suffix) {
        return switch (suffix) {
            case "n" -> 1e-9;
            case "u" -> 1e-6;
            case "m" -> 1e-3;
            case "k", "K" -> 1e3;
            case "" -> 1D;
            default -> suffix.isEmpty() ? 1D : 1D;
        };
    }

    private record Parsed(double amount, String suffix) {
    }
}
