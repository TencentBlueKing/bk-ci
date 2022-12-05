package com.tencent.bkrepo.common.bksync

import com.tencent.bkrepo.common.api.util.HumanReadable
import kotlin.math.ceil

/**
 * 合并结果
 * */
data class MergeResult(
    // 重复使用分块数
    val reuse: Int,
    // 增量数据大小
    val deltaDataLength: Long,
    // 旧文件大小
    val totalSize: Long,
    // 旧文件名称
    val name: String,
    // 分块大小
    val blockSize: Int,
    val sha256: String?,
    val md5: String?
) {
    // 重复使用大小
    private val reuseSize = reuse.toLong() * blockSize

    // 源文件大小
    private val baseSizeReadable = HumanReadable.size(totalSize)

    // 源文件分块数
    private val originBlocksCount = ceil(totalSize.toDouble() / blockSize).toInt()

    // 重复率
    private val hitRate = String.format("%.2f", (reuse.toFloat() / originBlocksCount) * 100)
    private val reuseSizeReadable = HumanReadable.size(reuseSize)
    private val deltaDataLengthReadable = HumanReadable.size(deltaDataLength)

    // 新文件大小
    private val newFileSizeReadable = HumanReadable.size(reuseSize + deltaDataLength)

    override fun toString(): String {
        return "old source[$name]($baseSizeReadable),reuse block[$reuse/$originBlocksCount]($hitRate%)," +
            "size[$reuseSizeReadable/$deltaDataLengthReadable/$newFileSizeReadable](r/d/n)," +
            "new file md5[$md5], sha256[$sha256]."
    }
}
