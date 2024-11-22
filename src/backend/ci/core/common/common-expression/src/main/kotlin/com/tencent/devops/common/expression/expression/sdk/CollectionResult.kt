package com.tencent.devops.common.expression.expression.sdk

import com.tencent.devops.common.expression.ContextNotFoundException
import com.tencent.devops.common.expression.context.PipelineContextData

/**
 * 从集合类型上下文取出值的封装对象
 * 如 IReadOnlyArray，IReadOnlyObject
 */
open class CollectionResult(
    open val type: CollectionResultType,
    open val value: Any?
) {
    constructor(value: Any?) : this(
        type = if (value == null) {
            CollectionResultType.NO_VALUE
        } else {
            CollectionResultType.VALUE
        },
        value = value
    )

    fun noKey() = type == CollectionResultType.NO_KEY

    fun throwIfNoKey() {
        if (noKey()) {
            throw ContextNotFoundException()
        }
    }

    companion object {
        fun noKey() = CollectionPipelineResult(CollectionResultType.NO_KEY, null)
    }
}

enum class CollectionResultType {
    // 集合中不存在取值的KEY
    NO_KEY,

    // 集合中存在KEY但是取值为空
    NO_VALUE,

    // 存在KEY且取值不为空
    VALUE
}

data class CollectionPipelineResult(
    override val type: CollectionResultType,
    override val value: PipelineContextData?
) : CollectionResult(type, value) {
    constructor(value: PipelineContextData?) : this(
        type = if (value == null) {
            CollectionResultType.NO_VALUE
        } else {
            CollectionResultType.VALUE
        },
        value = value
    )

    companion object {
        fun noKey() = CollectionPipelineResult(CollectionResultType.NO_KEY, null)
    }
}