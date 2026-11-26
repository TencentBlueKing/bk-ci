/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.common.log.pojo.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "日志面板级别（产品口径 INFO，存储口径 LOG）")
enum class LogPanelLevel {
    @Schema(title = "信息")
    INFO,
    @Schema(title = "警告")
    WARN,
    @Schema(title = "错误")
    ERROR,
    @Schema(title = "调试")
    DEBUG;

    fun toEsLogType(): LogType = when (this) {
        INFO -> LogType.LOG
        WARN -> LogType.WARN
        ERROR -> LogType.ERROR
        DEBUG -> LogType.DEBUG
    }

    companion object {
        val DEFAULT: List<LogPanelLevel> = listOf(INFO, WARN, ERROR)

        fun parseList(raw: String?): List<LogPanelLevel> {
            if (raw.isNullOrBlank()) return DEFAULT
            val parsed = raw.split(',')
                .mapNotNull { token ->
                    val name = token.trim().uppercase()
                    if (name.isEmpty()) null
                    else entries.find { it.name == name }
                }
                .distinct()
            return parsed.ifEmpty { DEFAULT }
        }
    }
}
