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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.repository.service.query

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.constant.REPO_NAME
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.model.TNode
import org.slf4j.LoggerFactory
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
class NodeQueryServiceImpl(
    private val nodeDao: NodeDao,
    private val nodeQueryBuilder: NodeQueryBuilder,
    private val permissionManager: PermissionManager
) : NodeQueryService {

    /**
     * 查询节点
     */
    override fun query(queryModel: QueryModel): Page<Map<String, Any>> {
        logger.debug("Node query: [$queryModel]")
        val query = nodeQueryBuilder.build(queryModel)
        return doQuery(query)
    }

    /**
     * 查询节点(提供外部使用，需要鉴权)
     */
    override fun userQuery(operator: String, queryModel: QueryModel): Page<Map<String, Any>> {
        logger.debug("User node query: [$queryModel]")
        // 解析projectId和repoName
        val query = nodeQueryBuilder.build(queryModel)
        var projectId: String? = null
        val repoNameList = mutableListOf<String>()
        for (rule in (queryModel.rule as Rule.NestedRule).rules) {
            if (rule is Rule.QueryRule && rule.field == REPO_NAME) {
                when (rule.operation) {
                    OperationType.IN -> (rule.value as List<String>).forEach { repoNameList.add(it) }
                    else -> repoNameList.add(rule.value.toString())
                }
            }
            if (rule is Rule.QueryRule && rule.field == PROJECT_ID) {
                projectId = rule.value.toString()
            }
        }
        // 鉴权
        repoNameList.forEach {
            permissionManager.checkPermission(operator, ResourceType.REPO, PermissionAction.READ, projectId!!, it)
        }

        return doQuery(query)
    }

    private fun doQuery(query: Query): Page<Map<String, Any>> {
        val nodeList = nodeDao.find(query, MutableMap::class.java) as List<MutableMap<String, Any>>
        // metadata格式转换，并排除id字段
        nodeList.forEach {
            it[TNode::metadata.name]?.let { metadata -> it[TNode::metadata.name] = convert(metadata as List<Map<String, String>>) }
            it[TNode::createdDate.name]?.let { createDate -> it[TNode::createdDate.name] = LocalDateTime.ofInstant((createDate as Date).toInstant(), ZoneId.systemDefault()) }
            it[TNode::lastModifiedDate.name]?.let { lastModifiedDate -> it[TNode::lastModifiedDate.name] = LocalDateTime.ofInstant((lastModifiedDate as Date).toInstant(), ZoneId.systemDefault()) }
            it.remove("_id")
        }
        val countQuery = Query.of(query).limit(0).skip(0)
        val count = nodeDao.count(countQuery)
        val page = if (query.limit == 0) 0 else (query.skip / query.limit).toInt()
        return Page(page, query.limit, count, nodeList)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeQueryServiceImpl::class.java)

        fun convert(metadataList: List<Map<String, String>>): Map<String, String> {
            return metadataList.filter { it.containsKey("key") && it.containsKey("value") }.map { it.getValue("key") to it.getValue("value") }.toMap()
        }
    }
}
