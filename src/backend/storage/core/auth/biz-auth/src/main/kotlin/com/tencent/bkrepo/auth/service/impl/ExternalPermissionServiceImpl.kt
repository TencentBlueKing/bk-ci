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

package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TExternalPermission
import com.tencent.bkrepo.auth.pojo.externalPermission.CreateExtPermissionRequest
import com.tencent.bkrepo.auth.pojo.externalPermission.ExternalPermission
import com.tencent.bkrepo.auth.pojo.externalPermission.ListExtPermissionOption
import com.tencent.bkrepo.auth.pojo.externalPermission.UpdateExtPermissionRequest
import com.tencent.bkrepo.auth.repository.ExternalPermissionRepository
import com.tencent.bkrepo.auth.service.ExternalPermissionService
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.security.util.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExternalPermissionServiceImpl(
    private val externalPermissionRepository: ExternalPermissionRepository,
    private val mongoTemplate: MongoTemplate
): ExternalPermissionService {
    override fun createExtPermission(request: CreateExtPermissionRequest) {
        with(request) {
            val userId = SecurityUtils.getUserId()
            val tExternalPermission = TExternalPermission(
                url = url,
                headers = headers,
                projectId = projectId,
                repoName = repoName,
                scope = scope,
                platformWhiteList = platformWhiteList,
                enabled = enabled,
                createdDate = LocalDateTime.now(),
                createdBy = userId,
                lastModifiedDate = LocalDateTime.now(),
                lastModifiedBy = userId
            )
            externalPermissionRepository.insert(tExternalPermission)
            logger.info("$userId create external permission[$tExternalPermission]")
        }
    }

    override fun updateExtPermission(request: UpdateExtPermissionRequest) {
        with(request) {
            val userId = SecurityUtils.getUserId()
            val tExternalPermission = externalPermissionRepository.findById(id)
                .orElseThrow { NotFoundException(AuthMessageCode.AUTH_EXT_PERMISSION_NOT_EXIST, id) }
            url?.let { tExternalPermission.url = it }
            headers?.let { tExternalPermission.headers = it }
            projectId?.let { tExternalPermission.projectId = it }
            repoName?.let { tExternalPermission.repoName = it }
            scope?.let { tExternalPermission.scope = it }
            platformWhiteList?.let { tExternalPermission.platformWhiteList = it }
            enabled?.let { tExternalPermission.enabled = it }
            tExternalPermission.lastModifiedDate = LocalDateTime.now()
            tExternalPermission.lastModifiedBy = userId
            externalPermissionRepository.save(tExternalPermission)
            logger.info("$userId update external permission[$tExternalPermission]")
        }
    }

    override fun listExtPermission(): List<ExternalPermission> {
        return externalPermissionRepository.findAll().map { convert(it) }
    }

    override fun listExtPermissionPage(listExtPermissionOption: ListExtPermissionOption): Page<ExternalPermission> {
        with(listExtPermissionOption) {
            val query = Query()
            url?.let { query.addCriteria(where(TExternalPermission::url).regex(url!!)) }
            projectId?.let { query.addCriteria(where(TExternalPermission::projectId).isEqualTo(projectId)) }
            repoName?.let { query.addCriteria(where(TExternalPermission::repoName).isEqualTo(repoName)) }
            scope?.let { query.addCriteria(where(TExternalPermission::scope).isEqualTo(scope)) }
            enabled?.let { query.addCriteria(where(TExternalPermission::enabled).isEqualTo(enabled)) }
            val total = mongoTemplate.count(query, TExternalPermission::class.java)
            val pageRequest = Pages.ofRequest(pageNumber, pageSize)
            val records =
                mongoTemplate.find(query.with(pageRequest), TExternalPermission::class.java).map { convert(it) }
            return Pages.ofResponse(pageRequest, total, records)
        }
    }

    override fun deleteExtPermission(id: String) {
        val userId = SecurityUtils.getUserId()
        externalPermissionRepository.findById(id)
            .orElseThrow { NotFoundException(AuthMessageCode.AUTH_EXT_PERMISSION_NOT_EXIST, id) }
        externalPermissionRepository.deleteById(id)
        logger.info("$userId delete external permission[$id]")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExternalPermissionServiceImpl::class.java)
        fun convert(tExternalPermission: TExternalPermission): ExternalPermission {
            with(tExternalPermission) {
                return ExternalPermission(
                    id = id!!,
                    url = url,
                    headers = headers,
                    projectId = projectId,
                    repoName = repoName,
                    scope = scope,
                    platformWhiteList = platformWhiteList,
                    enabled = enabled,
                    createdDate = createdDate,
                    createdBy = createdBy,
                    lastModifiedDate = lastModifiedDate,
                    lastModifiedBy = lastModifiedBy
                )
            }
        }
    }
}
