package com.tencent.devops.common.pipeline

/**
 * Model 公共变量组展开的运行期开关。
 *
 * 提供两类线程级控制：
 * 1. processing：在一次展开处理过程中开启，用于抑制"处理逻辑内部再次反序列化 Model"时的重复展开，
 *    从而避免重入递归与重复的本地/远程调用（等价于历史实现里 ThreadLocal 标记的语义）。
 * 2. disabled：业务方主动关闭展开的场景（如纯内存 deepCopy、数据拷贝等无需展开公共变量的路径），
 *    用于避免不必要的性能开销。
 */
object ModelPublicVarExpansion {

    private val processing = ThreadLocal.withInitial { false }
    private val disabled = ThreadLocal.withInitial { false }

    /**
     * 当前线程是否正处于一次公共变量展开处理过程中。
     */
    fun isProcessing(): Boolean = processing.get()

    /**
     * 当前线程是否已主动关闭公共变量展开。
     */
    fun isDisabled(): Boolean = disabled.get()

    /**
     * 在展开处理期间开启 processing 标记，块内反序列化的 Model 不会再次触发展开。
     * 保留调用前的值以支持嵌套，退出时精确复原，避免污染线程状态。
     */
    fun <T> runProcessing(block: () -> T): T {
        val previous = processing.get()
        processing.set(true)
        return try {
            block()
        } finally {
            if (previous) processing.set(true) else processing.remove()
        }
    }

    /**
     * 在代码块内反序列化 Model 时跳过公共变量展开。
     * 适用于明确不需要展开的场景（如 deepCopy、纯数据搬运），以获得更好的性能。
     */
    fun <T> withoutExpansion(block: () -> T): T {
        val previous = disabled.get()
        disabled.set(true)
        return try {
            block()
        } finally {
            if (previous) disabled.set(true) else disabled.remove()
        }
    }
}
