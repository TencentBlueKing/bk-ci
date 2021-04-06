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
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.service.PackageService
import com.tencent.bkrepo.repository.util.PackageQueryHelper
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

/**
 * 校正被删除的包数据
 */
@Component
class PackageVersionCorrectionJob(
    private val mongoTemplate: MongoTemplate,
    private val packageDao: PackageDao,
    private val packageVersionDao: PackageVersionDao,
    private val packageService: PackageService,
    private val nodeDao: NodeDao
) {

    fun correct(): List<Any> {
        // 遍历仓库
        val dryRun = HttpContextHolder.getRequestOrNull()?.getParameter("dry-run")?.toBoolean() ?: true
        val result = mutableListOf<Any>()
        queryRepository().forEach { repo ->
            logger.info("Correct repo[${repo.projectId}/${repo.name}], type[${repo.type}]")
            // 查询包
            val query = PackageQueryHelper.packageListQuery(repo.projectId, repo.name, null)
            var pageNumber = 1
            var pageRequest = Pages.ofRequest(pageNumber, DEFAULT_PAGE_SIZE)
            var packages = packageDao.find(query.with(pageRequest))
            while (packages.isNotEmpty()) {
                packages.forEach { pkg ->
                    packageVersionDao.listByPackageId(pkg.id.orEmpty()).forEach { version ->
                        // 查询节点
                        if (version.artifactPath?.isNotBlank() == true) {
                            if (!nodeDao.exists(repo.projectId, repo.name, version.artifactPath!!)) {
                                val displayVersion = "${pkg.projectId}/${pkg.repoName}/${pkg.name}-${version.name}"
                                logger.info("Package version [$displayVersion] has been deleted.")
                                if (!dryRun) {
                                    packageService.deleteVersion(repo.projectId, repo.name, pkg.key, version.name)
                                }
                                val record = mapOf(
                                    "projectId" to repo.projectId,
                                    "repoName" to repo.name,
                                    "packageKey" to pkg.key,
                                    "versionName" to version.name
                                )
                                result.add(record)
                            }
                        }
                    }
                }
                pageNumber += 1
                pageRequest = Pages.ofRequest(pageNumber, DEFAULT_PAGE_SIZE)
                packages = packageDao.find(query.with(pageRequest))
            }
        }
        return result
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
