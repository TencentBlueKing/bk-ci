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

package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "云桌面获取token签名")
data class DesktopTokenSign(
    @get:Schema(title = "指纹（约定为md5(mac_addr+token)）")
    val fingerprint: String,
    @get:Schema(title = "应用id")
    val appId: String,
    @get:Schema(title = "原始文件名")
    val fileName: String,
    @get:Schema(title = "文件版本")
    val fileVersion: String,
    @get:Schema(title = "修改日期")
    val fileUpdateTime: String,
    @get:Schema(title = "产品名称")
    val productName: String,
    @get:Schema(title = "产品版本")
    val productVersion: String,
    @get:Schema(title = "sha1")
    val sha1: String,
    @get:Schema(title = "产品名称")
    val timestamp: Long,
    @get:Schema(title = "公钥")
    val publicKey: String,
    @get:Schema(title = "签名")
    val sign: String
)
