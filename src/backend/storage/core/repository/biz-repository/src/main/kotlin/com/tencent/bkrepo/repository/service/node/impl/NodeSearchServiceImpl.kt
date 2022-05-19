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

package com.tencent.bkrepo.repository.service.node.impl

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.util.MongoEscapeUtils
import com.tencent.bkrepo.common.security.http.core.HttpAuthProperties
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.repo.RepoListOption
import com.tencent.bkrepo.repository.pojo.software.CountResult
import com.tencent.bkrepo.repository.pojo.software.ProjectPackageOverview
import com.tencent.bkrepo.repository.search.node.NodeQueryContext
import com.tencent.bkrepo.repository.search.node.NodeQueryInterpreter
import com.tencent.bkrepo.repository.service.node.NodeSearchService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * 节点自定义查询服务实现类
 */
@Suppress("UNCHECKED_CAST")
@Service
class NodeSearchServiceImpl(
    private val nodeDao: NodeDao,
    private val nodeQueryInterpreter: NodeQueryInterpreter,
    private val repositoryService: RepositoryService,
    private val httpAuthProperties: HttpAuthProperties
) : NodeSearchService {

    override fun search(queryModel: QueryModel): Page<Map<String, Any?>> {
        val context = nodeQueryInterpreter.interpret(queryModel) as NodeQueryContext
        return doQuery(context)
    }

    override fun nodeOverview(
        userId: String,
        projectId: String,
        name: String,
        exRepo: String?
    ): List<ProjectPackageOverview> {
        val repos = if (httpAuthProperties.enabled) {
            repositoryService.listPermissionRepo(
                userId = userId,
                projectId = projectId,
                option = RepoListOption(
                    type = RepositoryType.GENERIC.name
                )
            ).map { it.name }
        } else {
            repositoryService.listRepo(
                projectId = projectId,
                type = RepositoryType.GENERIC.name
            ).map { it.name }
        }
        val genericRepos = if (exRepo != null && exRepo.isNotBlank()) {
            repos.filter { it !in (exRepo.split(',')) }
        } else repos

        if (genericRepos.isEmpty()) return listOf()
        val criteria = Criteria.where(TNode::repoName.name).`in`(genericRepos)
        criteria.and(TNode::projectId.name).`is`(projectId)
            .and(TNode::deleted.name).`is`(null)
            .and(TNode::folder.name).`is`(false)

        val escapedValue = MongoEscapeUtils.escapeRegexExceptWildcard(name)
        val regexPattern = escapedValue.replace("*", ".*")
        criteria.and(TNode::name.name).regex("^$regexPattern$", "i")
        val aggregation = Aggregation.newAggregation(
            TNode::class.java,
            Aggregation.match(criteria),
            Aggregation.group("\$${TNode::repoName.name}").count().`as`("count")
        )
        val result = nodeDao.aggregate(aggregation, CountResult::class.java).mappedResults
        return transTree(projectId, result)
    }

    private fun transTree(projectId: String, list: List<CountResult>): List<ProjectPackageOverview> {
        val projectSet = mutableSetOf<ProjectPackageOverview>()
        projectSet.add(
            ProjectPackageOverview(
                projectId = projectId,
                repos = mutableSetOf(),
                sum = 0L
            )
        )
        list.sortedByDescending { it.count }.map { pojo ->
            val repoOverview = ProjectPackageOverview.RepoPackageOverview(
                repoName = pojo.id,
                packages = pojo.count
            )
            projectSet.first().repos.add(repoOverview)
            projectSet.first().sum += pojo.count
        }
        return projectSet.toList()
    }

    private fun doQuery(context: NodeQueryContext): Page<Map<String, Any?>> {
        val query = context.mongoQuery
        val nodeList = nodeDao.find(query, MutableMap::class.java) as List<MutableMap<String, Any?>>
        // metadata格式转换，并排除id字段
        nodeList.forEach {
            it.remove("_id")
            it[NodeInfo::createdDate.name]?.let { createDate ->
                it[TNode::createdDate.name] = convertDateTime(createDate)
            }
            it[NodeInfo::lastModifiedDate.name]?.let { lastModifiedDate ->
                it[TNode::lastModifiedDate.name] = convertDateTime(lastModifiedDate)
            }
            it[NodeInfo::deleted.name]?.let { deleted ->
                it[TNode::deleted.name] = convertDateTime(deleted)
            }
            it[NodeInfo::metadata.name]?.let { metadata ->
                it[NodeInfo::metadata.name] = convert(metadata as List<Map<String, Any>>)
            }
        }
        val countQuery = Query.of(query).limit(0).skip(0)
        val totalRecords = nodeDao.count(countQuery)
        val pageNumber = if (query.limit == 0) 0 else (query.skip / query.limit).toInt()

        return Page(pageNumber + 1, query.limit, totalRecords, nodeList)
    }

    companion object {
        fun convert(metadataList: List<Map<String, Any>>): Map<String, Any> {
            return metadataList.filter { it.containsKey("key") && it.containsKey("value") }
                .map { it.getValue("key").toString() to it.getValue("value") }
                .toMap()
        }

        fun convertDateTime(value: Any): LocalDateTime? {
            return if (value is Date) {
                LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault())
            } else null
        }
    }
}
