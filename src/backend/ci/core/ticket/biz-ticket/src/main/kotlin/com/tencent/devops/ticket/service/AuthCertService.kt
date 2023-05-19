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

package com.tencent.devops.ticket.service

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
class AuthCertService @Autowired constructor(
    private val certService: CertService,
    private val authTokenApi: AuthTokenApi
) {

    fun getCert(projectId: String, offset: Int, limit: Int, token: String): ListInstanceResponseDTO? {
        authTokenApi.checkToken(token)
        val certInfos = certService.list(projectId, offset, limit)
        val result = ListInstanceInfo()
        if (certInfos?.records == null) {
            logger.info("$projectId no credential")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        certInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.certId
            entity.displayName = it.certId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${certInfos?.count}")
        return result.buildListInstanceResult(entityInfo, certInfos.count)
    }

    fun getCertInfo(ids: List<Any>?, token: String): FetchInstanceInfoResponseDTO? {
        authTokenApi.checkToken(token)
        val certInfos = certService.getCertByIds(ids!!.toSet() as Set<String>)
        val result = FetchInstanceInfo()
        if (certInfos == null || certInfos.isEmpty()) {
            logger.info("$ids no credential")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        certInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.certId
            entity.displayName = it.certId
            entity.iamApprover = arrayListOf(it.creator)
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${certInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchCert(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int,
        token: String
    ): SearchInstanceInfo {
        authTokenApi.checkToken(token)
        val certInfos = certService.searchByCertId(
            projectId = projectId,
            offset = offset,
            limit = limit,
            certId = keyword)
        val result = SearchInstanceInfo()
        if (certInfos?.records == null) {
            logger.info("$projectId no cert")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        certInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.certId
            entity.displayName = it.certId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${certInfos?.count}")
        return result.buildSearchInstanceResult(entityInfo, certInfos.count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(AuthCertService::class.java)
    }
}
