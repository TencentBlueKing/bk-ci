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

package com.tencent.devops.store.pojo.vo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceRespItem(
    @ApiModelProperty("扩展服务ID", required = true)
    val serviceId: String,
    @ApiModelProperty("扩展服务名称", required = true)
    val serviceName: String,
    @ApiModelProperty("扩展服务代码", required = true)
    val serviceCode: String,
    @ApiModelProperty("开发语言", required = true)
    val language: String?,
    @ApiModelProperty("扩展服务所属范畴，TRIGGER：触发器类扩展服务 TASK：任务类扩展服务", required = true)
    val category: String,
    @ApiModelProperty("logo链接")
    val logoUrl: String? = null,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("微扩展服务状态", required = true)
    val serviceStatus: String,
    @ApiModelProperty("项目", required = true)
    val projectName: String,
    @ApiModelProperty("是否有处于上架状态的扩展服务扩展服务版本", required = true)
    val releaseFlag: Boolean,
    @ApiModelProperty("发布者", required = true)
    val publisher: String,
    @ApiModelProperty("发布时间", required = true)
    val publishTime: String,
    @ApiModelProperty("创建人", required = true)
    val creator: String,
    @ApiModelProperty("扩展点Id", required = true)
    val itemIds: Set<String>,
    @ApiModelProperty("扩展点", required = true)
    val itemName: List<String>,
    @ApiModelProperty("是否可卸载标签")
    val isUninstall: Boolean? = false,
    @ApiModelProperty("修改人", required = true)
    val modifier: String,
    @ApiModelProperty("创建时间", required = true)
    val createTime: String,
    @ApiModelProperty("创建时间", required = true)
    val updateTime: String
)
