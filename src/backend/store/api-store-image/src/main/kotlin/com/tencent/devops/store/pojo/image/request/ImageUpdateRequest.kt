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
package com.tencent.devops.store.pojo.image.request

import com.tencent.devops.common.pipeline.type.docker.ImageType
import io.swagger.annotations.ApiModelProperty

data class ImageUpdateRequest(
    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,
    @ApiModelProperty("所属分类ID", required = true)
    val classifyId: String,
    @ApiModelProperty("功能标签", required = false)
    val labelList: List<String>,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("镜像来源 BKDEVOPS:蓝盾，THIRD:第三方，不传默认为THIRD", required = true)
    val imageSourceType: ImageType = ImageType.THIRD,
    @ApiModelProperty("镜像仓库地址", required = true)
    val imageRepoUrl: String,
    @ApiModelProperty("镜像在仓库的名称", required = true)
    val imageRepoName: String,
    @ApiModelProperty("镜像在仓库的路径", required = true)
    val imageRepoPath: String,
    @ApiModelProperty("凭证ID", required = true)
    val ticketId: String,
    @ApiModelProperty("镜像大小", required = false)
    val imageSize: String,
    @ApiModelProperty("镜像TAG", required = true)
    val imageTag: String,
    @ApiModelProperty("LOGO url", required = true)
    val logoUrl: String,
    @ApiModelProperty("镜像图标（BASE64字符串）", required = false)
    val icon: String,
    @ApiModelProperty("镜像简介）", required = false)
    val summary: String,
    @ApiModelProperty("镜像描述", required = false)
    val description: String,
    @ApiModelProperty("发布者", required = true)
    val publisher: String,
    @ApiModelProperty("是否公开 true：公开，false：不公开", required = false)
    val publicFlag: Boolean? = null,
    @ApiModelProperty("是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @ApiModelProperty("是否官方认证 true：是，false：否", required = false)
    val certificationFlag: Boolean? = null,
    @ApiModelProperty("权重（数值越大代表权重越高）")
    val weight: Int? = null
)