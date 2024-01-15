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
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

data class ImageUpdateRequest(
    @Schema(name = "镜像名称", required = true)
    val imageName: String,
    @Schema(name = "所属分类ID", required = true)
    val classifyId: String,
    @Schema(name = "功能标签", required = false)
    val labelIdList: List<String>?,
    @Schema(name = "镜像所属范畴ID列表", required = false)
    val categoryIdList: List<String>?,
    @Schema(name = "镜像适用的构建机类型", required = true)
    val agentTypeScope: List<ImageAgentTypeEnum>,
    @Schema(name = "版本号", required = true)
    val version: String,
    @Schema(name = "调试项目", required = false)
    val projectCode: String?,
    @Schema(name = "镜像来源", required = true)
    val imageSourceType: ImageType,
    @Schema(name = "镜像仓库地址", required = false)
    val imageRepoUrl: String?,
    @Schema(name = "镜像在仓库的名称", required = true)
    val imageRepoName: String,
    @Schema(name = "凭证ID", required = false)
    val ticketId: String?,
    @Schema(name = "镜像大小", required = false)
    val imageSize: String?,
    @Schema(name = "镜像TAG", required = true)
    val imageTag: String,
    @Schema(name = "dockerFile类型", required = false)
    val dockerFileType: String?,
    @Schema(name = "dockerFile内容", required = false)
    val dockerFileContent: String?,
    @Schema(name = "LOGO url", required = true)
    val logoUrl: String,
    @Schema(name = "镜像图标（BASE64字符串）", required = false)
    val icon: String?,
    @Schema(name = "镜像简介）", required = false)
    val summary: String?,
    @Schema(name = "镜像描述", required = false)
    val description: String?,
    @Schema(name = "发布者", required = true)
    val publisher: String,
    @Schema(name = "是否公开 true：公开，false：不公开", required = false)
    val publicFlag: Boolean? = null,
    @Schema(name = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @Schema(name = "是否官方认证 true：是，false：否", required = false)
    val certificationFlag: Boolean? = null,
    @Schema(name = "研发来源 SELF_DEVELOPED：自研 THIRD_PARTY：第三方", required = false)
    val rdType: ImageRDTypeEnum?,
    @Schema(name = "权重（数值越大代表权重越高）")
    val weight: Int? = null
)
