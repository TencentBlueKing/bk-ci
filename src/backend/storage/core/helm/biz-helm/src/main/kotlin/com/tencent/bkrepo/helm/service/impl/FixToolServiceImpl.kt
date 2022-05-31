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

package com.tencent.bkrepo.helm.service.impl

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.readYamlString
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.exception.RemoteErrorCodeException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.helm.constants.CHART_PACKAGE_FILE_EXTENSION
import com.tencent.bkrepo.helm.constants.FULL_PATH
import com.tencent.bkrepo.helm.constants.NODE_FULL_PATH
import com.tencent.bkrepo.helm.constants.SIZE
import com.tencent.bkrepo.helm.constants.SLEEP_MILLIS
import com.tencent.bkrepo.helm.exception.HelmBadRequestException
import com.tencent.bkrepo.helm.exception.HelmFileNotFoundException
import com.tencent.bkrepo.helm.pojo.artifact.HelmArtifactInfo
import com.tencent.bkrepo.helm.pojo.fixtool.DateTimeRepairResponse
import com.tencent.bkrepo.helm.pojo.fixtool.PackageManagerResponse
import com.tencent.bkrepo.helm.pojo.fixtool.RepairResponse
import com.tencent.bkrepo.helm.pojo.metadata.HelmChartMetadata
import com.tencent.bkrepo.helm.pojo.metadata.HelmIndexYamlMetadata
import com.tencent.bkrepo.helm.service.FixToolService
import com.tencent.bkrepo.helm.utils.DecompressUtil.getArchivesContent
import com.tencent.bkrepo.helm.utils.HelmMetadataUtils
import com.tencent.bkrepo.helm.utils.HelmUtils
import com.tencent.bkrepo.helm.utils.TimeFormatUtil
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.packages.request.PopulatedPackageVersion
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.SortedSet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FixToolServiceImpl(
    private val storageService: StorageService
) : FixToolService, AbstractChartService() {

    override fun repairPackageCreatedDate(): List<DateTimeRepairResponse> {
        val repairResponse = mutableListOf<DateTimeRepairResponse>()
        val successRepoName: MutableList<RepairResponse> = mutableListOf()
        val failedRepoName: MutableList<RepairResponse> = mutableListOf()
        logger.info("starting repair package created date for historical data")
        val repositoryList = repositoryClient.pageByType(0, 1000, "HELM").data?.records ?: run {
            logger.warn("no helm repository found, return.")
            emptyList<RepositoryDetail>()
        }
        val helmLocalRepositoryList = repositoryList.filter { it.category == RepositoryCategory.LOCAL }.toList()
        logger.info(
            "find [${helmLocalRepositoryList.size}] HELM local " +
                "repository ${helmLocalRepositoryList.map { it.projectId to it.name }}"
        )
        helmLocalRepositoryList.forEach {
            try {
                doRepairCreatedDate(it.projectId, it.name)
                successRepoName.add(RepairResponse(it.projectId, it.name))
            } catch (exception: RuntimeException) {
                successRepoName.add(RepairResponse(it.projectId, it.name))
            }
        }
        repairResponse.add(DateTimeRepairResponse(successRepoName, failedRepoName))
        return repairResponse
    }

    private fun doRepairCreatedDate(projectId: String, repoName: String) {
        val request = HttpContextHolder.getRequest()
        request.setAttribute(ARTIFACT_INFO_KEY, ArtifactInfo(projectId, repoName, ""))
        try {
            // 查询索引文件
            val helmIndexYamlMetadata = helmIndexYamlMetadata(projectId, repoName)
            if (helmIndexYamlMetadata.entries.isEmpty()) {
                logger.error("not found entries in index-cache.yaml")
                return
            }
            val helmNodeList = mutableListOf<NodeInfo>()
            val entries = helmIndexYamlMetadata.entries
            entries.keys.forEach { name ->
                helmNodeList.clear()
                var page = 1
                var packageMetadataList =
                    queryPackagePage(projectId, repoName, page, name).records.map { resolveNode(it) }
                if (packageMetadataList.isEmpty()) {
                    logger.info("no package with name [$name] found in repo [$projectId/$repoName], skip.")
                    return@forEach
                }
                while (packageMetadataList.isNotEmpty()) {
                    helmNodeList.addAll(packageMetadataList)
                    page += 1
                    packageMetadataList =
                        queryPackagePage(projectId, repoName, page, name).records.map { resolveNode(it) }
                }
                try {
                    // 修改时间
                    repairCreatedDate(entries, name, helmNodeList)
                } catch (exception: RuntimeException) {
                    logger.error(
                        "Failed to to repair created date for [$name] in repo [$projectId/$repoName].",
                        exception
                    )
                    throw exception
                }
            }
            uploadIndexYamlMetadata(indexYamlMetadata = helmIndexYamlMetadata)
        } catch (exception: RuntimeException) {
            logger.error("repair created date for repo [$projectId/$repoName] failed, message: ${exception.message}")
            throw exception
        }
    }

    private fun repairCreatedDate(
        entries: MutableMap<String, SortedSet<HelmChartMetadata>>,
        name: String,
        helmNodeList: MutableList<NodeInfo>
    ) {
        entries[name]?.forEach { it ->
            val digest = it.digest
            val nodeInfo = helmNodeList.first { it.sha256 == digest }
            it.created = TimeFormatUtil.convertToUtcTime(
                LocalDateTime.parse(
                    nodeInfo.createdDate,
                    DateTimeFormatter.ISO_DATE_TIME
                )
            )
        }
    }

    private fun helmIndexYamlMetadata(projectId: String, repoName: String): HelmIndexYamlMetadata {
        val nodeDetail =
            nodeClient.getNodeDetail(projectId, repoName, HelmUtils.getIndexCacheYamlFullPath()).data ?: run {
                logger.error("query index-cache.yaml file failed in repo [$projectId/$repoName]")
                throw HelmFileNotFoundException(
                    "the file index-cache.yaml not found in repo [$projectId/$repoName]"
                )
            }
        val inputStream = storageService.load(nodeDetail.sha256!!, Range.full(nodeDetail.size), null) ?: run {
            logger.error("load index-cache.yaml file stream is null in repo [$projectId/$repoName]")
            throw HelmFileNotFoundException(
                "the file index-cache.yaml not found in repo [$projectId/$repoName]"
            )
        }
        return inputStream.use { it.readYamlString() }
    }

    override fun fixPackageVersion(): List<PackageManagerResponse> {
        val packageManagerList = mutableListOf<PackageManagerResponse>()
        // 查找所有仓库
        logger.info("starting add package manager function to historical data")
        val repositoryList = repositoryClient.pageByType(0, 1000, "HELM").data?.records ?: run {
            logger.warn("no helm repository found, return.")
            return emptyList()
        }
        val helmLocalRepositoryList = repositoryList.filter { it.category == RepositoryCategory.LOCAL }.toList()
        logger.info(
            "find [${helmLocalRepositoryList.size}] HELM local " +
                "repository ${repositoryList.map { it.projectId to it.name }}"
        )
        helmLocalRepositoryList.forEach {
            val packageManagerResponse = addPackageManager(it.projectId, it.name)
            packageManagerList.add(packageManagerResponse)
        }
        return packageManagerList
    }

    private fun addPackageManager(projectId: String, repoName: String): PackageManagerResponse {
        // 查询仓库下面的所有package的包
        var successCount = 0L
        var failedCount = 0L
        var totalCount = 1L
        val failedSet = mutableSetOf<String>()
        val startTime = LocalDateTime.now()

        try {
            // 查询索引文件
            val nodeDetail =
                nodeClient.getNodeDetail(projectId, repoName, HelmUtils.getIndexCacheYamlFullPath()).data ?: run {
                    logger.error("query index-cache.yaml file failed in repo [$projectId/$repoName]")
                    throw HelmFileNotFoundException(
                        "the file index-cache.yaml not found in repo [$projectId/$repoName]"
                    )
                }
            // sleep 0.1s
            Thread.sleep(100)
            val inputStream = storageService.load(nodeDetail.sha256!!, Range.full(nodeDetail.size), null) ?: run {
                logger.error("load index-cache.yaml file stream is null in repo [$projectId/$repoName]")
                throw HelmFileNotFoundException(
                    "load index-cache.yaml file stream is null in repo [$projectId/$repoName]"
                )
            }
            val helmIndexYamlMetadata = inputStream.use { it.readYamlString<HelmIndexYamlMetadata>() }
            if (helmIndexYamlMetadata.entries.isEmpty()) {
                return PackageManagerResponse.emptyResponse(projectId, repoName)
            }

            val packageSize = helmIndexYamlMetadata.entries.size
            val helmNodeList = mutableListOf<NodeInfo>()
            helmIndexYamlMetadata.entries.keys.forEach { name ->
                helmNodeList.clear()
                var page = 1
                val packageMetadataPage = queryPackagePage(projectId, repoName, page, name)
                var packageMetadataList = packageMetadataPage.records.map { resolveNode(it) }
                if (packageMetadataList.isEmpty()) {
                    logger.info("no package with name [$name] found in repo [$projectId/$repoName], skip.")
                    return@forEach
                }
                while (packageMetadataList.isNotEmpty()) {
                    helmNodeList.addAll(packageMetadataList)
                    page += 1
                    packageMetadataList =
                        queryPackagePage(projectId, repoName, page, name).records.map { resolveNode(it) }
                }
                try {
                    logger.info(
                        "Retrieved $packageSize packages to add package manager in repo [$projectId/$repoName], " +
                            "process: $totalCount/$packageSize, " +
                            "current package: [$name] with [${helmNodeList.size}] versions."
                    )
                    // 添加包管理
                    doAddPackageManager(projectId, repoName, helmNodeList, name)
                    logger.info("Success to add package manager for [$name] in repo [$projectId/$repoName].")
                    successCount += 1
                } catch (exception: RuntimeException) {
                    logger.error(
                        "Failed to to add package manager for [$name] in repo [$projectId/$repoName].",
                        exception
                    )
                    failedSet.add(name)
                    failedCount += 1
                } finally {
                    totalCount += 1
                }
            }
            val durationSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
            logger.info(
                "Repair helm package populate in repo [$projectId/$repoName], " +
                    "total: $totalCount, success: $successCount, failed: $failedCount, " +
                    "duration $durationSeconds s totally."
            )
            return PackageManagerResponse(
                projectId = projectId,
                repoName = repoName,
                totalCount = totalCount,
                successCount = successCount,
                failedCount = failedCount,
                failedSet = failedSet
            )
        } catch (exception: HelmFileNotFoundException) {
            logger.error("package populate for repo [$projectId/$repoName] failed, message: ${exception.message}")
            return PackageManagerResponse.emptyResponse(
                projectId,
                repoName,
                description = exception.message
            )
        } catch (exception: RuntimeException) {
            logger.error("fresh index file for repo [$projectId/$repoName] failed, skip. message: ", exception)
            return PackageManagerResponse.emptyResponse(
                projectId,
                repoName,
                description = "fresh index file for repo [$projectId/$repoName] failed."
            )
        }
    }

    private fun doAddPackageManager(
        projectId: String,
        repoName: String,
        nodeInfoList: List<NodeInfo>,
        name: String
    ) {
        var description: String = StringPool.EMPTY
        val versionList = nodeInfoList.map { it ->
            with(it) {
                // sleep 0.1s
                Thread.sleep(100)
                val helmChartMetadata = storageService.load(sha256!!, Range.full(size), null)
                    ?.use {
                        it.getArchivesContent(CHART_PACKAGE_FILE_EXTENSION).byteInputStream()
                            .readYamlString<HelmChartMetadata>()
                    }
                    ?: throw IllegalStateException("src node not found in repo [$projectId/$repoName]")
                val version = helmChartMetadata.version
                description = helmChartMetadata.description.orEmpty()
                PopulatedPackageVersion(
                    createdBy = createdBy,
                    createdDate = LocalDateTime.parse(createdDate),
                    lastModifiedBy = lastModifiedBy,
                    lastModifiedDate = LocalDateTime.parse(lastModifiedDate),
                    name = version,
                    size = size,
                    downloads = 0,
                    manifestPath = HelmUtils.getIndexCacheYamlFullPath(),
                    artifactPath = HelmUtils.getChartFileFullPath(name, version),
                    metadata = null
                )
            }
        }.toList()
        try {
            populatePackage(versionList, nodeInfoList.first(), name, description)
        } catch (exception: RemoteErrorCodeException) {
            logger.error(
                "add package manager for [$name] failed " +
                    "in repo [$projectId/$repoName]."
            )
            throw exception
        }
        logger.info("add package manager for package [$name] success in repo [$projectId/$repoName]")
    }

    private fun queryPackagePage(
        projectId: String,
        repoName: String,
        page: Int,
        name: String
    ): Page<Map<String, Any?>> {
        val ruleList = mutableListOf<Rule>(
            Rule.QueryRule("projectId", projectId, OperationType.EQ),
            Rule.QueryRule("repoName", repoName, OperationType.EQ),
            Rule.QueryRule("folder", false, OperationType.EQ),
            Rule.QueryRule("fullPath", "tgz", OperationType.SUFFIX),
            Rule.QueryRule("fullPath", "/$name-", OperationType.PREFIX)
        )
        val queryModel = QueryModel(
            page = PageLimit(page, pageSize),
            sort = Sort(listOf("lastModifiedDate"), Sort.Direction.DESC),
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

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    override fun metaDataRegenerate(userId: String, artifactInfo: HelmArtifactInfo) {
        logger.info("handling meta data regenerate request: [$artifactInfo]")
        when (getRepositoryInfo(artifactInfo).category) {
            RepositoryCategory.REMOTE -> {
                val message = "Unable to regenerate metadata for remote repository " +
                    "[${artifactInfo.projectId}/${artifactInfo.repoName}]"
                logger.warn(message)
                throw HelmBadRequestException(message)
            }
        }
        val nodeList = queryNodeList(artifactInfo, false)
        logger.info(
            "query node list for regenerating meta data in repo [${artifactInfo.getRepoIdentify()}]" +
                ", size [${nodeList.size}] "
        )
        val context = ArtifactQueryContext()
        nodeList.forEach {
            Thread.sleep(SLEEP_MILLIS)
            try {
                val path = it[NODE_FULL_PATH] as String
                val chartMetadata = queryHelmChartMetadata(context, path)
                val metaMap = HelmMetadataUtils.convertToMap(chartMetadata)
                context.putAttribute(FULL_PATH, path)
                createVersion(
                    userId = context.userId,
                    projectId = artifactInfo.projectId,
                    repoName = artifactInfo.repoName,
                    chartInfo = chartMetadata,
                    size = context.getLongAttribute(SIZE)!!,
                    isOverwrite = true
                )
                val metadataSaveRequest = MetadataSaveRequest(
                    artifactInfo.projectId,
                    artifactInfo.repoName,
                    path,
                    metaMap,
                    context.userId
                )
                metadataClient.saveMetadata(metadataSaveRequest)
            } catch (e: Exception) {
                logger.error("error occurred while updating meta data, error: ${e.message}")
            }
        }
    }

    companion object {
        private const val pageSize = 10000
        private val logger = LoggerFactory.getLogger(FixToolServiceImpl::class.java)
    }
}
