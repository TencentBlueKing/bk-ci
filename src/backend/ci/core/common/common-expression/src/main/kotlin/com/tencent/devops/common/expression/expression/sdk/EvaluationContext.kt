package com.tencent.devops.common.expression.expression.sdk

import com.tencent.devops.common.expression.expression.EvaluationOptions
import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.ITraceWriter

class EvaluationContext(
    val tarce: ITraceWriter?,
    val state: Any?,
    ops: EvaluationOptions?,
    val node: ExpressionNode?
) {
    val options: EvaluationOptions = EvaluationOptions(ops)
    val memory: EvaluationMemory
    private val mTraceResults = mutableMapOf<ExpressionNode, String>()
    private val mTraceMemory: MemoryCounter

    init {
        if (options.maxMemory == 0) {
            // Set a reasonable default max memory
            options.maxMemory = 1048576 // 1 mb
        }

        memory = EvaluationMemory(options.maxMemory, node)

        mTraceMemory = MemoryCounter(null, options.maxMemory)
    }

    fun setTraceResult(
        node: ExpressionNode,
        result: EvaluationResult
    ) {
        // Remove if previously added. This typically should not happen. This could happen
        // due to a badly authored function. So we'll handle it and track memory correctly.
        val oldValue = mTraceResults[node]
        if (oldValue != null) {
            mTraceMemory.remove(oldValue)
            mTraceResults.remove(node)
        }

        // Check max memory
        val value = ExpressionUtility.formatValue(result)
        if (mTraceMemory.tryAdd(value)) {
            // Store the result
            mTraceResults[node] = value
        }
    }

    fun tryGetTraceResult(node: ExpressionNode): Pair<String?, Boolean> {
        val value = mTraceResults[node] ?: return Pair(null, false)
        return Pair(value, true)
    }
}
