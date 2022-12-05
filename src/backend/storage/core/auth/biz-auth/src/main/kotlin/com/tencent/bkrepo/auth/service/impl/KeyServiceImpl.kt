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

package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TKey
import com.tencent.bkrepo.auth.pojo.Key
import com.tencent.bkrepo.auth.repository.KeyRepository
import com.tencent.bkrepo.auth.service.KeyService
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.security.util.SecurityUtils
import org.apache.commons.codec.binary.Hex
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Base64

@Service
class KeyServiceImpl(
    private val keyRepository: KeyRepository
) : KeyService {
    override fun createKey(name: String, key: String) {
        Preconditions.checkArgument(validateKeyFormat(key), "key")
        val fingerprint = calculateFingerprint(key)
        val userId = SecurityUtils.getUserId()
        keyRepository.findByFingerprint(fingerprint)?.let {
            throw ErrorCodeException(AuthMessageCode.AUTH_DUP_KEY)
        }
        val tKey = TKey(
            name = name,
            key = key,
            fingerprint = fingerprint,
            userId = userId,
            createAt = LocalDateTime.now()
        )
        keyRepository.save(tKey)
    }

    override fun listKey(): List<Key> {
        val userId = SecurityUtils.getUserId()
        return keyRepository.findByUserId(userId).map { transfer(it) }
    }

    override fun deleteKey(id: String) {
        val userId = SecurityUtils.getUserId()
        val key = keyRepository.findById(id).orElseThrow { ErrorCodeException(AuthMessageCode.AUTH_DELETE_KEY_FAILED) }
        if (key.userId != userId) {
            throw ErrorCodeException(AuthMessageCode.AUTH_DELETE_KEY_FAILED)
        }
        keyRepository.delete(key)
    }

    private fun transfer(tKey: TKey): Key {
        return Key(
            id = tKey.id!!,
            name = tKey.name,
            fingerprint = tKey.fingerprint,
            createAt = tKey.createAt
        )
    }

    private fun validateKeyFormat(key: String): Boolean {
        val parts = key.split(" ")
        if (parts.size != 3) {
            return false
        }

        val keyType = parts[0].trim()
        if (!KEY_TYPES.contains(keyType)) {
            return false
        }

        val encodeString = parts[1].trim()
        val bytes = try {
            Base64.getDecoder().decode(encodeString)
        } catch (e: IllegalArgumentException) {
            return false
        }
        var startIndex = 0
        var endIndex = startIndex + DATA_LENGTH_HEADER_SIZE - 1
        val dataLength = Hex.encodeHexString(bytes.slice(IntRange(startIndex, endIndex)).toByteArray()).toInt(16)
        startIndex = endIndex + 1
        endIndex = startIndex + dataLength - 1
        val decodeKeyType = bytes.slice(IntRange(startIndex, endIndex)).toByteArray().toString(Charsets.UTF_8)
        if (decodeKeyType != keyType) {
            return false
        }

        return true
    }

    private fun calculateFingerprint(publicKey: String): String {
        val derFormat = publicKey.split(" ")[1].trim()
        val messageDigest = MessageDigest.getInstance("MD5")
        val digest = messageDigest.digest(Base64.getDecoder().decode(derFormat))
        val toRet = StringBuilder()
        for (i in digest.indices) {
            if (i != 0) toRet.append(":")
            val b = digest[i].toInt() and 0xff
            val hex = Integer.toHexString(b)
            if (hex.length == 1) toRet.append("0")
            toRet.append(hex)
        }
        return toRet.toString()
    }

    companion object {
        private val KEY_TYPES = listOf(
            "ecdsa-sha2-nistp256", "ecdsa-sha2-nistp384", "ecdsa-sha2-nistp521",
            "ssh-ed25519", "ssh-dss", "ssh-rsa"
        )

        private const val DATA_LENGTH_HEADER_SIZE = 4
    }
}
