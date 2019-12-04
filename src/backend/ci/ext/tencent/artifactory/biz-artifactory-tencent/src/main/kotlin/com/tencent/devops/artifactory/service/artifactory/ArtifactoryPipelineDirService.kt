package com.tencent.devops.artifactory.service.artifactory

import com.tencent.devops.artifactory.service.JFrogService
import com.tencent.devops.artifactory.pojo.FileChecksums
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.service.PipelineDirService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_NAME
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.BadRequestException

@Service
class ArtifactoryPipelineDirService @Autowired constructor(
    private val jFrogPropertiesApi: JFrogPropertiesApi,
    private val jFrogService: JFrogService,
    private val pipelineService: PipelineService
) : PipelineDirService {
    override fun list(userId: String, projectId: String, path: String): List<FileInfo> {
        return list(userId, projectId, path, AuthPermission.VIEW)
    }

    override fun list(userId: String, projectId: String, argPath: String, authPermission: AuthPermission): List<FileInfo> {
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getPipelinePath(projectId, path)
        val jFrogFileInfoList = jFrogService.list(realPath, false, 1)

        return when {
            pipelineService.isRootDir(path) -> {
                pipelineService.getRootPathFileList(userId, projectId, path, jFrogFileInfoList, authPermission)
            }
            pipelineService.isPipelineDir(path) -> {
                val pipelineId = pipelineService.getPipelineId(path)
                pipelineService.validatePermission(userId, projectId, pipelineId, authPermission, "用户($userId)在工程($projectId)下没有流水线${authPermission.alias}权限")
                pipelineService.getPipelinePathList(projectId, path, jFrogFileInfoList)
            }
            else -> {
                val pipelineId = pipelineService.getPipelineId(path)
                pipelineService.validatePermission(userId, projectId, pipelineId, authPermission, "用户($userId)在工程($projectId)下没有流水线${authPermission.alias}权限")
                pipelineService.getBuildPathList(projectId, path, jFrogFileInfoList)
            }
        }
    }

    override fun show(userId: String, projectId: String, argPath: String): FileDetail {
        val path = JFrogUtil.normalize(argPath)
        if (!JFrogUtil.isValid(path)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        // 项目目录不存在时，创建根目录
        val realPath = JFrogUtil.getPipelinePath(projectId, path)
        if (JFrogUtil.isRoot(path) && !jFrogService.exist(realPath)) {
            jFrogService.mkdir(realPath)
        }

        val jFrogFileInfo = jFrogService.file(realPath)
        val jFrogProperties = jFrogPropertiesApi.getProperties(realPath)
        val jFrogPropertiesMap = mutableMapOf<String, String>()
        jFrogProperties.map {
            jFrogPropertiesMap[it.key] = it.value.joinToString(",")
        }
        if (jFrogProperties.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            val pipelineId = jFrogProperties[ARCHIVE_PROPS_PIPELINE_ID]!!.first()
            val pipelineName = pipelineService.getPipelineName(projectId, pipelineId)
            jFrogPropertiesMap[ARCHIVE_PROPS_PIPELINE_NAME] = pipelineName
        }
        val checksums = jFrogFileInfo.checksums
        return if (checksums == null) {
            FileDetail(
                pipelineService.getDirectoryName(projectId, path),
                path,
                pipelineService.getFullName(projectId, path),
                path,
                jFrogFileInfo.size,
                LocalDateTime.parse(jFrogFileInfo.created, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                LocalDateTime.parse(jFrogFileInfo.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                FileChecksums("", "", ""),
                jFrogPropertiesMap
            )
        } else {
            FileDetail(
                pipelineService.getName(projectId, path),
                path,
                pipelineService.getFullName(projectId, path),
                path,
                jFrogFileInfo.size,
                LocalDateTime.parse(jFrogFileInfo.created, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                LocalDateTime.parse(jFrogFileInfo.lastModified, DateTimeFormatter.ISO_DATE_TIME).timestamp(),
                FileChecksums(
                    checksums.sha256,
                    checksums.sha1,
                    checksums.md5
                ),
                jFrogPropertiesMap
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryPipelineDirService::class.java)
    }
}