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
package com.tencent.devops.store.pojo.image.request

import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("上架镜像请求报文体")
data class MarketImageUpdateRequest(
    @ApiModelProperty("镜像代码", required = true)
    val imageCode: String,
    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,
    @ApiModelProperty("镜像分类代码", required = true)
    val classifyCode: String,
    @ApiModelProperty("镜像标签列表", required = false)
    val labelIdList: ArrayList<String>?,
    @ApiModelProperty("镜像所属范畴CATEGORY_CODE", required = false)
    val category: String?,
    @ApiModelProperty("镜像适用的构建机类型", required = true)
    val agentTypeScope: List<ImageAgentTypeEnum>,
    @ApiModelProperty("镜像简介", required = false)
    val summary: String?,
    @ApiModelProperty("镜像描述", required = false)
    val description: String?,
    @ApiModelProperty("logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("icon图标base64字符串", required = false)
    val iconData: String?,
    @ApiModelProperty("ticket身份ID", required = false)
    val ticketId: String?,
    @ApiModelProperty("镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val imageSourceType: ImageType,
    @ApiModelProperty("镜像仓库地址", required = false)
    val imageRepoUrl: String?,
    @ApiModelProperty("镜像在仓库中的名称", required = true)
    val imageRepoName: String,
    @ApiModelProperty("镜像tag", required = true)
    val imageTag: String,
    @ApiModelProperty("dockerFile类型", required = false)
    val dockerFileType: String?,
    @ApiModelProperty("dockerFile内容", required = false)
    val dockerFileContent: String?,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("发布类型，NEW：新上架 INCOMPATIBILITY_UPGRADE：非兼容性升级 " +
        "COMPATIBILITY_UPGRADE：兼容性功能更新 COMPATIBILITY_FIX：兼容性问题修正", required = true)
    val releaseType: ReleaseTypeEnum,
    @ApiModelProperty("版本日志内容", required = true)
    val versionContent: String,
    @ApiModelProperty("发布者", required = true)
    val publisher: String
)
