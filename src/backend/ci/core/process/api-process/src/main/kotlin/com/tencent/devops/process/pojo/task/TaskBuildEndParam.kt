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

package com.tencent.devops.process.pojo.task

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import io.swagger.annotations.ApiModelProperty

data class TaskBuildEndParam(
    @ApiModelProperty("项目id", required = false)
    val projectId: String,
    @ApiModelProperty("流水线id", required = false)
    val pipelineId: String,
    @ApiModelProperty("构建id", required = false)
    val buildId: String,
    @ApiModelProperty("容器作业id", required = false)
    val containerId: String,
    @ApiModelProperty("任务id", required = false)
    val taskId: String,
    @ApiModelProperty("执行次数", required = false)
    val executeCount: Int,
    @ApiModelProperty("状态", required = false)
    var buildStatus: BuildStatus,
    @ApiModelProperty("插件版本", required = false)
    val atomVersion: String? = null,
    @ApiModelProperty("错误类型", required = false)
    var errorType: ErrorType? = null,
    @ApiModelProperty("错误代码", required = false)
    var errorCode: Int? = null,
    @ApiModelProperty("错误信息", required = false)
    var errorMsg: String? = null
)
