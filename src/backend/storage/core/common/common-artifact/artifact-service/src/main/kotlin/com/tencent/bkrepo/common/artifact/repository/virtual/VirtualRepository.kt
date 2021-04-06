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

package com.tencent.bkrepo.common.artifact.repository.virtual

import com.tencent.bkrepo.common.artifact.constant.TRAVERSED_LIST
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.core.AbstractArtifactRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import org.slf4j.LoggerFactory

/**
 * 虚拟仓库抽象逻辑
 */
abstract class VirtualRepository : AbstractArtifactRepository() {

    override fun query(context: ArtifactQueryContext): Any? {
        val artifactInfo = context.artifactInfo
        val virtualConfiguration = context.getVirtualConfiguration()
        val repoList = virtualConfiguration.repositoryList
        val traversedList = getTraversedList(context)
        for (repoIdentify in repoList) {
            if (repoIdentify in traversedList) {
                continue
            }
            traversedList.add(repoIdentify)
            try {
                val subRepoDetail = repositoryClient.getRepoDetail(repoIdentify.projectId, repoIdentify.name).data!!
                val repository = ArtifactContextHolder.getRepository(subRepoDetail.category)
                val subContext = context.copy(subRepoDetail)
                require(subContext is ArtifactQueryContext)
                repository.query(subContext)?.let {
                    if (logger.isDebugEnabled) {
                        logger.debug("Artifact[$artifactInfo] is found in repo[$repoIdentify].")
                    }
                    return it
                }
            } catch (ignored: Exception) {
                logger.warn("Query Artifact[$artifactInfo] from repo[$repoIdentify] failed: ${ignored.message}")
            }
        }
        return null
    }

    override fun search(context: ArtifactSearchContext): List<Any> {
        val artifactInfo = context.artifactInfo
        val virtualConfiguration = context.getVirtualConfiguration()
        val repoList = virtualConfiguration.repositoryList
        val traversedList = getTraversedList(context)
        for (repoIdentify in repoList) {
            if (repoIdentify in traversedList) {
                continue
            }
            traversedList.add(repoIdentify)
            try {
                val subRepoDetail = repositoryClient.getRepoDetail(repoIdentify.projectId, repoIdentify.name).data!!
                val repository = ArtifactContextHolder.getRepository(subRepoDetail.category)
                val subContext = context.copy(subRepoDetail)
                require(subContext is ArtifactSearchContext)
                repository.search(subContext).let {
                    if (logger.isDebugEnabled) {
                        logger.debug("Artifact[$artifactInfo] is found in repo[$repoIdentify].")
                    }
                    return it
                }
            } catch (ignored: Exception) {
                logger.warn("Search Artifact[$artifactInfo] from repo[$repoIdentify] failed: ${ignored.message}")
            }
        }
        return emptyList()
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        val artifactInfo = context.artifactInfo
        val virtualConfiguration = context.getVirtualConfiguration()
        val repoList = virtualConfiguration.repositoryList
        val traversedList = getTraversedList(context)
        for (repoIdentify in repoList) {
            if (repoIdentify in traversedList) {
                if (logger.isDebugEnabled) {
                    logger.debug("Repository[$repoIdentify] has been traversed, skip it.")
                }
                continue
            }
            traversedList.add(repoIdentify)
            try {
                val projectId = repoIdentify.projectId
                val repoName = repoIdentify.name
                val subRepoDetail = repositoryClient.getRepoDetail(projectId, repoName).data!!
                val repository = ArtifactContextHolder.getRepository(subRepoDetail.category)
                val subContext = context.copy(subRepoDetail)
                require(subContext is ArtifactDownloadContext)
                require(repository is AbstractArtifactRepository)
                repository.onDownload(subContext)?.let {
                    if (logger.isDebugEnabled) {
                        logger.debug("Artifact[$artifactInfo] is found in repo[$repoIdentify].")
                    }
                    return it
                } ?: run {
                    if (logger.isDebugEnabled) {
                        logger.debug("Artifact[$artifactInfo] is not found in repo[$repoIdentify], skipped.")
                    }
                }
            } catch (ignored: RuntimeException) {
                logger.warn("Download Artifact[$artifactInfo] from repo[$repoIdentify] failed: ${ignored.message}")
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    protected fun getTraversedList(context: ArtifactContext): MutableList<RepositoryIdentify> {
        return context.getAttribute(TRAVERSED_LIST) as? MutableList<RepositoryIdentify> ?: let {
            val selfRepoInfo = context.repositoryDetail
            val traversedList = mutableListOf(RepositoryIdentify(selfRepoInfo.projectId, selfRepoInfo.name))
            context.putAttribute(TRAVERSED_LIST, traversedList)
            return traversedList
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VirtualRepository::class.java)
    }
}
