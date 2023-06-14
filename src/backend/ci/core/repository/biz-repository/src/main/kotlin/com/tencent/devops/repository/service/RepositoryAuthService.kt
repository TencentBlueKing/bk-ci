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

package com.tencent.devops.repository.service

import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryAuthService @Autowired constructor(
    val repositoryService: RepositoryService,
    val authTokenApi: AuthTokenApi
) {

    fun getRepository(
        projectId: String,
        offset: Int,
        limit: Int,
        token: String
    ): ListInstanceResponseDTO? {
        authTokenApi.checkToken(token)
        val repositoryInfos =
            repositoryService.listByProject(setOf(projectId), null, offset, limit)
        val result = ListInstanceInfo()
        if (repositoryInfos?.records == null) {
            logger.info("project $projectId no code base")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        repositoryInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.repositoryId!!.toString()
            entity.displayName = it.aliasName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${repositoryInfos?.count}")
        return result.buildListInstanceResult(entityInfo, repositoryInfos.count)
    }

    // 此处用户iam无权限跳转回调，已页面拿到的id透传。 此处页面拿到的为hashId
    fun getRepositoryInfo(ids: List<Any>?, token: String): FetchInstanceInfoResponseDTO? {
        authTokenApi.checkToken(token)
        var repositoryInfos = repositoryService.getInfoByIds(ids as List<Long>)
        val result = FetchInstanceInfo()
        if (repositoryInfos == null || repositoryInfos.isEmpty()) {
            repositoryInfos = repositoryService.getInfoByHashIds(ids as List<String>)
            if (repositoryInfos == null) {
                logger.info("$ids not matched to the codebase")
                return result.buildFetchInstanceFailResult()
            }
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        repositoryInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.repositoryId!!.toString()
            entity.displayName = it.aliasName
            entity.iamApprover = if (it.createUser == null) emptyList() else arrayListOf(it.createUser)
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${repositoryInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchRepositoryInstances(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int,
        token: String
    ): SearchInstanceInfo {
        logger.info("searchInstance keyword[$keyword] projectId[$projectId], limit[$limit] , offset[$offset]")
        authTokenApi.checkToken(token)
        val repositoryRecords = repositoryService.searchByAliasName(
            projectId = projectId,
            limit = limit,
            offset = offset,
            aliasName = keyword
        )
        logger.info("repositoryRecords $repositoryRecords")
        val count = repositoryRecords?.count ?: 0L
        val repositorytInfo = mutableListOf<InstanceInfoDTO>()
        repositoryRecords?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.repositoryId!!.toString()
            entity.displayName = it.aliasName
            repositorytInfo.add(entity)
        }
        logger.info("repositorytInfo $repositorytInfo")
        val result = SearchInstanceInfo()
        return result.buildSearchInstanceResult(repositorytInfo, count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(RepositoryAuthService::class.java)
    }
}
