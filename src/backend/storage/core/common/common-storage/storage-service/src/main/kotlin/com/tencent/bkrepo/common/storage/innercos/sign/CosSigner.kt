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

package com.tencent.bkrepo.common.storage.innercos.sign

import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.common.storage.innercos.request.CosRequest
import java.security.MessageDigest
import java.time.Duration
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CosSigner {
    private const val ALGORITHM = "sha1"
    private const val HMAC_SHA1_ALGORITHM = "HmacSHA1"
    private const val SHA1_ALGORITHM = "SHA-1"
    private fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }

    fun sign(request: CosRequest, credentials: InnerCosCredentials, expiredTime: Duration): String {
        val currentTimestamp = System.currentTimeMillis() / 1000
        val signTime = "$currentTimestamp;${currentTimestamp + expiredTime.seconds}"
        val signKey = hmacSha1(signTime, credentials.secretKey)
        val formatString = buildFormatString(request)
        val stringToSign = buildStringToSign(signTime, formatString)
        val signature = hmacSha1(stringToSign, signKey)
        val signedHeaderList = buildSignedMember(request.headers.keys)
        val signedParameterList = buildSignedMember(request.parameters.keys)
        return buildAuthorization(credentials.secretId, signTime, signedHeaderList, signedParameterList, signature)
    }

    private fun buildSignedMember(keys: MutableSet<String>): String {
        return keys.joinToString(";") { it.toLowerCase() }
    }

    private fun buildFormatString(request: CosRequest): String {
        return StringBuilder()
            .appendln(request.getFormatMethod())
            .appendln(request.getFormatUri())
            .appendln(request.getFormatParameters())
            .appendln(request.getFormatHeaders())
            .toString()
    }

    private fun buildStringToSign(signTime: String, formatString: String): String {
        return StringBuilder()
            .appendln(ALGORITHM)
            .appendln(signTime)
            .appendln(sha1(formatString))
            .toString()
    }

    private fun buildAuthorization(
        secretId: String,
        signTime: String,
        signedHeaderList: String,
        signedParameterList: String,
        signature: String
    ): String {
        return StringBuilder()
            .append("q-sign-algorithm=").append(ALGORITHM).append('&')
            .append("q-ak=").append(secretId).append('&')
            .append("q-sign-time=").append(signTime).append('&')
            .append("q-key-time=").append(signTime).append('&')
            .append("q-header-list=").append(signedHeaderList).append('&')
            .append("q-url-param-list=").append(signedParameterList).append('&')
            .append("q-signature=").append(signature)
            .toString()
    }

    private fun sha1(content: String): String {
        val digest = MessageDigest.getInstance(SHA1_ALGORITHM)
        digest.update(content.toByteArray())
        return digest.digest().toHexString()
    }

    private fun hmacSha1(content: String, key: String): String {
        val signingKey = SecretKeySpec(key.toByteArray(), HMAC_SHA1_ALGORITHM)
        val mac = Mac.getInstance(HMAC_SHA1_ALGORITHM).apply { init(signingKey) }
        return mac.doFinal(content.toByteArray()).toHexString()
    }
}
