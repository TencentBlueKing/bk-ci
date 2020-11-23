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
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.experience.constant.ExperienceConstant
import com.tencent.devops.experience.constant.ProductCategoryEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceGroupDao
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
    private val experienceBaseService: ExperienceBaseService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val experienceGroupDao: ExperienceGroupDao,
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

        var recordIds = experienceBaseService.getRecordIdsByUserId(userId)

        if (groupByBundleId) {
            recordIds = experienceDao.listIdsGroupByBundleId(
                dslContext,
                recordIds,
                expireTime,
                true
            ).map { it.value1() }.toMutableSet()
        }

        val records = experienceDao.listByIds(dslContext, recordIds, platform, expireTime, true, offset, limit)

        // 同步图片
        syncIcon(records)

        val result = records.map {
            AppExperience(
                experienceHashId = HashUtil.encodeLongId(it.id),
                platform = Platform.valueOf(it.platform),
                source = Source.valueOf(it.source),
                logoUrl = it.logoUrl,
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

    private fun syncIcon(records: Result<TExperienceRecord>) {
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
        if (unSyncIconProjectIds.isNotEmpty()) {
            val projectList =
                client.get(ServiceProjectResource::class).listByProjectCode(unSyncIconProjectIds).data ?: listOf()
            projectList.forEach {
                projectToIcon[it.projectCode] = UrlUtil.transformLogoAddr(it.logoAddr)
            }
            unSyncIconProjectIds.forEach {
                experienceDao.updateIconByProjectIds(dslContext, it, projectToIcon[it] ?: "")
            }

            records.forEach {
                if (StringUtils.isBlank(it.logoUrl)) {
                    it.logoUrl = projectToIcon[it.projectId] ?: ""
                }
            }
        }
    }

    fun detail(userId: String, experienceHashId: String, platform: Int, appVersion: String?): AppExperienceDetail {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        val experience = experienceDao.get(dslContext, experienceId)
        val projectId = experience.projectId
        val bundleIdentifier = experience.bundleIdentifier

        val isExpired = DateUtil.isExpired(experience.endDate)
        val canExperience = experienceBaseService.userCanExperience(userId, experienceId)

        val logoUrl = UrlUtil.transformLogoAddr(experience.logoUrl)
        val projectName = experience.projectId
        val version = experience.version
        val shareUrl = experienceDownloadService.getQrCodeUrl(experienceHashId)
        val path = experience.artifactoryPath
        val artifactoryType = ArtifactoryType.valueOf(experience.artifactoryType)
        val experienceName =
            if (StringUtils.isBlank(experience.experienceName)) experience.projectId else experience.experienceName
        val versionTitle =
            if (StringUtils.isBlank(experience.versionTitle)) experience.name else experience.versionTitle
        val categoryId = if (experience.category < 0) ProductCategoryEnum.LIFE.id else experience.category
        val publicExperience = experienceGroupDao.count(dslContext, experience.id, ExperienceConstant.PUBLIC_GROUP) > 0

        val isOldVersion = VersionUtil.compare(appVersion, "2.0.0") < 0
        val changeLog = if (isOldVersion) {
            getChangeLog(projectId, bundleIdentifier, PlatformEnum.of(platform)?.name, 1, 1000, true)
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
            packageName = experience.name,
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
            createDate = experience.updateTime.let { if (isOldVersion) it.timestamp() else it.timestampmilli() },
            endDate = experience.endDate.let { if (isOldVersion) it.timestamp() else it.timestampmilli() },
            publicExperience = publicExperience,
            remark = experience.remark,
            bundleIdentifier = experience.bundleIdentifier
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
            getChangeLog(experience.projectId, experience.bundleIdentifier, experience.platform, page, pageSize, false)
        val hasNext = if (changeLog.size < pageSize) {
            false
        } else {
            experienceDao.countByBundleIdentifier(
                dslContext,
                experience.projectId,
                experience.bundleIdentifier,
                experience.platform
            ) > page * pageSize
        }

        return Pagination(hasNext, changeLog)
    }

    private fun getChangeLog(
        projectId: String,
        bundleIdentifier: String,
        platform: String?,
        page: Int,
        pageSize: Int,
        isOldVersion: Boolean
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
                createDate = it.createTime.run { if (isOldVersion) timestamp() else timestampmilli() },
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
        return experienceDownloadService.getExternalDownloadUrl(userId, experienceId)
    }

    fun history(userId: String, appVersion: String?, projectId: String): List<AppExperienceSummary> {
        val expireTime = DateUtil.today()
        val experienceList = experienceDao.list(dslContext, projectId, null, null)

        val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data
            ?: throw RuntimeException("ProjectId $projectId cannot find.")
        val logoUrl = UrlUtil.transformLogoAddr(projectInfo.logoAddr)

        val recordIds = experienceBaseService.getRecordIdsByUserId(userId)
        val isOldVersion = VersionUtil.compare(appVersion, "2.0.0") < 0

        val appExperienceSummaryList = experienceList.map {
            val isExpired = DateUtil.isExpired(it.endDate, expireTime)
            val canExperience = recordIds.contains(it.id) || it.creator == userId

            AppExperienceSummary(
                experienceHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                platform = Platform.valueOf(it.platform),
                version = it.version,
                remark = it.remark ?: "",
                expireDate = it.endDate.run { if (isOldVersion) timestamp() else timestampmilli() },
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
