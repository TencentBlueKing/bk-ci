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

package com.tencent.bkrepo.common.artifact.repository.virtual

import com.tencent.bkrepo.common.artifact.constant.TRAVERSED_LIST
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactTransferContext
import com.tencent.bkrepo.common.artifact.repository.context.RepositoryHolder
import com.tencent.bkrepo.common.artifact.repository.core.AbstractArtifactRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class VirtualRepository : AbstractArtifactRepository() {

    @Autowired
    lateinit var repositoryClient: RepositoryClient

    override fun search(context: ArtifactSearchContext): Any? {
        val artifactInfo = context.artifactInfo
        val virtualConfiguration = context.repositoryConfiguration as VirtualConfiguration
        val repoList = virtualConfiguration.repositoryList
        val traversedList = getTraversedList(context)
        for (repoIdentify in repoList) {
            if (repoIdentify in traversedList) {
                logger.debug("Repository[$repoIdentify] has been traversed, skip it.")
                continue
            }
            traversedList.add(repoIdentify)
            try {
                val subRepoInfo = repositoryClient.detail(repoIdentify.projectId, repoIdentify.name).data!!
                val repository = RepositoryHolder.getRepository(subRepoInfo.category) as AbstractArtifactRepository
                val subContext = context.copy(repositoryInfo = subRepoInfo) as ArtifactSearchContext
                repository.search(subContext)?.let { jsonObj ->
                    logger.debug("Artifact[$artifactInfo] is found it Repository[$repoIdentify].")
                    return jsonObj
                } ?: logger.debug("Artifact[$artifactInfo] is not found in Repository[$repoIdentify], skipped.")
            } catch (ignored: Exception) {
                logger.warn("Search Artifact[$artifactInfo] from Repository[$repoIdentify] failed: ${ignored.message}")
            }
        }
        return null
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        val artifactInfo = context.artifactInfo
        val virtualConfiguration = context.repositoryConfiguration as VirtualConfiguration
        val repoList = virtualConfiguration.repositoryList
        val traversedList = getTraversedList(context)
        for (repoIdentify in repoList) {
            if (repoIdentify in traversedList) {
                logger.debug("Repository[$repoIdentify] has been traversed, skip it.")
                continue
            }
            traversedList.add(repoIdentify)
            try {
                val subRepoInfo = repositoryClient.detail(repoIdentify.projectId, repoIdentify.name).data!!
                val repository = RepositoryHolder.getRepository(subRepoInfo.category) as AbstractArtifactRepository
                val subContext = context.copy(repositoryInfo = subRepoInfo) as ArtifactDownloadContext
                repository.onDownload(subContext)?.let {
                    logger.debug("Artifact[$artifactInfo] is found it Repository[$repoIdentify].")
                    return it
                } ?: logger.debug("Artifact[$artifactInfo] is not found in Repository[$repoIdentify], skipped.")
            } catch (ignored: Exception) {
                logger.warn("Download Artifact[$artifactInfo] from Repository[$repoIdentify] failed: ${ignored.message}")
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    protected fun getTraversedList(context: ArtifactTransferContext): MutableList<RepositoryIdentify> {
        return context.contextAttributes[TRAVERSED_LIST] as? MutableList<RepositoryIdentify> ?: let {
            val selfRepoInfo = context.repositoryInfo
            val traversedList = mutableListOf(RepositoryIdentify(selfRepoInfo.projectId, selfRepoInfo.name))
            context.contextAttributes[TRAVERSED_LIST] = traversedList
            return traversedList
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VirtualRepository::class.java)
    }
}
