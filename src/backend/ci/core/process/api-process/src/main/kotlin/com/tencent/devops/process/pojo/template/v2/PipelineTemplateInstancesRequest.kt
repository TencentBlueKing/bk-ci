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

import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板实例请求体")
data class PipelineTemplateInstancesRequest(
    @get:Schema(title = "实例化引用类型", required = true)
    val templateRefType: TemplateRefType? = TemplateRefType.ID,
    @get:Schema(title = "路径引用时,模板引用方式，可能tag/分支/commitId", required = false)
    val templateRef: String?,
    @get:Schema(title = "是否使用模版设置", required = true)
    val useTemplateSettings: Boolean,
    @get:Schema(title = "是否本次开启PAC", required = true)
    val enablePac: Boolean,
    @get:Schema(title = "版本描述", required = false)
    val description: String?,
    @get:Schema(title = "提交动作", required = false)
    val targetAction: CodeTargetAction?,
    @get:Schema(title = "代码库hashId", required = true)
    val repoHashId: String?,
    @get:Schema(title = "指定分支", required = true)
    val targetBranch: String?,
    @get:Schema(title = "模板实例发布实体", required = true)
    val instanceReleaseInfos: List<PipelineTemplateInstanceReleaseInfo>
)
