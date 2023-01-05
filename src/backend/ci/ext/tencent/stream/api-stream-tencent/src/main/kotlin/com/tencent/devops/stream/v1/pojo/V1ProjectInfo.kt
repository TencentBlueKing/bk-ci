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

package com.tencent.devops.stream.v1.pojo

import com.tencent.devops.common.pipeline.enums.BuildStatus
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂和GTICI项目详细信息")
data class V1ProjectCIInfo(
    @ApiModelProperty("工蜂项目ID")
    val id: Long,
    @ApiModelProperty("蓝盾项目id")
    val projectCode: String?,
    @ApiModelProperty("是否为工蜂公共项目")
    val public: Boolean?,
    @ApiModelProperty("工蜂项目名称")
    val name: String?,
    @ApiModelProperty("工蜂项目名称带有路径")
    val nameWithNamespace: String?,
    @ApiModelProperty("https-git链接")
    val httpsUrlToRepo: String?,
    @ApiModelProperty("项目网页链接")
    val webUrl: String?,
    @ApiModelProperty("项目头像")
    val avatarUrl: String?,
    @ApiModelProperty("项目描述")
    val description: String?,
    @ApiModelProperty("是否开启CI功能")
    val enableCI: Boolean?,
    @ApiModelProperty("Build pushed branches")
    val buildPushedBranches: Boolean?,
    @ApiModelProperty("Build pushed pull request")
    val buildPushedPullRequest: Boolean?,
    @ApiModelProperty("是否开启Mr锁定")
    val enableMrBlock: Boolean?,
    @ApiModelProperty("当前授权人")
    val authUserId: String?,
    @ApiModelProperty("CI相关信息")
    val ciInfo: V1CIInfo?
)

@ApiModel("CI相关信息")
data class V1CIInfo(
    @ApiModelProperty("是否开启STREAM")
    val enableCI: Boolean,
    @ApiModelProperty("最后一次构建信息")
    val lastBuildMessage: String?,
    @ApiModelProperty("最后一次构建状态")
    val lastBuildStatus: BuildStatus?,
    @ApiModelProperty("流水线ID")
    val lastBuildPipelineId: String?,
    @ApiModelProperty("构建ID")
    val lastBuildId: String?
)
