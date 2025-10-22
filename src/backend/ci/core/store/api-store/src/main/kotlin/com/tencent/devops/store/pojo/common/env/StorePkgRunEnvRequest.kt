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

@Schema(title = "store组件安装包运行时环境信息请求报文体")
data class StorePkgRunEnvRequest(
    @get:Schema(title = "store组件类型", required = true)
    @field:BkField(patternStyle = BkStyleEnum.CODE_STYLE)
    val storeType: String,
    @get:Schema(title = "开发语言", required = true)
    @field:BkField(patternStyle = BkStyleEnum.LANGUAGE_STYLE)
    val language: String,
    @get:Schema(title = "支持的操作系统名称", required = true)
    @field:BkField(patternStyle = BkStyleEnum.COMMON_STYLE, maxLength = 100)
    val osName: String,
    @get:Schema(title = "支持的操作系统架构", required = true)
    @field:BkField(patternStyle = BkStyleEnum.COMMON_STYLE, maxLength = 100)
    val osArch: String,
    @get:Schema(title = "运行时版本", required = true)
    @field:BkField(patternStyle = BkStyleEnum.COMMON_STYLE, maxLength = 100)
    val runtimeVersion: String,
    @get:Schema(title = "安装包名称", required = true)
    @field:BkField(patternStyle = BkStyleEnum.COMMON_STYLE, maxLength = 100)
    val pkgName: String,
    @get:Schema(title = "安装包下载路径", required = true)
    @field:BkField(patternStyle = BkStyleEnum.COMMON_STYLE, maxLength = 1000)
    val pkgDownloadPath: String,
    @get:Schema(title = "是否为默认安装包", required = true)
    @field:BkField(patternStyle = BkStyleEnum.BOOLEAN_STYLE)
    val defaultFlag: Boolean
)
