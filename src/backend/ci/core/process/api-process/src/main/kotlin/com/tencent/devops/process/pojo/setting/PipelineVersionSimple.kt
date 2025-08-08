/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.pojo.setting

import com.tencent.devops.common.pipeline.enums.VersionStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线版本摘要")
data class PipelineVersionSimple(
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线创建人", required = true)
    val creator: String,
    @get:Schema(title = "创建时间戳", required = true)
    val createTime: Long,
    @get:Schema(title = "更新操作人", required = true)
    val updater: String?,
    @get:Schema(title = "更新时间戳", required = true)
    val updateTime: Long?,
    @get:Schema(title = "流水线版本号", required = true)
    val version: Int,
    @get:Schema(title = "流水线版本名称", required = true)
    val versionName: String,
    @get:Schema(title = "YAML编排版本", required = false)
    var yamlVersion: String?,
    @get:Schema(title = "是否还有构建记录引用该版本标识", required = false)
    val referFlag: Boolean? = null,
    @get:Schema(title = "关联构建记录总数", required = false)
    val referCount: Int? = null,
    @get:Schema(title = "发布版本号", required = false)
    val versionNum: Int?,
    @get:Schema(title = "编排版本号", required = false)
    val pipelineVersion: Int? = null,
    @get:Schema(title = "触发器版本号", required = false)
    val triggerVersion: Int? = null,
    @get:Schema(title = "配置版本号", required = false)
    val settingVersion: Int? = null,
    @get:Schema(title = "草稿版本标识", required = false)
    val status: VersionStatus? = VersionStatus.RELEASED,
    @get:Schema(title = "版本变更说明", required = false)
    val description: String? = null,
    @get:Schema(title = "调试构建ID", required = false)
    val debugBuildId: String? = null,
    @get:Schema(title = "该版本的来源版本（空时一定为主路径）", required = false)
    val baseVersion: Int? = null,
    @get:Schema(title = "基准版本的版本名称")
    var baseVersionName: String? = null,
    @get:Schema(title = "当前最新正式版本标识", required = false)
    var latestReleasedFlag: Boolean? = false
)
