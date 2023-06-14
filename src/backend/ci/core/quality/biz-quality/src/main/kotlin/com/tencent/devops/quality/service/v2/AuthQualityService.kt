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
 *
 */

package com.tencent.devops.quality.service.v2

import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.quality.dao.QualityNotifyGroupDao
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthQualityService @Autowired constructor(
    val qualityRuleDao: QualityRuleDao,
    val qualityGroupDao: QualityNotifyGroupDao,
    val dslContext: DSLContext,
    val authTokenApi: AuthTokenApi
) {

    fun getQualityRule(projectId: String, offset: Int, limit: Int, token: String): ListInstanceResponseDTO? {
        authTokenApi.checkToken(token)
        val qualityRuleInfos = qualityRuleDao.list(
            dslContext = dslContext,
            projectId = projectId,
            offset = offset,
            limit = limit)
        val result = ListInstanceInfo()
        if (qualityRuleInfos == null) {
            logger.info(projectId + "Quality rules under the project")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        qualityRuleInfos?.map {
            // iam内存的iam类型为加密后的，故在页面上选择到的也需加密
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        val count = qualityRuleDao.count(dslContext, projectId)
        logger.info("entityInfo $entityInfo, count $count")
        return result.buildListInstanceResult(entityInfo, count)
    }

    // 用于iam无权限跳转回调，id类型取决于页面能拿到的id，此处页面拿到的为加密id
    fun getQualityRuleInfoByIds(encodeIds: List<Any>?, token: String): FetchInstanceInfoResponseDTO? {
        val ids = encodeIds?.map { HashUtil.decodeIdToLong(it.toString()) }
        authTokenApi.checkToken(token)
        val qualityRuleInfos = qualityRuleDao.listByIds(
            dslContext = dslContext,
            ruleIds = ids!!.toSet() as Set<String>)
        val result = FetchInstanceInfo()
        if (qualityRuleInfos == null || qualityRuleInfos.isEmpty()) {
            logger.info("$ids not quality rule")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        qualityRuleInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entity.iamApprover = arrayListOf(it.createUser)
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${qualityRuleInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchQualityRule(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int,
        token: String
    ): SearchInstanceInfo {
        authTokenApi.checkToken(token)
        val qualityRuleInfos = qualityRuleDao.searchByIdLike(
            dslContext = dslContext,
            projectId = projectId,
            offset = offset,
            limit = limit,
            name = keyword)
        val result = SearchInstanceInfo()
        if (qualityRuleInfos == null) {
            logger.info(
                projectId + "There is no Quality user group under the project"
            )
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        qualityRuleInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        val count = qualityRuleDao.countByIdLike(
            dslContext = dslContext,
            projectId = projectId,
            name = keyword
        )
        logger.info("entityInfo $entityInfo, count $count")
        return result.buildSearchInstanceResult(entityInfo, count)
    }

    fun getQualityGroup(projectId: String, offset: Int, limit: Int, token: String): ListInstanceResponseDTO? {
        authTokenApi.checkToken(token)
        val qualityGroupInfos = qualityGroupDao.list(
            dslContext = dslContext,
            projectId = projectId,
            offset = offset,
            limit = limit)
        val result = ListInstanceInfo()
        if (qualityGroupInfos == null) {
            logger.info(projectId + "The project is grouped under the Quality")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        qualityGroupInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        val count = qualityGroupDao.count(dslContext, projectId)
        logger.info("entityInfo $entityInfo, count $count")
        return result.buildListInstanceResult(entityInfo, count)
    }

    // 用于iam无权限跳转回调，id类型取决于页面能拿到的id，此处页面拿到的为加密id
    fun getQualityGroupInfoByIds(encodeIds: List<Any>?, token: String): FetchInstanceInfoResponseDTO? {
        val ids = encodeIds?.map { HashUtil.decodeIdToLong(it.toString()) }
        authTokenApi.checkToken(token)
        val qualityGroupInfos = qualityGroupDao.list(
            dslContext = dslContext,
            groupIds = ids!!.map { it.toString().toLong() }
        )
        val result = FetchInstanceInfo()
        if (qualityGroupInfos == null || qualityGroupInfos.isEmpty()) {
            logger.info("$ids not quality rule")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        qualityGroupInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entity.iamApprover = arrayListOf(it.creator)
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${qualityGroupInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchQualityGroup(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int,
        token: String
    ): SearchInstanceInfo {
        authTokenApi.checkToken(token)
        val qualityGroupInfos = qualityGroupDao.searchByNameLike(
            dslContext = dslContext,
            projectId = projectId,
            offset = offset,
            limit = limit,
            name = keyword)
        val result = SearchInstanceInfo()
        if (qualityGroupInfos == null) {
            logger.info(
                projectId + "There is no Quality user group under the project"
            )
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        qualityGroupInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        val count = qualityGroupDao.countByIdLike(
            dslContext = dslContext,
            projectId = projectId,
            name = keyword
        )
        logger.info("entityInfo $entityInfo, count $count")
        return result.buildSearchInstanceResult(entityInfo, count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(AuthQualityService::class.java)
    }
}
