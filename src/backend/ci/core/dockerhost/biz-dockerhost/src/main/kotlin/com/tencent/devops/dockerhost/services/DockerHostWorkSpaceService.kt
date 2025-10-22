/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dockerhost.services

import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.dockerhost.config.DockerHostConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files

@Component
class DockerHostWorkSpaceService(private val dockerHostConfig: DockerHostConfig) {

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
