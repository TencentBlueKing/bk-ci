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

package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceCreateInfo(
    @ApiModelProperty("扩展服务code")
    val serviceCode: String,
    @ApiModelProperty("扩展服务Name")
    val serviceName: String,
    @ApiModelProperty("所属分类")
    val classify: String? = "",
    @ApiModelProperty("服务版本")
    val version: String,
    @ApiModelProperty("状态")
    val status: Int,
    @ApiModelProperty("状态对应的描述")
    val statusMsg: String? = null,
    @ApiModelProperty("LOGO url")
    val logoUrl: String? = null,
    @ApiModelProperty("icon图标base64字符串")
    val iconData: String? = null,
    @ApiModelProperty("扩展服务简介")
    val summary: String? = null,
    @ApiModelProperty("扩展服务描述")
    val description: String? = null,
    @ApiModelProperty("扩展服务发布者")
    val publisher: String? = null,
    @ApiModelProperty("发布时间")
    val publishTime: Long,
    @ApiModelProperty("是否是最后版本")
    val latestFlag: Boolean? = false,
    @ApiModelProperty("删除标签")
    val deleteFlag: Boolean? = false,
    @ApiModelProperty("添加用户")
    val creatorUser: String
)
