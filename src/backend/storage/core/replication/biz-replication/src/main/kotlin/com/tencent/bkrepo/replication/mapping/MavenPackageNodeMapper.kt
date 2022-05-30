/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.replication.mapping

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MavenPackageNodeMapper(
    private val nodeClient: NodeClient
) : PackageNodeMapper {

    override fun type() = RepositoryType.MAVEN

    override fun map(
        packageSummary: PackageSummary,
        packageVersion: PackageVersion,
        type: RepositoryType
    ): List<String> {
        with(packageSummary) {
            val artifactPath = packageVersion.contentPath
            require(artifactPath != null) { "artifactPath for $key is null in [$projectId/$repoName]" }
            val path = artifactPath.substringBeforeLast('/')
            val listNodePage = nodeClient.listNode(projectId, repoName, path).data!!
            val fullPathList = listNodePage.map { it.fullPath }
            if (logger.isDebugEnabled) {
                logger.debug(
                    "artifact key [$key] corresponding to node fullPath $fullPathList, size: [${fullPathList.size}]."
                )
            }
            return fullPathList
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MavenPackageNodeMapper::class.java)
    }
}
