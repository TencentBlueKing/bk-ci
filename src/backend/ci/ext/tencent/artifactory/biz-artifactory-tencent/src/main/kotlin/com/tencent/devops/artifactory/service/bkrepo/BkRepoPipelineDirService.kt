package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.client.BkRepoClient
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.service.PipelineDirService
import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.archive.pojo.JFrogFileInfo
import com.tencent.devops.common.auth.api.AuthPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class BkRepoPipelineDirService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val bkRepoClient: BkRepoClient
) : PipelineDirService {
    override fun list(userId: String, projectId: String, path: String): List<FileInfo> {
        return list(userId, projectId, path, AuthPermission.VIEW)
    }

    override fun list(userId: String, projectId: String, path: String, authPermission: AuthPermission): List<FileInfo> {
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val jFrogFileInfoList = bkRepoClient.listFile(userId, projectId, RepoUtils.PIPELINE_REPO, normalizedPath).map {
            JFrogFileInfo(
                uri = it.fullPath.removePrefix("/"),
                size = it.size,
                lastModified = it.lastModifiedDate,
                folder = it.folder
            )
        }

        return when {
            pipelineService.isRootDir(normalizedPath) -> {
                pipelineService.getRootPathFileList(userId, projectId, normalizedPath, jFrogFileInfoList, authPermission)
            }
            pipelineService.isPipelineDir(normalizedPath) -> {
                val pipelineId = pipelineService.getPipelineId(normalizedPath)
                pipelineService.validatePermission(userId, projectId, pipelineId, authPermission, "用户($userId)在工程($projectId)下没有流水线${authPermission.alias}权限")
                pipelineService.getPipelinePathList(projectId, normalizedPath, jFrogFileInfoList)
            }
            else -> {
                val pipelineId = pipelineService.getPipelineId(normalizedPath)
                pipelineService.validatePermission(userId, projectId, pipelineId, authPermission, "用户($userId)在工程($projectId)下没有流水线${authPermission.alias}权限")
                pipelineService.getBuildPathList(projectId, normalizedPath, jFrogFileInfoList)
            }
        }
    }

    override fun show(userId: String, projectId: String, path: String): FileDetail {
        logger.info("show, userId: $userId, projectId: $projectId, path: $path")
        val normalizedPath = PathUtils.checkAndNormalizeAbsPath(path)
        val fileDetail = bkRepoClient.getFileDetail(userId, projectId, RepoUtils.PIPELINE_REPO, normalizedPath)
            ?: throw NotFoundException("文件不存在")
        return RepoUtils.toFileDetail(fileDetail)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoPipelineDirService::class.java)
    }
}