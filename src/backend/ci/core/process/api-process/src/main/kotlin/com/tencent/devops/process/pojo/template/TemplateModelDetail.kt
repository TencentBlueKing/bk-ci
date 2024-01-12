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

package com.tencent.devops.process.pojo.template

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "")
data class TemplateModelDetail(
    @Schema(name = "版本列表", required = false)
    val versions: List<TemplateVersion>,
    @Schema(name = "当前版本", required = false)
    val currentVersion: TemplateVersion,
    @Schema(name = "最新版本", required = false)
    val latestVersion: TemplateVersion,
    @Schema(name = "模板名称", required = false)
    val templateName: String,
    @Schema(name = "解释说明", required = false)
    val description: String,
    @Schema(name = "创建者", required = false)
    val creator: String,
    @Schema(name = "模板模型", required = false)
    val template: Model,
    @Schema(name = "模板类型", required = false)
    val templateType: String,
    @Schema(name = "logo的url地址", required = false)
    val logoUrl: String,
    @Schema(name = "是否有操作权限", required = false)
    val hasPermission: Boolean,
    @Schema(name = "参数列表", required = false)
    val params: List<BuildFormProperty>,
    @Schema(name = "模板参数构建", required = false)
    val templateParams: List<BuildFormProperty>? = null
)
