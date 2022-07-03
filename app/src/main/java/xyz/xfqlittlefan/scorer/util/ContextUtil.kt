package xyz.xfqlittlefan.scorer.util

import android.content.Context
import androidx.core.content.ContextCompat

fun Context.compatCheckSelfPermission(permission: String) = ContextCompat.checkSelfPermission(this, permission)