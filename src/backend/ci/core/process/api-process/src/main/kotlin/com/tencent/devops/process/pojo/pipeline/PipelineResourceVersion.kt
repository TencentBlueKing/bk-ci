/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.VersionStatus
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@Suppress("LongParameterList", "LongMethod")
@ApiModel("流水线版本-详细内容")
data class PipelineResourceVersion(
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("记录版本号", required = true)
    val version: Int,
    @ApiModelProperty("JSON编排内容（POJO）", required = true)
    val model: Model,
    @ApiModelProperty("YAML编排内容", required = false)
    val yaml: String?,
    @ApiModelProperty("创建者", required = true)
    val creator: String,
    @ApiModelProperty("版本名称", required = true)
    val versionName: String? = "init",
    @ApiModelProperty("版本创建时间", required = true)
    val createTime: LocalDateTime,
    @ApiModelProperty("编排版本号", required = false)
    val pipelineVersion: Int?,
    @ApiModelProperty("触发器版本号", required = false)
    val triggerVersion: Int?,
    @ApiModelProperty("设置版本号", required = false)
    val settingVersion: Int?,
    @ApiModelProperty("是否还有构建记录引用该版本标识", required = false)
    val referFlag: Boolean? = null,
    @ApiModelProperty("关联构建记录总数", required = false)
    val referCount: Int? = null,
    @ApiModelProperty("草稿版本标识", required = false)
    val status: VersionStatus? = VersionStatus.RELEASED,
    @ApiModelProperty("版本变更说明", required = false)
    val description: String? = null
)
