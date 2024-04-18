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

package com.tencent.devops.store.pojo.extservice.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateExtensionServiceDTO(
    @get:Schema(title = "扩展服务Code")
    val serviceCode: String,
    @get:Schema(title = "扩展服务Name")
    val serviceName: String,
    @get:Schema(title = "所属分类")
    val category: String,
    @get:Schema(title = "服务版本")
    val version: String,
    @get:Schema(title = "状态")
    val status: Int,
    @get:Schema(title = "状态对应的描述")
    val statusMsg: String?,
    @get:Schema(title = "LOGO url")
    val logoUrl: String?,
    @get:Schema(title = "ICON")
    val icon: String?,
    @get:Schema(title = "扩展服务简介")
    val sunmmary: String?,
    @get:Schema(title = "扩展服务描述")
    val description: String?,
    @get:Schema(title = "扩展服务发布者")
    val publisher: String?,
    @get:Schema(title = "发布时间")
    val publishTime: Long,
    @get:Schema(title = "是否是最后版本")
    val latestFlag: Int
)
