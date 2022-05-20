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

package com.tencent.bkrepo.oci.util

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.HttpHeaders.CONTENT_TYPE
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.oci.constant.DOCKER_API_VERSION
import com.tencent.bkrepo.oci.constant.DOCKER_CONTENT_DIGEST
import com.tencent.bkrepo.oci.constant.DOCKER_HEADER_API_VERSION
import com.tencent.bkrepo.oci.constant.DOCKER_UPLOAD_UUID
import com.tencent.bkrepo.oci.constant.OCI_API_PREFIX
import com.tencent.bkrepo.oci.pojo.digest.OciDigest
import javax.servlet.http.HttpServletResponse

/**
 * oci 响应工具
 */
object OciResponseUtils {

    fun getResponseLocationURI(path: String, domain: String): String {
        return UrlFormatter.format(
            domain, OCI_API_PREFIX + path.trimStart('/')
        )
    }

    fun buildUploadResponse(domain: String, digest: OciDigest, locationStr: String, response: HttpServletResponse) {
        uploadResponse(domain, response, HttpStatus.CREATED, locationStr, digest.toString())
    }

    fun buildBlobUploadUUIDResponse(domain: String, uuid: String, locationStr: String, response: HttpServletResponse) {
        uploadResponse(domain, response, HttpStatus.ACCEPTED, locationStr, null, uuid)
    }

    fun buildDownloadResponse(digest: OciDigest, response: HttpServletResponse, size: Long? = null) {
        downloadResponse(
            response,
            digest.toString(),
            MediaTypes.APPLICATION_OCTET_STREAM,
            size
        )
    }

    fun buildDeleteResponse(response: HttpServletResponse) {
        deleteResponse(response)
    }

    private fun uploadResponse(
        domain: String,
        response: HttpServletResponse = HttpContextHolder.getResponse(),
        status: HttpStatus,
        locationStr: String,
        digest: String? = null,
        uuid: String? = null
    ) {
        val location = getResponseLocationURI(locationStr, domain)
        response.status = status.value
        response.addHeader(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        digest?.let {
            response.addHeader(DOCKER_CONTENT_DIGEST, digest)
        }
        response.addHeader(DOCKER_UPLOAD_UUID, uuid)
        response.addHeader(HttpHeaders.LOCATION, location)
        uuid?.let {
            response.addHeader(DOCKER_UPLOAD_UUID, uuid)
        }
    }

    private fun downloadResponse(
        response: HttpServletResponse = HttpContextHolder.getResponse(),
        digest: String,
        mediaType: String,
        size: Long? = null
    ) {
        response.status = HttpStatus.OK.value
        response.addHeader(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        response.addHeader(DOCKER_CONTENT_DIGEST, digest)
        response.addHeader(HttpHeaders.ETAG, digest)
        size?.let {
            response.addHeader(HttpHeaders.CONTENT_LENGTH, size.toString())
        }
        response.addHeader(CONTENT_TYPE, mediaType)
    }

    private fun deleteResponse(
        response: HttpServletResponse = HttpContextHolder.getResponse()
    ) {
        response.status = HttpStatus.ACCEPTED.value
    }
}
