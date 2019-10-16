package com.tencent.devops.experience.service

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceDownloadDao
import com.tencent.devops.experience.dao.TokenDao
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceCount
import com.tencent.devops.experience.pojo.ExperienceUserCount
import com.tencent.devops.experience.pojo.enums.Platform
import com.tencent.devops.experience.util.DateUtil
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Service
class ExperienceDownloadService @Autowired constructor(
    private val dslContext: DSLContext,
    private val tokenDao: TokenDao,
    private val experienceDao: ExperienceDao,
    private val experienceDownloadDao: ExperienceDownloadDao,
    private val client: Client
) {
    fun getDownloadUrl(token: String): DownloadUrl {
        val tokenRecord = tokenDao.getOrNull(dslContext, token) ?: throw NotFoundException("token不存在")
        if (tokenRecord.expireTime.isBefore(LocalDateTime.now())) {
            throw PermissionForbiddenException("token已过期")
        }

        val experienceId = tokenRecord.experienceId
        val userId = tokenRecord.userId
        val experienceRecord = experienceDao.get(dslContext, experienceId)

        val isExpired = DateUtil.isExpired(experienceRecord.endDate)
        if (isExpired) {
            throw PermissionForbiddenException("体验已过期")
        }
        if (!experienceRecord.online) {
            throw PermissionForbiddenException("体验已下架")
        }

        return serviceGetExternalDownloadUrl(userId, experienceId)
    }

    fun serviceGetExternalDownloadUrl(userId: String, experienceId: Long): DownloadUrl {
        val experienceRecord = experienceDao.get(dslContext, experienceId)
        val projectId = experienceRecord.projectId
        val path = experienceRecord.artifactoryPath
        val platform = Platform.valueOf(experienceRecord.platform)

        val isExpired = DateUtil.isExpired(experienceRecord.endDate)
        if (isExpired) {
            throw PermissionForbiddenException("体验已过期")
        }
        if (!experienceRecord.online) {
            throw PermissionForbiddenException("体验已下架")
        }

        val artifactoryType = com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(experienceRecord.artifactoryType)
        if (!client.get(ServiceArtifactoryResource::class).check(projectId, artifactoryType, path).data!!) {
            throw NotFoundException("文件不存在")
        }
        val experienceHashId = HashUtil.encodeLongId(experienceId)
        val url = if (path.endsWith(".ipa", true)) {
            "${HomeHostUtil.outerApiServerHost()}/artifactory/api/app/artifactories/$projectId/$artifactoryType/filePlist?experienceHashId=$experienceHashId&path=$path"
        } else {
            client.get(ServiceArtifactoryResource::class).externalUrl(projectId, artifactoryType, userId, path, 24*3600, false).data!!.url
        }
        val fileDetail = client.get(ServiceArtifactoryResource::class).show(projectId, artifactoryType, path).data!!

        count(experienceId, userId)
        return DownloadUrl(url, platform, fileDetail.size)
    }

    fun serviceGetExternalPlistUrl(userId: String, experienceId: Long): DownloadUrl {
        val experienceRecord = experienceDao.get(dslContext, experienceId)
        val projectId = experienceRecord.projectId
        val path = experienceRecord.artifactoryPath
        val platform = Platform.valueOf(experienceRecord.platform)

        val isExpired = DateUtil.isExpired(experienceRecord.endDate)
        if (isExpired) {
            throw PermissionForbiddenException("体验已过期")
        }
        if (!experienceRecord.online) {
            throw PermissionForbiddenException("体验已下架")
        }

        val artifactoryType = com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(experienceRecord.artifactoryType)
        if (!client.get(ServiceArtifactoryResource::class).check(projectId, artifactoryType, path).data!!) {
            throw NotFoundException("文件不存在")
        }
        val experienceHashId = HashUtil.encodeLongId(experienceId)
        val url = "${HomeHostUtil.outerApiServerHost()}/artifactory/api/app/artifactories/$projectId/$artifactoryType/filePlist?experienceHashId=$experienceHashId&path=$path"
//        val url = client.get(ServiceArtifactoryResource::class).externalUrl(projectId, artifactoryType, userId, path, 24*3600, false).data!!.url
        val fileDetail = client.get(ServiceArtifactoryResource::class).show(projectId, artifactoryType, path).data!!

        count(experienceId, userId)
        return DownloadUrl(url, platform, fileDetail.size)
    }

    fun serviceGetInnerDownloadUrl(userId: String, experienceId: Long): String {
        val experienceRecord = experienceDao.get(dslContext, experienceId)
        val projectId = experienceRecord.projectId
        val path = experienceRecord.artifactoryPath

        val isExpired = DateUtil.isExpired(experienceRecord.endDate)
        if (isExpired) {
            throw PermissionForbiddenException("体验已过期")
        }
        if (!experienceRecord.online) {
            throw PermissionForbiddenException("体验已下架")
        }

        val artifactoryType = com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(experienceRecord.artifactoryType)
        if (!client.get(ServiceArtifactoryResource::class).check(projectId, artifactoryType, path).data!!) {
            throw NotFoundException("文件不存在")
        }

        count(experienceId, userId)
        return client.get(ServiceArtifactoryResource::class).downloadUrl(projectId, artifactoryType, userId, path, 24*3600, false).data!!.url
    }

    fun count(experienceId: Long, userId: String) {
        val experienceDownloadRecord = experienceDownloadDao.getOrNull(dslContext, experienceId, userId)
        if (experienceDownloadRecord == null) {
            experienceDownloadDao.create(dslContext, experienceId, userId)
        } else {
            experienceDownloadDao.plusTimes(dslContext, experienceDownloadRecord.id)
        }
    }

    fun downloadCount(userId: String, projectId: String, experienceHashId: String): ExperienceCount {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id

        val downloadTimes = experienceDownloadDao.sumTimes(dslContext, experienceId)
        val downloadUsers = experienceDownloadDao.count(dslContext, experienceId)
        return ExperienceCount(downloadUsers, downloadTimes)
    }

    fun downloadUserCount(userId: String, projectId: String, experienceHashId: String, offset: Int, limit: Int): Pair<Long, List<ExperienceUserCount>> {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id

        val count = experienceDownloadDao.count(dslContext, experienceId)
        val finalOffset = if (limit == -1) 0 else offset
        val finalLimit = if (limit == -1) count.toInt() else limit
        val downloadRecordList = experienceDownloadDao.list(dslContext, experienceId, finalOffset, finalLimit)

        val list = downloadRecordList.map {
            ExperienceUserCount(
                    it.userId,
                    it.times,
                    it.updateTime.timestamp()
            )
        }
        return Pair(count, list)
    }
}