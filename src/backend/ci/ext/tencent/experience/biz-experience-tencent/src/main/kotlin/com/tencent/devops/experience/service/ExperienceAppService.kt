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

package com.tencent.devops.experience.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.Permission
import com.tencent.devops.artifactory.util.UrlUtil
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.VersionUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.experience.constant.ExperienceCode.BK_GRANT_EXPERIENCE_PERMISSION
import com.tencent.devops.experience.constant.ExperienceCode.BK_NO_PERMISSION_QUERY_EXPERIENCE
import com.tencent.devops.experience.constant.ExperienceConditionEnum
import com.tencent.devops.experience.constant.ExperienceConstant
import com.tencent.devops.experience.constant.ExperienceConstant.ORGANIZATION_OUTER
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.constant.GroupIdTypeEnum
import com.tencent.devops.experience.constant.ProductCategoryEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceDownloadDetailDao
import com.tencent.devops.experience.dao.ExperienceLastDownloadDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.dao.ExperiencePushSubscribeDao
import com.tencent.devops.experience.pojo.AppExperience
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceInstallPackage
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.enums.Source
import com.tencent.devops.experience.util.DateUtil
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL.timestamp
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.text.MessageFormat
import java.time.LocalDateTime
import java.util.concurrent.Executors
import javax.ws.rs.core.Response

@Service
@SuppressWarnings("LongParameterList", "MagicNumber", "TooGenericExceptionThrown")
class ExperienceAppService(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val experienceDao: ExperienceDao,
    private val experiencePublicDao: ExperiencePublicDao,
    private val experienceBaseService: ExperienceBaseService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val experienceLastDownloadDao: ExperienceLastDownloadDao,
    private val experienceDownloadDetailDao: ExperienceDownloadDetailDao,
    private val experiencePushSubscribeDao: ExperiencePushSubscribeDao,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val experienceService: ExperienceService
) {

    private val executorService = Executors.newFixedThreadPool(2)

    fun list(
        userId: String,
        offset: Int,
        limit: Int,
        groupByBundleId: Boolean,
        platform: Int? = null,
        organization: String? = null
    ): Pagination<AppExperience> {
        return experienceBaseService.list(
            userId = userId,
            offset = offset,
            limit = limit,
            groupByBundleId = groupByBundleId,
            platform = platform,
            isOuter = organization == ORGANIZATION_OUTER
        )
    }

    @SuppressWarnings("ComplexMethod", "LongMethod")
    fun detail(
        userId: String,
        experienceHashId: String,
        platform: Int,
        appVersion: String?,
        organization: String?,
        forceNew: Boolean
    ): AppExperienceDetail {
        var experienceId = HashUtil.decodeIdToLong(experienceHashId)
        var experience = experienceDao.get(dslContext, experienceId)
        val projectId = experience.projectId
        val bundleIdentifier = experience.bundleIdentifier
        val platform = experience.platform
        val newestPublic = experienceBaseService.getNewestPublic(projectId, bundleIdentifier, platform)
        val isOldVersion = VersionUtil.compare(appVersion, "2.0.0") < 0
        val isOuter = organization == ORGANIZATION_OUTER
        val isPublic = !isOuter && experienceBaseService.isPublic(experienceId, false)
        // 移除红点
        removeRedPoint(userId, experienceId)
        // 当APP前端传递的experienceId和公开体验的app被覆盖后T_EXPERIENCE_PUBLIC表中的RecordId不一致时，则将experienceId置为更新后的RecordId
        if (forceNew && newestPublic != null && newestPublic.recordId != experienceId) {
            experienceId = newestPublic.recordId
            experience = experienceDao.get(dslContext, experienceId)
        }
        val isInPrivate = experienceBaseService.isInPrivate(experienceId, userId, isOuter)
        // 新版本且没权限
        if (!isOldVersion && !isPublic && !isInPrivate) {
            throw ErrorCodeException(
                statusCode = 403,
                defaultMessage = MessageFormat.format(
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_GRANT_EXPERIENCE_PERMISSION,
                        language = I18nUtil.getLanguage(userId)
                    ), experience.creator
                ),
                errorCode = ExperienceMessageCode.EXPERIENCE_NEED_PERMISSION
            )
        }
        val isSubscribe = experienceBaseService.isSubscribe(experienceId, userId, platform, bundleIdentifier, projectId)
        val isExpired = DateUtil.isExpired(experience.endDate)
        val logoUrl = UrlUtil.toOuterPhotoAddr(experience.logoUrl)
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
        val isPrivate = experienceBaseService.isPrivate(experience, isOuter, userId)
        val experienceCondition = getExperienceCondition(isPublic, isPrivate, isInPrivate)
        val lastDownloadMap = experienceBaseService.getLastDownloadMap(userId)

        val changeLog = if (isOldVersion) {
            getChangeLog(
                userId = userId,
                projectId = projectId,
                bundleIdentifier = bundleIdentifier,
                platform = null,
                page = 1,
                pageSize = 1000,
                isOldVersion = true
            )
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
            platform = PlatformEnum.valueOf(platform),
            version = version,
            expired = isExpired,
            canExperience = isPublic || isInPrivate,
            online = experience.online,
            subscribe = isSubscribe,
            changeLog = changeLog,
            experienceName = experienceName,
            versionTitle = versionTitle,
            categoryId = categoryId,
            productOwner = objectMapper.readValue(experience.productOwner),
            createDate = experience.updateTime.let { if (isOldVersion) it.timestamp() else it.timestampmilli() },
            endDate = experience.endDate.let { if (isOldVersion) it.timestamp() else it.timestampmilli() },
            publicExperience = isPublic,
            remark = experience.remark,
            bundleIdentifier = experience.bundleIdentifier,
            experienceCondition = experienceCondition.id,
            appScheme = experience.scheme,
            lastDownloadHashId = lastDownloadMap[experience.projectId +
                    experience.bundleIdentifier +
                    experience.platform]
                ?.let { l -> HashUtil.encodeLongId(l) } ?: ""
        )
    }

    /**
     * 删除红点
     */
    private fun removeRedPoint(userId: String, experienceId: Long) {
        executorService.submit {
            redisOperation.sremove(ExperienceConstant.redPointKey(userId), experienceId.toString())
        }
    }

    private fun getExperienceCondition(
        isPublic: Boolean,
        isPrivate: Boolean,
        isInPrivate: Boolean
    ): ExperienceConditionEnum {
        return if (isPublic && !isPrivate) {
            ExperienceConditionEnum.JUST_PUBLIC
        } else if (!isPublic && isPrivate) {
            ExperienceConditionEnum.JUST_PRIVATE
        } else if (isInPrivate) {
            ExperienceConditionEnum.BOTH_WITH_PRIVATE
        } else {
            ExperienceConditionEnum.BOTH_WITHOUT_PRIVATE
        }
    }

    fun changeLog(
        userId: String,
        experienceHashId: String,
        page: Int,
        pageSize: Int,
        organization: String?,
        showAll: Boolean?
    ): Pagination<ExperienceChangeLog> {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        val experience = experienceDao.get(dslContext, experienceId)
        val changeLog =
            getChangeLog(
                userId = userId,
                projectId = experience.projectId,
                bundleIdentifier = experience.bundleIdentifier,
                platform = experience.platform,
                page = if (page <= 0) 1 else page,
                pageSize = if (pageSize <= 0) 10 else pageSize,
                isOldVersion = false,
                isOuter = organization == ORGANIZATION_OUTER,
                showAll = showAll ?: false
            )
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
        userId: String,
        projectId: String,
        bundleIdentifier: String,
        platform: String?,
        page: Int,
        pageSize: Int,
        isOldVersion: Boolean,
        isOuter: Boolean = false,
        showAll: Boolean = false
    ): List<ExperienceChangeLog> {
        val groupIdTypeEnum = if (showAll) GroupIdTypeEnum.ALL else GroupIdTypeEnum.JUST_PRIVATE
        val recordIds = experienceBaseService.getRecordIdsByUserId(userId, groupIdTypeEnum, isOuter)
        val now = LocalDateTime.now()
        val lastDownloadRecord = platform?.let {
            experienceLastDownloadDao.get(
                dslContext,
                userId = userId,
                bundleId = bundleIdentifier,
                projectId = projectId,
                platform = it
            )
        }

        val experienceList = experienceDao.listByBundleIdentifier(
            dslContext = dslContext,
            projectId = projectId,
            bundleIdentifier = bundleIdentifier,
            platform = platform,
            recordIds = recordIds,
            offset = (page - 1) * pageSize,
            limit = pageSize
        )

        return experienceList.map {
            ExperienceChangeLog(
                experienceHashId = HashUtil.encodeLongId(it.id),
                version = it.version,
                creator = it.creator,
                createDate = it.createTime.run { if (isOldVersion) timestamp() else timestampmilli() },
                changelog = it.remark ?: "",
                experienceName = it.experienceName,
                size = it.size,
                logoUrl = UrlUtil.toOuterPhotoAddr(it.logoUrl),
                bundleIdentifier = it.bundleIdentifier,
                appScheme = it.scheme,
                expired = now.isAfter(it.endDate),
                lastDownloadHashId = lastDownloadRecord?.let { last ->
                    HashUtil.encodeLongId(last.lastDonwloadRecordId)
                } ?: "",
                versionTitle = it.versionTitle
            )
        }.toList()
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
                    client.get(ServiceArtifactoryResource::class)
                        .show(experience.creator, projectId, artifactoryType, path).data
                if (null != fileDetail) {
                    experienceDao.updateSize(dslContext, experience.id, fileDetail.size)
                    experience.size = fileDetail.size
                }
            }
        }
    }

    fun downloadUrl(userId: String, experienceHashId: String, organization: String?): DownloadUrl {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        // 移除红点
        removeRedPoint(userId, experienceId)
        return experienceDownloadService.getExternalDownloadUrl(
            userId = userId,
            experienceId = experienceId,
            isOuter = organization == ORGANIZATION_OUTER
        )
    }

    fun history(userId: String, appVersion: String?, projectId: String): List<AppExperienceSummary> {
        val expireTime = DateUtil.today()
        val experienceList = experienceDao.list(dslContext, projectId, null, null)

        val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data
            ?: throw RuntimeException("ProjectId $projectId cannot find.")
        val logoUrl = UrlUtil.toOuterPhotoAddr(projectInfo.logoAddr)

        val recordIds = experienceBaseService.getRecordIdsByUserId(userId, GroupIdTypeEnum.ALL)
        val isOldVersion = VersionUtil.compare(appVersion, "2.0.0") < 0

        val appExperienceSummaryList = experienceList.map {
            val isExpired = DateUtil.isExpired(it.endDate, expireTime)
            val canExperience = recordIds.contains(it.id) || it.creator == userId

            AppExperienceSummary(
                experienceHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                platform = PlatformEnum.valueOf(it.platform),
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

    fun appStoreRedirect(id: String, userId: String): Response {
        val publicRecord = experiencePublicDao.getById(dslContext, HashUtil.decodeIdToLong(id))
            ?: return Response.status(Response.Status.NOT_FOUND).build()

        experienceDownloadDetailDao.create(
            dslContext = dslContext,
            userId = userId,
            recordId = publicRecord.recordId,
            projectId = publicRecord.projectId,
            bundleIdentifier = publicRecord.bundleIdentifier,
            platform = publicRecord.platform
        )

        return Response
            .temporaryRedirect(URI.create(publicRecord.externalLink))
            .build()
    }

    fun publicExperiences(userId: String, platform: Int, offset: Int, limit: Int): List<AppExperience> {
        val platformStr = PlatformEnum.of(platform)?.name
        val recordIds = mutableListOf<Long>()

        // 订阅的需要置顶
        val subscribeRecordIds = experiencePublicDao.listSubscribeRecordIds(dslContext, userId, platformStr, 100)
        recordIds.addAll(subscribeRecordIds)

        // 普通的公开体验
        val normalRecords = experienceDownloadDetailDao.listIdsForPublic(
            dslContext, userId, platformStr, 100
        ).filterNot { recordIds.contains(it) }
        recordIds.addAll(normalRecords)

        // 过滤内部体验ID
        val privateRecordIds = experienceBaseService.getRecordIdsByUserId(userId, GroupIdTypeEnum.JUST_PRIVATE, false)
        recordIds.removeAll(privateRecordIds)

        // 找到结果
        val recordMap = experienceDao.listOnline(dslContext, recordIds).map { it.id to it }.toMap()

        // 排序
        val records = mutableListOf<TExperienceRecord>()
        for (recordId in recordIds) {
            recordMap[recordId]?.let { records.add(it) }
        }

        return experienceBaseService.toAppExperiences(userId, records)
    }

    fun installPackages(
        userId: String,
        platform: Int,
        appVersion: String?,
        organization: String?,
        experienceHashId: String
    ): Pagination<AppExperienceInstallPackage> {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        if (!experienceBaseService.userCanExperience(userId, experienceId, organization == ORGANIZATION_OUTER)) {
            throw ErrorCodeException(
                statusCode = 403,
                defaultMessage = MessageUtil.getMessageByLocale(
                    messageCode = BK_NO_PERMISSION_QUERY_EXPERIENCE,
                    language = I18nUtil.getLanguage(userId)
                ),
                errorCode = ExperienceMessageCode.EXPERIENCE_NEED_PERMISSION
            )
        }
        val experience = experienceDao.get(dslContext, experienceId)
        val projectId = experience.projectId
        val artifactoryPath = experience.artifactoryPath
        val artifactoryType =
            com.tencent.devops.experience.pojo.enums.ArtifactoryType.valueOf(experience.artifactoryType)
        val detailPermission = try {
            experienceService.hasArtifactoryPermission(
                userId = userId,
                projectId = projectId,
                path = artifactoryPath,
                artifactoryType = artifactoryType,
                permission = Permission.VIEW
            )
        } catch (e: Exception) {
            logger.warn("get permission failed!", e)
            false
        }
        return Pagination(
            false,
            listOf(
                AppExperienceInstallPackage(
                    name = experience.name,
                    projectId = projectId,
                    path = artifactoryPath,
                    artifactoryType = artifactoryType.name,
                    detailPermission = detailPermission,
                    size = experience.size
                )
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceAppService::class.java)
    }
}
