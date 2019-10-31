package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.BadRequestException
import javax.ws.rs.core.Response

@Service
class AppArtifactoryService @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
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

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryDownloadService::class.java)
    }
}