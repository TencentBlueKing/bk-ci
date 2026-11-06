/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.log.util

import com.tencent.devops.common.log.pojo.LogPanelLine
import com.tencent.devops.common.log.pojo.enums.LogPanelLevel
import com.tencent.devops.common.log.pojo.enums.LogType

object LogPanelQueryBuilder {

    const val DEFAULT_PAGE_SIZE = 1000
    const val MAX_PAGE_SIZE = 1000
    const val BACKFILL_MAX = 3000

    /** 运行中 after 回看窗口：覆盖 ES refresh / bulk 可见性延迟，默认 15s */
    const val DEFAULT_LOOKBACK_MS = 15_000L
    const val MAX_LOOKBACK_MS = 60_000L

    fun normalizePageSize(pageSize: Int?): Int {
        val size = pageSize ?: DEFAULT_PAGE_SIZE
        return size.coerceIn(1, MAX_PAGE_SIZE)
    }

    fun normalizeLookbackMs(lookbackMs: Long?): Long {
        val window = lookbackMs ?: DEFAULT_LOOKBACK_MS
        return window.coerceIn(0L, MAX_LOOKBACK_MS)
    }

    /**
     * 运行中 ES 近实时：后写的 lineNo 可能先可搜。after 若只查 lineNo > cursor，
     * 游标会跨过尚未 refresh 的行，后续永远拉不回来。
     * lineNo 是构建全局号，按插件过滤后本来就不连续，不能用“号段空洞”当缺失。
     * 回填范围：cursor 之前、且 timestamp >= sinceTimestamp - lookback。
     * 窗口里经常已有超过 pageSize 的行。按时间升序截断会留下最老的一页，
     * 靠近游标的延迟行进不了结果，下次 sinceTimestamp 前移后也不会再被查到。
     * 所以回填按 timestamp、lineNo 降序取 [BACKFILL_MAX] 条，再交给前端按 lineNo 合并。
     */
    data class AfterLookback(
        val cursorLineNo: Long,
        val backfillFromTimestamp: Long?
    ) {
        val enabled: Boolean get() = backfillFromTimestamp != null
    }

    fun resolveAfterLookback(
        cursorLineNo: Long,
        sinceTimestamp: Long?,
        lookbackMs: Long?
    ): AfterLookback {
        val window = normalizeLookbackMs(lookbackMs)
        if (window <= 0L || sinceTimestamp == null || sinceTimestamp <= 0L) {
            return AfterLookback(cursorLineNo = cursorLineNo, backfillFromTimestamp = null)
        }
        return AfterLookback(
            cursorLineNo = cursorLineNo,
            backfillFromTimestamp = (sinceTimestamp - window).coerceAtLeast(0L)
        )
    }

    /**
     * 与 ES 回填查询一致：timestamp、lineNo 降序保留最靠近游标的 [limit] 条，再按时间、行号升序返回。
     * 行数不超过 limit 时不丢行。
     */
    fun selectBackfillNearCursor(
        lines: List<LogPanelLine>,
        limit: Int = BACKFILL_MAX
    ): List<LogPanelLine> {
        if (limit <= 0 || lines.isEmpty()) {
            return emptyList()
        }
        val nearest = if (lines.size <= limit) {
            lines
        } else {
            lines.sortedWith(
                compareByDescending<LogPanelLine> { it.timestamp }.thenByDescending { it.lineNo }
            ).take(limit)
        }
        return nearest.sortedWith(compareBy({ it.timestamp }, { it.lineNo }))
    }

    fun mergeByLineNo(primary: List<LogPanelLine>): List<LogPanelLine> {
        if (primary.size <= 1) return primary
        return primary
            .associateBy { it.lineNo }
            .values
            .sortedWith(compareBy({ it.timestamp }, { it.lineNo }))
    }

    fun esLogTypeNames(levels: List<LogPanelLevel>): List<String> =
        levels.map { it.toEsLogType().name }.distinct()

    fun includeAllTypes(levels: List<LogPanelLevel>): Boolean =
        LogPanelLevel.entries.all { it in levels }

    fun levelFromEs(logType: String?): String = when (logType) {
        LogType.WARN.name -> LogPanelLevel.WARN.name
        LogType.ERROR.name -> LogPanelLevel.ERROR.name
        LogType.DEBUG.name -> LogPanelLevel.DEBUG.name
        else -> LogPanelLevel.INFO.name
    }

    fun cleanedMessage(status: Int): String? = when (status) {
        2, 3 -> "构建日志已超过保留期，已被清理，无法查看。"
        else -> null
    }
}
