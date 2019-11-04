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
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.timestamp
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.ws.rs.BadRequestException

@Service
class ArtifactoryThirdPartyService @Autowired constructor(
    private val artifactoryService: ArtifactoryService,
    private val jFrogService: JFrogService
) {
    @Value("\${artifactory.thirdPartyUrl:#{null}}")
    private val artifactoryThirdPartyUrl: String? = null

    private val accessToken = "H9KSONm5DWdN2eSGhrXSE62PsjO9pG1l"

    fun createThirdPartyDownloadUrl(projectId: String, artifactoryType: ArtifactoryType, path: String): Url {
        val normalPath = JFrogUtil.normalize(path)
        if (!JFrogUtil.isValid(normalPath)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val realPath = JFrogUtil.getRealPath(projectId, artifactoryType, path)
        if (!jFrogService.exist(realPath)) {
            logger.error("Path $path is not exist")
            throw BadRequestException("文件不存在")
        }

        val relativePath = "$projectId/${path.removePrefix("/")}"
        val url = when (artifactoryType) {
            ArtifactoryType.PIPELINE -> "$artifactoryThirdPartyUrl/jfrog/storage/thirdparty/download/archive/$relativePath"
            ArtifactoryType.CUSTOM_DIR -> "$artifactoryThirdPartyUrl/jfrog/storage/thirdparty/download/custom/$relativePath"
        }

        val timestamp = LocalDateTime.now().timestamp()
        val sign = ShaUtils.sha1("access_token=$accessToken&path=$relativePath&timestamp=$timestamp".toByteArray())
        return Url("$url?timestamp=$timestamp&sign=$sign")
    }

    fun createThirdPartyUploadUrl(projectId: String, artifactoryType: ArtifactoryType, path: String): Url {
        val normalPath = JFrogUtil.normalize(path)
        if (!JFrogUtil.isValid(normalPath)) {
            logger.error("Path $path is not valid")
            throw BadRequestException("非法路径")
        }

        val relativePath = "$projectId/${path.removePrefix("/")}"
        val url = when (artifactoryType) {
            ArtifactoryType.PIPELINE -> "$artifactoryThirdPartyUrl/jfrog/storage/thirdparty/upload/archive/$relativePath"
            ArtifactoryType.CUSTOM_DIR -> "$artifactoryThirdPartyUrl/jfrog/storage/thirdparty/upload/custom/$relativePath"
        }

        val timestamp = LocalDateTime.now().timestamp()
        val sign = ShaUtils.sha1("access_token=$accessToken&path=$relativePath&timestamp=$timestamp".toByteArray())
        return Url("$url?timestamp=$timestamp&sign=$sign")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactoryType::class.java)
    }
}