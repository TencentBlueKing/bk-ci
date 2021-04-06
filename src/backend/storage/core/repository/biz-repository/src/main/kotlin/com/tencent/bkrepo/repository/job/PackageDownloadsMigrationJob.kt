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

package com.tencent.bkrepo.repository.job

import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.download.DownloadsMigrationRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.service.PackageDownloadsService
import com.tencent.bkrepo.repository.service.PackageService
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 下载统计数据迁移任务
 */
@Component
class PackageDownloadsMigrationJob(
    private val mongoTemplate: MongoTemplate,
    private val packageService: PackageService,
    private val packageDownloadsService: PackageDownloadsService
) {

    fun migrate() {
        // 遍历仓库
        queryRepository().forEach { repo ->
            logger.info("Migrate repo[${repo.projectId}/${repo.name}], type[${repo.type}]")
            // 查询包
            var pageNumber = 1
            var packageOption = PackageListOption(pageNumber = pageNumber, pageSize = DEFAULT_PAGE_SIZE)
            var packagePage = packageService.listPackagePage(repo.projectId, repo.name, option = packageOption)
            while (packagePage.records.isNotEmpty()) {
                val packageList = packagePage.records
                packageList.forEach { pkg ->
                    logger.info("Migrate package[${pkg.projectId}/${pkg.repoName}/${pkg.name}]")
                    // 查询版本
                    val versionOption = VersionListOption(pageNumber = 1, pageSize = DEFAULT_PAGE_SIZE)
                    val versionPage = packageService.listVersionPage(repo.projectId, repo.name, pkg.key, versionOption)
                    val versionList = versionPage.records
                    // 遍历版本
                    versionList.forEach { version ->
                        val displayVersion = "${pkg.projectId}/${pkg.repoName}/${pkg.name}-${version.name}"
                        logger.info("Migrate version[$displayVersion]")
                        // 查询下载统计
                        val request = buildLegacyQueryRequest(pkg, version)
                        val legacyDownloads = queryLegacyDownloads(request)
                        logger.info("Find [${legacyDownloads.size}] legacy download stats.")
                        legacyDownloads.forEach { downloads ->
                            val migrationRequest = DownloadsMigrationRequest(
                                projectId = pkg.projectId,
                                repoName = pkg.repoName,
                                packageName = pkg.name,
                                packageKey = pkg.key,
                                packageVersion = version.name,
                                date = downloads.date,
                                count = downloads.count
                            )
                            logger.info(
                                "Migrate [${downloads.count}] downloads on [${downloads.date}] " +
                                    "for version[$displayVersion]"
                            )
                            packageDownloadsService.migrate(migrationRequest)
                        }
                    }
                }

                pageNumber += 1
                packageOption = PackageListOption(pageNumber = pageNumber, pageSize = DEFAULT_PAGE_SIZE)
                packagePage = packageService.listPackagePage(repo.projectId, repo.name, option = packageOption)
            }
        }
    }

    private fun buildLegacyQueryRequest(pkg: PackageSummary, version: PackageVersion): LegacyQueryRequest {
        return when (pkg.type) {
            PackageType.RPM -> buildRpmRequest(pkg, version)
            PackageType.NPM -> buildNpmRequest(pkg, version)
            else -> LegacyQueryRequest(
                projectId = pkg.projectId,
                repoName = pkg.repoName,
                artifact = version.contentPath.orEmpty(),
                version = version.name
            )
        }
    }

    private fun buildRpmRequest(pkg: PackageSummary, version: PackageVersion): LegacyQueryRequest {
        return LegacyQueryRequest(
            projectId = pkg.projectId,
            repoName = pkg.repoName,
            artifact = version.contentPath.orEmpty(),
            version = null
        )
    }

    private fun buildNpmRequest(pkg: PackageSummary, version: PackageVersion): LegacyQueryRequest {
        return if (version.contentPath.orEmpty().contains("/download/")) {
            val artifactPath = version.contentPath.orEmpty()
            LegacyQueryRequest(
                projectId = pkg.projectId,
                repoName = pkg.repoName,
                artifact = artifactPath.removePrefix("/"),
                version = artifactPath.removeSuffix(".tgz")
            )
        } else {
            LegacyQueryRequest(
                projectId = pkg.projectId,
                repoName = pkg.repoName,
                artifact = pkg.name,
                version = version.name
            )
        }
    }

    private fun queryLegacyDownloads(request: LegacyQueryRequest): List<LegacyDownloads> {
        if (request.artifact.isBlank()) return emptyList()
        val criteria = Criteria.where("projectId").isEqualTo(request.projectId)
            .and("repoName").isEqualTo(request.repoName)
            .and("artifact").isEqualTo(request.artifact)
            .apply { request.version?.let { and("version").isEqualTo(it) } }
        val query = Query.query(criteria)
        return mongoTemplate.find(query, LegacyDownloads::class.java, "artifact_download_statistics")
    }

    private fun queryRepository(): List<TRepository> {
        val criteria = Criteria.where("type").inValues(RepositoryType.NPM, RepositoryType.RPM)
            .and("category").isEqualTo(RepositoryCategory.LOCAL)
        val query = Query.query(criteria)
        return mongoTemplate.find(query, TRepository::class.java)
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10000
        private val logger = LoggerHolder.jobLogger
    }
}

data class LegacyDownloads(
    val projectId: String,
    val repoName: String,
    val artifact: String,
    val version: String? = null,
    val date: LocalDate,
    val count: Long
)

data class LegacyQueryRequest(
    val projectId: String,
    val repoName: String,
    val artifact: String,
    val version: String?
)
