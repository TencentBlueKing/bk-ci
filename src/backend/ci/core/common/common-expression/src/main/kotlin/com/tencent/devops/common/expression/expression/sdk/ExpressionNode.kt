package com.tencent.devops.common.expression.expression.sdk

import com.tencent.devops.common.expression.NotSupportedException
import com.tencent.devops.common.expression.expression.EvaluationOptions
import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.IExpressionNode
import com.tencent.devops.common.expression.expression.ITraceWriter
import com.tencent.devops.common.expression.expression.ValueKind

abstract class ExpressionNode : IExpressionNode {

    var container: Container? = null

    var level: Int = 0
        private set

    private var mName: String? = null

    var name: String
        get() = if (!mName.isNullOrBlank()) {
            mName!!
        } else {
            this::class.java.name
        }
        set(value) {
            mName = value
        }

    protected abstract val traceFullyRealized: Boolean

    override fun evaluate(trace: ITraceWriter?, state: Any?, options: EvaluationOptions?): EvaluationResult {
        if (container != null) {
            throw NotSupportedException("Expected IExpressionNode.Evaluate to be called on root node only.")
        }

        val eTrace = EvaluationTraceWriter(trace)
        val context = EvaluationContext(eTrace, state, options, this)
        eTrace.info("Evaluating: ${convertToExpression()}")
        val result = evaluate(context)

        // Trace the result
        traceTreeResult(context, result.value, result.kind)

        return result
    }

    fun evaluate(context: EvaluationContext): EvaluationResult {
        // Evaluate
        level = if (container == null) {
            0
        } else {
            container!!.level + 1
        }
        traceVerbose(context, level, "Evaluating $name:")
        var (coreMemory, coreResult) = evaluateCore(context)

        if (coreMemory == null) {
            coreMemory = ResultMemory()
        }

        // Convert to canonical value
        val (kind, raw, value) = ExpressionUtility.convertToCanonicalValue(coreResult)

        // The depth can be safely trimmed when the total size of the core result is known,
        // or when the total size of the core result can easily be determined.
        val trimDepth = coreMemory.isTotal || (raw == null && ExpressionUtility.isPrimitive(kind))

        // Account for the memory overhead of the core result
        val coreBytes = coreMemory.bytes ?: EvaluationMemory.calculateBytes(raw ?: value)
        context.memory.addAmount(level, coreBytes, trimDepth)

        // Account for the memory overhead of the conversion result
        if (raw != null) {
            val conversionBytes = EvaluationMemory.calculateBytes(value)
            context.memory.addAmount(level, conversionBytes)
        }

        val result = EvaluationResult(context, level, value, kind, raw)

        // Store the trace result
        if (this.traceFullyRealized) {
            context.setTraceResult(this, result)
        }

        return result
    }

    abstract fun convertToExpression(): String

    abstract fun convertToRealizedExpression(context: EvaluationContext): String

    protected abstract fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?>

    private fun traceTreeResult(
        context: EvaluationContext,
        result: Any?,
        kind: ValueKind
    ) {
        // Get the realized expression
        val realizedExpression = convertToRealizedExpression(context)

        // Format the result
        val traceValue = ExpressionUtility.formatValue(result, kind)

        // Only trace the realized expression if it is meaningfully different
        if (realizedExpression != traceValue) {
            if (kind == ValueKind.Number && realizedExpression == "'$traceValue'") {
                // Don't bother tracing the realized expression when the result is a number and the
                // realized expresion is a precisely matching string.
            } else {
                context.trace?.info("Expanded: $realizedExpression")
            }
        }

        // Always trace the result
        context.trace?.info("Result: $traceValue")
    }

    companion object {
        private fun traceVerbose(context: EvaluationContext, level: Int, message: String?) {
            context.trace?.verbose("".padStart(level * 2, '.') + (message ?: ""))
        }
    }
}
