/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
    @get:Schema(title = "镜像名称", required = true)
    val imageName: String,
    @get:Schema(title = "所属分类ID", required = true)
    val classifyId: String,
    @get:Schema(title = "功能标签", required = false)
    val labelIdList: List<String>?,
    @get:Schema(title = "镜像所属范畴ID列表", required = false)
    val categoryIdList: List<String>?,
    @get:Schema(title = "镜像适用的构建机类型", required = true)
    val agentTypeScope: List<ImageAgentTypeEnum>,
    @get:Schema(title = "版本号", required = true)
    val version: String,
    @get:Schema(title = "调试项目", required = false)
    val projectCode: String?,
    @get:Schema(title = "镜像来源", required = true)
    val imageSourceType: ImageType,
    @get:Schema(title = "镜像仓库地址", required = false)
    val imageRepoUrl: String?,
    @get:Schema(title = "镜像在仓库的名称", required = true)
    val imageRepoName: String,
    @get:Schema(title = "凭证ID", required = false)
    val ticketId: String?,
    @get:Schema(title = "镜像大小", required = false)
    val imageSize: String?,
    @get:Schema(title = "镜像TAG", required = true)
    val imageTag: String,
    @get:Schema(title = "dockerFile类型", required = false)
    val dockerFileType: String?,
    @get:Schema(title = "dockerFile内容", required = false)
    val dockerFileContent: String?,
    @get:Schema(title = "LOGO url", required = true)
    val logoUrl: String,
    @get:Schema(title = "镜像图标（BASE64字符串）", required = false)
    val icon: String?,
    @get:Schema(title = "镜像简介）", required = false)
    val summary: String?,
    @get:Schema(title = "镜像描述", required = false)
    val description: String?,
    @get:Schema(title = "发布者", required = true)
    val publisher: String,
    @get:Schema(title = "是否公开 true：公开，false：不公开", required = false)
    val publicFlag: Boolean? = null,
    @get:Schema(title = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @get:Schema(title = "是否官方认证 true：是，false：否", required = false)
    val certificationFlag: Boolean? = null,
    @get:Schema(title = "研发来源 SELF_DEVELOPED：自研 THIRD_PARTY：第三方", required = false)
    val rdType: ImageRDTypeEnum?,
    @get:Schema(title = "权重（数值越大代表权重越高）")
    val weight: Int? = null
)
