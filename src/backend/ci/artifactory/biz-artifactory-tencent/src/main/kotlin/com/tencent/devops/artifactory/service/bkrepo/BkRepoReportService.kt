package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.client.BkRepoClient
import com.tencent.devops.artifactory.service.ReportService
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.artifactory.util.RepoUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service
class BkRepoReportService @Autowired constructor(
    private val bkRepoClient: BkRepoClient
) : ReportService {
    override fun get(
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        path: String
    ): Response {
        logger.info("get, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, , " +
            "elementId: $elementId, path: $path")
        val normalizedPath = JFrogUtil.normalize(path)
        val realPath = "/$pipelineId/$buildId/$elementId/${normalizedPath.removePrefix("/")}"
        bkRepoClient.getFileDetail("", projectId, RepoUtils.REPORT_REPO, realPath)
            ?: throw NotFoundException("文件($path)不存在")
        logger.info("get file content, projectId: $projectId, repo: $RepoUtils.REPORT_REPO , realPath:$realPath")
        val response = bkRepoClient.getFileContent("", projectId, RepoUtils.REPORT_REPO, realPath)
        return Response.ok(response.first, MediaType.valueOf(response.second.toString())).build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}