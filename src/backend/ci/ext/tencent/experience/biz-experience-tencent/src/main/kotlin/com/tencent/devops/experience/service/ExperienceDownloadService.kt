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
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.constant.GroupIdTypeEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceDownloadDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.dao.TokenDao
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceCount
import com.tencent.devops.experience.pojo.ExperienceUserCount
import com.tencent.devops.experience.pojo.download.CheckVersionParam
import com.tencent.devops.experience.pojo.download.CheckVersionVO
import com.tencent.devops.experience.util.DateUtil
import com.tencent.devops.experience.util.StringUtil
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExperienceDownloadService @Autowired constructor(
    private val dslContext: DSLContext,
    private val tokenDao: TokenDao,
    private val experienceDao: ExperienceDao,
    private val experienceDownloadDao: ExperienceDownloadDao,
    private val experiencePublicDao: ExperiencePublicDao,
    private val experienceBaseService: ExperienceBaseService,
    private val client: Client
) {
    fun checkVersion(userId: String, platform: Int, params: List<CheckVersionParam>): List<CheckVersionVO> {
        if (params.isEmpty()) {
            return emptyList()
        }

        val experienceRecordIds = experienceBaseService.getRecordIdsByUserId(userId, GroupIdTypeEnum.ALL)
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

    fun getExternalDownloadUrl(userId: String, experienceId: Long): DownloadUrl {
        val canExperience = experienceBaseService.userCanExperience(userId, experienceId)
        if (!canExperience) {
            throw ErrorCodeException(
                statusCode = 403,
                defaultMessage = "没有权限下载资源",
                errorCode = ExperienceMessageCode.USER_NEED_EXP_X_PERMISSION
            )
        }

        val experienceRecord = experienceDao.get(dslContext, experienceId)
        checkIfExpired(experienceRecord)

        val artifactoryType = ArtifactoryType.valueOf(experienceRecord.artifactoryType)
        if (!client.get(ServiceArtifactoryResource::class)
                .check(experienceRecord.projectId, artifactoryType, experienceRecord.artifactoryPath).data!!
        ) {
            throw ErrorCodeException(
                statusCode = 404,
                defaultMessage = "文件不存在",
                errorCode = ExperienceMessageCode.EXP_FILE_NOT_FOUND
            )
        }
        val experienceHashId = HashUtil.encodeLongId(experienceId)

        val projectId = experienceRecord.projectId
        val path = experienceRecord.artifactoryPath
        val platform = PlatformEnum.valueOf(experienceRecord.platform)
        val url = if (path.endsWith(".ipa", true)) {
            "${HomeHostUtil.outerApiServerHost()}/artifactory/api/app/artifactories/$projectId/$artifactoryType/filePlist?experienceHashId=$experienceHashId&path=$path"
        } else {
            client.get(ServiceArtifactoryResource::class)
                .externalUrl(projectId, artifactoryType, userId, path, 24 * 3600, false).data!!.url
        }
        val fileDetail = client.get(ServiceArtifactoryResource::class).show(projectId, artifactoryType, path).data!!

        addDownloadRecord(experienceId, userId)
        return DownloadUrl(StringUtil.chineseUrlEncode(url), platform, fileDetail.size)
    }

    fun getInnerDownloadUrl(userId: String, experienceId: Long): String {
        val canExperience = experienceBaseService.userCanExperience(userId, experienceId)
        if (!canExperience) {
            throw ErrorCodeException(
                statusCode = 403,
                defaultMessage = "没有权限下载资源",
                errorCode = ExperienceMessageCode.USER_NEED_EXP_X_PERMISSION
            )
        }

        val experienceRecord = experienceDao.get(dslContext, experienceId)
        val projectId = experienceRecord.projectId
        val path = experienceRecord.artifactoryPath

        checkIfExpired(experienceRecord)

        val artifactoryType = ArtifactoryType.valueOf(experienceRecord.artifactoryType)
        if (!client.get(ServiceArtifactoryResource::class).check(projectId, artifactoryType, path).data!!) {
            throw ErrorCodeException(
                statusCode = 404,
                defaultMessage = "文件不存在",
                errorCode = ExperienceMessageCode.EXP_FILE_NOT_FOUND
            )
        }

        addDownloadRecord(experienceId, userId)
        return client.get(ServiceArtifactoryResource::class)
            .downloadUrl(projectId, artifactoryType, userId, path, 24 * 3600, false).data!!.url
    }

    fun getQrCodeUrl(experienceHashId: String): String {
        val url =
            "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=experienceDetail&experienceId=$experienceHashId"
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

    fun addDownloadRecord(experienceId: Long, userId: String) {
        val experienceDownloadRecord = experienceDownloadDao.getOrNull(dslContext, experienceId, userId)
        if (experienceDownloadRecord == null) {
            experienceDownloadDao.create(dslContext, experienceId, userId)
        } else {
            experienceDownloadDao.plusTimes(dslContext, experienceDownloadRecord.id)
        }

        experiencePublicDao.addDownloadTimeByRecordId(dslContext, experienceId)
    }

    fun downloadCount(userId: String, projectId: String, experienceHashId: String): ExperienceCount {
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
        val downloadRecordList = experienceDownloadDao.list(dslContext, experienceId, finalOffset, finalLimit)

        val list = downloadRecordList.map {
            ExperienceUserCount(
                userId = it.userId,
                times = it.times,
                latestTime = it.updateTime.timestamp()
            )
        }
        return Pair(count, list)
    }
}
