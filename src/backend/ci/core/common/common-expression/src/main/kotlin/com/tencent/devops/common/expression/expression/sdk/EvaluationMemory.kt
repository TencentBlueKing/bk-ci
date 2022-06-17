package com.tencent.devops.common.expression.expression.sdk

import com.tencent.devops.common.expression.InvalidOperationException
import com.tencent.devops.common.expression.resources.ExpressionResources

/**
 * 这只是一个内部类。
 *
 * 此类用于跟踪当前内存消耗
 * 跨越整个表达式求值。
 */
@Suppress("NestedBlockDepth")
class EvaluationMemory(private val maxBytes: Int, private val node: ExpressionNode?) {
    private val mDepths = mutableListOf<Int>()
    private var mMaxActiveDepth: Int = -1
    private var mTotalAmount: Int = 0

    fun addAmount(depth: Int, bytes: Int, trimDepth: Boolean = false) {
        // Trim deeper depths
        if (trimDepth) {
            while (mMaxActiveDepth > depth) {
                val amount = mDepths[mMaxActiveDepth]

                if (amount > 0) {
                    // Sanity checked
                    if (amount > mTotalAmount) {
                        throw InvalidOperationException("Bytes to subtract exceeds total bytes")
                    }

                    // Subtract from the total
                    mTotalAmount = checked {
                        mTotalAmount - amount
                    }

                    // Reset the amount
                    mDepths[mMaxActiveDepth] = 0
                }

                mMaxActiveDepth--
            }
        }

        // Grow the depths
        if (depth > mMaxActiveDepth) {
            // Grow the list
            while (mDepths.count() <= depth) {
                mDepths.add(0)
            }

            // Adjust the max active depth
            mMaxActiveDepth = depth
        }

        mDepths[depth] = checked {
            // Add to the depth
            mDepths[depth] + bytes
        }
        mTotalAmount = checked {
            // Add to the total
            mTotalAmount + bytes
        }

        // Check max
        if (mTotalAmount > maxBytes) {
            throw InvalidOperationException(ExpressionResources.exceededAllowedMemory(node?.convertToExpression()))
        }
    }

    companion object {
        // TODO: 目前是从c#直接挪过来，后续可能要修改
        private const val cMinObjectSize = 24
        private const val cStringBaseOverhead = 26

        // TODO: c# 中checked关键字表示计算溢出会抛出异常，kotlin暂时没找到相关
        fun checked(f: () -> Int): Int = f()

        fun calculateBytes(obj: Any?): Int {
            return if (obj is String?) {
                // TODO: C#
                // This measurement doesn't have to be perfect
                // https://codeblog.jonskeet.uk/2011/04/05/of-memory-and-strings/

                checked {
                    cStringBaseOverhead + ((obj?.length ?: 0) * Char.SIZE_BYTES)
                }
            } else {
                cMinObjectSize
            }
        }
    }
}
