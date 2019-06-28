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

package com.tencent.devops.project.pojo.service

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("OP服务-显示模型")
data class OPPServiceVO(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("服务名称", required = true)
    val name: String,
    @ApiModelProperty("服务类型ID", required = true)
    val serviceTypeId: Long,
    @ApiModelProperty("是否在页面显示")
    val showProjectList: Boolean = true,
    @ApiModelProperty("showNav")
    val showNav: Boolean = true,
    @ApiModelProperty("状态（是否默认显示灰色）")
    val status: String = "ok",
    @ApiModelProperty("链接1")
    val link: String?,
    @ApiModelProperty("链接2")
    val linkNew: String?,
    @ApiModelProperty("注入类型")
    val injectType: String?,
    @ApiModelProperty("iframeUrl")
    val iframeUrl: String?,
    @ApiModelProperty("cssUrl")
    val cssUrl: String?,
    @ApiModelProperty("jsUrl")
    val jsUrl: String?,
    @ApiModelProperty("grayCssUrl")
    val grayCssUrl: String?,
    @ApiModelProperty("grayJsUrl")
    val grayJsUrl: String?,
    @ApiModelProperty("projectIdType")
    val projectIdType: String?,
    @ApiModelProperty("创建人")
    val createdUser: String,
    @ApiModelProperty("创建时间")
    val createdTime: String,
    @ApiModelProperty("修改人修改时间")
    val updatedUser: String,
    @ApiModelProperty("修改时间")
    val updatedTime: String

)