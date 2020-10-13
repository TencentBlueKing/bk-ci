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

package com.tencent.bkrepo.npm.artifact.repository

import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactListContext
import com.tencent.bkrepo.common.artifact.repository.context.RepositoryHolder
import com.tencent.bkrepo.common.artifact.repository.core.AbstractArtifactRepository
import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import com.tencent.bkrepo.npm.constants.SEARCH_REQUEST
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NpmVirtualRepository : VirtualRepository() {
    override fun list(context: ArtifactListContext): NpmSearchResponse {
        val list = mutableListOf<NpmSearchResponse>()
        val searchRequest = context.contextAttributes[SEARCH_REQUEST] as MetadataSearchRequest
        val virtualConfiguration = context.repositoryConfiguration as VirtualConfiguration
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
                val subRepoInfo = repositoryClient.detail(repoIdentify.projectId, repoIdentify.name).data!!
                val repository = RepositoryHolder.getRepository(subRepoInfo.category) as AbstractArtifactRepository
                val subContext = context.copy(repositoryInfo = subRepoInfo) as ArtifactListContext
                repository.list(subContext)?.let { map ->
                    list.add(map as NpmSearchResponse)
                }
            } catch (exception: Exception) {
                logger.error("list Artifact[${context.artifactInfo}] from Repository[$repoIdentify] failed: ${exception.message}")
            }
        }
        return recordMap(list, searchRequest)
    }

    private fun recordMap(list: List<NpmSearchResponse>, searchRequest: MetadataSearchRequest): NpmSearchResponse {
        if (list.isNullOrEmpty() || list[0].objects.isNullOrEmpty() || list[1].objects.isNullOrEmpty()) {
            return NpmSearchResponse()
        }
        val size = searchRequest.size
        val firstList = list[0].objects
        val secondList = list[1].objects
        return if (firstList.size >= size) {
            NpmSearchResponse(objects = firstList.subList(0, size))
        } else {
            firstList.addAll(secondList)
            if (firstList.size > size) {
                NpmSearchResponse(objects = firstList.subList(0, size))
            } else {
                NpmSearchResponse(objects = firstList)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmVirtualRepository::class.java)
    }
}
