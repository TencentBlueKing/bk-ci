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

package com.tencent.bkrepo.repository.dao

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.mongo.dao.sharding.HashShardingMongoDao
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.util.NodeQueryHelper
import org.springframework.data.domain.Page
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 节点 Dao
 */
@Repository
class NodeDao : HashShardingMongoDao<TNode>() {
    /**
     * 查询节点
     */
    fun findNode(projectId: String, repoName: String, fullPath: String): TNode? {
        if (PathUtils.isRoot(fullPath)) {
            return buildRootNode(projectId, repoName)
        }
        return this.findOne(NodeQueryHelper.nodeQuery(projectId, repoName, fullPath))
    }

    /**
     * 查询节点是否存在
     */
    fun exists(projectId: String, repoName: String, fullPath: String): Boolean {
        if (PathUtils.isRoot(fullPath)) {
            return true
        }
        return this.exists(NodeQueryHelper.nodeQuery(projectId, repoName, fullPath))
    }

    /**
     * 根据[sha256]分页查询节点，需要遍历所有分表
     *
     * @param includeDeleted 是否包含被删除的节点
     */
    fun pageBySha256(
        sha256: String,
        option: NodeListOption,
        includeDeleted: Boolean = false
    ): Page<TNode> {
        val pageRequest = Pages.ofRequest(option.pageNumber, option.pageSize)

        // 构造查询条件
        val criteria = where(TNode::sha256).isEqualTo(sha256).and(TNode::folder).isEqualTo(false)
        if (!includeDeleted) {
            criteria.and(TNode::deleted).isEqualTo(null)
        }
        val query = Query(criteria)
        if (!option.includeMetadata) {
            query.fields().exclude(TNode::metadata.name)
        }

        return pageWithoutShardingKey(pageRequest, query)
    }

    companion object {
        fun buildRootNode(projectId: String, repoName: String): TNode {
            return TNode(
                createdBy = StringPool.EMPTY,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = StringPool.EMPTY,
                lastModifiedDate = LocalDateTime.now(),
                projectId = projectId,
                repoName = repoName,
                folder = true,
                path = PathUtils.ROOT,
                name = StringPool.EMPTY,
                fullPath = PathUtils.ROOT,
                size = 0
            )
        }
    }
}
