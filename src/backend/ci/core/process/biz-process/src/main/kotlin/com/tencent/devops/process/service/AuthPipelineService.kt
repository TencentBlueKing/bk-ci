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

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
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
    fun pipelineInfo(
        callBackInfo: CallbackRequestDTO,
        token: String,
        returnPipelineId: Boolean? = false
    ): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent?.id ?: "" // FETCH_INSTANCE_INFO场景下iam不会传parentId
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return getPipeline(
                    projectId = projectId,
                    offset = page.offset.toInt(),
                    limit = page.limit.toInt(),
                    token = token,
                    returnPipelineId = returnPipelineId!!
                )
            }

            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return getPipelineInfo(ids, token, returnPipelineId!!)
            }

            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return searchPipeline(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.limit.toInt(),
                    offset = page.offset.toInt(),
                    token = token,
                    returnPipelineId = returnPipelineId!!
                )
            }

            else -> return null
        }
    }

    private fun searchPipeline(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int,
        token: String,
        returnPipelineId: Boolean
    ): SearchInstanceInfo {
        authTokenApi.checkToken(token)
        val pipelineInfos = pipelineListFacadeService.searchByPipelineName(
            projectId = projectId,
            pipelineName = keyword,
            limit = limit,
            offset = offset
        )
        val result = SearchInstanceInfo()
        if (pipelineInfos.records.isEmpty()) {
            logger.info("project $projectId no pipeline")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos.records.map {
            val entityId = if (returnPipelineId) {
                it.pipelineId
            } else {
                it.id?.toString() ?: "0"
            }
            val entity = InstanceInfoDTO()
            entity.id = entityId
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos.count}")
        return result.buildSearchInstanceResult(entityInfo, pipelineInfos.count)
    }

    private fun getPipeline(
        projectId: String,
        offset: Int,
        limit: Int,
        token: String,
        returnPipelineId: Boolean
    ): ListInstanceResponseDTO {
        authTokenApi.checkToken(token)
        val pipelineInfos = pipelineListFacadeService.getPipelinePage(
            projectId = projectId,
            limit = limit,
            offset = offset
        )
        val result = ListInstanceInfo()
        if (pipelineInfos.records.isEmpty()) {
            logger.info("$projectId There is no assembly line under the project")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos.records.map {
            val entityId = if (returnPipelineId) {
                it.pipelineId
            } else {
                it.id?.toString() ?: "0"
            }
            val entity = InstanceInfoDTO()
            entity.id = entityId
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos.count}")
        return result.buildListInstanceResult(entityInfo, pipelineInfos.count)
    }

    private fun getPipelineInfo(
        ids: List<String>?,
        token: String,
        returnPipelineId: Boolean
    ): FetchInstanceInfoResponseDTO {
        authTokenApi.checkToken(token)

        val pipelineId = ids!!.first().toString()
        val idNumType = pipelineId.matches("-?\\d+(\\.\\d+)?".toRegex()) // 判断是否为纯数字

        val pipelineInfos = if (idNumType) {
            // 纯数字按自增id获取
            pipelineListFacadeService.getByAutoIds(ids.map { it.toLong() })
        } else {
            // 非纯数字按pipelineId获取
            pipelineListFacadeService.getByPipelineIds(pipelineIds = ids.toSet())
        }
        val result = FetchInstanceInfo()

        if (pipelineInfos.isEmpty()) {
            logger.info("$ids does not match to the enable pipeline")
            return result.buildFetchInstanceFailResult()
        }

        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos.map {
            val entityId = if (returnPipelineId) {
                it.pipelineId
            } else {
                it.id?.toString() ?: "0"
            }
            val entity = InstanceInfoDTO()
            entity.id = entityId
            entity.displayName = it.pipelineName
            entity.iamApprover = arrayListOf(it.createUser)
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthPipelineService::class.java)
    }
}
