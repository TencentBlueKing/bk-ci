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
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模版资源")
data class PipelineTemplateResource(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "模板ID", required = true)
    val templateId: String,
    @get:Schema(title = "模板类型", required = true)
    val type: PipelineTemplateType,
    @get:Schema(title = "研发商店状态", required = true)
    val storeStatus: TemplateStatusEnum = TemplateStatusEnum.NEVER_PUBLISHED,
    @get:Schema(title = "配置版本号", required = false)
    val settingVersion: Int,
    @get:Schema(title = "模版全局ID", required = true)
    val version: Long,
    @get:Schema(title = "版本排序号", required = true)
    val number: Int,
    @get:Schema(title = "模板发布-版本名称", required = false)
    val versionName: String? = null,
    @get:Schema(title = "模板发布-版本号", required = false)
    val versionNum: Int? = null,
    @get:Schema(title = "模板发布-配置版本号", required = false)
    val settingVersionNum: Int? = null,
    @get:Schema(title = "模板发布-模板编排版本号", required = false)
    val pipelineVersion: Int? = null,
    @get:Schema(title = "模板发布-模板触发器版本号", required = false)
    val triggerVersion: Int? = null,
    @get:Schema(title = "源模板项目ID", required = false)
    val srcTemplateProjectId: String? = null,
    @get:Schema(title = "源模板ID", required = false)
    val srcTemplateId: String? = null,
    @get:Schema(title = "源模板版本", required = false)
    val srcTemplateVersion: Long? = null,
    @get:Schema(title = "草稿来源版本", required = false)
    val baseVersion: Long? = null,
    @get:Schema(title = "来源版本名称", required = false)
    val baseVersionName: String? = null,
    @get:Schema(title = "构建参数", required = false)
    val params: List<BuildFormProperty>? = emptyList(),
    @get:Schema(title = "编排", required = false)
    val model: ITemplateModel,
    @get:Schema(title = "编排yaml", required = false)
    val yaml: String?,
    @get:Schema(title = "状态", required = true)
    val status: VersionStatus,
    @get:Schema(title = "分支状态", required = false)
    val branchAction: BranchVersionAction? = null,
    @get:Schema(title = "版本发布描述", required = false)
    val description: String? = null,
    @get:Schema(title = "排序权重，草稿版本权重为100，其他状态的权重为0", required = false)
    val sortWeight: Int? = 100,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null,
    @get:Schema(title = "发布时间", required = false)
    val releaseTime: Long? = null,
    @get:Schema(title = "创建时间", required = false)
    val createdTime: Long? = null,
    @get:Schema(title = "更新时间", required = false)
    val updateTime: Long? = null
) {
    constructor(
        pTemplateResourceWithoutVersion: PTemplateResourceWithoutVersion,
        pTemplateResourceOnlyVersion: PTemplateResourceOnlyVersion
    ) : this(
        projectId = pTemplateResourceWithoutVersion.projectId,
        templateId = pTemplateResourceWithoutVersion.templateId,
        type = pTemplateResourceWithoutVersion.type,
        version = pTemplateResourceOnlyVersion.version,
        settingVersion = pTemplateResourceOnlyVersion.settingVersion,
        number = pTemplateResourceOnlyVersion.number,
        versionName = pTemplateResourceOnlyVersion.versionName,
        versionNum = pTemplateResourceOnlyVersion.versionNum,
        pipelineVersion = pTemplateResourceOnlyVersion.pipelineVersion,
        triggerVersion = pTemplateResourceOnlyVersion.triggerVersion,
        settingVersionNum = pTemplateResourceOnlyVersion.settingVersionNum,
        srcTemplateProjectId = pTemplateResourceWithoutVersion.srcTemplateProjectId,
        srcTemplateId = pTemplateResourceWithoutVersion.srcTemplateId,
        srcTemplateVersion = pTemplateResourceWithoutVersion.srcTemplateVersion,
        // 如果请求有传递基准版本,则使用基准版本,否则使用最新版本
        baseVersion = pTemplateResourceWithoutVersion.baseVersion ?: pTemplateResourceOnlyVersion.baseVersion,
        baseVersionName = pTemplateResourceWithoutVersion.baseVersionName
            ?: pTemplateResourceOnlyVersion.baseVersionName,
        params = pTemplateResourceWithoutVersion.params,
        model = pTemplateResourceWithoutVersion.model,
        yaml = pTemplateResourceWithoutVersion.yaml,
        status = pTemplateResourceWithoutVersion.status,
        branchAction = pTemplateResourceWithoutVersion.branchAction,
        description = pTemplateResourceWithoutVersion.description,
        sortWeight = pTemplateResourceWithoutVersion.sortWeight,
        creator = pTemplateResourceWithoutVersion.creator,
        updater = pTemplateResourceWithoutVersion.updater
    )
}
