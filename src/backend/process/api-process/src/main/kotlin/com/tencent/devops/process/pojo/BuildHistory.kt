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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.pojo

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("历史构建模型")
data class BuildHistory(
    @ApiModelProperty("构建ID", required = true)
    val id: String,
    @ApiModelProperty("启动用户", required = true)
    val userId: String,
    @ApiModelProperty("触发条件", required = true)
    val trigger: String,
    @ApiModelProperty("构建号", required = true)
    val buildNum: Int?,
    @ApiModelProperty("编排文件版本号", required = true)
    val pipelineVersion: Int,
    @ApiModelProperty("开始时间", required = true)
    val startTime: Long,
    @ApiModelProperty("结束时间", required = true)
    val endTime: Long?,
    @ApiModelProperty("状态", required = true)
    val status: String,
    @ApiModelProperty("结束原因", required = true)
    val deleteReason: String?,
    @ApiModelProperty("服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @ApiModelProperty("是否是手机启动", required = false)
    val isMobileStart: Boolean = false,
    @ApiModelProperty("原材料", required = false)
    val material: List<PipelineBuildMaterial>?,
    @ApiModelProperty("排队于", required = false)
    val queueTime: Long?,
    @ApiModelProperty("构件列表", required = false)
    val artifactList: List<FileInfo>?,
    @ApiModelProperty("备注", required = false)
    val remark: String?,
    @ApiModelProperty("总耗时(秒)", required = false)
    val totalTime: Long?,
    @ApiModelProperty("运行耗时(秒，不包括人工审核时间)", required = false)
    val executeTime: Long?,
    @ApiModelProperty("启动参数", required = false)
    val buildParameters: List<BuildParameters>?,
    @ApiModelProperty("WebHookType", required = false)
    val webHookType: String?,
    @ApiModelProperty("启动类型(新)", required = false)
    val startType: String?,
    @ApiModelProperty("推荐版本号", required = false)
    val recommendVersion: String?
)