/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.common.log.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "日志面板行")
data class LogPanelLine(
    @get:Schema(title = "行号", required = true)
    val lineNo: Long,
    @get:Schema(title = "时间戳毫秒", required = true)
    val timestamp: Long,
    @get:Schema(title = "正文", required = true)
    val message: String,
    @get:Schema(title = "产品级别 INFO/WARN/ERROR/DEBUG", required = true)
    val level: String,
    @get:Schema(title = "插件 tag", required = false)
    val tag: String = "",
    @get:Schema(title = "子 tag", required = false)
    val subTag: String = "",
    @get:Schema(title = "执行次数", required = false)
    val executeCount: Int? = 1
)
