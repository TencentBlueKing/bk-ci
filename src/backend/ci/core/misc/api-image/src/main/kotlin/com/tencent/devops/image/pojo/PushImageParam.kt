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

package com.tencent.devops.image.pojo

import io.swagger.annotations.ApiModelProperty

data class PushImageParam(
    @ApiModelProperty("用户ID", required = true)
    val userId: String,
    @ApiModelProperty("源镜像名称", required = true)
    val srcImageName: String,
    @ApiModelProperty("源镜像tag", required = true)
    val srcImageTag: String,
    @ApiModelProperty("镜像仓库地址", required = true)
    val repoAddress: String,
    @ApiModelProperty("目标镜像命名空间", required = true)
    val namespace: String,
    @ApiModelProperty("凭证ID", required = false)
    val ticketId: String?,
    @ApiModelProperty("目标镜像名称", required = true)
    val targetImageName: String,
    @ApiModelProperty("目标镜像tag", required = true)
    val targetImageTag: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("插件执行次数", required = false)
    val executeCount: Int?
) {
    fun outStr(): String {
        return StringBuffer().append("pull image from jfrog, projectId: $projectId, ")
            .append("pipelineId: $pipelineId, ")
            .append("buildId: $buildId, ")
            .append("repoAddress: $repoAddress, ")
            .append("namespace: $namespace, ")
            .append("ticketId: $ticketId, ")
            .append("srcImageName: $srcImageName, ")
            .append("srcImageTag: $srcImageTag, ")
            .append("targetImageName: $targetImageName, ")
            .append("targetImageTag: $targetImageTag, ")
            .append("executeCount: $executeCount")
            .toString()
    }
}
