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

package com.tencent.bkrepo.rds.service.impl

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.rds.config.RdsProperties
import com.tencent.bkrepo.rds.constants.NAME
import com.tencent.bkrepo.rds.constants.NODE_FULL_PATH
import com.tencent.bkrepo.rds.constants.NODE_METADATA
import com.tencent.bkrepo.rds.constants.NODE_METADATA_NAME
import com.tencent.bkrepo.rds.constants.NODE_METADATA_VERSION
import com.tencent.bkrepo.rds.constants.PROJECT_ID
import com.tencent.bkrepo.rds.constants.REPO_NAME
import com.tencent.bkrepo.rds.exception.RdsFileNotFoundException
import com.tencent.bkrepo.rds.pojo.RdsDomainInfo
import com.tencent.bkrepo.rds.pojo.artifact.RdsArtifactInfo
import com.tencent.bkrepo.rds.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.rds.service.ChartInfoService
import com.tencent.bkrepo.rds.utils.ChartParserUtil
import com.tencent.bkrepo.rds.utils.ObjectBuilderUtil
import com.tencent.bkrepo.rds.utils.RdsUtils
import java.time.LocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ChartInfoServiceImpl(
    private val rdsProperties: RdsProperties
) : AbstractChartService(), ChartInfoService {
    @Permission(ResourceType.REPO, PermissionAction.READ)
    override fun allChartsList(artifactInfo: RdsArtifactInfo, startTime: LocalDateTime?): ResponseEntity<Any> {
        return lockAction(artifactInfo.projectId, artifactInfo.repoName) { chartListSearch(artifactInfo, startTime) }
    }

    private fun chartListSearch(artifactInfo: RdsArtifactInfo, startTime: LocalDateTime?): ResponseEntity<Any> {
        val indexYamlMetadata = if (!exist(
                projectId = artifactInfo.projectId,
                repoName = artifactInfo.repoName,
                fullPath = RdsUtils.getIndexCacheYamlFullPath()
            )
        ) {
            RdsUtils.initIndexYamlMetadata()
        } else {
            queryOriginalIndexYaml()
        }
        val startDate = startTime ?: LocalDateTime.MIN
        return ResponseEntity.ok().body(
            ChartParserUtil.searchJson(indexYamlMetadata, artifactInfo.getArtifactFullPath(), startDate)
        )
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    override fun isExists(artifactInfo: RdsArtifactInfo) {
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
        artifactInfo: RdsArtifactInfo,
        packageKey: String,
        version: String
    ): PackageVersionInfo {
        with(artifactInfo) {
            val name = PackageKeys.resolveRds(packageKey)
            val fullPath = String.format("/%s-%s.tgz", name, version)
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: run {
                logger.warn("node [$fullPath] don't found.")
                throw RdsFileNotFoundException("node [$fullPath] don't found.")
            }
            val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data ?: run {
                logger.warn("packageKey [$packageKey] don't found.")
                throw RdsFileNotFoundException("packageKey [$packageKey] don't found.")
            }
            val basicInfo = ObjectBuilderUtil.buildBasicInfo(nodeDetail, packageVersion)
            return PackageVersionInfo(basicInfo, emptyMap())
        }
    }

    override fun getRegistryDomain(): RdsDomainInfo {
        return RdsDomainInfo(UrlFormatter.formatHost(rdsProperties.domain))
    }

    companion object {
        const val CURRENT_PAGE = 0
        const val SIZE = 5
        val logger: Logger = LoggerFactory.getLogger(ChartInfoServiceImpl::class.java)
    }
}
