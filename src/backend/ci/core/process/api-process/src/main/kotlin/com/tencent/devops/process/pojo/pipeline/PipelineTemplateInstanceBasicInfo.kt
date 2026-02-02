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

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.process.pojo.template.TemplateType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线实例化基础信息")
data class PipelineTemplateInstanceBasicInfo(
    @get:Schema(title = "模版ID")
    val templateId: String,
    @get:Schema(title = "模版名称")
    val templateName: String,
    @get:Schema(title = "模版版本")
    val templateVersion: Long,
    @get:Schema(title = "模版版本名称")
    val templateVersionName: String?,
    @get:Schema(title = "模版设置版本")
    val templateSettingVersion: Int,
    @get:Schema(title = "公共/约束/自定义模式", required = true)
    val templateMode: TemplateType,
    @get:Schema(title = "源模板项目ID", required = false)
    val templateSrcTemplateProjectId: String? = null,
    @get:Schema(title = "源模板ID", required = false)
    val templateSrcTemplateId: String? = null,
    @get:Schema(title = "源模板版本", required = false)
    val templateSrcTemplateVersion: Long? = null,
    @get:Schema(title = "实例化model,如果从模版实例化,有完整的编排内容", required = false)
    val instanceModel: Model,
    @get:Schema(title = "实例化类型")
    val instanceType: PipelineInstanceTypeEnum = PipelineInstanceTypeEnum.FREEDOM,
    @get:Schema(title = "模版实例化状态", required = false)
    val status: TemplatePipelineStatus = TemplatePipelineStatus.UPDATED,
    @get:Schema(title = "模版引用方式", required = false)
    val refType: TemplateRefType? = TemplateRefType.ID
)
