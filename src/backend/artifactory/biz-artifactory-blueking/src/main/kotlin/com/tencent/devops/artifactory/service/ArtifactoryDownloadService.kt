package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.dao.TokenDao
import com.tencent.devops.artifactory.pojo.DownloadUrl
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.pojo.enums.Platform
import com.tencent.devops.artifactory.service.pojo.FileShareInfo
import com.tencent.devops.artifactory.service.pojo.JFrogAQLFileInfo
import com.tencent.devops.artifactory.util.EmailUtil
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.shorturl.ShortUrlApi
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.project.api.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.regex.Pattern
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Response

@Service
class ArtifactoryDownloadService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val tokenDao: TokenDao,
    private val pipelineService: PipelineService,
    private val customDirService: CustomDirService,
    private val artifactoryService: ArtifactoryService,
    private val shortUrlApi: ShortUrlApi,
    private val jFrogService: JFrogService,
    private val jFrogApiService: JFrogApiService,
    private val jFrogAQLService: JFrogAQLService,
    private val jFrogPropertiesApi: JFrogPropertiesApi
) {
    private val regex = Pattern.compile(",|;")

    fun getDownloadUrl(token: String): DownloadUrl {
        val tokenRecord = tokenDao.getOrNull(dslContext, token) ?: throw NotFoundException("token不存在")
        if (tokenRecord.expireTime.isBefore(LocalDateTime.now())) {
            throw PermissionForbiddenException("token已过期")
        }

        val userId = tokenRecord.userId
        val projectId = tokenRecord.projectId
        val artifactoryType = ArtifactoryType.valueOf(tokenRecord.artifactoryType)
        val path = tokenRecord.path

        val url = serviceGetExternalDownloadUrl(userId, projectId, artifactoryType, path, 300).url
        val platform = if (path.endsWith(".ipa")) Platform.IOS else Platform.ANDROID

        return DownloadUrl(url, platform)
    }

    fun serviceGetExternalDownloadUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, directed: Boolean = false): Url {
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val url = jFrogApiService.externalDownloadUrl(realPath, userId, ttl, directed)
        return Url(url)
    }

    fun serviceGetInnerDownloadUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, directed: Boolean = false): Url {
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val url = jFrogApiService.internalDownloadUrl(realPath, ttl, userId)
        return Url(url)
    }

    fun getDownloadUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String): Url {
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        if (artifactoryType == ArtifactoryType.CUSTOM_DIR && path.startsWith("/share/")) {
            if (!customDirService.isProjectUser(userId, projectId)) {
                throw CustomException(Response.Status.BAD_REQUEST, "用户($userId)不是项目($projectId)成员")
            }
        } else {
            val properties = jFrogPropertiesApi.getProperties(realPath)
            if (!properties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
                logger.warn("[$userId|$projectId|$artifactoryType|$argPath] The properties($properties) doesn't contain '$ARCHIVE_PROPS_PIPELINE_ID'")
                throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "元数据(pipelineId)不存在，请通过共享下载文件")
            }
            val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
            pipelineService.validatePermission(userId, projectId, pipelineId, BkAuthPermission.DOWNLOAD, "用户($userId)在工程($projectId)下没有流水线${pipelineId}下载构建权限")
        }

        val url = jFrogApiService.downloadUrl(realPath)
        val url2 = url.replace("devgw.", "gw.")
        return Url(url, url2)
    }

    fun getIoaUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String): Url {
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        if (artifactoryType == ArtifactoryType.CUSTOM_DIR && path.startsWith("/share/")) {
            if (!customDirService.isProjectUser(userId, projectId)) {
                throw CustomException(Response.Status.BAD_REQUEST, "用户($userId)不是项目($projectId)成员")
            }
        } else {
            val properties = jFrogPropertiesApi.getProperties(realPath)
            if (!properties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
                throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "元数据(pipelineId)不存在，请通过共享下载文件")
            }
            val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
            pipelineService.validatePermission(userId, projectId, pipelineId, BkAuthPermission.DOWNLOAD, "用户($userId)在工程($projectId)下没有流水线${pipelineId}下载构建权限")
        }

        val url = jFrogApiService.ioaDownloadUrl(realPath)
        return Url(url)
    }

    fun getExternalUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String): Url {
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val properties = jFrogPropertiesApi.getProperties(realPath)
        if (!properties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            throw RuntimeException("元数据(pipelineId)不存在")
        }
        if (!properties.containsKey(ARCHIVE_PROPS_BUILD_ID)) {
            throw RuntimeException("元数据(buildId)不存在")
        }
        val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
        val buildId = properties[ARCHIVE_PROPS_BUILD_ID]!!.first()
        pipelineService.validatePermission(userId, projectId, pipelineId, BkAuthPermission.DOWNLOAD, "用户($userId)在工程($projectId)下没有流水线${pipelineId}下载构建权限")

        val url = "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive&projectId=$projectId&pipelineId=$pipelineId&buildId=$buildId"
        val shortUrl = shortUrlApi.getShortUrl(url, 300)
        return Url(shortUrl)
    }

    fun shareUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, downloadUsers: String) {
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        when (artifactoryType) {
            ArtifactoryType.PIPELINE -> {
                val pipelineId = pipelineService.getPipelineId(path)
                pipelineService.validatePermission(userId, projectId, pipelineId, BkAuthPermission.SHARE, "用户($userId)在工程($projectId)下没有流水线${pipelineId}分享权限")
            }
            ArtifactoryType.CUSTOM_DIR -> {
                customDirService.validatePermission(userId, projectId)
            }
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val downloadUrl = jFrogApiService.internalDownloadUrl(realPath, ttl, downloadUsers)
        val jFrogDetail = jFrogService.file(realPath)
        val fileName = JFrogUtil.getFileName(path)
        val projectName = client.get(ServiceProjectResource::class).get(projectId).data!!.projectName

        val days = ttl / (3600 * 24)
        val title = EmailUtil.getShareEmailTitle(userId, fileName, 1)
        val body = EmailUtil.getShareEmailBody(projectName, title, userId, days, listOf(FileShareInfo(fileName, jFrogDetail.checksums?.md5 ?: "", projectName, downloadUrl)))
        val receivers = downloadUsers.split(",").toSet()
        receivers.forEach {
            if (it.startsWith("g_")) throw BadRequestException("Invalid download users")
        }

        val emailNotifyMessage = makeEmailNotifyMessage(title, body, receivers)
        client.get(ServiceNotifyResource::class).sendEmailNotify(emailNotifyMessage)
    }

    fun getThirdPartyDownloadUrl(projectId: String, pipelineId: String, buildId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int?): List<String> {
        val pathArray = regex.split(argPath)

        val repoPathPrefix = JFrogUtil.getRepoPath()
        val jFrogFileInfoList = mutableListOf<JFrogAQLFileInfo>()

        pathArray.forEach { path ->
            val normalizedPath = JFrogUtil.normalize(path)
            val realPath = if (path.startsWith("/")) normalizedPath else "/$normalizedPath"

            val pathPrefix = if (artifactoryType == ArtifactoryType.PIPELINE) {
                "/" + JFrogUtil.getPipelinePathPrefix(projectId).removePrefix(repoPathPrefix) + "$pipelineId/$buildId/" + JFrogUtil.getParentFolder(realPath).removePrefix("/")
            } else {
                "/" + JFrogUtil.getCustomDirPathPrefix(projectId).removePrefix(repoPathPrefix) + JFrogUtil.getParentFolder(realPath).removePrefix("/")
            }
            val fileName = JFrogUtil.getFileName(path)

            val jFrogAQLFileInfoList = jFrogAQLService.searchFileByRegex(repoPathPrefix, setOf(pathPrefix), setOf(fileName))
            logger.info("Path($path) match file list: $jFrogFileInfoList")

            jFrogFileInfoList.addAll(jFrogAQLFileInfoList)
        }

        logger.info("Match file list: $jFrogFileInfoList")
        val fileInfoList = artifactoryService.transferJFrogAQLFileInfo(projectId, jFrogFileInfoList, emptyList(), false)
        logger.info("Transfer file list: $fileInfoList")

        val filePathList = fileInfoList.map {
            JFrogUtil.getRealPath(projectId, artifactoryType, it.fullPath)
        }
        return jFrogApiService.batchThirdPartyDownloadUrl(filePathList, ttl ?: 24*3600).map { it.value }
    }

    private fun makeEmailNotifyMessage(title: String, body: String, receivers: Set<String>): EmailNotifyMessage {
        val emailNotifyMessage = EmailNotifyMessage()
        emailNotifyMessage.addAllReceivers(receivers)
        emailNotifyMessage.title = title
        emailNotifyMessage.body = body
        emailNotifyMessage.format = EnumEmailFormat.HTML
        return emailNotifyMessage
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryDownloadService::class.java)
    }
}