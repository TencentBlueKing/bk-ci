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

package com.tencent.bkrepo.docker.helpers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Charsets
import com.tencent.bkrepo.common.api.constant.StringPool.EMPTY
import com.tencent.bkrepo.docker.constant.DOCKER_SCHEMA_VERSION
import com.tencent.bkrepo.docker.model.DockerDigest
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import java.lang.System.arraycopy

/**
 * to parse manifest digest
 */
object DockerManifestDigester {

    private val logger = LoggerFactory.getLogger(DockerManifestDigester::class.java)

    private const val DOCKER_SIG = "signatures"
    private const val DOCKER_PROTECT = "protected"
    private const val DOCKER_FORMAT_LENGTH = "formatLength"
    private const val DOCKER_FORMAT_TAIL = "formatTail"

    /**
     * calculate digest from config byte data
     * @param manifestBytes docker manifest config byte array
     * @return DockerDigest metadata property
     */
    fun calcDigest(manifestBytes: ByteArray): DockerDigest? {
        val manifest = mapper().readTree(manifestBytes)
        val schemaVersion = manifest.get(DOCKER_SCHEMA_VERSION) ?: run {
            logger.warn("unable to determine the schema version of the manifest")
            return null
        }
        val schema = schemaVersion.asInt()
        val digest = if (schema == 1) {
            getSchema1Digest(manifestBytes, manifest)
        } else {
            if (schema != 2) {
                logger.warn("unknown schema version [$schema] for manifest file")
                return null
            }
            getSchema2Digest(manifestBytes)
        }
        return DockerDigest.fromSha256(digest)
    }

    private fun getSchema2Digest(configBytes: ByteArray): String {
        val digest = DigestUtils.getSha256Digest()
        DigestUtils.updateDigest(digest, configBytes)
        return Hex.encodeHexString(digest.digest())
    }

    private fun getSchema1Digest(configBytes: ByteArray, manifest: JsonNode): String {
        var formatLength = 0
        var formatTail = EMPTY
        val signatures = manifest.get(DOCKER_SIG) ?: return getHexDigest(configBytes, formatLength, formatTail)
        val sig = signatures.iterator()
        while (sig.hasNext()) {
            val signature = sig.next() as JsonNode
            var protectJson = signature.get(DOCKER_PROTECT)
            protectJson?.let {
                val protectedBytes = Base64.decodeBase64(protectJson.asText())
                protectJson = mapper().readTree(protectedBytes)
                formatLength = protectJson.get(DOCKER_FORMAT_LENGTH).asInt()
                formatTail = protectJson.get(DOCKER_FORMAT_TAIL).asText()
                formatTail = String(Base64.decodeBase64(formatTail), Charsets.UTF_8)
            }
        }

        return getHexDigest(configBytes, formatLength, formatTail)
    }

    private fun getHexDigest(configBytes: ByteArray, formatLength: Int, formatTail: String): String {
        val formatTailLength = formatTail.length
        val bytes = configBytes.copyOf(formatLength + formatTailLength)
        arraycopy(formatTail.toByteArray(), 0, bytes, formatLength, formatTailLength)
        val digest = DigestUtils.getSha256Digest()
        DigestUtils.updateDigest(digest, bytes)
        return Hex.encodeHexString(digest.digest())
    }

    private fun mapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper
    }
}
