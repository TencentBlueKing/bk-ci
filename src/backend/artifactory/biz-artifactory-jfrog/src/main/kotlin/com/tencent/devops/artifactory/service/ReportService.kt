package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.util.JFrogUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service
class ReportService @Autowired constructor(private val jFrogService: JFrogService) {
    fun get(projectId: String, pipelineId: String, buildId: String, elementId: String, path: String): Response {
        logger.info("report get ($projectId, $pipelineId, $buildId, $elementId, $path)")

        val normalizePath = JFrogUtil.normalize(path)
        val realPath = JFrogUtil.getReportPath(projectId, pipelineId, buildId, elementId, normalizePath)
        if (!jFrogService.exist(realPath)) {
            logger.error("文件($realPath)不存在")
            throw NotFoundException("文件($path)不存在")
        }

        val response = jFrogService.get(realPath)
        return Response.ok(response.first, MediaType.valueOf(response.second.toString())).build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}