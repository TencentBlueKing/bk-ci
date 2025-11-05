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

package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线资源只有版本信息")
data class PipelineResourceOnlyVersion(
    @get:Schema(title = "记录版本号", required = true)
    val version: Int,
    @get:Schema(title = "版本名称", required = true)
    val versionName: String? = null,
    @get:Schema(title = "发布版本号", required = false)
    val versionNum: Int? = null,
    @get:Schema(title = "编排版本号", required = false)
    val pipelineVersion: Int? = null,
    @get:Schema(title = "触发器版本号", required = false)
    val triggerVersion: Int? = null,
    @get:Schema(title = "设置版本号", required = false)
    val settingVersion: Int?,
    @get:Schema(title = "默认的基础版本,取的是最新的正式版本", required = false)
    val baseVersion: Int? = null,
    @get:Schema(title = "基础版本名称,用于记录操作日志", required = false)
    val baseVersionName: String? = null,
    @get:Schema(title = "正式版本号,正式版本不一定是正式版本,可能是草稿或者分支,如第一次创建流水线", required = false)
    val releaseVersion: Int? = null,
    @get:Schema(title = "最新的正式版本名称", required = false)
    val releaseVersionName: String? = null
) {
    constructor(pipelineResource: PipelineResourceVersion) : this(
        version = pipelineResource.version,
        versionName = pipelineResource.versionName,
        versionNum = pipelineResource.versionNum,
        pipelineVersion = pipelineResource.pipelineVersion,
        triggerVersion = pipelineResource.triggerVersion,
        settingVersion = pipelineResource.settingVersion,
        baseVersion = pipelineResource.baseVersion
    )
}
