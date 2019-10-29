package com.tencent.devops.dockerhost.service

import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.dockerhost.config.TXDockerHostConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files

@Component
class DockerHostWorkSpaceService(private val dockerHostConfig: TXDockerHostConfig) {

    private val logger = LoggerFactory.getLogger(DockerHostWorkSpaceService::class.java)

    fun createSymbolicLink(hostWorkspace: String): String {
        val hostWorkspaceFile = File(hostWorkspace)
        if (!hostWorkspaceFile.exists()) {
            hostWorkspaceFile.mkdirs() // 新建的流水线的工作空间路径为空则新建目录
        }
        val shaContent = ShaUtils.sha1(hostWorkspace.toByteArray())
        val linkFilePathDir = dockerHostConfig.hostPathLinkDir
        val linkFileDir = File(linkFilePathDir)
        if (!linkFileDir.exists()) {
            linkFileDir.mkdirs()
        }
        val linkPath = "$linkFilePathDir/$shaContent"
        logger.info("hostWorkspace:$hostWorkspace linkPath is: $linkPath")
        val link = FileSystems.getDefault().getPath(linkPath)
        if (!link.toFile().exists()) {
            val target = FileSystems.getDefault().getPath(hostWorkspace)
            Files.createSymbolicLink(link, target) // 为真实工作空间地址创建软链
        }
        return linkPath
    }
}
