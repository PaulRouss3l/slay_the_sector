package com.slaythesector.util;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

/**
 * Thin logging wrapper so all mod logs share a consistent prefix and a single
 * logger category. Use {@code STSLog.info(...)} etc. instead of calling
 * {@code Global.getLogger(...)} directly.
 */
public final class STSLog {

    private STSLog() {}

    private static final String PREFIX = "[SlayTheSector] ";

    private static Logger log() {
        return Global.getLogger(STSLog.class);
    }

    public static void info(String msg) {
        log().info(PREFIX + msg);
    }

    public static void warn(String msg) {
        log().warn(PREFIX + msg);
    }

    public static void error(String msg, Throwable t) {
        log().error(PREFIX + msg, t);
    }
}
