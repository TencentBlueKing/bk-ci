package com.tencent.devops.common.pipeline

/**
 * Model 反序列化标记管理器
 *
 * 使用 ThreadLocal 计数机制来跟踪 ModelDeserializer 的调用嵌套，
 * 防止循环调用和重复绑定问题。
 */
object ModelDeserializeMarker {

    // 线程局部变量：计数式标记（支持嵌套，初始值 0）
    private val insideCount = ThreadLocal.withInitial { 0 }

    /**
     * 进入 ModelDeserializer 时标记（计数+1）
     */
    fun markInside() {
        val current = insideCount.get()
        insideCount.set(current + 1)
    }

    /**
     * 退出 ModelDeserializer 时清除标记（计数-1，为 0 时移除 ThreadLocal）
     */
    fun clearMark() {
        val current = insideCount.get()
        if (current <= 1) {
            insideCount.remove()
        } else {
            insideCount.set(current - 1)
        }
    }

    /**
     * 判断是否处于 ModelDeserializer 内部（计数 > 0 即视为 inside）
     */
    fun isInside(): Boolean = insideCount.get() > 0
}
