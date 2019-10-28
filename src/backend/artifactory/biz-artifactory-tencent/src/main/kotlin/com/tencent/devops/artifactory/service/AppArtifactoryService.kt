package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.experience.api.service.ServiceExperienceResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.BadRequestException
import javax.ws.rs.core.Response

@Service
class AppArtifactoryService @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
    private val client: Client,
    private val jFrogApiService: JFrogApiService,
    private val pipelineService: PipelineService,
    private val jFrogPropertiesApi: JFrogPropertiesApi
) {
    fun getExternalDownloadUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, directed: Boolean = false): Url {
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val properties = jFrogPropertiesApi.getProperties(realPath)
        if (!properties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "元数据(pipelineId)不存在，请通过共享下载文件")
        }
        val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
        pipelineService.validatePermission(userId, projectId, pipelineId, AuthPermission.DOWNLOAD, "用户($userId)在工程($projectId)下没有流水线${pipelineId}下载构建权限")

        val url = jFrogApiService.externalDownloadUrl(realPath, userId, ttl, directed)
        return Url(url)
    }

    fun getExternalDownloadUrlDirected(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int): Url {
        return getExternalDownloadUrl(userId, projectId, artifactoryType, argPath, ttl, true)
    }
    fun getExternalPlistDownloadUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, directed: Boolean = false): Url {
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        val properties = jFrogPropertiesApi.getProperties(realPath)
        if (!properties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "元数据(pipelineId)不存在，请通过共享下载文件")
        }
        val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
        pipelineService.validatePermission(userId, projectId, pipelineId, AuthPermission.DOWNLOAD, "用户($userId)在工程($projectId)下没有流水线${pipelineId}下载构建权限")

//        val url = jFrogApiService.externalDownloadUrl(realPath, userId, ttl, directed)
        val url = "${HomeHostUtil.outerApiServerHost()}/artifactory/api/app/artifactories/$projectId/$artifactoryType/filePlist?path=$argPath"
        return Url(url)
    }

    fun getPlistFile(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, directed: Boolean = false, experienceHashId: String?): String {

        val userName = if (experienceHashId != null) {
            val experience = client.get(ServiceExperienceResource::class).get(userId, projectId, experienceHashId)
            if (experience.isOk() && experience.data != null) {
                experience.data!!.creator
            } else {
                userId
            }
        } else {
            userId
        }

        val ipaExternalDownloadUrl = getExternalDownloadUrlDirected(userName, projectId, artifactoryType, argPath, ttl)
        val fileProperties = artifactoryService.getProperties(projectId, artifactoryType, argPath)
        var bundleIdentifier = ""
        var appTitle = ""
        var appVersion = ""
        fileProperties.forEach {
            when (it.key) {
                "bundleIdentifier" -> bundleIdentifier = it.value
                "appTitle" -> appTitle = it.value
                "appVersion" -> appVersion = it.value
                else -> null
            }
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
            "<plist version=\"1.0\">\n" +
            "<dict>\n" +
            "    <key>items</key>\n" +
            "    <array>\n" +
            "        <dict>\n" +
            "            <key>assets</key>\n" +
            "            <array>\n" +
            "                <dict>\n" +
            "                    <key>kind</key>\n" +
            "                    <string>software-package</string>\n" +
            "                    <key>url</key>\n" +
            "                    <string>${ipaExternalDownloadUrl.url.replace(" ","")}</string>\n" +
            "                </dict>\n" +
            "            </array>\n" +
            "            <key>metadata</key>\n" +
            "            <dict>\n" +
            "                <key>bundle-identifier</key>\n" +
            "                <string>$bundleIdentifier</string>\n" +
            "                <key>bundle-version</key>\n" +
            "                <string>$appVersion</string>\n" +
            "                <key>title</key>\n" +
            "                <string>$appTitle</string>\n" +
            "                <key>kind</key>\n" +
            "                <string>software</string>\n" +
            "            </dict>\n" +
            "        </dict>\n" +
            "    </array>\n" +
            "</dict>\n" +
            "</plist>"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryDownloadService::class.java)
    }
}