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

package com.tencent.bkrepo.auth.pojo.externalPermission

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("外部权限")
data class ExternalPermission(
    @ApiModelProperty("id")
    val id: String,
    @ApiModelProperty("外部权限回调地址")
    val url: String,
    @ApiModelProperty("请求头")
    val headers: Map<String, String>? = emptyMap(),
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("仓库名")
    val repoName: String,
    @ApiModelProperty("生效微服务")
    val scope: String,
    @ApiModelProperty("平台账号白名单，白名单内不会校验外部权限")
    val platformWhiteList: List<String>? = emptyList(),
    @ApiModelProperty("是否启用")
    val enabled: Boolean,
    @ApiModelProperty("创建日期")
    var createdDate: LocalDateTime,
    @ApiModelProperty("创建人")
    var createdBy: String,
    @ApiModelProperty("最后修改日期")
    var lastModifiedDate: LocalDateTime,
    @ApiModelProperty("最后修改人")
    var lastModifiedBy: String
)
