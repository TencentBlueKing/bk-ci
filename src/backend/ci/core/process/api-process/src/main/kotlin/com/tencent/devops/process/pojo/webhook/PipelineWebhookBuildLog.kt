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

package com.tencent.devops.process.pojo.webhook

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线webhook-触发日志")
data class PipelineWebhookBuildLog(
    val id: Long? = null,
    @ApiModelProperty("代码库类型", required = true)
    val codeType: String,
    @ApiModelProperty("仓库名", required = true)
    val repoName: String? = null,
    @ApiModelProperty("commitId", required = true)
    val commitId: String? = null,
    @ApiModelProperty("事件内容", required = true)
    val requestContent: String,
    @ApiModelProperty("接受请求时间", required = true)
    val receivedTime: Long,
    @ApiModelProperty("完成时间", required = true)
    var finishedTime: Long? = null,
    val createdTime: Long,
    @ApiModelProperty("触发构建日志详情", required = true)
    val detail: MutableList<PipelineWebhookBuildLogDetail> = mutableListOf()
)

@ApiModel("流水线webhook-触发日志明细")
data class PipelineWebhookBuildLogDetail(
    val id: Long? = null,
    val logId: Long? = null,
    @ApiModelProperty("代码库类型", required = true)
    val codeType: String,
    @ApiModelProperty("仓库名", required = true)
    val repoName: String,
    @ApiModelProperty("commitId", required = true)
    val commitId: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("插件ID", required = true)
    val taskId: String,
    @ApiModelProperty("插件名", required = true)
    val taskName: String,
    @ApiModelProperty("是否成功触发", required = true)
    val success: Boolean,
    @ApiModelProperty("触发结果,如果触发成功就是buildId,触发不成功就是不成功原因", required = true)
    val triggerResult: String?,
    val createdTime: Long
)
