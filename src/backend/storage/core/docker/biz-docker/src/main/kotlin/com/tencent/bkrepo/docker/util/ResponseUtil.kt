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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.docker.util

import com.tencent.bkrepo.docker.constant.DOCKER_API_VERSION
import com.tencent.bkrepo.docker.constant.DOCKER_CONTENT_DIGEST
import com.tencent.bkrepo.docker.constant.DOCKER_HEADER_API_VERSION
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST_LIST
import com.tencent.bkrepo.docker.constant.HTTP_FORWARDED_PROTO
import com.tencent.bkrepo.docker.constant.HTTP_PROTOCOL_HTTP
import com.tencent.bkrepo.docker.constant.HTTP_PROTOCOL_HTTPS
import com.tencent.bkrepo.docker.errors.DockerV2Errors
import com.tencent.bkrepo.docker.manifest.ManifestType
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.response.DockerResponse
import org.apache.commons.io.output.NullOutputStream
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.io.InputStream
import java.net.URI
import java.util.Objects
import java.util.regex.Pattern
import javax.ws.rs.core.UriBuilder
import javax.xml.bind.DatatypeConverter
import kotlin.streams.toList

/**
 * docker repo service utility
 */
object ResponseUtil {

    private val logger = LoggerFactory.getLogger(ResponseUtil::class.java)

    private val OLD_USER_AGENT_PATTERN = Pattern.compile("^(?:docker\\/1\\.(?:3|4|5|6|7(?!\\.[0-9]-dev))|Go ).*$")

    private const val LOCAL_HOST = "localhost"

    val EMPTY_BLOB_CONTENT: ByteArray =
        DatatypeConverter.parseHexBinary("1f8b080000096e8800ff621805a360148c5800080000ffff2eafb5ef00040000")

    fun isEmptyBlob(digest: DockerDigest): Boolean {
        return digest.toString() == emptyBlobDigest().toString()
    }

    fun emptyBlobDigest(): DockerDigest {
        return DockerDigest("sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4")
    }

    fun emptyBlobHeadResponse(): DockerResponse {
        return ResponseEntity.ok().header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
            .header(DOCKER_CONTENT_DIGEST, emptyBlobDigest().toString()).header(HttpHeaders.CONTENT_LENGTH, "32")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE).build()
    }

    fun emptyBlobGetResponse(): DockerResponse {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_LENGTH, "32")
            .header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
            .header(DOCKER_CONTENT_DIGEST, emptyBlobDigest().toString())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE).body(EMPTY_BLOB_CONTENT)
    }

    fun putHasStream(httpHeaders: HttpHeaders): Boolean {
        val headerValues = httpHeaders["User-Agent"]
        headerValues?.let {
            val headerIter = it.iterator()
            while (headerIter.hasNext()) {
                val userAgent = headerIter.next() as String
                logger.debug("User agent header: [$userAgent]")
                if (OLD_USER_AGENT_PATTERN.matcher(userAgent).matches()) {
                    return true
                }
            }
        }
        return false
    }

    fun consumeStreamAndReturnError(stream: InputStream): DockerResponse {
        NullOutputStream().use {
            stream.copyTo(it)
        }
        return DockerV2Errors.unauthorizedUpload()
    }

    fun getAcceptableManifestTypes(httpHeaders: HttpHeaders): List<ManifestType> {
        return httpHeaders.accept.stream().filter { Objects.nonNull(it) }.map { ManifestType.from(it) }.toList()
    }

    // get docker return url
    fun getDockerURI(path: String, httpHeaders: HttpHeaders): URI {
        val hostHeaders = httpHeaders["Host"]
        var host = LOCAL_HOST
        var port: Int? = null
        if (hostHeaders != null && hostHeaders.isNotEmpty()) {
            val parts = (hostHeaders[0] as String).split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            host = parts[0]
            if (parts.size > 1) {
                port = Integer.valueOf(parts[1])
            }
        }

        val builder = UriBuilder.fromPath("v2/$path").host(host).scheme(getProtocol(httpHeaders))
        port?.let {
            builder.port(port)
        }
        return builder.build()
    }

    // build return manifest path
    fun buildManifestPath(dockerRepo: String, tag: String, manifestType: ManifestType): String {
        return if (ManifestType.Schema2List == manifestType) {
            "/$dockerRepo/$tag/$DOCKER_MANIFEST_LIST"
        } else {
            "/$dockerRepo/$tag/$DOCKER_MANIFEST"
        }
    }

    /**
     * determine to return http protocol
     * prefix or https prefix
     */
    private fun getProtocol(httpHeaders: HttpHeaders): String {
        val protocolHeaders = httpHeaders[HTTP_FORWARDED_PROTO]
        if (protocolHeaders == null || protocolHeaders.isEmpty()) {
            return HTTP_PROTOCOL_HTTP
        }
        return if (protocolHeaders.isNotEmpty()) {
            protocolHeaders.iterator().next() as String
        } else {
            logger.debug("X-Forwarded-Proto does not exist, return https.")
            HTTP_PROTOCOL_HTTPS
        }
    }
}
