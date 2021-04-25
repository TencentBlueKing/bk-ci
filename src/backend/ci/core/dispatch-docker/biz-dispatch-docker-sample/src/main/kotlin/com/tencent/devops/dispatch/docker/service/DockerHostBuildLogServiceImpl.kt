package com.tencent.devops.dispatch.docker.service

import com.tencent.devops.dispatch.docker.pojo.FormatLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DockerHostBuildLogServiceImpl : DockerHostBuildLogService {

    private val logger = LoggerFactory.getLogger(DockerHostBuildLogServiceImpl::class.java)

    override fun sendFormatLog(formatLog: FormatLog): Boolean {
        logger.info("send formatLog: $formatLog")
        return true
    }
}
