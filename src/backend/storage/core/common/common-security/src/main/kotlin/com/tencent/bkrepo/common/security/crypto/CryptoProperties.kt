/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.security.crypto

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("security.crypto")
data class CryptoProperties(
    var rsaAlgorithm: String = "RSA/ECB/PKCS1Padding",
    // 公私钥必须配置，不然多实例部署时会存在无法解析出加密内容的问题
    var privateKeyStr: String = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMaoDhrj" +
        "+Da2tGpawrE8et6vHBjprVj0UiCEza7JVymYTo9gd/pxNJRnbf6NehUL1WP8D6f5e2XZEDNfqXOqyEjPqOKtWIYI6ZLQeQIuAXgyGE5aP3" +
        "/KVHFnxk+IuzcJtvqTAthfeuVXGel9ATP8hlEyDuCJe7/orBjIVYFk3p+PAgMBAAECgYAGYwLJFIk3YRpdzPszbYlZvXF" +
        "+z4x2LqyxRPPD6c82lCH6dBSHZbpWBxk/NNc29AFxTHpIYTn5ZUgjDrFI+bWkqxvgqWS/oyfB6rxajIQjTeorsGvt" +
        "/oumxQA7hvUE2XXLi218RXCURWgz/FZnvNhGhPYUOJWHoPeNlVx3V5mG8QJBAOzP9iSPcw1YJkv6uAgY4MRv1GqPu3NcMif" +
        "+DQVPOZCNPq7ynSg15Zl3HMpl6jAZNJ/AUXRby3tLhO8WiWr6C8cCQQDWwKbhy4AZ5SDigFIPtk" +
        "/655Uzprm2JaZSvGkBeOSB9EYCUC1ApKeImrufPZpSj3Ood/fbMyA6cl8Bswl2z335AkBTNa" +
        "+ToSQYKEUspWhM0BEKdRD6cI65NkgZbVc96lybwkWoS2+VVXrbtdLT+4OSawjmqTj13dtd82c" +
        "+a3jVsg65AkEAn1kiO0caDZzj8s2OlpQL8rwmDMZ45Lw5FwkwzWPcAsWzsQG3IlFK8uUFtRoryXkiM" +
        "+6Y3nCoSFYXQxaLPjqmWQJBAI2tn28XAHFcSd0UnS8L6exJuMdjCw4huI5" +
        "FOeZ0arf5NrWDFoKU30Crw2ozmRBcDvtjDVH9sn8oLC2ObCFItlM=",
    var publicKeyStr: String = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGqA4a4" +
        "/g2trRqWsKxPHrerxwY6a1Y9FIghM2uyVcpmE6PYHf6cTSUZ23+jXoVC9Vj/A+n+Xtl2RAzX6lzqshIz6jirViGCOmS0HkCLgF4MhhOWj9" +
        "/ylRxZ8ZPiLs3Cbb6kwLYX3rlVxnpfQEz/IZRMg7giXu/6KwYyFWBZN6fjwIDAQAB"
)
