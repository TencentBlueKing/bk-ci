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
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.helm.artifact.HelmArtifactInfo
import com.tencent.bkrepo.helm.constants.CHART_NOT_FOUND
import com.tencent.bkrepo.helm.constants.NAME
import com.tencent.bkrepo.helm.constants.NODE_FULL_PATH
import com.tencent.bkrepo.helm.constants.NODE_METADATA
import com.tencent.bkrepo.helm.constants.NODE_METADATA_NAME
import com.tencent.bkrepo.helm.constants.NODE_METADATA_VERSION
import com.tencent.bkrepo.helm.constants.NO_CHART_NAME_FOUND
import com.tencent.bkrepo.helm.constants.PROJECT_ID
import com.tencent.bkrepo.helm.constants.REPO_NAME
import com.tencent.bkrepo.helm.exception.HelmFileNotFoundException
import com.tencent.bkrepo.helm.model.metadata.HelmChartMetadata
import com.tencent.bkrepo.helm.model.metadata.HelmIndexYamlMetadata
import com.tencent.bkrepo.helm.pojo.user.BasicInfo
import com.tencent.bkrepo.helm.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.helm.service.ChartInfoService
import com.tencent.bkrepo.helm.service.ChartRepositoryService
import com.tencent.bkrepo.helm.utils.TimeFormatUtil
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ChartInfoServiceImpl(
    private val chartRepositoryService: ChartRepositoryService
) : AbstractChartService(), ChartInfoService {
    @Permission(ResourceType.REPO, PermissionAction.READ)
    override fun allChartsList(artifactInfo: HelmArtifactInfo, startTime: LocalDateTime?): ResponseEntity<Any> {
        if (startTime != null) {
            val nodeList = queryNodeList(artifactInfo, lastModifyTime = startTime)
            val indexYamlMetadata = chartRepositoryService.buildIndexYamlMetadata(nodeList, artifactInfo, true)
            return ResponseEntity.ok().body(convertUtcTime(indexYamlMetadata).entries)
        }
        chartRepositoryService.freshIndexFile(artifactInfo)
        val indexYamlMetadata = queryOriginalIndexYaml()
        return searchJson(indexYamlMetadata, artifactInfo.getArtifactFullPath())
    }

    private fun searchJson(indexYamlMetadata: HelmIndexYamlMetadata, urls: String): ResponseEntity<Any> {
        val urlList = urls.removePrefix("/").split("/").filter { it.isNotBlank() }
        when (urlList.size) {
            // Without name and version
            0 -> {
                return ResponseEntity.ok().body(convertUtcTime(indexYamlMetadata).entries)
            }
            // query with name
            1 -> {
                val chartName = urlList[0]
                val chartList = indexYamlMetadata.entries[chartName]
                chartList?.forEach { convertUtcTime(it) }
                return if (chartList == null) {
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body(CHART_NOT_FOUND)
                } else {
                    ResponseEntity.ok().body(chartList)
                }
            }
            // query with name and version
            2 -> {
                val chartName = urlList[0]
                val chartVersion = urlList[1]
                val chartList =
                    indexYamlMetadata.entries[chartName] ?: return ResponseEntity.ok().body(NO_CHART_NAME_FOUND)
                val helmChartMetadataList = chartList.filter {
                    chartVersion == it.version
                }.toList()
                return if (helmChartMetadataList.isNotEmpty()) {
                    require(helmChartMetadataList.size == 1) {
                        "find more than one version [$chartVersion] in package [$chartName]."
                    }
                    ResponseEntity.ok().body(convertUtcTime(helmChartMetadataList.first()))
                } else {
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(mapOf("error" to "no chart version found for $chartName-$chartVersion"))
                }
            }
            else -> {
                // ERROR_NOT_FOUND
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CHART_NOT_FOUND)
            }
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    override fun isExists(artifactInfo: HelmArtifactInfo) {
        val response = HttpContextHolder.getResponse()
        val status: HttpStatus = with(artifactInfo) {
            val projectId = Rule.QueryRule(PROJECT_ID, projectId)
            val repoName = Rule.QueryRule(REPO_NAME, repoName)
            val urlList = this.getArtifactFullPath().trimStart('/').split("/").filter { it.isNotBlank() }
            val rule: Rule? = when (urlList.size) {
                // query with name
                1 -> {
                    val name = Rule.QueryRule(NODE_METADATA_NAME, urlList[0])
                    Rule.NestedRule(mutableListOf(repoName, projectId, name))
                }
                // query with name and version
                2 -> {
                    val name = Rule.QueryRule(NODE_METADATA_NAME, urlList[0])
                    val version = Rule.QueryRule(NODE_METADATA_VERSION, urlList[1])
                    Rule.NestedRule(mutableListOf(repoName, projectId, name, version))
                }
                else -> {
                    null
                }
            }
            if (rule != null) {
                val queryModel = QueryModel(
                    page = PageLimit(CURRENT_PAGE, SIZE),
                    sort = Sort(listOf(NAME), Sort.Direction.ASC),
                    select = mutableListOf(PROJECT_ID, REPO_NAME, NODE_FULL_PATH, NODE_METADATA),
                    rule = rule
                )
                val nodeList: List<Map<String, Any?>>? = nodeClient.search(queryModel).data?.records
                if (nodeList.isNullOrEmpty()) HttpStatus.NOT_FOUND else HttpStatus.OK
            } else {
                HttpStatus.NOT_FOUND
            }
        }
        response.status = status.value()
    }

    override fun detailVersion(
        userId: String,
        artifactInfo: HelmArtifactInfo,
        packageKey: String,
        version: String
    ): PackageVersionInfo {
        with(artifactInfo) {
            val name = PackageKeys.resolveHelm(packageKey)
            val fullPath = String.format("/%s-%s.tgz", name, version)
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: run {
                logger.warn("node [$fullPath] don't found.")
                throw HelmFileNotFoundException("node [$fullPath] don't found.")
            }
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw HelmFileNotFoundException("packageKey [$packageKey] don't found.")
            }
            val basicInfo = buildBasicInfo(nodeDetail, packageVersion)
            return PackageVersionInfo(basicInfo, emptyMap())
        }
    }

    companion object {
        const val CURRENT_PAGE = 0
        const val SIZE = 5

        val logger: Logger = LoggerFactory.getLogger(ChartInfoServiceImpl::class.java)

        fun convertUtcTime(indexYamlMetadata: HelmIndexYamlMetadata): HelmIndexYamlMetadata {
            indexYamlMetadata.entries.forEach { it ->
                val chartMetadataSet = it.value
                chartMetadataSet.forEach { chartMetadata ->
                    convertUtcTime(chartMetadata)
                }
            }
            return indexYamlMetadata
        }

        fun convertUtcTime(helmChartMetadata: HelmChartMetadata): HelmChartMetadata {
            helmChartMetadata.created?.let {
                helmChartMetadata.created = TimeFormatUtil.formatLocalTime(TimeFormatUtil.convertToLocalTime(it))
            }
            return helmChartMetadata
        }

        fun buildBasicInfo(nodeDetail: NodeDetail, packageVersion: PackageVersion): BasicInfo {
            with(nodeDetail) {
                return BasicInfo(
                    packageVersion.name,
                    fullPath,
                    size,
                    sha256.orEmpty(),
                    md5.orEmpty(),
                    packageVersion.stageTag,
                    projectId,
                    repoName,
                    packageVersion.downloads,
                    createdBy,
                    createdDate,
                    lastModifiedBy,
                    lastModifiedDate
                )
            }
        }
    }
}
