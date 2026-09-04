/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.log.util

import com.tencent.devops.common.log.pojo.enums.LogPanelLevel
import com.tencent.devops.common.log.pojo.enums.LogType

object LogPanelQueryBuilder {

    const val DEFAULT_PAGE_SIZE = 200
    const val MAX_PAGE_SIZE = 500

    fun normalizePageSize(pageSize: Int?): Int {
        val size = pageSize ?: DEFAULT_PAGE_SIZE
        return size.coerceIn(1, MAX_PAGE_SIZE)
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
