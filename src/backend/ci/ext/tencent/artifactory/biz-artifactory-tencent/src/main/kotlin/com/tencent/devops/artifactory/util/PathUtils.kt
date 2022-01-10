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

package com.tencent.devops.artifactory.util

import com.tencent.devops.common.service.utils.HomeHostUtil
import java.net.URLEncoder
import java.nio.file.Paths
import javax.ws.rs.BadRequestException

object PathUtils {
    fun checkAndNormalizeAbsPath(path: String): String {
        val normalizePath = Paths.get(path).normalize().toString()
        if (!normalizePath.startsWith("/")) {
            throw BadRequestException("非法路径")
        }
        return normalizePath
    }

    fun normalize(relativePath: String): String {
        return Paths.get(relativePath).normalize().toString()
    }

    fun buildArchiveLink(projectId: String, pipelineId: String, buildId: String): String {
        return "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive" +
                "&projectId=$projectId&pipelineId=$pipelineId&buildId=$buildId"
    }

    fun buildDetailLink(projectId: String, artifactoryType: String, path: String): String {
        return "${HomeHostUtil.outerServerHost()}/share/artifactoryDetail/?flag=artifactoryDetail" +
                "&projectId=$projectId&artifactoryType=$artifactoryType" +
                "&x-devops-project-id=$projectId&artifactoryPath=${
                    URLEncoder.encode(
                        path,
                        "utf-8"
                    )
                }"
    }
}
