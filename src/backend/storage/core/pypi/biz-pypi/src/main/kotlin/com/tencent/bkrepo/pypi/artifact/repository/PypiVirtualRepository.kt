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

package com.tencent.bkrepo.pypi.artifact.repository

import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactListContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.RepositoryHolder
import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import com.tencent.bkrepo.pypi.artifact.xml.Value
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PypiVirtualRepository : VirtualRepository(), PypiRepository {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PypiVirtualRepository::class.java)
    }

    /**
     * 整合多个仓库的内容。
     */
    override fun list(context: ArtifactListContext) {
        val virtualConfiguration = context.repositoryConfiguration as VirtualConfiguration

        val repoList = virtualConfiguration.repositoryList
        val traversedList = getTraversedList(context)
        for (repoIdentify in repoList) {
            if (repoIdentify in traversedList) {
                logger.debug("Repository[$repoIdentify] has been traversed, skip it.")
                continue
            }
            traversedList.add(repoIdentify)
            val subRepoInfo = repositoryClient.detail(repoIdentify.projectId, repoIdentify.name).data!!
            val repository = RepositoryHolder.getRepository(subRepoInfo.category)
            val subContext = context.copy(repositoryInfo = subRepoInfo) as ArtifactListContext
            repository.list(subContext)
        }
    }

    override fun searchNodeList(context: ArtifactSearchContext, xmlString: String): MutableList<Value>? {
        val valueList: MutableList<Value> = mutableListOf()
        val virtualConfiguration = context.repositoryConfiguration as VirtualConfiguration
        val repoList = virtualConfiguration.repositoryList
        val traversedList = getTraversedList(context)
        for (repoIdentify in repoList) {
            if (repoIdentify in traversedList) {
                logger.debug("Repository[$repoIdentify] has been traversed, skip it.")
                continue
            }
            traversedList.add(repoIdentify)
            val subRepoInfo = repositoryClient.detail(repoIdentify.projectId, repoIdentify.name).data!!
            val repository = RepositoryHolder.getRepository(subRepoInfo.category) as PypiRepository
            val subContext = context.copy(subRepoInfo) as ArtifactSearchContext
            val subValueList = repository.searchNodeList(subContext, xmlString)
            subValueList?.let {
                valueList.addAll(it)
            }
        }
        return valueList
    }
}
