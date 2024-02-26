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

package com.tencent.devops.ticket.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "证书-tls证书加密内容")
data class CertTls(
    @get:Schema(title = "Base64编码的加密公钥", required = true)
    val publicKey: String,
    @get:Schema(title = "服务器crt证书名", required = true)
    val serverCrtFileName: String,
    @get:Schema(title = "Base64编码的加密后的证书内容", required = true)
    val serverCrtFile: String,
    @get:Schema(title = "服务器crt证书sha1", required = true)
    val serverCrtSha1: String,
    @get:Schema(title = "服务器key证书名", required = true)
    val serverKeyFileName: String,
    @get:Schema(title = "Base64编码的加密后的证书内容", required = true)
    val serverKeyFile: String,
    @get:Schema(title = "服务器key证书sha1", required = true)
    val serverKeySha1: String,
    @get:Schema(title = "客户端crt证书名", required = true)
    val clientCrtFileName: String?,
    @get:Schema(title = "Base64编码的加密后的证书内容", required = true)
    val clientCrtFile: String?,
    @get:Schema(title = "客户端crt证书sha1", required = true)
    val clientCrtSha1: String?,
    @get:Schema(title = "客户端key证书名", required = true)
    val clientKeyFileName: String?,
    @get:Schema(title = "Base64编码的加密后的证书内容", required = true)
    val clientKeyFile: String?,
    @get:Schema(title = "客户端key证书sha1", required = true)
    val clientKeySha1: String?
)
