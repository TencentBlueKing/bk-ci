/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.docker.service

import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.service.exception.RemoteErrorCodeException
import com.tencent.bkrepo.docker.artifact.DockerPackageRepo
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST
import com.tencent.bkrepo.docker.constant.REPO_TYPE
import com.tencent.bkrepo.docker.response.PackageManagerResponse
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class FixToolServiceImpl(
    private val packageRepo: DockerPackageRepo
) : FixToolService {

    @Autowired
    lateinit var repositoryClient: RepositoryClient

    @Autowired
    lateinit var nodeClient: NodeClient

    override fun fixPackageVersion(): List<PackageManagerResponse> {
        val packageManagerList = mutableListOf<PackageManagerResponse>()
        // 查找所有仓库
        logger.info("start add package  to historical data")
        val repositoryList = repositoryClient.pageByType(0, 1000, REPO_TYPE).data?.records ?: run {
            logger.warn("no docker repository found, return.")
            return emptyList()
        }
        logger.info("find [${repositoryList.size}] DOCKER repository ${repositoryList.map { it.projectId to it.name }}")
        repositoryList.forEach {
            val packageManagerResponse = addPackageManager(it.projectId, it.name)
            packageManagerList.add(packageManagerResponse.copy(projectId = it.projectId, repoName = it.name))
        }
        return packageManagerList
    }

    private fun addPackageManager(projectId: String, repoName: String): PackageManagerResponse {
        // 查询仓库下面的所有package的包
        var successCount = 0L
        var failedCount = 0L
        var totalCount = 0L
        val failedSet = mutableSetOf<String>()
        val startTime = LocalDateTime.now()

        // 分页查询文件节点，以package.json文件为后缀
        var page = 1
        val packageMetadataPage = queryPackagePage(projectId, repoName, page)
        var packageMetadataList = packageMetadataPage.records.map { resolveNode(it) }
        if (packageMetadataList.isEmpty()) {
            logger.info("no package found in repo [$projectId/$repoName], skip.")
            return PackageManagerResponse(totalCount = 0, successCount = 0, failedCount = 0, failedSet = emptySet())
        }
        while (packageMetadataList.isNotEmpty()) {
            packageMetadataList.forEach {
                logger.info(
                    "Retrieved ${packageMetadataList.size} records to add package manager, " +
                        "process: $totalCount/${packageMetadataPage.totalRecords}"
                )
                var name = ""
                try {
                    // 添加包管理
                    name = doAddPackageManager(it.createdBy, projectId, repoName, it)
                    logger.info("Success to add package manager for [$name] in repo [$projectId/$repoName].")
                    successCount += 1
                } catch (exception: RuntimeException) {
                    logger.error("fail to add package manager for [$name] in repo [$projectId/$repoName].", exception)
                    failedSet.add(name)
                    failedCount += 1
                } finally {
                    totalCount += 1
                }
            }
            page += 1
            packageMetadataList = queryPackagePage(projectId, repoName, page).records.map { resolveNode(it) }
        }
        val durationSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
        logger.info(
            "Repair docker package metadata file in repo [$projectId/$repoName], total: $totalCount, " +
                "success: $successCount, failed: $failedCount, duration $durationSeconds s totally."
        )
        return PackageManagerResponse(
            totalCount = totalCount,
            successCount = successCount,
            failedCount = failedCount,
            failedSet = failedSet
        )
    }

    private fun doAddPackageManager(
        userId: String,
        projectId: String,
        repoName: String,
        nodeInfo: NodeInfo
    ): String {
        val name = nodeInfo.path.trimStart().trimEnd().split("/")
        val artifactName = name[1]
        val tag = name[2]
        try {
            val request =
                PackageVersionCreateRequest(
                    projectId = projectId,
                    repoName = repoName,
                    packageName = artifactName,
                    packageKey = PackageKeys.ofDocker(artifactName),
                    packageType = PackageType.DOCKER,
                    packageDescription = null,
                    versionName = tag,
                    size = nodeInfo.size,
                    manifestPath = nodeInfo.fullPath,
                    artifactPath = null,
                    stageTag = null,
                    metadata = null,
                    overwrite = true,
                    createdBy = userId
                )
            packageRepo.createVersion(request)
        } catch (exception: RemoteErrorCodeException) {
            if (exception.errorMessage == CommonMessageCode.RESOURCE_EXISTED.getKey()) {
                logger.warn(
                    "the package manager for [$name] with version [$tag] is already exists " +
                        "in repo [$projectId/$repoName], skip."
                )
                return artifactName
            }
            logger.error(
                "add package manager for [$artifactName] with version [$tag] " +
                    "failed in repo [$projectId/$repoName]."
            )
            throw exception
        }
        logger.info("add package manager for package [$artifactName] success in repo [$projectId/$repoName]")
        return artifactName
    }

    private fun queryPackagePage(projectId: String, repoName: String, page: Int): Page<Map<String, Any?>> {
        val ruleList = mutableListOf<Rule>(
            Rule.QueryRule("projectId", projectId, OperationType.EQ),
            Rule.QueryRule("repoName", repoName, OperationType.EQ),
            Rule.QueryRule("name", DOCKER_MANIFEST, OperationType.EQ)
        )
        val queryModel = QueryModel(
            page = PageLimit(page, pageSize),
            sort = null,
            select = mutableListOf(),
            rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)
        )
        return nodeClient.search(queryModel).data!!
    }

    private fun resolveNode(record: Map<String, Any?>): NodeInfo {
        return NodeInfo(
            createdBy = record["createdBy"] as String,
            createdDate = record["createdDate"] as String,
            lastModifiedBy = record["lastModifiedBy"] as String,
            lastModifiedDate = record["lastModifiedDate"] as String,
            folder = record["folder"] as Boolean,
            path = record["path"] as String,
            name = record["name"] as String,
            fullPath = record["fullPath"] as String,
            size = record["size"].toString().toLong(),
            sha256 = record["sha256"] as String,
            md5 = record["md5"] as String,
            projectId = record["projectId"] as String,
            repoName = record["repoName"] as String,
            metadata = null
        )
    }

    companion object {
        private const val pageSize = 10000
        private val logger = LoggerFactory.getLogger(FixToolServiceImpl::class.java)
    }
}
