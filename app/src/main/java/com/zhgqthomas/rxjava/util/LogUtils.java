package com.zhgqthomas.rxjava.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;


public class LogUtils {

    private static String LOG_PREFIX = "github_";
    private static final int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static final int MAX_LOG_TAG_LENGTH = 23;
    private static Boolean DEBUG = null;

    private LogUtils() {
    }

    public static String makeLogTag(String str) {
        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1);
        }

        return LOG_PREFIX + str;
    }

    public static void setPrefix(String prefix) {
        LOG_PREFIX = prefix;
    }

    public static boolean isDebug() {
        return DEBUG != null && DEBUG;
    }

    public static void syncIsDebug(Context context) {
        if (null == DEBUG) {
            DEBUG = null != context.getApplicationInfo() &&
                    (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        }
    }

    /**
     * Don't use this when obfuscating class names!
     */
    public static String makeLogTag(Class cls) {
        return makeLogTag(cls.getSimpleName());
    }

    public static void d(final String tag, String message) {
        if (isDebug()) Log.d(tag, message);
    }

    public static void v(final String tag, String message) {
        if (isDebug()) Log.v(tag, message);
    }

    public static void i(final String tag, String message) {
        if (isDebug()) Log.i(tag, message);
    }

    public static void w(final String tag, String message) {
        if (isDebug()) Log.w(tag, message);
    }

    public static void e(final String tag, String message) {
        if (isDebug()) Log.e(tag, message);
    }
}
