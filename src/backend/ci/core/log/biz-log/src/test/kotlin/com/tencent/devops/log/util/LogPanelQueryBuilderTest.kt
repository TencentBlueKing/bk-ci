package com.tencent.devops.log.util

import com.tencent.devops.common.log.pojo.enums.LogPanelLevel
import com.tencent.devops.common.log.pojo.enums.LogType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogPanelQueryBuilderTest {

    @Test
    fun parseList_defaultsToInfoWarnError() {
        assertEquals(LogPanelLevel.DEFAULT, LogPanelLevel.parseList(null))
        assertEquals(LogPanelLevel.DEFAULT, LogPanelLevel.parseList("  "))
        assertEquals(LogPanelLevel.DEFAULT, LogPanelLevel.parseList("NOPE"))
    }

    @Test
    fun parseList_acceptsCommaSeparated() {
        val levels = LogPanelLevel.parseList("info, ERROR, warn, info")
        assertEquals(listOf(LogPanelLevel.INFO, LogPanelLevel.ERROR, LogPanelLevel.WARN), levels)
    }

    @Test
    fun infoMapsToEsLogType() {
        assertEquals(LogType.LOG, LogPanelLevel.INFO.toEsLogType())
        assertEquals(listOf("LOG", "WARN", "ERROR"), LogPanelQueryBuilder.esLogTypeNames(LogPanelLevel.DEFAULT))
        assertEquals("INFO", LogPanelQueryBuilder.levelFromEs("LOG"))
        assertEquals("WARN", LogPanelQueryBuilder.levelFromEs(LogType.WARN.name))
    }

    @Test
    fun pageSizeClamped() {
        assertEquals(1000, LogPanelQueryBuilder.normalizePageSize(null))
        assertEquals(1, LogPanelQueryBuilder.normalizePageSize(0))
        assertEquals(1000, LogPanelQueryBuilder.normalizePageSize(9999))
        assertEquals(1000, LogPanelQueryBuilder.BACKFILL_MAX)
        assertEquals(15_000L, LogPanelQueryBuilder.normalizeLookbackMs(null))
        assertEquals(0L, LogPanelQueryBuilder.normalizeLookbackMs(-1))
        assertEquals(60_000L, LogPanelQueryBuilder.normalizeLookbackMs(999_000))
    }

    @Test
    fun afterLookbackDisabledWithoutSinceTimestamp() {
        val window = LogPanelQueryBuilder.resolveAfterLookback(100, null, null)
        assertEquals(100, window.cursorLineNo)
        assertEquals(null, window.backfillFromTimestamp)
        assertEquals(false, window.enabled)
    }

    @Test
    fun afterLookbackUsesSinceTimestampMinusWindow() {
        val window = LogPanelQueryBuilder.resolveAfterLookback(11834, 1_789_113_675_000L, 15_000L)
        assertEquals(true, window.enabled)
        assertEquals(1_789_113_660_000L, window.backfillFromTimestamp)
    }

    @Test
    fun backfillKeepsLateRowsNearCursorWhenWindowExceedsPageSize() {
        val window = (1..4000).map { line(it.toLong(), 10_000L + it, "line-$it") }
        val selected = LogPanelQueryBuilder.selectBackfillNearCursor(window)
        assertEquals(LogPanelQueryBuilder.BACKFILL_MAX, selected.size)
        assertEquals(3001L, selected.first().lineNo)
        assertEquals(4000L, selected.last().lineNo)
        assertTrue(selected.any { it.lineNo == 3990L })
        assertFalse(selected.any { it.lineNo == 1L })
        val oldestPage = window.take(LogPanelQueryBuilder.MAX_PAGE_SIZE)
        assertFalse(oldestPage.any { it.lineNo == 3990L })
    }

    @Test
    fun backfillOrdersByLineNoMatchingCursorEvenWhenTimestampsInterleave() {
        val lines = listOf(
            line(10, 50_000L, "early-line-late-ts"),
            line(95, 10_000L, "near-cursor-95"),
            line(100, 10_001L, "near-cursor-100")
        )
        val selected = LogPanelQueryBuilder.selectBackfillNearCursor(lines, limit = 2)
        assertEquals(listOf(95L, 100L), selected.map { it.lineNo })
    }

    @Test
    fun mergeByLineNoInsertsLateVisibleRowsInOrder() {
        val shown = line(11834, 2000, "fetch-764")
        val late = line(11831, 2500, "finish-dist")
        val newer = line(11835, 2100, "fetch-765")
        val merged = LogPanelQueryBuilder.mergeByLineNo(listOf(shown, newer, late, shown))
        assertEquals(listOf(11831L, 11834L, 11835L), merged.map { it.lineNo })
        assertEquals("finish-dist", merged.first().message)
    }

    private fun line(no: Long, ts: Long, msg: String) = com.tencent.devops.common.log.pojo.LogPanelLine(
        lineNo = no,
        timestamp = ts,
        message = msg,
        level = "INFO"
    )

    @Test
    fun includeAllTypesOnlyWhenFourLevels() {
        assertFalse(LogPanelQueryBuilder.includeAllTypes(LogPanelLevel.DEFAULT))
        assertTrue(LogPanelQueryBuilder.includeAllTypes(LogPanelLevel.entries.toList()))
    }

    @Test
    fun cleanedMessageDelegatesToI18nForExpiredIndex() {
        assertTrue(LogPanelQueryBuilder.isCleanedStatus(2))
        assertTrue(LogPanelQueryBuilder.isCleanedStatus(3))
        assertFalse(LogPanelQueryBuilder.isCleanedStatus(0))
        assertEquals("i18n-cleaned", LogPanelQueryBuilder.cleanedMessage(2) { "i18n-cleaned" })
        assertEquals("i18n-cleaned", LogPanelQueryBuilder.cleanedMessage(3) { "i18n-cleaned" })
        assertEquals(null, LogPanelQueryBuilder.cleanedMessage(0) { "i18n-cleaned" })
    }
}
