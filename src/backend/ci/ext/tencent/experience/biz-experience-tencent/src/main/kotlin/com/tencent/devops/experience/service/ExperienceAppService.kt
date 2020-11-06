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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.experience.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.code.BSExperienceAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.experience.constant.ProductCategoryEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.pojo.AppExperience
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.enums.Platform
import com.tencent.devops.experience.pojo.enums.Source
import com.tencent.devops.experience.util.DateUtil
import com.tencent.devops.experience.util.UrlUtil
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class ExperienceAppService(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val bsAuthProjectApi: BSAuthProjectApi,
    private val experienceDao: ExperienceDao,
    private val groupService: GroupService,
    private val experienceService: ExperienceService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val experienceServiceCode: BSExperienceAuthServiceCode,
    private val client: Client
) {

    fun list(userId: String, offset: Int, limit: Int): List<AppExperience> {
        val expireTime = DateUtil.today()

        val projectIdList = mutableListOf<String>()
        // 用户所在项目
        val userProjectIdList = bsAuthProjectApi.getUserProjects(experienceServiceCode, userId, null)
        projectIdList.addAll(userProjectIdList)
        // 用户所在的内部用户（inner user）体验的项目列表
        val innerUserProjectIdList =
            experienceDao.getProjectIdByInnerUser(dslContext, userId, expireTime, true)?.map { it.value1() }
        if (innerUserProjectIdList != null) {
            projectIdList.addAll(innerUserProjectIdList)
        }

        // 用户所在的体验组的项目列表
        val groupUserProjectIdList = experienceDao.getProjectIdByGroupUser(dslContext, userId)?.map { it.value1() }

        if (groupUserProjectIdList != null) {
            projectIdList.addAll(groupUserProjectIdList)
        }

        val experienceIdList = experienceDao.listIDGroupByProjectIdAndBundleIdentifier(
            dslContext,
            projectIdList.distinct().toSet(),
            expireTime,
            true
        )
        val experienceList = experienceDao.list(dslContext, experienceIdList.map { it.value1() }.toSet())

        val groupIdSet = mutableSetOf<String>()
        experienceList.forEach {
            val experienceGroups = objectMapper.readValue<Set<String>>(it.experienceGroups)
            groupIdSet.addAll(experienceGroups)
        }
        val groupMap = groupService.serviceGet(groupIdSet)

        val userCanExperienceList = mutableListOf<TExperienceRecord>()
        experienceList.forEach {
            val userSet = mutableSetOf<String>()
            val innerUsers = objectMapper.readValue<Set<String>>(it.innerUsers)
            val experienceGroups = objectMapper.readValue<Set<String>>(it.experienceGroups)
            userSet.addAll(innerUsers)
            experienceGroups.forEach {
                if (groupMap.containsKey(it)) {
                    userSet.addAll(groupMap[it]!!.innerUsers)
                }
            }

            if (userSet.contains(userId) || userId == it.creator) {
                userCanExperienceList.add(it)
            }
        }

        val subUserCanExperienceList = if (limit == -1) {
            userCanExperienceList
        } else {
            if (offset >= userCanExperienceList.size) {
                emptyList<TExperienceRecord>()
            } else {
                val toIndex =
                    if ((offset + limit) >= userCanExperienceList.size) userCanExperienceList.size else offset + limit
                userCanExperienceList.subList(offset, toIndex)
            }
        }

        if (subUserCanExperienceList.isEmpty()) {
            return emptyList()
        }
        val projectIds = subUserCanExperienceList.map { it.projectId }.toSet()
        val projectMap = mutableMapOf<String, ProjectVO>()
        val projectList = client.get(ServiceProjectResource::class).listByProjectCode(projectIds).data ?: listOf()
        projectList.forEach {
            projectMap[it.projectCode] = it
        }

        return subUserCanExperienceList.map {
            val projectId = it.projectId
            val logoUrl = UrlUtil.transformLogoAddr(projectMap[projectId]!!.logoAddr)
            val projectName = projectMap[projectId]!!.projectName
            AppExperience(
                experienceHashId = HashUtil.encodeLongId(it.id),
                platform = Platform.valueOf(it.platform),
                source = Source.valueOf(it.source),
                logoUrl = logoUrl,
                name = projectName,
                version = it.version,
                bundleIdentifier = it.bundleIdentifier
            )
        }
    }

    fun detail(userId: String, experienceHashId: String): AppExperienceDetail {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        val experience = experienceDao.get(dslContext, experienceId)
        val projectId = experience.projectId
        val bundleIdentifier = experience.bundleIdentifier

        val isExpired = DateUtil.isExpired(experience.endDate)
        val canExperience = experienceService.userCanExperience(userId, experienceId)

        val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data
            ?: throw RuntimeException("ProjectId $projectId cannot find.")
        val logoUrl = UrlUtil.transformLogoAddr(projectInfo.logoAddr)
        val projectName = projectInfo.projectName ?: ""
        val version = experience.version
        val shareUrl =
            "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=experienceDetail&experienceId=$experienceHashId"

        val path = experience.artifactoryPath
        val artifactoryType = ArtifactoryType.valueOf(experience.artifactoryType)
        val fileDetail = client.get(ServiceArtifactoryResource::class).show(projectId, artifactoryType, path).data!!

        val experienceList = experienceDao.listByBundleIdentifier(dslContext, projectId, bundleIdentifier)
        val changeLog = experienceList.map {
            ExperienceChangeLog(
                experienceHashId = HashUtil.encodeLongId(it.id),
                version = it.version,
                creator = it.creator,
                createDate = it.createTime.timestamp(),
                changelog = it.remark ?: ""
            )
        }

        val experienceName =
            if (StringUtils.isBlank(experience.experienceName)) experience.projectId else experience.experienceName
        val versionTitle =
            if (StringUtils.isBlank(experience.versionTitle)) experience.name else experience.versionTitle
        val categoryId = if (experience.category < 0) ProductCategoryEnum.LIFE.id else experience.category

        return AppExperienceDetail(
            experienceHashId = experienceHashId,
            size = fileDetail.size,
            logoUrl = logoUrl,
            shareUrl = shareUrl,
            name = projectName,
            platform = Platform.valueOf(experience.platform),
            version = version,
            expired = isExpired,
            canExperience = canExperience,
            online = experience.online,
            changeLog = changeLog,
            experienceName = experienceName,
            versionTitle = versionTitle,
            categoryId = categoryId,
            productOwner = experience.productOwner
        )
    }

    fun downloadUrl(userId: String, experienceHashId: String): DownloadUrl {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        return experienceDownloadService.serviceGetExternalDownloadUrl(userId, experienceId)
    }

    fun downloadPlistUrl(userId: String, experienceHashId: String): DownloadUrl {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        return experienceDownloadService.serviceGetExternalPlistUrl(userId, experienceId)
    }

    fun history(userId: String, projectId: String): List<AppExperienceSummary> {
        val expireTime = DateUtil.today()
        val experienceList = experienceDao.list(dslContext, projectId, null, null)

        val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data
            ?: throw RuntimeException("ProjectId $projectId cannot find.")
        val logoUrl = UrlUtil.transformLogoAddr(projectInfo.logoAddr)

        val groupIdSet = mutableSetOf<String>()
        experienceList.forEach {
            val experienceGroups = objectMapper.readValue<Set<String>>(it.experienceGroups)
            groupIdSet.addAll(experienceGroups)
        }
        val groupMap = groupService.serviceGet(groupIdSet)

        val appExperienceSummaryList = experienceList.map {
            val userSet = mutableSetOf<String>()
            val innerUsers = objectMapper.readValue<Set<String>>(it.innerUsers)
            val experienceGroups = objectMapper.readValue<Set<String>>(it.experienceGroups)
            userSet.addAll(innerUsers)
            experienceGroups.forEach {
                if (groupMap.containsKey(it)) {
                    userSet.addAll(groupMap[it]!!.innerUsers)
                }
            }
            val isExpired = DateUtil.isExpired(it.endDate, expireTime)
            val canExperience = userSet.contains(userId) || userId == it.creator

            AppExperienceSummary(
                experienceHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                platform = Platform.valueOf(it.platform),
                version = it.version,
                remark = it.remark ?: "",
                expireDate = it.endDate.timestamp(),
                source = Source.valueOf(it.source),
                logoUrl = logoUrl,
                creator = it.creator,
                expired = isExpired,
                canExperience = canExperience,
                online = it.online
            )
        }
        return appExperienceSummaryList.filter { appExperienceSummary ->
            appExperienceSummary.canExperience && !appExperienceSummary.expired
        }
    }
}
