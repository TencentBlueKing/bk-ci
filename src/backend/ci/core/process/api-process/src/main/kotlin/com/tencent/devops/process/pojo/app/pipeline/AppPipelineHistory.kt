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

package com.tencent.devops.process.pojo.app.pipeline

import com.tencent.devops.common.pipeline.enums.StartType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("手机侧流水线构建历史模型")
data class AppPipelineHistory(
    @ApiModelProperty("项目id", required = false)
    val projectId: String,
    @ApiModelProperty("流水线id", required = false)
    val pipelineId: String,
    @ApiModelProperty("构建号id", required = false)
    val buildId: String,
    @ApiModelProperty("用户id", required = false)
    val userId: String,
    @ApiModelProperty("启动构建类型", required = false)
    val trigger: StartType,
    @ApiModelProperty("构建次数", required = false)
    val buildNum: Int?,
    @ApiModelProperty("启动时间", required = false)
    val startTime: Long?,
    @ApiModelProperty("结束时间", required = false)
    val endTime: Long?,
    @ApiModelProperty("状态", required = false)
    val status: String,
    @ApiModelProperty("当前服务器时间戳", required = false)
    val curTimestamp: Long,
    @ApiModelProperty("流水线版本", required = false)
    val pipelineVersion: Int,
    @ApiModelProperty("文件数量", required = false)
    var fileCount: Int = 0,
    @ApiModelProperty("所有文件总大小", required = false)
    var allFileSize: Long = 0,
    @ApiModelProperty("包版本", required = false)
    val packageVersion: String?,
    @ApiModelProperty("是否使用移动端构建", required = false)
    var isMobileStart: Boolean = false
)
