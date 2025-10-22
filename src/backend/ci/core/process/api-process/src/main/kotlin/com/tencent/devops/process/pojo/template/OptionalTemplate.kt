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

package com.tencent.devops.process.pojo.template

import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板列表")
data class OptionalTemplateList(
    @get:Schema(title = "数量", required = false)
    val count: Int,
    @get:Schema(title = "页数", required = false)
    val page: Int?,
    @get:Schema(title = "每页数量", required = false)
    val pageSize: Int?,
    @get:Schema(title = "模板列表", required = false)
    val templates: Map<String, OptionalTemplate>
)

@Schema(title = "模板")
data class OptionalTemplate(
    @get:Schema(title = "模版名称", required = true)
    val name: String,
    @get:Schema(title = "模版ID", required = true)
    val templateId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "版本ID", required = true)
    val version: Long,
    @get:Schema(title = "最新版本号", required = true)
    val versionName: String,
    @get:Schema(title = "模板类型", required = true)
    val templateType: String,
    @get:Schema(title = "模板类型描述", required = true)
    val templateTypeDesc: String,
    @get:Schema(title = "应用范畴", required = true)
    val category: List<String?>,
    @get:Schema(title = "模版logo", required = true)
    val logoUrl: String,
    @get:Schema(title = "阶段集合", required = true)
    val stages: List<Stage>,
    @get:Schema(title = "克隆模板设置项是否存在", required = false)
    val cloneTemplateSettingExist: CloneTemplateSettingExist? = null,
    @get:Schema(title = "模版描述", required = false)
    val desc: String? = null
)

@Schema(title = "克隆模板设置")
data class CloneTemplateSettingExist(
    val notifySettingExist: Boolean,
    val concurrencySettingExist: Boolean,
    val labelSettingExist: Boolean,
    @get:Schema(title = "是否继承项目流水线语言风格", required = false)
    var inheritedDialect: Boolean? = true,
    @get:Schema(title = "流水线语言风格", required = false)
    var pipelineDialect: String? = null
) {
    companion object {
        fun fromSetting(setting: PipelineSetting?, pipelinesWithLabels: Set<String>?): CloneTemplateSettingExist {
            if (setting == null) {
                return CloneTemplateSettingExist(false, false, false)
            }
            return CloneTemplateSettingExist(
                notifySettingExist = !setting.notifySettingIsNull(),
                concurrencySettingExist = !setting.concurrencySettingIsNull(),
                labelSettingExist = pipelinesWithLabels?.contains(setting.pipelineId) ?: false,
                inheritedDialect = setting.pipelineAsCodeSettings?.inheritedDialect ?: true,
                pipelineDialect = setting.pipelineAsCodeSettings?.pipelineDialect
            )
        }
    }
}
