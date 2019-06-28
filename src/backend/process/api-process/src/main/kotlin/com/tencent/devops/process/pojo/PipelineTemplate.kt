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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.container.Stage
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线-模板信息")
data class PipelineTemplate(
    @ApiModelProperty("模板名称", required = true)
    val name: String,
    @ApiModelProperty("模板描述", required = false)
    val desc: String?,
    @ApiModelProperty("应用范畴", required = true)
    val category: List<String?>,
    @ApiModelProperty("模板图标", required = false)
    val icon: String?,
    @ApiModelProperty("模板LOGO路径", required = false)
    val logoUrl: String?,
    @ApiModelProperty("模板作者", required = true)
    val author: String,
    @ApiModelProperty("插件数量", required = true)
    val atomNum: Int,
    @ApiModelProperty("源模版ID", required = true)
    val srcTemplateId: String?,
    @ApiModelProperty("是否为公共模版", required = true)
    val publicFlag: Boolean,
    @ApiModelProperty("阶段集合", required = true)
    val stages: List<Stage>
)