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

package com.tencent.bkrepo.docker.manifest

import com.tencent.bkrepo.common.api.constant.StringPool.EMPTY
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.docker.constant.DOCKER_MEDIA_TYPE
import com.tencent.bkrepo.docker.constant.DOCKER_SCHEMA_VERSION
import org.springframework.http.MediaType

/**
 * enum type of manifest
 */
enum class ManifestType(private val mediaType: String) {

    Schema1("application/vnd.docker.distribution.manifest.v1+json"),
    Schema1Signed("application/vnd.docker.distribution.manifest.v1+prettyjws"),
    Schema2("application/vnd.docker.distribution.manifest.v2+json"),
    Schema2List("application/vnd.docker.distribution.manifest.list.v2+json");

    override fun toString(): String {
        return this.mediaType
    }

    companion object {

        /**
         * get ManifestType from http media type
         * @param mediaType http MediaType
         * @return ManifestType manifest type result
         */
        fun from(mediaType: MediaType?): ManifestType {
            return from(mediaType.toString())
        }

        /**
         * get ManifestType from content type
         * @param contentType
         * @return ManifestType manifest type result
         */
        fun from(contentType: String): ManifestType {
            val values = values()
            val size = values.size

            for (index in 0 until size) {
                val manifestType = values[index]
                if (manifestType.mediaType == contentType) {
                    return manifestType
                }
            }

            return Schema1Signed
        }

        /**
         * get ManifestType from manifest byte data
         * @param manifestBytes manifest byte data
         * @return ManifestType manifest type result
         */
        fun from(manifestBytes: ByteArray): ManifestType {
            val manifest = JsonUtils.objectMapper.readTree(manifestBytes)
            val schemaVersionNode = manifest.get(DOCKER_SCHEMA_VERSION)
            schemaVersionNode?.let {
                val schemaVersion = schemaVersionNode.intValue()
                if (schemaVersion == 1) {
                    return Schema1Signed
                }
            }
            val mediaType = manifest.get(DOCKER_MEDIA_TYPE)
            var contentType = EMPTY
            mediaType?.let {
                contentType = mediaType.textValue()
            }
            return from(contentType)
        }
    }
}
