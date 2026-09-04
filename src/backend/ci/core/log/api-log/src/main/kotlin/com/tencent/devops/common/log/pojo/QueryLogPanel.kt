/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.common.log.pojo

import com.tencent.devops.common.log.pojo.enums.LogStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "日志面板分页结果")
data class QueryLogPanel(
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "该维度是否已写完", required = true)
    val finished: Boolean,
    @get:Schema(title = "构建级日志是否已被清理", required = true)
    val cleaned: Boolean,
    @get:Schema(title = "查询状态码，复用 LogStatus", required = true)
    val status: Int = LogStatus.SUCCEED.status,
    @get:Schema(title = "状态说明", required = false)
    val message: String? = null,
    @get:Schema(title = "子 tag", required = false)
    val subTags: List<String>? = null,
    @get:Schema(title = "本页日志，时间升序", required = true)
    val logs: List<LogPanelLine> = emptyList(),
    @get:Schema(title = "本页首行号", required = false)
    val startLineNo: Long? = null,
    @get:Schema(title = "本页末行号", required = false)
    val endLineNo: Long? = null,
    @get:Schema(title = "是否还有更早的日志（向上翻）", required = true)
    val hasBefore: Boolean = false,
    @get:Schema(title = "是否还有更新的日志（向下跟随）", required = true)
    val hasAfter: Boolean = false,
    @get:Schema(title = "实际生效的级别", required = true)
    val levels: List<String> = emptyList()
)
