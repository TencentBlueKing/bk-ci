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

package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工作空间信息")
data class Workspace(
    @ApiModelProperty("工作空间ID<只读>", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    val workspaceId: Long?,
    @ApiModelProperty("工作空间名称")
    val workspaceName: String,
    @ApiModelProperty("远程开发仓库地址")
    val repositoryUrl: String,
    @ApiModelProperty("仓库分支")
    val branch: String,
    @ApiModelProperty("devfile配置路径")
    val devFilePath: String?,
    @ApiModelProperty("devfile 内容")
    val yaml: String?,
    @ApiModelProperty("工作空间模板ID")
    val wsTemplateId: Int?,
    @ApiModelProperty("工作空间状态<只读>", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    val status: WorkspaceStatus?,
    @ApiModelProperty("状态最近更新时间<只读>", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    val lastStatusUpdateTime: Long?,
    @ApiModelProperty("休眠时间<只读>", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    val sleepingTime: Long?,
    @ApiModelProperty("工作空间创建人<只读>", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    val createUserId: String,
    @ApiModelProperty("工作空间操作路径")
    val workPath: String?,
    @ApiModelProperty("工作空间默认打开工程相对路径，默认根目录")
    val workspaceFolder: String?,
    @ApiModelProperty("工作空间对应的IP")
    val hostName: String?
)
