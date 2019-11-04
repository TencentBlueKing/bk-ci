/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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