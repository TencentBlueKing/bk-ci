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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("")
data class TemplateModelDetail(
    @ApiModelProperty("版本列表", required = false)
    val versions: List<TemplateVersion>,
    @ApiModelProperty("当前版本", required = false)
    val currentVersion: TemplateVersion,
    @ApiModelProperty("最新版本", required = false)
    val latestVersion: TemplateVersion,
    @ApiModelProperty("模板名称", required = false)
    val templateName: String,
    @ApiModelProperty("解释说明", required = false)
    val description: String,
    @ApiModelProperty("创建者", required = false)
    val creator: String,
    @ApiModelProperty("模板模型", required = false)
    val template: Model,
    @ApiModelProperty("模板类型", required = false)
    val templateType: String,
    @ApiModelProperty("logo的url地址", required = false)
    val logoUrl: String,
    @ApiModelProperty("是否有操作权限", required = false)
    val hasPermission: Boolean, // 管理员权限
    @ApiModelProperty("参数列表", required = false)
    val params: List<BuildFormProperty>,
    @ApiModelProperty("模板参数构建", required = false)
    val templateParams: List<BuildFormProperty>? = null
)
