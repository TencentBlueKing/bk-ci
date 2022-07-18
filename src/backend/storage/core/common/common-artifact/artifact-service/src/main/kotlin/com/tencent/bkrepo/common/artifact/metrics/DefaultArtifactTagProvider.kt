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

package com.tencent.bkrepo.common.artifact.metrics

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.constant.SOURCE_IN_MEMORY
import com.tencent.bkrepo.common.artifact.constant.SOURCE_IN_REMOTE
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactDataReceiver
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.FileArtifactInputStream
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import org.apache.commons.logging.LogFactory

/**
 * 默认构件提供tag实现
 * */
class DefaultArtifactTagProvider(
    private val storageProperties: StorageProperties,
    private val artifactMetricsProperties: ArtifactMetricsProperties
) : ArtifactTransferTagProvider {
    override fun getTags(): Iterable<Tag> {
        val repositoryDetail = ArtifactContextHolder.getRepoDetail() ?: return TagUtils.tagOfProjectAndRepo(
            StringPool.UNKNOWN,
            StringPool.UNKNOWN
        )
        return TagUtils.tagOfProjectAndRepo(
            repositoryDetail.projectId,
            repositoryDetail.name,
            artifactMetricsProperties.includeRepositories
        )
    }

    override fun getTags(inputStream: ArtifactInputStream, includeRepoInfo: Boolean): Iterable<Tag> {
        val repositoryDetail = ArtifactContextHolder.getRepoDetail()
        val path = if (inputStream is FileArtifactInputStream) inputStream.file.path else SOURCE_IN_REMOTE
        return getTags(repositoryDetail, includeRepoInfo, path)
    }

    override fun getTags(receiver: ArtifactDataReceiver, includeRepoInfo: Boolean): Iterable<Tag> {
        val repositoryDetail = ArtifactContextHolder.getRepoDetail()
        val path = getPath(receiver)
        return getTags(repositoryDetail, includeRepoInfo, path)
    }

    private fun getTags(
        repositoryDetail: RepositoryDetail?,
        includeRepoInfo: Boolean,
        path: String
    ): Tags {
        val credentials = repositoryDetail?.let { getStorageCredentials(it.storageCredentials) }
        if (!includeRepoInfo) {
            return Tags.of(
                PATH,
                getTagPath(credentials, path)
            )
        }
        return Tags.of(
            REPO_TAG, getRepoTagValue(repositoryDetail),
            PATH,
            getTagPath(credentials, path)
        )
    }

    private fun getPath(receiver: ArtifactDataReceiver): String {
        if (receiver.inMemory) {
            return SOURCE_IN_MEMORY
        }
        return receiver.filePath.toString()
    }

    private fun getTagPath(credentials: StorageCredentials?, path: String): String {
        if (path == SOURCE_IN_MEMORY || path == SOURCE_IN_REMOTE) {
            return path
        }
        credentials ?: return StringPool.UNKNOWN
        with(credentials) {
            if (path.startsWith(upload.location)) {
                return upload.location
            }
            if (path.startsWith(upload.localPath)) {
                return upload.localPath
            }
            if (path.startsWith(cache.path)) {
                return cache.path
            }
            logger.warn("Unknown path[$path] origin with key[${credentials.key}]")
            return StringPool.UNKNOWN
        }
    }

    private fun getStorageCredentials(credentials: StorageCredentials?): StorageCredentials {
        return credentials ?: storageProperties.defaultStorageCredentials()
    }

    private fun getRepoTagValue(repositoryDetail: RepositoryDetail?): String {
        if (repositoryDetail == null) {
            return StringPool.UNKNOWN
        }
        with(repositoryDetail) {
            return "$projectId/$name"
        }
    }

    companion object {
        private val logger = LogFactory.getLog(DefaultArtifactTagProvider::class.java)
        private const val PATH = "path"
        const val REPO_TAG = "repo"
    }
}
