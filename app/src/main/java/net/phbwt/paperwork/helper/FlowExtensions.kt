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


inline fun <T1, T2, T3, T4, T5, T6, R> combine6(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> {
    return combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
        )
    }
}