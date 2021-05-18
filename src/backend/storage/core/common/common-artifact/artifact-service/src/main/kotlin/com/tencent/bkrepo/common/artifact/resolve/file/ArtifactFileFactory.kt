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

package com.tencent.bkrepo.common.artifact.resolve.file

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.resolve.file.multipart.MultipartArtifactFile
import com.tencent.bkrepo.common.artifact.resolve.file.stream.OctetStreamArtifactFile
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

/**
 * ArtifactFile工厂方法
 */
// LateinitUsage: 静态成员通过init构造函数初始化
@Suppress("LateinitUsage")
@Component
class ArtifactFileFactory(
    storageProperties: StorageProperties,
    storageHealthMonitor: StorageHealthMonitor
) {

    init {
        monitor = storageHealthMonitor
        properties = storageProperties
    }

    companion object {

        private lateinit var monitor: StorageHealthMonitor
        private lateinit var properties: StorageProperties

        const val ARTIFACT_FILES = "artifact.files"

        fun build(inputStream: InputStream): ArtifactFile {
            return OctetStreamArtifactFile(inputStream, monitor, properties, getStorageCredentials()).apply {
                track(this)
            }
        }

        fun build(multipartFile: MultipartFile): ArtifactFile {
            return MultipartArtifactFile(multipartFile, monitor, properties, getStorageCredentials()).apply {
                track(this)
            }
        }

        private fun getStorageCredentials(): StorageCredentials {
            return ArtifactContextHolder.getRepoDetail()?.storageCredentials ?: properties.defaultStorageCredentials()
        }

        @Suppress("UNCHECKED_CAST")
        private fun track(artifactFile: ArtifactFile) {
            val attributes = RequestContextHolder.getRequestAttributes() ?: return
            var artifactFileList = attributes.getAttribute(ARTIFACT_FILES, SCOPE_REQUEST) as? MutableList<ArtifactFile>
            if (artifactFileList == null) {
                artifactFileList = mutableListOf()
                attributes.setAttribute(ARTIFACT_FILES, artifactFileList, SCOPE_REQUEST)
            }
            artifactFileList.add(artifactFile)
        }
    }
}
