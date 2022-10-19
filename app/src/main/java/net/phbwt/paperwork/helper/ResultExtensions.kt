package net.phbwt.paperwork.helper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map


// TODO : something less ugly
fun <T1, T2, R> combineResults(
    r1: Result<T1>,
    r2: Result<T2>,
    transform: (T1, T2) -> R,
): Result<R> = when {
    r1.isFailure -> Result.failure(r1.exceptionOrNull()!!)
    r2.isFailure -> Result.failure(r2.exceptionOrNull()!!)
    else -> runCatching { transform(r1.getOrThrow(), r2.getOrThrow()) }
}

fun <T1, T2, R> combineResultFlows(
    f1: Flow<Result<T1>>,
    f2: Flow<Result<T2>>,
    transform: (T1, T2) -> R,
): Flow<Result<R>> = combine(f1, f2) { r1, r2 ->
    combineResults(r1, r2, transform)
}


// map the flow / map the result
fun <R, F> Flow<Result<R>>.mapResultFlow(transform: (R) -> F): Flow<Result<F>> =
    this.map { it.mapCatching(transform) }