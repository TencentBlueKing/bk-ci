package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.client.BkRepoClient
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.AppService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.service.artifactory.ArtifactoryDownloadService
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class BkRepoAppService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val bkRepoClient: BkRepoClient,
    private val bkRepoService: BkRepoService
) : AppService {
    override fun getExternalDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        directed: Boolean
    ): Url {
        logger.info("getExternalDownloadUrl, userId: $userId, projectId: $projectId, " +
            "artifactoryType: $artifactoryType, path: $path, ttl: $ttl, directed: $directed")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)

        val properties = bkRepoClient.listMetadata(userId, projectId, RepoUtils.getRepoByType(artifactoryType), normalizedPath)
        if (properties[ARCHIVE_PROPS_PIPELINE_ID].isNullOrBlank()) {
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "元数据(pipelineId)不存在，请通过共享下载文件")
        }
        val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]
        pipelineService.validatePermission(
            userId,
            projectId,
            pipelineId!!,
            AuthPermission.DOWNLOAD,
            "用户($userId)在工程($projectId)下没有流水线${pipelineId}下载构建权限"
        )

        val url = bkRepoService.externalDownloadUrl(
            userId,
            projectId,
            artifactoryType,
            normalizedPath,
            ttl,
            directed
        )
        return Url(url)
    }

    override fun getExternalDownloadUrlDirected(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int): Url {
        return getExternalDownloadUrl(userId, projectId, artifactoryType, argPath, ttl, true)
    }
    override fun getExternalPlistDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        directed: Boolean
    ): Url {
        logger.info("getExternalPlistDownloadUrl, userId: $userId, projectId: $projectId, " +
            "artifactoryType: $artifactoryType, path: $path, ttl: $ttl, directed: $directed")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)

        val properties = bkRepoClient.listMetadata(userId, projectId, RepoUtils.getRepoByType(artifactoryType), normalizedPath)
        if (properties[ARCHIVE_PROPS_PIPELINE_ID].isNullOrBlank()) {
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "元数据(pipelineId)不存在，请通过共享下载文件")
        }

        val pipelineId = properties[ARCHIVE_PROPS_PIPELINE_ID]
        pipelineService.validatePermission(
            userId,
            projectId,
            pipelineId!!,
            AuthPermission.DOWNLOAD,
            "用户($userId)在工程($projectId)下没有流水线${pipelineId}下载构建权限"
        )

        // todo
        throw OperationException("not implemented")
//        val url = "${HomeHostUtil.outerApiServerHost()}/artifactory/api/app/artifactories/$projectId/$artifactoryType/filePlist?path=$normalizedPath"
//        return Url(url)
    }

    override fun getPlistFile(userId: String, projectId: String, artifactoryType: ArtifactoryType, path: String, ttl: Int, directed: Boolean, experienceHashId: String?): String {
        // todo
        throw OperationException("not implemented")
//        val userName = if (experienceHashId != null) {
//            val experience = client.get(ServiceExperienceResource::class).get(userId, projectId, experienceHashId)
//            if (experience.isOk() && experience.data != null) {
//                experience.data!!.creator
//            } else {
//                userId
//            }
//        } else {
//            userId
//        }
//
//        val ipaExternalDownloadUrl = getExternalDownloadUrlDirected(userName, projectId, artifactoryType, path, ttl)
//
//        val fileProperties = bkRepoClient.listMetadata(userId, projectId, RepoUtils.getRepoByType(artifactoryType), path).map {
//            Property(it.key, it.value)
//        }
//        var bundleIdentifier = ""
//        var appTitle = ""
//        var appVersion = ""
//        fileProperties.forEach {
//            when (it.key) {
//                "bundleIdentifier" -> bundleIdentifier = it.value
//                "appTitle" -> appTitle = it.value
//                "appVersion" -> appVersion = it.value
//                else -> null
//            }
//        }
//        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//            "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
//            "<plist version=\"1.0\">\n" +
//            "<dict>\n" +
//            "    <key>items</key>\n" +
//            "    <array>\n" +
//            "        <dict>\n" +
//            "            <key>assets</key>\n" +
//            "            <array>\n" +
//            "                <dict>\n" +
//            "                    <key>kind</key>\n" +
//            "                    <string>software-package</string>\n" +
//            "                    <key>url</key>\n" +
//            "                    <string>${ipaExternalDownloadUrl.url.replace(" ","")}</string>\n" +
//            "                </dict>\n" +
//            "            </array>\n" +
//            "            <key>metadata</key>\n" +
//            "            <dict>\n" +
//            "                <key>bundle-identifier</key>\n" +
//            "                <string>$bundleIdentifier</string>\n" +
//            "                <key>bundle-version</key>\n" +
//            "                <string>$appVersion</string>\n" +
//            "                <key>title</key>\n" +
//            "                <string>$appTitle</string>\n" +
//            "                <key>kind</key>\n" +
//            "                <string>software</string>\n" +
//            "            </dict>\n" +
//            "        </dict>\n" +
//            "    </array>\n" +
//            "</dict>\n" +
//            "</plist>"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryDownloadService::class.java)
    }
}