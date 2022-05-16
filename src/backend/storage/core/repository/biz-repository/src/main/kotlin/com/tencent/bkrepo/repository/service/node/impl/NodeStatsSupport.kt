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

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.service.node.NodeStatsOperation
import com.tencent.bkrepo.repository.util.NodeQueryHelper
import org.springframework.data.mongodb.core.aggregation.Aggregation.group
import org.springframework.data.mongodb.core.aggregation.Aggregation.match
import org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

/**
 * 节点统计接口
 */
open class NodeStatsSupport(
    nodeBaseService: NodeBaseService
) : NodeStatsOperation {

    private val nodeDao: NodeDao = nodeBaseService.nodeDao

    override fun computeSize(artifact: ArtifactInfo): NodeSizeInfo {
        val projectId = artifact.projectId
        val repoName = artifact.repoName
        val fullPath = artifact.getArtifactFullPath()
        val node = nodeDao.findNode(projectId, repoName, fullPath)
            ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
        // 节点为文件直接返回
        if (!node.folder) {
            return NodeSizeInfo(subNodeCount = 0, size = node.size)
        }
        val listOption = NodeListOption(includeFolder = true, deep = true)
        val criteria = NodeQueryHelper.nodeListCriteria(projectId, repoName, node.fullPath, listOption)
        val count = nodeDao.count(Query(criteria))
        val size = aggregateComputeSize(criteria)
        return NodeSizeInfo(subNodeCount = count, size = size)
    }

    override fun countFileNode(artifact: ArtifactInfo): Long {
        with(artifact) {
            val listOption = NodeListOption(
                includeFolder = false,
                includeMetadata = false,
                deep = true,
                sort = false
            )
            val query = NodeQueryHelper.nodeListQuery(projectId, repoName, getArtifactFullPath(), listOption)
            return nodeDao.count(query)
        }
    }

    override fun aggregateComputeSize(criteria: Criteria): Long {
        val aggregation = newAggregation(
            match(criteria),
            group().sum(TNode::size.name).`as`(NodeSizeInfo::size.name)
        )
        val aggregateResult = nodeDao.aggregate(aggregation, HashMap::class.java)
        return aggregateResult.mappedResults.firstOrNull()?.get(NodeSizeInfo::size.name) as? Long ?: 0
    }
}
