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

package com.tencent.devops.process.pojo.template.v2

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模版资源只有版本信息")
data class PTemplateResourceOnlyVersion(
    @get:Schema(title = "模版版本", required = true)
    val version: Long,
    @get:Schema(title = "版本排序号", required = true)
    val number: Int,
    @get:Schema(title = "模板发布-版本名称", required = false)
    val versionName: String? = null,
    @get:Schema(title = "模板发布-版本号", required = false)
    val versionNum: Int? = null,
    @get:Schema(title = "模板发布-模板编排版本号", required = false)
    val pipelineVersion: Int? = null,
    @get:Schema(title = "模板发布-模板触发器版本号", required = false)
    val triggerVersion: Int? = null,
    @get:Schema(title = "配置版本号", required = false)
    val settingVersion: Int,
    @get:Schema(title = "模板发布-配置版本号", required = false)
    val settingVersionNum: Int? = null,
    @get:Schema(title = "来源版本", required = false)
    val baseVersion: Long? = null,
    @get:Schema(title = "来源版本名称", required = false)
    val baseVersionName: String? = null
) {
    constructor(pipelineTemplateResource: PipelineTemplateResource) : this(
        version = pipelineTemplateResource.version,
        number = pipelineTemplateResource.number,
        versionName = pipelineTemplateResource.versionName,
        versionNum = pipelineTemplateResource.versionNum,
        pipelineVersion = pipelineTemplateResource.pipelineVersion,
        triggerVersion = pipelineTemplateResource.triggerVersion,
        settingVersion = pipelineTemplateResource.settingVersion,
        settingVersionNum = pipelineTemplateResource.settingVersionNum,
        baseVersion = pipelineTemplateResource.baseVersion,
        baseVersionName = pipelineTemplateResource.baseVersionName
    )
}
