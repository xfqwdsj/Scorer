package xyz.xfqlittlefan.scorer.utils

inline fun <K, V> Map<K, V>.forEach(action: (key: K, value: V, isFirst: Boolean, isLast: Boolean) -> Unit) {
    val entries = iterator()
    var hasNext = entries.hasNext()
    var isFirst = true
    while (hasNext) {
        val entry = entries.next()
        hasNext = entries.hasNext()
        val isLast = !hasNext
        action(entry.key, entry.value, isFirst, isLast)
        isFirst = false
    }
}