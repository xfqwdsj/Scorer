package xyz.xfqlittlefan.scorer.utils

import androidx.lifecycle.Observer
import androidx.navigation.NavController

fun <T> NavController.registerResult(key: String, onResult: (result: T) -> Unit) {
    val liveData = currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)
    val observer: Observer<T> = object : Observer<T> {
        override fun onChanged(data: T) {
            onResult(data)
            currentBackStackEntry?.savedStateHandle?.remove<T>(key)
            liveData?.removeObserver(this)
        }
    }
    liveData?.observeForever(observer)
}

fun <T> NavController.sendResult(key: String, result: T) {
    previousBackStackEntry?.savedStateHandle?.set(key, result)
}

val NavController.currentRoute
    get() = currentBackStackEntry?.destination?.route