/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.environment.service

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
class AuthEnvService @Autowired constructor(
    private val envService: EnvService,
    private val authTokenApi: AuthTokenApi
) {
    fun getEnv(projectId: String, offset: Int, limit: Int, token: String): ListInstanceResponseDTO? {
        authTokenApi.checkToken(token)
        val envInfos =
            envService.listEnvironmentByLimit(projectId, offset, limit)
        val result = ListInstanceInfo()
        if (envInfos?.records == null) {
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        envInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.envHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${envInfos?.count}")
        return result.buildListInstanceResult(entityInfo, envInfos.count)
    }

    fun getEnvInfo(hashId: List<Any>?, token: String): FetchInstanceInfoResponseDTO? {
        authTokenApi.checkToken(token)
        val envInfos = envService.listRawEnvByHashIdsAllType(hashId as List<String>)
        val result = FetchInstanceInfo()
        if (envInfos == null || envInfos.isEmpty()) {
            logger.info("$hashId no env")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        envInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.envHashId
            entity.displayName = it.name
            entity.iamApprover = arrayListOf(it.createdUser)
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${envInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchEnv(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int,
        token: String
    ): SearchInstanceInfo {
        authTokenApi.checkToken(token)
        val envInfos = envService.searchByName(
            projectId = projectId,
            offset = offset,
            limit = limit,
            envName = keyword)
        val result = SearchInstanceInfo()
        if (envInfos?.records == null) {
            logger.info("There is no environment under the project $projectId ")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        envInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.envHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${envInfos?.count}")
        return result.buildSearchInstanceResult(entityInfo, envInfos.count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(AuthEnvService::class.java)
    }
}
