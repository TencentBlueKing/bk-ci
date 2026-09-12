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
        assertEquals(3000, LogPanelQueryBuilder.BACKFILL_MAX)
    }

    @Test
    fun includeAllTypesOnlyWhenFourLevels() {
        assertFalse(LogPanelQueryBuilder.includeAllTypes(LogPanelLevel.DEFAULT))
        assertTrue(LogPanelQueryBuilder.includeAllTypes(LogPanelLevel.entries.toList()))
    }

    @Test
    fun cleanedMessageForExpiredIndex() {
        assertEquals("构建日志已超过保留期，已被清理，无法查看。", LogPanelQueryBuilder.cleanedMessage(2))
        assertEquals("构建日志已超过保留期，已被清理，无法查看。", LogPanelQueryBuilder.cleanedMessage(3))
        assertEquals(null, LogPanelQueryBuilder.cleanedMessage(0))
    }
}
