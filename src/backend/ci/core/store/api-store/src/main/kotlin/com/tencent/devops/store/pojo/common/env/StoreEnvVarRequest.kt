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

package com.tencent.devops.store.pojo.common.env

import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "store组件环境变量请求报文体")
data class StoreEnvVarRequest(
    @get:Schema(title = "store组件代码", required = true)
    @field:BkField(patternStyle = BkStyleEnum.CODE_STYLE)
    val storeCode: String,
    @get:Schema(title = "store组件类型", required = true)
    @field:BkField(patternStyle = BkStyleEnum.CODE_STYLE)
    val storeType: String,
    @get:Schema(title = "变量名", required = true)
    @field:BkField(patternStyle = BkStyleEnum.CODE_STYLE)
    val varName: String,
    @get:Schema(title = "变量值", required = true)
    @field:BkField(patternStyle = BkStyleEnum.COMMON_STYLE)
    val varValue: String,
    @get:Schema(title = "描述", required = false)
    @field:BkField(patternStyle = BkStyleEnum.NOTE_STYLE, required = false)
    val varDesc: String?,
    @get:Schema(title = "变量值是否加密", required = true)
    @field:BkField(patternStyle = BkStyleEnum.BOOLEAN_STYLE)
    val encryptFlag: Boolean,
    @get:Schema(title = "适用范围 TEST：测试 PRD：正式 ALL：所有", required = true)
    @field:BkField(patternStyle = BkStyleEnum.SCOPE_STYLE)
    val scope: String
)
