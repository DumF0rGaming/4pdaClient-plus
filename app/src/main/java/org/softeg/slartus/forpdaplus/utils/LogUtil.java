package org.softeg.slartus.forpdaplus.utils;

import android.util.Log;

import org.softeg.slartus.forpdaplus.BuildConfig;

/**
 * Created by isanechek on 26.12.15.
 */
public class LogUtil {

    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static void D(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void I(String tag, String msg) {
        if (DEBUG) {
            Log.i(tag, msg);
        }
    }

    public static void V(String tag, String msg) {
        if (DEBUG) {
            Log.v(tag, msg);
        }
    }

    public static void E(String tag, String msg) {
        if (DEBUG) {
            Log.e(tag, msg);
        }
    }
}
