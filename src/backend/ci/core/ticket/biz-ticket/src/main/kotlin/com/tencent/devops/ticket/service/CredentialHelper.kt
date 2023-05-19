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

package com.tencent.devops.ticket.service

import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64

@Suppress("ALL")
@Component
class CredentialHelper {
    companion object {
        private val SSH_PRIVATE_REGEX = Regex("^(-----BEGIN (RSA|OPENSSH) PRIVATE KEY-----)[\\s\\S]*" +
            "(-----END (RSA|OPENSSH) PRIVATE KEY-----)\$")
    }

    @Value("\${credential.mixer}")
    val credentialMixer = "******"

    @Value("\${credential.aes-key}")
    private val aesKey = "C/R%3{?OS}IeGT21"

    fun isValid(credentialCreate: CredentialCreate): Boolean {
        return isValid(
            credentialType = credentialCreate.credentialType,
            v1 = credentialCreate.v1,
            v2 = credentialCreate.v2
        )
    }

    fun isValid(credentialUpdate: CredentialUpdate): Boolean {
        return isValid(
            credentialType = credentialUpdate.credentialType,
            v1 = credentialUpdate.v1,
            v2 = credentialUpdate.v2,
            update = true
        )
    }

    private fun isValid(credentialType: CredentialType, v1: String, v2: String?, update: Boolean = false): Boolean {
        return when (credentialType) {
            CredentialType.PASSWORD -> {
                true
            }
            CredentialType.ACCESSTOKEN -> {
                true
            }
            CredentialType.OAUTHTOKEN -> {
                true
            }
            CredentialType.USERNAME_PASSWORD -> {
                true
            }
            CredentialType.SECRETKEY -> {
                true
            }
            CredentialType.APPID_SECRETKEY -> {
                v2 ?: return false
                true
            }
            CredentialType.SSH_PRIVATEKEY -> {
                if (!SSH_PRIVATE_REGEX.matches(v1)) {
                    update && v1 == credentialMixer
                } else {
                    true
                }
            }
            CredentialType.TOKEN_SSH_PRIVATEKEY -> {
                v2 ?: return false
                if (!SSH_PRIVATE_REGEX.matches(v2)) {
                    update && v2 == credentialMixer
                } else {
                    true
                }
            }
            CredentialType.TOKEN_USERNAME_PASSWORD -> {
                true
            }
            CredentialType.COS_APPID_SECRETID_SECRETKEY_REGION -> {
                true
            }
            CredentialType.MULTI_LINE_PASSWORD -> {
                true
            }
        }
    }

    fun encryptCredential(
        aesEncryptedCredential: String?,
        publicKeyByteArray: ByteArray,
        serverPrivateKeyByteArray: ByteArray
    ): String? {

        if (aesEncryptedCredential.isNullOrBlank()) {
            return null
        }
        try {
            val credential = AESUtil.decrypt(aesKey, aesEncryptedCredential!!)
            val credentialEncryptedContent =
                DHUtil.encrypt(credential.toByteArray(), publicKeyByteArray, serverPrivateKeyByteArray)
            return String(Base64.getEncoder().encode(credentialEncryptedContent))
        } catch (ignored: Throwable) {
            throw ignored
        }
    }

    fun decryptCredential(aesCredential: String?): String? {
        if (aesCredential.isNullOrBlank()) {
            return null
        }
        return AESUtil.decrypt(aesKey, aesCredential!!)
    }

    fun encryptCredential(credential: String?): String? {
        if (credential.isNullOrBlank() || credential == credentialMixer) {
            return null
        }
        return AESUtil.encrypt(aesKey, credential!!)
    }
}
