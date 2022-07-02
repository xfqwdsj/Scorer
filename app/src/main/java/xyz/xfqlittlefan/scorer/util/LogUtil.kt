package xyz.xfqlittlefan.scorer.util

import android.util.Log

object LogUtil {
    private const val TAG_DEFAULT = "Scorer"
    const val TAG_SERVER = "Scorer.Server"
    const val TAG_CLIENT = "Scorer.Client"
    
    fun v(msg: String, tag: String = TAG_DEFAULT) = Log.v(tag, msg)

    fun v(msg: String?, tr: Throwable?, tag: String = TAG_DEFAULT) = Log.v(tag, msg, tr)

    fun d(msg: String, tag: String = TAG_DEFAULT) = Log.d(tag, msg)

    fun d(msg: String?, tr: Throwable?, tag: String = TAG_DEFAULT) = Log.d(tag, msg, tr)

    fun i(msg: String, tag: String = TAG_DEFAULT) = Log.i(tag, msg)

    fun i(msg: String?, tr: Throwable?, tag: String = TAG_DEFAULT) = Log.i(tag, msg, tr)

    fun w(msg: String, tag: String = TAG_DEFAULT) = Log.w(tag, msg)

    fun w(msg: String?, tr: Throwable?, tag: String = TAG_DEFAULT) = Log.w(tag, msg, tr)

    fun w(tr: Throwable?, tag: String = TAG_DEFAULT) = Log.w(tag, tr)

    fun e(msg: String, tag: String = TAG_DEFAULT) = Log.e(tag, msg)

    fun e(msg: String?, tr: Throwable?, tag: String = TAG_DEFAULT) = Log.e(tag, msg, tr)

    fun wtf(msg: String?, tag: String = TAG_DEFAULT) = Log.wtf(tag, msg)

    fun wtf(tr: Throwable, tag: String = TAG_DEFAULT) = Log.wtf(tag, tr)

    fun wtf(msg: String?, tr: Throwable?, tag: String = TAG_DEFAULT) = Log.wtf(tag, msg, tr)
}