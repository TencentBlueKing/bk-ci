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

import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "流水线模版资源更新请求体")
data class PipelineTemplateResourceUpdateInfo(
    @get:Schema(title = "版本号", required = true)
    val version: Long? = null,
    @get:Schema(title = "版本排序号-根据版本发布顺序递增", required = true)
    val number: Int? = null,
    @get:Schema(title = "版本名称", required = true)
    val versionName: String? = null,
    @get:Schema(title = "模板配置发布版本号", required = true)
    val settingVersionNum: Int? = null,
    @get:Schema(title = "模板发布版本号", required = true)
    val versionNum: Int? = null,
    @get:Schema(title = "模板编排版本号", required = true)
    val pipelineVersion: Int? = null,
    @get:Schema(title = "模板触发器版本号", required = true)
    val triggerVersion: Int? = null,
    @get:Schema(title = "草稿来源版本", required = true)
    val baseVersion: Long? = null,
    @get:Schema(title = "草稿来源版本名称", required = true)
    val baseVersionName: String? = null,
    @get:Schema(title = "构建参数", required = false)
    val params: List<BuildFormProperty>? = null,
    @get:Schema(title = "编排", required = true)
    val model: ITemplateModel? = null,
    @get:Schema(title = "编排yaml", required = true)
    val yaml: String? = null,
    @get:Schema(title = "状态", required = true)
    val status: VersionStatus? = null,
    @get:Schema(title = "分支状态", required = true)
    val branchAction: BranchVersionAction? = null,
    @get:Schema(title = "版本发布描述", required = true)
    val description: String? = null,
    @get:Schema(title = "更新人", required = true)
    val updater: String? = null,
    @get:Schema(title = "发布时间", required = true)
    val releaseTime: LocalDateTime? = null,
    @get:Schema(title = "排序权重，草稿版本权重为100，其他状态的版本权重为0", required = false)
    val sortWeight: Int? = null,
    @get:Schema(title = "研发商店状态", required = false)
    val storeStatus: TemplateStatusEnum? = null
)
