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

package com.tencent.devops.stream.pojo

import com.tencent.devops.common.pipeline.enums.BuildStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Git和Stream项目详细信息")
data class StreamProjectCIInfo(
    @Schema(description = "Git项目ID")
    val id: Long,
    @Schema(description = "蓝盾项目id")
    val projectCode: String?,
    @Schema(description = "是否为stream 公共项目")
    val public: Boolean?,
    @Schema(description = "stream 项目名称")
    val name: String?,
    @Schema(description = "stream 项目名称带有路径")
    val nameWithNamespace: String?,
    @Schema(description = "https-git链接")
    val httpsUrlToRepo: String?,
    @Schema(description = "项目网页链接")
    val webUrl: String?,
    @Schema(description = "项目头像")
    val avatarUrl: String?,
    @Schema(description = "项目描述")
    val description: String?,
    @Schema(description = "是否开启CI功能")
    val enableCI: Boolean?,
    @Schema(description = "Build pushed branches")
    val buildPushedBranches: Boolean?,
    @Schema(description = "Build pushed pull request")
    val buildPushedPullRequest: Boolean?,
    @Schema(description = "是否开启Mr锁定")
    val enableMrBlock: Boolean?,
    @Schema(description = "当前授权人")
    val authUserId: String?,
    @Schema(description = "CI相关信息")
    val ciInfo: StreamCIInfo?
)

@Schema(description = "CI相关信息")
data class StreamCIInfo(
    @Schema(description = "是否开启STREAM")
    val enableCI: Boolean,
    @Schema(description = "最后一次构建信息")
    val lastBuildMessage: String?,
    @Schema(description = "最后一次构建状态")
    val lastBuildStatus: BuildStatus?,
    @Schema(description = "流水线ID")
    val lastBuildPipelineId: String?,
    @Schema(description = "构建ID")
    val lastBuildId: String?
)
