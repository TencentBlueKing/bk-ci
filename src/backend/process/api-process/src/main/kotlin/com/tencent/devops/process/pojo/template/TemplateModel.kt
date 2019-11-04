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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.pojo.template

import com.tencent.devops.process.pojo.PipelineId
import io.swagger.annotations.ApiModelProperty

data class TemplateListModel(
    val projectId: String,
    val hasPermission: Boolean, // 是否有操作权限，当前只有管理员才有操作权限
    val models: List<TemplateModel>,
    val count: Int
)

data class TemplateModel(
    @ApiModelProperty("模版名称", required = true)
    val name: String,
    @ApiModelProperty("模版ID", required = true)
    val templateId: String,
    @ApiModelProperty("版本ID", required = true)
    val version: Long,
    @ApiModelProperty("最新版本号", required = true)
    val versionName: String,
    @ApiModelProperty("模板类型", required = true)
    val templateType: String,
    @ApiModelProperty("模板类型描述", required = true)
    val templateTypeDesc: String,
    @ApiModelProperty("模版logo", required = true)
    val logoUrl: String,
    @ApiModelProperty("是否关联到市场", required = true)
    val storeFlag: Boolean,
    @ApiModelProperty("关联的代码库", required = true)
    val associateCodes: List<String>,
    @ApiModelProperty("关联的流水线", required = true)
    val associatePipelines: List<PipelineId>,
    @ApiModelProperty("是否有可更新实例", required = true)
    val hasInstance2Upgrade: Boolean,
    @ApiModelProperty("是否有模版操作权限", required = true)
    val hasPermission: Boolean
)