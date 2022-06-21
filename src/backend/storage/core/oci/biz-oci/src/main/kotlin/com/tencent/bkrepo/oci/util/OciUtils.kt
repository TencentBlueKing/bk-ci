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

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.readYamlString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.oci.constant.DOCKER_IMAGE_MANIFEST_MEDIA_TYPE_V1
import com.tencent.bkrepo.oci.constant.FILE_EXTENSION
import com.tencent.bkrepo.oci.constant.MANIFEST_INVALID_CODE
import com.tencent.bkrepo.oci.constant.MANIFEST_INVALID_DESCRIPTION
import com.tencent.bkrepo.oci.constant.MANIFEST_INVALID_MESSAGE
import com.tencent.bkrepo.oci.exception.OciBadRequestException
import com.tencent.bkrepo.oci.model.Descriptor
import com.tencent.bkrepo.oci.model.ManifestSchema1
import com.tencent.bkrepo.oci.model.ManifestSchema2
import com.tencent.bkrepo.oci.model.SchemaVersion
import com.tencent.bkrepo.oci.pojo.metadata.HelmChartMetadata
import com.tencent.bkrepo.oci.util.DecompressUtil.getArchivesContent
import java.io.InputStream

/**
 * oci blob 工具类
 */
object OciUtils {

    fun checkVersion(inputStream: InputStream): SchemaVersion {
        try {
            return JsonUtils.objectMapper.readValue(
                inputStream, SchemaVersion::class.java
            )
        } catch (e: Exception) {
            throw OciBadRequestException(MANIFEST_INVALID_MESSAGE, MANIFEST_INVALID_CODE, MANIFEST_INVALID_DESCRIPTION)
        }
    }

    fun checkVersion(mediaType: String): Int {
        if (DOCKER_IMAGE_MANIFEST_MEDIA_TYPE_V1 == mediaType) return 1
        return 2
    }

    fun streamToManifestV1(inputStream: InputStream): ManifestSchema1 {
        try {
            return JsonUtils.objectMapper.readValue(
                inputStream, ManifestSchema1::class.java
            )
        } catch (e: Exception) {
            throw OciBadRequestException(MANIFEST_INVALID_MESSAGE, MANIFEST_INVALID_CODE, MANIFEST_INVALID_DESCRIPTION)
        }
    }

    fun streamToManifestV2(inputStream: InputStream): ManifestSchema2 {
        try {
            return JsonUtils.objectMapper.readValue(
                inputStream, ManifestSchema2::class.java
            )
        } catch (e: Exception) {
            throw OciBadRequestException(MANIFEST_INVALID_MESSAGE, MANIFEST_INVALID_CODE, MANIFEST_INVALID_DESCRIPTION)
        }
    }

    fun parseChartInputStream(inputStream: InputStream): HelmChartMetadata {
        val result = inputStream.getArchivesContent(FILE_EXTENSION)
        return result.byteInputStream().readYamlString()
    }

    fun convertToMap(chartInfo: HelmChartMetadata): Map<String, Any> {
        return chartInfo.toJsonString().readJsonString()
    }

    fun manifestIterator(manifest: ManifestSchema2): List<Descriptor> {
        val list = mutableListOf<Descriptor>()
        list.add(manifest.config)
        list.addAll(manifest.layers)
        return list
    }

    fun manifestIteratorDegist(manifest: ManifestSchema2): List<String> {
        val list = mutableListOf<String>()
        list.add(manifest.config.digest)
        manifest.layers.forEach {
            list.add(it.digest)
        }
        return list
    }

    fun manifestIteratorDegist(manifest: ManifestSchema1): List<String> {
        val list = mutableListOf<String>()
        manifest.fsLayers.forEach {
            list.add(it.blobSum)
        }
        return list
    }

    fun manifestIterator(manifest: ManifestSchema2, mediaType: String): Descriptor? {
        val list = mutableListOf<Descriptor>()
        list.add(manifest.config)
        list.addAll(manifest.layers)
        return list.find { it.mediaType == mediaType }
    }

    /**
     * 根据packageKey获取对应的package name
     */
    fun getPackageNameFormPackageKey(
        packageKey: String,
        defaultType: RepositoryType,
        extraTypes: List<RepositoryType>
    ): String {
        var packageName = StringPool.EMPTY
        if (packageKey.startsWith(defaultType.name.toLowerCase())) {
            return PackageKeys.resolveName(defaultType.name.toLowerCase(), packageKey)
        }
        extraTypes.forEach {
            if (packageKey.startsWith(it.name.toLowerCase())) {
                packageName = PackageKeys.resolveName(it.name.toLowerCase(), packageKey)
                return@forEach
            }
        }
        return packageName
    }
}
