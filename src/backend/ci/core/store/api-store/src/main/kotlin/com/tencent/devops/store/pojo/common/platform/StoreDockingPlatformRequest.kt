/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.pojo.common.platform

import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.ErrorCodeInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "store组件对接平台请求报文体")
data class StoreDockingPlatformRequest(
    @get:Schema(title = "平台代码", required = true)
    @field:BkField(patternStyle = BkStyleEnum.CODE_STYLE)
    val platformCode: String,
    @get:Schema(title = "平台名称", required = true)
    @field:BkField(patternStyle = BkStyleEnum.NAME_STYLE)
    val platformName: String,
    @get:Schema(title = "网址", required = false)
    @field:BkField(minLength = 1, maxLength = 256, required = false)
    val website: String?,
    @get:Schema(title = "简介", required = true)
    @field:BkField(minLength = 1, maxLength = 1024)
    val summary: String,
    @get:Schema(title = "标签", required = false)
    val labels: List<String>? = null,
    @get:Schema(title = "负责人", required = true)
    @field:BkField(minLength = 1, maxLength = 50)
    val principal: String,
    @get:Schema(title = "平台logo地址", required = false)
    @field:BkField(minLength = 1, maxLength = 256, required = false)
    val logoUrl: String?,
    @get:Schema(title = "所属机构名称", required = true)
    @field:BkField(patternStyle = BkStyleEnum.NAME_STYLE)
    val ownerDeptName: String,
    @get:Schema(title = "平台所属错误码前缀", required = true)
    val errorCodePrefix: Int,
    @get:Schema(title = "平台所属错误码信息", required = false)
    val errorCodeInfo: List<ErrorCodeInfo>? = null
)
