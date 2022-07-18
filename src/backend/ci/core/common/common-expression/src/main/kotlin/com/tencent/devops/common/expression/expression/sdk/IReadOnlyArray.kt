package com.tencent.devops.common.expression.expression.sdk

interface IReadOnlyArray<T> : Iterable<T> {
    val count: Int

    operator fun get(index: Int): Any?
}
