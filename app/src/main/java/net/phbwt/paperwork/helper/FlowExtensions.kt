@file:OptIn(FlowPreview::class)

package net.phbwt.paperwork.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*


fun <T> Flow<T>.latestRelease(scope: CoroutineScope, initialValue: T): StateFlow<T> = this.stateIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = initialValue,
)

fun <T> Flow<T>.firstThenDebounce(timeoutMillis: Long = 1000): Flow<T> {
    var isFirst = true
    val a = debounce {
        if (isFirst) {
            isFirst = false
            0L
        } else {
            timeoutMillis
        }
    }
    return a
}
