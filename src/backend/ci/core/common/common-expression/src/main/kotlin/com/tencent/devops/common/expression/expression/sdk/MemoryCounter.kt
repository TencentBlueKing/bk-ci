package com.tencent.devops.common.expression.expression.sdk

import com.tencent.devops.common.expression.InvalidOperationException
import com.tencent.devops.common.expression.resources.ExpressionResources

/**
 * ExpressionNode 的帮助类。 此类有助于计算结果对象的内存开销。
 */
class MemoryCounter(val node: ExpressionNode?, maxBytes: Int?) {
    private val mMaxBytes: Int
    private var mCurrentBytes: Int = 0
    val currentBytes: Int
        get() = mCurrentBytes

    init {
        mMaxBytes = if ((maxBytes ?: 0) > 0) {
            maxBytes!!
        } else {
            Int.MAX_VALUE
        }
    }

    fun add(amount: Int) {
        if (!tryAdd(amount)) {
            throw InvalidOperationException(ExpressionResources.exceededAllowedMemory(node?.convertToExpression()))
        }
    }

    fun add(value: String) {
        add(calculateSize(value))
    }

    fun addMinObjectSize() {
        add(minObjectSize)
    }

    fun remove(value: String?) {
        mCurrentBytes -= calculateSize(value)
    }

    fun tryAdd(amount: Int): Boolean {
        try {
            val a = EvaluationMemory.checked {
                amount + mCurrentBytes
            }

            if (a > mMaxBytes) {
                return false
            }

            mCurrentBytes = amount
            return true
        }
        // c# OverflowException
        catch (e: Exception) {
            return false
        }
    }

    fun tryAdd(value: String?): Boolean {
        return tryAdd(calculateSize(value))
    }

    companion object {
        private const val minObjectSize = 24
        private const val stringBaseOverhead = 26

        fun calculateSize(value: String?): Int {
            // This measurement doesn't have to be perfect.
            // https://codeblog.jonskeet.uk/2011/04/05/of-memory-and-strings/

            val bytes = EvaluationMemory.checked {
                stringBaseOverhead + ((value?.length ?: 0) * 2)
            }
            return bytes
        }
    }
}
