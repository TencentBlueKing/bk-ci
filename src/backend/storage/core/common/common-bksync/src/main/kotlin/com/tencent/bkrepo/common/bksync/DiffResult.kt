package com.tencent.bkrepo.common.bksync

/**
 * diff的结果
 * */
data class DiffResult(
    val reuse: Int,
    val total: Int
) {
    val hitRate: Float = reuse.toFloat() / total
    override fun toString(): String {
        val hitRate = String.format("%.2f", hitRate * 100)
        return "reuse block($reuse/$total) $hitRate%."
    }
}
