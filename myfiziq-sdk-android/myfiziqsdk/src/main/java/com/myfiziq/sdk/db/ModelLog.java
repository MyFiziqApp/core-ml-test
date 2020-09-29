package com.myfiziq.sdk.db;

/**
 * @hide
 */


import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class ModelLog extends Model
{
    public enum Type
    {
        GENERAL,
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        ASSERT,
        NDK
    }

    @Persistent
    public long timestamp = 0;

    @Persistent
    public String tag = "";

    @Persistent
    public String value = "";

    @Persistent
    public int type = Type.GENERAL.ordinal();

    public ModelLog()
    {

    }

    public void set(Type t, String ta, String v)
    {
        timestamp = System.currentTimeMillis();
        tag = ta;
        value = v;
        type = t.ordinal();
        id = Model.newId();
    }

    public static void Log(String val)
    {
        ModelLog s = Orm.newModel(ModelLog.class);
        s.set(Type.GENERAL, "", val);
        s.save();
    }

    public static void Log(Type t, String val)
    {
        ModelLog s = Orm.newModel(ModelLog.class);
        s.set(t, "", val);
        s.save();
    }

    public static void Log(Type t, String ta, String val)
    {
        ModelLog s = Orm.newModel(ModelLog.class);
        s.set(t, ta, val);
        s.save();
    }

    public synchronized static void prune()
    {
        // default to logs older than 1 day ago.
        prune(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
    }

    public synchronized static void prune(long time)
    {
        ORMDbHelper db = ORMTable.dbFromModel(ModelLog.class);
        db.deleteModel(ModelLog.class, String.format("timestamp < %d", time));
    }

    public static void v(String msg)
    {
        Timber.v(msg);
        Log(Type.VERBOSE, msg);
    }

    public static void v(String tag, String msg)
    {
        Timber.tag(tag);
        Timber.v(msg);
        Log(Type.VERBOSE, tag, msg);
    }

    public static void v(String tag, String msg, Throwable tr)
    {
        Timber.tag(tag);
        Timber.v(tr, msg);
        Log(Type.VERBOSE, tag, getStackTrace(msg, tr));
    }

    public static void d(String msg)
    {
        Timber.d(msg);
        Log(Type.DEBUG, msg);
    }

    public static void d(String tag, String msg)
    {
        Timber.tag(tag);
        Timber.d(msg);
        Log(Type.DEBUG, tag, msg);
    }

    public static void d(String tag, String msg, Throwable tr)
    {
        Timber.tag(tag);
        Timber.d(tr, msg);
        Log(Type.DEBUG, tag, getStackTrace(msg, tr));
    }

    public static void i(String msg)
    {
        Timber.i(msg);
        Log(Type.INFO, msg);
    }

    public static void i(String tag, String msg)
    {
        Timber.tag(tag);
        Timber.i(msg);
        Log(Type.INFO, tag, msg);
    }

    public static void i(String tag, String msg, Throwable tr)
    {
        Timber.tag(tag);
        Timber.i(tr, msg);
        Log(Type.INFO, tag, getStackTrace(msg, tr));
    }

    public static void w(String msg)
    {
        Timber.w(msg);
        Log(Type.WARN, msg);
    }

    public static void w(String tag, String msg)
    {
        Timber.tag(tag);
        Timber.w(msg);
        Log(Type.WARN, tag, msg);
    }

    public static void w(String tag, String msg, Throwable tr)
    {
        Timber.tag(tag);
        Timber.w(tr, msg);
        Log(Type.WARN, tag, getStackTrace(msg, tr));
    }

    public static void w(String tag, Throwable tr)
    {
        Timber.tag(tag);
        Timber.w(tr);
        Log(Type.WARN, tag, getStackTrace(null, tr));
    }

    public static void e(String msg)
    {
        Timber.e(msg);
        Log(Type.ERROR, msg);
    }

    public static void e(String tag, String msg)
    {
        Timber.tag(tag);
        Timber.e(msg);
        Log(Type.ERROR, tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr)
    {
        Timber.tag(tag);
        Timber.e(tr, msg);
        Log(Type.ERROR, tag, getStackTrace(msg, tr));
    }

    public static void n(String msg)
    {
        Timber.d(msg);
        Log(Type.NDK, msg);
    }

    public static void n(String tag, String msg)
    {
        Timber.tag(tag);
        Timber.d(msg);
        Log(Type.NDK, tag, msg);
    }

    public static void n(String tag, String msg, Throwable tr)
    {
        Timber.tag(tag);
        Timber.d(tr, msg);
        Log(Type.NDK, tag, getStackTrace(msg, tr));
    }

    private static String getStackTrace(String msg, Throwable t)
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, "UTF-8"))
        {
            if (!TextUtils.isEmpty(msg))
            {
                ps.append(msg);
                ps.append(" ");
            }
            t.printStackTrace(ps);
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
        catch (Throwable tr)
        {

        }

        return "";
    }
}
