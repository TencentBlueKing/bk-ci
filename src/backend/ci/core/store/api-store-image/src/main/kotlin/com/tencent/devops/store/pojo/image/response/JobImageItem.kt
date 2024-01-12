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
package com.tencent.devops.store.pojo.image.response

import com.tencent.devops.common.pipeline.type.docker.ImageType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "JOB编辑页镜像选项信息")
data class JobImageItem(

    @Schema(description = "镜像ID", required = true)
    val id: String,

    @Schema(description = "镜像代码", required = true)
    val code: String,

    @Schema(description = "镜像名称", required = true)
    val name: String,

    @Schema(description = "版本号", required = true)
    val version: String,

    @Schema(description = "镜像默认版本号", required = true)
    val defaultVersion: String,

    @Schema(description = "镜像状态", required = true)
    val imageStatus: String,

    @Schema(description = "所属分类ID", required = true)
    val classifyId: String,

    @Schema(description = "所属分类编码", required = true)
    val classifyCode: String,

    @Schema(description = "所属分类名称", required = true)
    val classifyName: String,

    @Schema(description = "镜像logo", required = false)
    val logoUrl: String?,

    @Schema(description = "镜像图标", required = false)
    val icon: String?,

    @Schema(description = "镜像简介", required = false)
    val summary: String?,

    @Schema(description = "镜像说明文档链接", required = false)
    val docsLink: String?,

    @Schema(description = "发布者", required = false)
    val publisher: String?,

    @Schema(description = "发布时间", required = false)
    val pubTime: Long? = null,

    @Schema(description = "创建人", required = true)
    val creator: String,

    @Schema(description = "创建时间", required = true)
    val createTime: Long,

    @Schema(description = "是否为最新版本镜像 true：最新 false：非最新", required = true)
    val latestFlag: Boolean,

    @Schema(description = "镜像适用的Agent类型", required = true)
    var agentTypeScope: List<String>,

    @Schema(description = "镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val imageSourceType: ImageType,

    @Schema(description = "镜像仓库Url", required = false)
    val imageRepoUrl: String?,

    @Schema(description = "镜像仓库名称", required = true)
    val imageRepoName: String,

    @Schema(description = "镜像tag", required = true)
    val imageTag: String,

    @Schema(description = "镜像大小（MB字符串）", required = true)
    val imageSize: String,

    @Schema(description = "是否官方认证 true：是 false：否", required = false)
    val certificationFlag: Boolean?,

    @Schema(description = "是否为公共镜像 true：是 false：否", required = false)
    val publicFlag: Boolean?,

    @Schema(description = "镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = false)
    val imageType: String?,

    @Schema(description = "权重（数值越大代表权重越高）", required = false)
    val weight: Int?,

    @Schema(description = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean?,

    @Schema(description = "创建人", required = false)
    val labelNames: String?,

    @Schema(description = "是否可用标识", required = false)
    val availableFlag: Boolean,

    @Schema(description = "最近修改人", required = true)
    val modifier: String,

    @Schema(description = "最近修改时间", required = true)
    val updateTime: Long
)
