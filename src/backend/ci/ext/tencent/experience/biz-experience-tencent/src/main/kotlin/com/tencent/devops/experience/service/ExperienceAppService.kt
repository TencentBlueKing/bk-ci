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
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.VersionUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.experience.constant.ExperienceConstant
import com.tencent.devops.experience.constant.ProductCategoryEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceGroupDao
import com.tencent.devops.experience.dao.ExperienceGroupInnerDao
import com.tencent.devops.experience.dao.ExperienceInnerDao
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
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class ExperienceAppService(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val experienceDao: ExperienceDao,
    private val groupService: GroupService,
    private val experienceService: ExperienceService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val experienceGroupDao: ExperienceGroupDao,
    private val experienceGroupInnerDao: ExperienceGroupInnerDao,
    private val experienceInnerDao: ExperienceInnerDao,
    private val client: Client
) {

    private val executorService = Executors.newFixedThreadPool(2)

    fun list(
        userId: String,
        offset: Int,
        limit: Int,
        groupByBundleId: Boolean,
        platform: String? = null
    ): Pagination<AppExperience> {
        val expireTime = DateUtil.today()

        var recordIds = getRecordsByUserId(userId)

        if (groupByBundleId) {
            recordIds = experienceDao.listIdsGroupByBundleId(
                dslContext,
                recordIds,
                expireTime,
                true
            ).map { it.value1() }.toMutableSet()
        }

        val records = experienceDao.listByIds(dslContext, recordIds, platform, expireTime, true, offset, limit)

        val projectToIcon = syncAndGetIcon(records)

        val result = records.map {
            val projectId = it.projectId
            val logoUrl = UrlUtil.transformLogoAddr(projectToIcon[projectId]!!)
            AppExperience(
                experienceHashId = HashUtil.encodeLongId(it.id),
                platform = Platform.valueOf(it.platform),
                source = Source.valueOf(it.source),
                logoUrl = logoUrl,
                name = it.projectId,
                version = it.version,
                bundleIdentifier = it.bundleIdentifier
            )
        }

        val hasNext = if (result.size < limit) {
            false
        } else {
            experienceDao.countByIds(dslContext, recordIds, platform, expireTime, true) > offset + limit
        }

        return Pagination(hasNext, result)
    }

    private fun syncAndGetIcon(records: Result<TExperienceRecord>): MutableMap<String, String> {
        // 同步图片
        val projectToIcon = mutableMapOf<String, String>()
        val unSyncIconProjectIds = mutableSetOf<String>()

        records.forEach {
            if (StringUtils.isBlank(it.logoUrl)) {
                unSyncIconProjectIds.add(it.projectId)
            } else {
                projectToIcon[it.projectId] = it.logoUrl
            }
        }

        val projectList =
            client.get(ServiceProjectResource::class).listByProjectCode(unSyncIconProjectIds).data ?: listOf()
        projectList.forEach {
            projectToIcon[it.projectCode] = it.logoAddr ?: ""
        }
        projectToIcon.forEach {
            experienceDao.updateIconByProjectIds(dslContext, it.key, it.value)
        }
        return projectToIcon
    }

    private fun getRecordsByUserId(userId: String): MutableSet<Long> {
        val recordIds = mutableSetOf<Long>()
        // 把有自己的组的experience拿出来 && 把公开的experience拿出来
        val groupIds =
            experienceGroupInnerDao.listGroupIdsByUserId(dslContext, userId).map { it.value1() }.toMutableSet()
        groupIds.add(ExperienceConstant.PUBLIC_GROUP)
        recordIds.addAll(experienceGroupDao.listRecordIdByGroupIds(dslContext, groupIds).map { it.value1() }.toSet())
        // 把有自己的experience拿出来
        recordIds.addAll(experienceInnerDao.listRecordIdsByUserId(dslContext, userId).map { it.value1() }.toSet())
        return recordIds
    }

    fun detail(userId: String, experienceHashId: String, platform: Int, appVersion: String?): AppExperienceDetail {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        val experience = experienceDao.get(dslContext, experienceId)
        val projectId = experience.projectId
        val bundleIdentifier = experience.bundleIdentifier

        val isExpired = DateUtil.isExpired(experience.endDate)
        val canExperience = experienceService.userCanExperience(userId, experienceId)

        val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data
            ?: throw RuntimeException("ProjectId $projectId cannot find.")
        val logoUrl = UrlUtil.transformLogoAddr(projectInfo.logoAddr)
        val projectName = projectInfo.projectName
        val version = experience.version
        val shareUrl =
            "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=experienceDetail&experienceId=$experienceHashId"
        val path = experience.artifactoryPath
        val artifactoryType = ArtifactoryType.valueOf(experience.artifactoryType)
        val experienceName =
            if (StringUtils.isBlank(experience.experienceName)) experience.projectId else experience.experienceName
        val versionTitle =
            if (StringUtils.isBlank(experience.versionTitle)) experience.name else experience.versionTitle
        val categoryId = if (experience.category < 0) ProductCategoryEnum.LIFE.id else experience.category
        val publicExperience = experienceGroupDao.count(dslContext, experience.id, ExperienceConstant.PUBLIC_GROUP) > 0

        val changeLog = if (VersionUtil.compare(appVersion, "2.0.0") < 0) {
            getChangeLog(projectId, bundleIdentifier, PlatformEnum.of(platform)?.name, 1, 1000)
        } else {
            emptyList() // 新版本使用changeLog接口
        }

        // 同步文件大小到数据表
        syncExperienceSize(experience, projectId, artifactoryType, path)

        return AppExperienceDetail(
            experienceHashId = experienceHashId,
            size = experience.size,
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
            productOwner = objectMapper.readValue(experience.productOwner),
            createDate = experience.updateTime.timestamp(),
            endDate = experience.endDate.timestamp(),
            publicExperience = publicExperience
        )
    }

    fun changeLog(
        userId: String,
        experienceHashId: String,
        page: Int,
        pageSize: Int
    ): Pagination<ExperienceChangeLog> {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        val experience = experienceDao.get(dslContext, experienceId)
        val changeLog =
            getChangeLog(experience.projectId, experience.bundleIdentifier, experience.platform, page, pageSize)
        val hasNext = if (changeLog.size < pageSize) {
            false
        } else {
            experienceDao.countByBundleIdentifier(
                dslContext,
                experience.projectId,
                experience.bundleIdentifier,
                experience.platform
            ) < page * pageSize
        }

        return Pagination(hasNext, changeLog)
    }

    private fun getChangeLog(
        projectId: String,
        bundleIdentifier: String,
        platform: String?,
        page: Int,
        pageSize: Int
    ): List<ExperienceChangeLog> {
        val experienceList = experienceDao.listByBundleIdentifier(
            dslContext,
            projectId,
            bundleIdentifier,
            platform,
            (page - 1) * pageSize,
            pageSize
        )
        return experienceList.map {
            ExperienceChangeLog(
                experienceHashId = HashUtil.encodeLongId(it.id),
                version = it.version,
                creator = it.creator,
                createDate = it.createTime.timestamp(),
                changelog = it.remark ?: "",
                experienceName = it.experienceName,
                size = it.size
            )
        }
    }

    private fun syncExperienceSize(
        experience: TExperienceRecord,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String
    ) {
        if (experience.size == 0L) {
            executorService.submit {
                val fileDetail =
                    client.get(ServiceArtifactoryResource::class).show(projectId, artifactoryType, path).data
                if (null != fileDetail) {
                    experienceDao.updateSize(dslContext, experience.id, fileDetail.size)
                    experience.size = fileDetail.size
                }
            }
        }
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
