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

package com.tencent.devops.store.pojo.atom

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.version.VersionModel
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件发布部署模型")
data class ReleaseInfo(
    @get:Schema(title = "项目编码", required = true)
    var projectId: String = "",
    @get:Schema(title = "插件名称", required = true)
    @field:BkField(patternStyle = BkStyleEnum.NAME_STYLE)
    var name: String,
    @get:Schema(title = "开发语言", required = true)
    @field:BkField(patternStyle = BkStyleEnum.LANGUAGE_STYLE)
    var language: String,
    @get:Schema(title = "插件logo地址", required = true)
    @field:BkField(maxLength = 1024)
    var logoUrl: String,
    @get:Schema(title = "支持的操作系统", required = true)
    val os: ArrayList<String>,
    @get:Schema(title = "插件配置信息", required = true)
    val configInfo: AtomConfigInfo,
    @get:Schema(title = "插件所属范畴", required = true)
    val category: AtomCategoryEnum,
    @get:Schema(title = "所属插件分类代码", required = true)
    val classifyCode: String,
    @get:Schema(title = "适用Job类型", required = true)
    val jobType: JobTypeEnum,
    @JsonProperty(value = "labelCodes", required = false)
    @get:Schema(title = "标签标识集合", description = "labelCodes")
    val labelCodes: ArrayList<String>? = null,
    @get:Schema(title = "版本信息", required = true)
    val versionInfo: VersionModel,
    @get:Schema(title = "插件简介", required = true)
    @field:BkField(maxLength = 256)
    val summary: String,
    @get:Schema(title = "插件描述", required = true)
    @field:BkField(maxLength = 65535)
    var description: String
)
