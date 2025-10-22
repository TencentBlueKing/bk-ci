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

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "")
data class TemplateModelDetail(
    @get:Schema(title = "版本列表", required = false)
    val versions: List<TemplateVersion>,
    @get:Schema(title = "当前版本", required = false)
    val currentVersion: TemplateVersion,
    @get:Schema(title = "最新版本", required = false)
    val latestVersion: TemplateVersion,
    @get:Schema(title = "模板名称", required = false)
    val templateName: String,
    @get:Schema(title = "解释说明", required = false)
    val description: String,
    @get:Schema(title = "创建者", required = false)
    val creator: String,
    @get:Schema(title = "模板模型", required = false)
    val template: Model,
    @get:Schema(title = "模板类型", required = false)
    val templateType: String,
    @get:Schema(title = "logo的url地址", required = false)
    val logoUrl: String,
    @get:Schema(title = "是否有操作权限", required = false)
    val hasPermission: Boolean,
    @get:Schema(title = "参数列表", required = false)
    val params: List<BuildFormProperty>,
    @get:Schema(title = "模板参数构建", required = false)
    val templateParams: List<BuildFormProperty>? = null
)
