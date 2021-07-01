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

package com.tencent.devops.process.service

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
class AuthPipelineService @Autowired constructor(
    val authTokenApi: AuthTokenApi,
    val pipelineListFacadeService: PipelineListFacadeService
) {
    fun searchPipeline(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int,
        token: String
    ): SearchInstanceInfo {
        authTokenApi.checkToken(token)
//        val pipelineInfos =
//            client.get(ServiceAuthPipelineResource::class)
//                .searchPipelineInstances(projectId, offset, limit, keyword).data
        val pipelineInfos = pipelineListFacadeService.searchByPipelineName(
            projectId = projectId,
            pipelineName = keyword,
            limit = limit,
            offset = offset
        )
        val result = SearchInstanceInfo()
        if (pipelineInfos?.records == null) {
            logger.info("$projectId 项目下无流水线")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.id?.toString() ?: "0"
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos?.count}")
        return result.buildSearchInstanceResult(entityInfo, pipelineInfos.count)
    }

    fun getPipeline(projectId: String, offset: Int, limit: Int, token: String): ListInstanceResponseDTO? {
        authTokenApi.checkToken(token)
//        val pipelineInfos =
//            client.get(ServiceAuthPipelineResource::class)
//                .pipelineList(projectId, offset, limit).data
        val pipelineInfos = pipelineListFacadeService.getPipelinePage(
            projectId = projectId,
            limit = limit,
            offset = offset
        )
        val result = ListInstanceInfo()
        if (pipelineInfos?.records == null) {
            logger.info("$projectId 项目下无流水线")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.id?.toString() ?: "0"
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos?.count}")
        return result.buildListInstanceResult(entityInfo, pipelineInfos.count)
    }

    fun getPipelineInfo(ids: List<Any>?, token: String): FetchInstanceInfoResponseDTO? {
        authTokenApi.checkToken(token)
//        val pipelineInfos =
//            client.get(ServiceAuthPipelineResource::class)
//                .pipelineInfos(ids!!.toSet() as Set<String>).data
        val pipelineInfos = pipelineListFacadeService.getPipelineByIds(pipelineIds = ids!!.toSet() as Set<String>)
        val result = FetchInstanceInfo()

        if (pipelineInfos == null || pipelineInfos.isEmpty()) {
            logger.info("$ids 未匹配到启用流水线")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.id?.toString() ?: "0"
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
