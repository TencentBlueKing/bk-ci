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

package com.tencent.devops.store.pojo.template

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工作台模板列表项")
data class MyTemplateItem(
    @ApiModelProperty("模板ID", required = true)
    val templateId: String,
    @ApiModelProperty("模板代码", required = true)
    val templateCode: String,
    @ApiModelProperty("模板名称", required = true)
    val templateName: String,
    @ApiModelProperty("模板logo", required = false)
    val logoUrl: String?,
    @ApiModelProperty("版本号", required = false)
    val version: String,
    @ApiModelProperty(
        "模板状态，INIT：初始化|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGED：已下架",
        required = true
    )
    val templateStatus: String,
    @ApiModelProperty("模板所属项目代码", required = false)
    val projectCode: String,
    @ApiModelProperty("模板所属项目名称", required = false)
    val projectName: String?,
    @ApiModelProperty("是否有处于上架状态的模板版本", required = true)
    val releaseFlag: Boolean,
    @ApiModelProperty("创建人", required = true)
    val creator: String,
    @ApiModelProperty("创建时间", required = true)
    val createTime: Long,
    @ApiModelProperty("修改人", required = true)
    val modifier: String,
    @ApiModelProperty("修改时间", required = true)
    val updateTime: Long
)