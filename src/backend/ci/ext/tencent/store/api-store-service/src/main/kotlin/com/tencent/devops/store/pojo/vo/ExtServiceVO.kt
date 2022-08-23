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

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展点对应的扩展服务信息")
data class ExtServiceVO(
    @ApiModelProperty("扩展服务Id", required = true)
    val serviceId: String,
    @ApiModelProperty("扩展服务名称", required = true)
    val serviceName: String,
    @ApiModelProperty("扩展服务code", required = true)
    val serviceCode: String,
    @ApiModelProperty("版本", required = true)
    val version: String,
    @ApiModelProperty("扩展服务简介", required = false)
    val summary: String?,
    @ApiModelProperty("扩展服务开发者信息", required = true)
    val vendor: ExtServiceVendorVO,
    @ApiModelProperty("扩展服务访问路径前缀", required = true)
    val baseUrl: String,
    @ApiModelProperty("自扩展服务前端针对该扩展点的配置信息Json串", required = false)
    val props: Map<String, Any>?
)
