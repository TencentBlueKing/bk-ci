/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.artifactory.service.artifactory

import com.tencent.devops.artifactory.service.JFrogService
import com.tencent.devops.artifactory.service.ReportService
import com.tencent.devops.artifactory.util.JFrogUtil
import com.tencent.devops.common.archive.util.MimeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.ws.rs.NotFoundException

@Service
class ArtifactoryReportService @Autowired constructor(
    private val jFrogService: JFrogService
) : ReportService {
    override fun get(projectId: String, pipelineId: String, buildId: String, elementId: String, path: String) {
        logger.info("get, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, elementId: $elementId, path: $path")
        val normalizePath = JFrogUtil.normalize(path)
        val realPath = JFrogUtil.getReportPath(projectId, pipelineId, buildId, elementId, normalizePath)
        if (!jFrogService.exist(realPath)) {
            logger.error("文件($realPath)不存在")
            throw NotFoundException("文件($path)不存在")
        }

        val fileContent = jFrogService.get(realPath)
        val response = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).response!!
        response.contentType = MimeUtil.mediaType(path)
        FileCopyUtils.copy(fileContent.first.inputStream(), response.outputStream)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}