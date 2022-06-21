package com.tencent.devops.common.expression.expression.sdk

interface IReadOnlyObject {
    val values: Iterable<Any?>

    fun tryGetValue(key: String): Pair<Any?, Boolean>
}
