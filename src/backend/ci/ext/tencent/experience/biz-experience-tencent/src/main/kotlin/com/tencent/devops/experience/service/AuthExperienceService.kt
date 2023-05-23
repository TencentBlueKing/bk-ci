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

package com.tencent.devops.experience.service

import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.GroupDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthExperienceService @Autowired constructor(
    val experienceDao: ExperienceDao,
    val experienceGroupDao: GroupDao,
    val dslContext: DSLContext,
    val authTokenApi: AuthTokenApi
) {
    fun getExperienceTask(
        projectId: String,
        offset: Int,
        limit: Int,
        token: String
    ): ListInstanceResponseDTO? {
        authTokenApi.checkToken(token)
        val experienceTaskInfos = experienceDao.search(
            dslContext = dslContext,
            projectId = projectId,
            offset = offset,
            limit = limit,
            name = null
        )
        val result = ListInstanceInfo()
        if (experienceTaskInfos == null) {
            logger.info("$projectId No experience under the project")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        // 因iam内存的id类型为encode，故此处需要返回加密id
        experienceTaskInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        val count = experienceDao.countByProject(dslContext, projectId)
        logger.info("entityInfo $entityInfo, count $count")
        return result.buildListInstanceResult(entityInfo, count)
    }

    // 此处为无权限跳转回调，id类型根据页面能获取的id为准。此处为hashId
    fun getExperienceTaskInfo(
        hashIds: List<Any>?,
        token: String
    ): FetchInstanceInfoResponseDTO? {
        val ids = hashIds?.map { HashUtil.decodeIdToLong(it.toString()) }
        authTokenApi.checkToken(token)
        val experienceTaskInfos = experienceDao.list(dslContext, ids!!.toSet())
        val result = FetchInstanceInfo()
        if (experienceTaskInfos == null || experienceTaskInfos.isEmpty()) {
            logger.info("$hashIds No Experience")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        experienceTaskInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${experienceTaskInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchExperienceTask(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int,
        token: String
    ): SearchInstanceInfo {
        authTokenApi.checkToken(token)
        val experienceTaskInfos = experienceDao.search(
            dslContext = dslContext,
            projectId = projectId,
            offset = offset,
            limit = limit,
            name = keyword
        )
        val result = SearchInstanceInfo()
        if (experienceTaskInfos == null) {
            logger.info("$projectId No experience under the project")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        experienceTaskInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        val count = experienceDao.countByProject(dslContext, projectId)
        logger.info("entityInfo $entityInfo, count $count")
        return result.buildSearchInstanceResult(entityInfo, count)
    }

    fun getExperienceGroup(
        projectId: String,
        offset: Int,
        limit: Int,
        token: String
    ): ListInstanceResponseDTO? {
        authTokenApi.checkToken(token)
        val experienceGroupInfos = experienceGroupDao.list(
            dslContext = dslContext,
            projectId = projectId,
            offset = offset,
            limit = limit
        )
        val result = ListInstanceInfo()
        if (experienceGroupInfos == null) {
            logger.info("$projectId No experience group under the project")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        experienceGroupInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        val count = experienceGroupDao.count(dslContext, projectId)
        logger.info("entityInfo $entityInfo, count $count")
        return result.buildListInstanceResult(entityInfo, count)
    }

    // 此处为无权限跳转回调，id类型根据页面能获取的id为准。此处为hashId
    fun getExperienceGroupInfo(
        hashIds: List<Any>?,
        token: String
    ): FetchInstanceInfoResponseDTO? {
        val ids = hashIds?.map { HashUtil.decodeIdToLong(it.toString()) }
        authTokenApi.checkToken(token)
        val experienceGroupInfos = experienceGroupDao.list(dslContext, ids!!.toSet())
        val result = FetchInstanceInfo()
        if (experienceGroupInfos == null || experienceGroupInfos.isEmpty()) {
            logger.info("$ids Inexperienced user group")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        experienceGroupInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${experienceGroupInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchExperienceGroup(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int,
        token: String
    ): SearchInstanceInfo {
        authTokenApi.checkToken(token)
        val experienceGroupInfos = experienceGroupDao.search(
            dslContext = dslContext,
            projectId = projectId,
            offset = offset,
            limit = limit,
            name = keyword
        )
        val result = SearchInstanceInfo()
        if (experienceGroupInfos == null) {
            logger.info("$projectId No experience user group under the project")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        experienceGroupInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = HashUtil.encodeLongId(it.id)
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        val count = experienceGroupDao.count(dslContext, projectId)
        logger.info("entityInfo $entityInfo, count $count")
        return result.buildSearchInstanceResult(entityInfo, count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(AuthExperienceService::class.java)
    }
}
