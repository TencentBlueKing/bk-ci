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

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.api.service.ServiceShortUrlResource
import com.tencent.devops.artifactory.pojo.CreateShortUrlRequest
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.util.UrlUtil
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.constant.GroupIdTypeEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceDownloadDao
import com.tencent.devops.experience.dao.ExperienceDownloadDetailDao
import com.tencent.devops.experience.dao.ExperienceLastDownloadDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.dao.TokenDao
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceCount
import com.tencent.devops.experience.pojo.ExperienceJumpInfo
import com.tencent.devops.experience.pojo.ExperienceUserCount
import com.tencent.devops.experience.pojo.download.CheckVersionParam
import com.tencent.devops.experience.pojo.download.CheckVersionVO
import com.tencent.devops.experience.pojo.download.DownloadRecordVO
import com.tencent.devops.experience.util.DateUtil
import com.tencent.devops.experience.util.StringUtil
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExperienceDownloadService @Autowired constructor(
    private val dslContext: DSLContext,
    private val tokenDao: TokenDao,
    private val experienceDao: ExperienceDao,
    private val experienceDownloadDao: ExperienceDownloadDao,
    private val experienceDownloadDetailDao: ExperienceDownloadDetailDao,
    private val experienceLastDownloadDao: ExperienceLastDownloadDao,
    private val experiencePublicDao: ExperiencePublicDao,
    private val experienceBaseService: ExperienceBaseService,
    private val experiencePushService: ExperiencePushService,
    private val client: Client
) {
    fun checkVersion(userId: String, platform: Int, params: List<CheckVersionParam>): List<CheckVersionVO> {
        val experienceRecordIds = if (params.isEmpty()) {
            mutableSetOf()
        } else experienceBaseService.getRecordIdsByUserId(
            userId,
            GroupIdTypeEnum.ALL
        )
        if (experienceRecordIds.isEmpty()) {
            return emptyList()
        }

        val updateRecords = experienceDao.listUpdates(
            dslContext = dslContext,
            recordIds = experienceRecordIds,
            platform = PlatformEnum.of(platform)?.name ?: "ANDROID",
            params = params
        )

        val updateMap = mutableMapOf<String, TExperienceRecord>()
        for (record in updateRecords) {
            if (updateMap.containsKey(record.bundleIdentifier)) {
                if (record.createTime.isAfter(updateMap[record.bundleIdentifier]!!.createTime)) {
                    updateMap[record.bundleIdentifier] = record
                }
            } else {
                updateMap[record.bundleIdentifier] = record
            }
        }

        return updateMap.values.map {
            CheckVersionVO(
                experienceHashId = HashUtil.encodeLongId(it.id),
                size = it.size,
                logoUrl = UrlUtil.toOuterPhotoAddr(it.logoUrl),
                experienceName = it.experienceName,
                createTime = it.createTime.timestampmilli(),
                bundleIdentifier = it.bundleIdentifier
            )
        }
    }

    fun getGatewayDownloadUrl(token: String): DownloadUrl {
        val tokenRecord = tokenDao.getOrNull(dslContext, token)
            ?: throw ErrorCodeException(
                statusCode = 404,
                defaultMessage = "token不存在",
                errorCode = ExperienceMessageCode.TOKEN_NOT_EXISTS
            )
        if (tokenRecord.expireTime.isBefore(LocalDateTime.now())) {
            throw ErrorCodeException(
                defaultMessage = "token已过期",
                errorCode = ExperienceMessageCode.TOKEN_EXPIRED
            )
        }

        val experienceId = tokenRecord.experienceId
        val userId = tokenRecord.userId
        val experienceRecord = experienceDao.get(dslContext, experienceId)

        checkIfExpired(experienceRecord)

        return getExternalDownloadUrl(userId, experienceId)
    }

    fun getExternalDownloadUrl(
        userId: String,
        experienceId: Long,
        isOuter: Boolean = false,
        ttl: Int? = null
    ): DownloadUrl {
        val canExperience = experienceBaseService.userCanExperience(userId, experienceId, isOuter)
        if (!canExperience) {
            throw ErrorCodeException(
                statusCode = 403,
                defaultMessage = "该版本不可用，可能已被下架、已过期或被新版本覆盖，请刷新页面重试",
                errorCode = ExperienceMessageCode.EXPERIENCE_NO_AVAILABLE
            )
        }

        val experienceRecord = experienceDao.get(dslContext, experienceId)
        checkIfExpired(experienceRecord)

        val artifactoryType = ArtifactoryType.valueOf(experienceRecord.artifactoryType)
        if (!client.get(ServiceArtifactoryResource::class)
                .check(
                    experienceRecord.creator,
                    experienceRecord.projectId,
                    artifactoryType,
                    experienceRecord.artifactoryPath
                ).data!!
        ) {
            throw ErrorCodeException(
                statusCode = 404,
                defaultMessage = "文件不存在",
                errorCode = ExperienceMessageCode.EXP_FILE_NOT_FOUND
            )
        }
        val experienceHashId = HashUtil.encodeLongId(experienceId)

        val projectId = experienceRecord.projectId
        val bundleIdentifier = experienceRecord.bundleIdentifier
        val path = experienceRecord.artifactoryPath
        val platform = PlatformEnum.valueOf(experienceRecord.platform)
        val url = if (path.endsWith(".ipa", true)) {
            val tail = ttl?.let { "&ttl=$ttl" } ?: ""
            "${HomeHostUtil.outerApiServerHost()}/artifactory/api/app/artifactories" +
                    "/$projectId/$artifactoryType/filePlist" +
                    "?experienceHashId=$experienceHashId&path=$path&x-devops-project-id=$projectId$tail"
        } else {
            client.get(ServiceArtifactoryResource::class)
                .externalUrl(
                    projectId = projectId,
                    artifactoryType = artifactoryType,
                    creatorId = experienceRecord.creator,
                    userId = userId,
                    path = path,
                    ttl = ttl ?: (24 * 3600),
                    directed = false
                ).data!!.url
        }
        val fileDetail = client.get(ServiceArtifactoryResource::class)
            .show(experienceRecord.creator, projectId, artifactoryType, path).data!!

        isNeedSubscribe(
            experienceId = experienceId,
            userId = userId,
            platform = platform,
            bundleIdentifier = bundleIdentifier,
            projectId = projectId
        )
        addDownloadRecord(experienceRecord, userId)
        return DownloadUrl(StringUtil.chineseUrlEncode(url), platform, fileDetail.size)
    }

    // 若为公开体验、用户第一次下载且未订阅过，则订阅
    private fun isNeedSubscribe(
        experienceId: Long,
        userId: String,
        platform: PlatformEnum,
        bundleIdentifier: String,
        projectId: String
    ) {
        val experienceHashId = HashUtil.encodeLongId(experienceId)
        val isPublicExperience = lazy {
            experienceBaseService.isPublicExperience(experienceId)
        }
        val isFirstDownload = lazy {
            experienceBaseService.isFirstDownload(
                platform = platform.name,
                bundleIdentifier = bundleIdentifier,
                projectId = projectId,
                userId = userId
            )
        }
        val isNotSubscribe = lazy {
            !experienceBaseService.isSubscribe(
                experienceId = experienceId,
                userId = userId,
                platform = platform.name,
                bundleIdentifier = bundleIdentifier,
                projectId = projectId
            )
        }
        if (isPublicExperience.value && isFirstDownload.value && isNotSubscribe.value) {
            val subscribe = experiencePushService.subscribe(
                userId = userId,
                experienceHashId = experienceHashId,
                platform = platform.id
            )
            logger.info("Subscribe Result: ${subscribe.message}")
        }
    }

    fun getInnerDownloadUrl(userId: String, experienceId: Long): String {
        val canExperience = experienceBaseService.userCanExperience(userId, experienceId)
        if (!canExperience) {
            throw ErrorCodeException(
                statusCode = 403,
                defaultMessage = "该版本不可用，可能已被下架、已过期或被新版本覆盖，请刷新页面重试",
                errorCode = ExperienceMessageCode.EXPERIENCE_NO_AVAILABLE
            )
        }

        val experienceRecord = experienceDao.get(dslContext, experienceId)
        val projectId = experienceRecord.projectId
        val path = experienceRecord.artifactoryPath

        checkIfExpired(experienceRecord)

        val artifactoryType = ArtifactoryType.valueOf(experienceRecord.artifactoryType)
        if (!client.get(ServiceArtifactoryResource::class)
                .check(experienceRecord.creator, projectId, artifactoryType, path).data!!
        ) {
            throw ErrorCodeException(
                statusCode = 404,
                defaultMessage = "文件不存在",
                errorCode = ExperienceMessageCode.EXP_FILE_NOT_FOUND
            )
        }

        addDownloadRecord(experienceRecord, userId)
        return client.get(ServiceArtifactoryResource::class)
            .downloadUrl(projectId, artifactoryType, experienceRecord.creator, path, 24 * 3600, false).data!!.url
    }

    fun getQrCodeUrl(experienceHashId: String): String {
        val url =
            "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html" +
                    "?flag=experienceDetail&experienceId=$experienceHashId"
        return client.get(ServiceShortUrlResource::class)
            .createShortUrl(CreateShortUrlRequest(url, 24 * 3600 * 3)).data!!
    }

    private fun checkIfExpired(experienceRecord: TExperienceRecord) {
        val isExpired = DateUtil.isExpired(experienceRecord.endDate)
        if (isExpired) {
            throw ErrorCodeException(
                defaultMessage = "体验已过期",
                errorCode = ExperienceMessageCode.EXP_EXPIRE
            )
        }
        if (!experienceRecord.online) {
            throw ErrorCodeException(
                defaultMessage = "体验已下架",
                errorCode = ExperienceMessageCode.EXP_REMOVED
            )
        }
    }

    fun addDownloadRecord(experienceRecord: TExperienceRecord, userId: String) {
        try {
            // 新增下载次数
            addUserDownloadTime(experienceRecord, userId)
            experienceDownloadDetailDao.create(
                dslContext = dslContext,
                userId = userId,
                recordId = experienceRecord.id,
                projectId = experienceRecord.projectId,
                bundleIdentifier = experienceRecord.bundleIdentifier,
                platform = experienceRecord.platform
            )

            // 更新最近下载记录
            experienceLastDownloadDao.upset(
                dslContext = dslContext,
                userId = userId,
                bundleId = experienceRecord.bundleIdentifier,
                projectId = experienceRecord.projectId,
                platform = experienceRecord.platform,
                recordId = experienceRecord.id
            )
        } catch (e: Exception) {
            logger.warn("addDownloadRecord error", e)
        }
    }

    fun downloadCount(experienceHashId: String): ExperienceCount {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id

        val downloadTimes = experienceDownloadDao.sumTimes(dslContext, experienceId)
        val downloadUsers = experienceDownloadDao.count(dslContext, experienceId)
        return ExperienceCount(downloadUsers, downloadTimes)
    }

    fun downloadUserCount(
        userId: String,
        projectId: String,
        experienceHashId: String,
        offset: Int,
        limit: Int
    ): Pair<Long, List<ExperienceUserCount>> {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id

        val count = experienceDownloadDao.count(dslContext, experienceId)
        val finalOffset = if (limit == -1) 0 else offset
        val finalLimit = if (limit == -1) count.toInt() else limit
        val downloadRecordList = experienceDownloadDao.listByExperienceId(
            dslContext,
            experienceId,
            finalOffset,
            finalLimit
        )

        val list = downloadRecordList.map {
            ExperienceUserCount(
                userId = it.userId,
                times = it.times,
                latestTime = it.updateTime.timestamp()
            )
        }
        return Pair(count, list)
    }

    fun records(userId: String, platform: Int, page: Int, pageSize: Int): Pagination<DownloadRecordVO> {
        val isParamLegal = userId.isEmpty() || PlatformEnum.of(platform) == null || page < 1 || pageSize < 0
        if (isParamLegal) {
            logger.info("params is illegal , userId:$userId , platform:$platform , page:$page , pageSize:$pageSize")
            return Pagination(false, emptyList())
        }

        val experienceIdDownloadTimeMap =
            experienceDownloadDao.distinctExperienceIdByUserId(
                dslContext = dslContext,
                userId = userId,
                limit = 10000
            )?.map { it.value1() to it.value2() }?.toMap()
        return if (null == experienceIdDownloadTimeMap || experienceIdDownloadTimeMap.isEmpty()) {
            Pagination(false, emptyList())
        } else {
            val experienceIdsByBundleId = experienceDao.listIdsGroupByBundleId(
                dslContext = dslContext,
                ids = experienceIdDownloadTimeMap.keys,
                expireTime = LocalDateTime.now(),
                online = true
            ).map { it.value1() }.toSet()

            val lastDownloadMap = experienceBaseService.getLastDownloadMap(userId)

            val offset = (page - 1) * pageSize
            if (offset >= experienceIdsByBundleId.size) {
                Pagination(false, emptyList())
            } else {
                var experiences = experienceDao.listByIds(
                    dslContext = dslContext,
                    ids = experienceIdsByBundleId,
                    platform = PlatformEnum.of(platform)?.name,
                    expireTime = LocalDateTime.now(),
                    online = true,
                    offset = 0,
                    limit = experienceIdsByBundleId.size,
                    experienceName = null
                ).asSequence().map {
                    DownloadRecordVO(
                        experienceHashId = HashUtil.encodeLongId(it.id),
                        size = it.size,
                        logoUrl = UrlUtil.toOuterPhotoAddr(it.logoUrl),
                        experienceName = it.experienceName,
                        versionTitle = it.versionTitle,
                        createTime = it.createTime.timestampmilli(),
                        downloadTime = experienceIdDownloadTimeMap[it.id]?.timestampmilli() ?: 0,
                        bundleIdentifier = it.bundleIdentifier,
                        appScheme = it.scheme,
                        expired = false,
                        lastDownloadHashId = lastDownloadMap[it.projectId + it.bundleIdentifier + it.platform]
                            ?.let { l -> HashUtil.encodeLongId(l) } ?: ""
                    )
                }.sortedByDescending { it.downloadTime }.toList()

                experiences = experiences.subList(offset, (offset + pageSize).coerceAtMost(experiences.size))

                val hasNext = page * pageSize < experienceDao.countByIds(
                    dslContext = dslContext,
                    ids = experienceIdsByBundleId,
                    platform = PlatformEnum.of(platform)?.name,
                    expireTime = LocalDateTime.now(),
                    online = true
                )
                Pagination(hasNext, experiences)
            }
        }
    }

    fun jumpInfo(projectId: String, bundleIdentifier: String, platform: String): ExperienceJumpInfo {
        if (platform != "ANDROID" && platform != "IOS") {
            logger.warn("platform is illegal , {}", platform)
            throw ErrorCodeException(
                statusCode = 403,
                defaultMessage = "平台错误",
                errorCode = ExperienceMessageCode.EXPERIENCE_NO_AVAILABLE
            )
        }

        val experiencePublicRecord = experiencePublicDao.getByBundleId(
            dslContext = dslContext,
            projectId = projectId,
            bundleIdentifier = bundleIdentifier,
            platform = platform
        )

        if (null == experiencePublicRecord) {
            logger.warn(
                "can not found record , projectId:{} , bundleIdentifier:{} , platform:{}",
                projectId,
                bundleIdentifier,
                platform
            )
            throw ErrorCodeException(
                statusCode = 403,
                defaultMessage = "未找到对应的体验",
                errorCode = ExperienceMessageCode.EXPERIENCE_NO_AVAILABLE
            )
        }

        val scheme = if (platform == "ANDROID") {
            "bkdevopsapp://bkdevopsapp/app/experience/expDetail/" +
                    HashUtil.encodeLongId(experiencePublicRecord.recordId)
        } else {
            "bkdevopsapp://app/experience/expDetail/" +
                    HashUtil.encodeLongId(experiencePublicRecord.id)
        }

        val shortUrlRequest = CreateShortUrlRequest(
            getExternalDownloadUrl(
                "third_app",
                experiencePublicRecord.recordId,
                false,
                10 * 60
            ).url, 10 * 60 * 2
        )
        return ExperienceJumpInfo(
            scheme,
            client.get(ServiceShortUrlResource::class).createShortUrl(shortUrlRequest).data!!
        )
    }

    private fun addUserDownloadTime(
        experienceRecord: TExperienceRecord,
        userId: String
    ) {
        val experienceDownloadRecord = experienceDownloadDao.getOrNull(dslContext, experienceRecord.id, userId)
        if (experienceDownloadRecord == null) {
            experienceDownloadDao.create(dslContext, experienceRecord.id, userId)
        } else {
            experienceDownloadDao.plusTimes(dslContext, experienceDownloadRecord.id)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceDownloadService::class.java)
    }
}
