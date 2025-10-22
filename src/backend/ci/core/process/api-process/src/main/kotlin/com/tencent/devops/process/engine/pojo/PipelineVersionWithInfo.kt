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

package com.tencent.devops.process.engine.pojo

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VersionStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线信息")
data class PipelineVersionWithInfo(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "流水线DI")
    val pipelineId: String,
    @get:Schema(title = "模板ID")
    val templateId: String?,
    @get:Schema(title = "流水线名称")
    val pipelineName: String,
    @get:Schema(title = "流水线描述")
    val pipelineDesc: String,
    @get:Schema(title = "版本")
    var version: Int = 1,
    @get:Schema(title = "创建时间")
    val createTime: Long,
    @get:Schema(title = "更新时间")
    val updateTime: Long?,
    @get:Schema(title = "版本创建者")
    val creator: String,
    @get:Schema(title = "上一次的更新者")
    val lastModifyUser: String,
    @get:Schema(title = "渠道号")
    val channelCode: ChannelCode,
    @get:Schema(title = "是否能够手动启动")
    val canManualStartup: Boolean,
    @get:Schema(title = "是否可以跳过")
    val canElementSkip: Boolean,
    @get:Schema(title = "任务数")
    val taskCount: Int,
    @get:Schema(title = "版本名称")
    var versionName: String = "",
    @get:Schema(title = "ID")
    val id: Long?,
    @get:Schema(title = "流水线组名称列表", required = false)
    var viewNames: List<String>? = null,
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
    @get:Schema(title = "草稿版本状态标识", required = false)
    val status: VersionStatus? = VersionStatus.RELEASED,
    @get:Schema(title = "版本变更说明", required = false)
    val description: String? = null,
    @get:Schema(title = "该版本的来源版本（空时一定为主路径）", required = false)
    val baseVersion: Int? = null,
    @get:Schema(title = "草稿的来源版本名称（只在草稿版本有值）", required = false)
    val baseVersionName: String? = null,
    @get:Schema(title = "调试构建ID", required = false)
    val debugBuildId: String? = null
)
