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

package com.tencent.devops.ticket.utils

import com.tencent.devops.common.api.util.DHKeyPair
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.pojo.enums.CredentialType
import java.util.Base64

@Suppress("ALL")
object CredentialContextUtils {

    fun getCredentialKey(key: String): String {
        // 参考CredentialType
        return if (key.startsWith("settings.") && (
                key.endsWith(".password") ||
                    key.endsWith(".access_token") ||
                    key.endsWith(".username") ||
                    key.endsWith(".secretKey") ||
                    key.endsWith(".appId") ||
                    key.endsWith(".privateKey") ||
                    key.endsWith(".passphrase") ||
                    key.endsWith(".token") ||
                    key.endsWith(".cosappId") ||
                    key.endsWith(".secretId") ||
                    key.endsWith(".region")
                )) {
            key.substringAfter("settings.").substringBeforeLast(".")
        } else {
            key
        }
    }

    fun getCredentialValue(valueList: List<String>, type: CredentialType, key: String): String? {
        if (valueList.isEmpty()) {
            return null
        }

        when (type) {
            CredentialType.PASSWORD -> {
                if (key.endsWith(".password")) {
                    return valueList[0]
                }
            }
            CredentialType.ACCESSTOKEN -> {
                if (key.endsWith(".access_token")) {
                    return valueList[0]
                }
            }
            CredentialType.USERNAME_PASSWORD -> {
                if (valueList.size >= 2) {
                    if (key.endsWith(".username")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".password")) {
                        return valueList[1]
                    }
                }
            }
            CredentialType.SECRETKEY -> {
                if (key.endsWith(".secretKey")) {
                    return valueList[0]
                }
            }
            CredentialType.APPID_SECRETKEY -> {
                if (valueList.size >= 2) {
                    if (key.endsWith(".appId")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".secretKey")) {
                        return valueList[1]
                    }
                }
            }
            CredentialType.SSH_PRIVATEKEY -> {
                if (valueList.size == 1) {
                    if (key.endsWith(".privateKey")) {
                        return valueList[0]
                    }
                }
                if (valueList.size >= 2) {
                    if (key.endsWith(".privateKey")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".passphrase")) {
                        return valueList[1]
                    }
                }
            }
            CredentialType.TOKEN_SSH_PRIVATEKEY -> {
                if (valueList.size == 2) {
                    if (key.endsWith(".token")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".privateKey")) {
                        return valueList[1]
                    }
                }
                if (valueList.size >= 3) {
                    if (key.endsWith(".token")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".privateKey")) {
                        return valueList[1]
                    }
                    if (key.endsWith(".passphrase")) {
                        return valueList[2]
                    }
                }
            }
            CredentialType.TOKEN_USERNAME_PASSWORD -> {
                if (valueList.size >= 3) {
                    if (key.endsWith(".token")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".username")) {
                        return valueList[1]
                    }
                    if (key.endsWith(".password")) {
                        return valueList[2]
                    }
                }
            }
            CredentialType.COS_APPID_SECRETID_SECRETKEY_REGION -> {
                if (valueList.size >= 4) {
                    if (key.endsWith(".cosappId")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".secretId")) {
                        return valueList[1]
                    }
                    if (key.endsWith(".secretKey")) {
                        return valueList[2]
                    }
                    if (key.endsWith(".region")) {
                        return valueList[3]
                    }
                }
            }
            CredentialType.MULTI_LINE_PASSWORD -> {
                if (valueList.isNotEmpty() && key.endsWith(".password")) {
                    return valueList[0]
                }
            }
        }
        return null
    }

    fun getDecodedCredentialList(
        credential: CredentialInfo,
        pair: DHKeyPair
    ): List<String> {
        val list = mutableListOf<String>()
        list.add(decode(credential.v1, credential.publicKey, pair.privateKey))
        credential.v2?.let { list.add(decode(it, credential.publicKey, pair.privateKey)) }
        credential.v3?.let { list.add(decode(it, credential.publicKey, pair.privateKey)) }
        credential.v4?.let { list.add(decode(it, credential.publicKey, pair.privateKey)) }
        return list
    }

    private fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
        val decoder = Base64.getDecoder()
        return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
    }
}
