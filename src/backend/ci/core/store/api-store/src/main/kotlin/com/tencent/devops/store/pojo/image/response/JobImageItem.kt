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
package com.tencent.devops.store.pojo.image.response

import com.tencent.devops.common.pipeline.type.docker.ImageType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "JOB编辑页镜像选项信息")
data class JobImageItem(

    @get:Schema(title = "镜像ID", required = true)
    val id: String,

    @get:Schema(title = "镜像代码", required = true)
    val code: String,

    @get:Schema(title = "镜像名称", required = true)
    val name: String,

    @get:Schema(title = "版本号", required = true)
    val version: String,

    @get:Schema(title = "镜像默认版本号", required = true)
    val defaultVersion: String,

    @get:Schema(title = "镜像状态", required = true)
    val imageStatus: String,

    @get:Schema(title = "所属分类ID", required = true)
    val classifyId: String,

    @get:Schema(title = "所属分类编码", required = true)
    val classifyCode: String,

    @get:Schema(title = "所属分类名称", required = true)
    val classifyName: String,

    @get:Schema(title = "镜像logo", required = false)
    val logoUrl: String?,

    @get:Schema(title = "镜像图标", required = false)
    val icon: String?,

    @get:Schema(title = "镜像简介", required = false)
    val summary: String?,

    @get:Schema(title = "镜像说明文档链接", required = false)
    val docsLink: String?,

    @get:Schema(title = "发布者", required = false)
    val publisher: String?,

    @get:Schema(title = "发布时间", required = false)
    val pubTime: Long? = null,

    @get:Schema(title = "创建人", required = true)
    val creator: String,

    @get:Schema(title = "创建时间", required = true)
    val createTime: Long,

    @get:Schema(title = "是否为最新版本镜像 true：最新 false：非最新", required = true)
    val latestFlag: Boolean,

    @get:Schema(title = "镜像适用的Agent类型", required = true)
    var agentTypeScope: List<String>,

    @get:Schema(title = "镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val imageSourceType: ImageType,

    @get:Schema(title = "镜像仓库Url", required = false)
    val imageRepoUrl: String?,

    @get:Schema(title = "镜像仓库名称", required = true)
    val imageRepoName: String,

    @get:Schema(title = "镜像tag", required = true)
    val imageTag: String,

    @get:Schema(title = "镜像大小（MB字符串）", required = true)
    val imageSize: String,

    @get:Schema(title = "是否官方认证 true：是 false：否", required = false)
    val certificationFlag: Boolean?,

    @get:Schema(title = "是否为公共镜像 true：是 false：否", required = false)
    val publicFlag: Boolean?,

    @get:Schema(title = "镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = false)
    val imageType: String?,

    @get:Schema(title = "权重（数值越大代表权重越高）", required = false)
    val weight: Int?,

    @get:Schema(title = "是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean?,

    @get:Schema(title = "创建人", required = false)
    val labelNames: String?,

    @get:Schema(title = "是否可用标识", required = false)
    val availableFlag: Boolean,

    @get:Schema(title = "最近修改人", required = true)
    val modifier: String,

    @get:Schema(title = "最近修改时间", required = true)
    val updateTime: Long
)
