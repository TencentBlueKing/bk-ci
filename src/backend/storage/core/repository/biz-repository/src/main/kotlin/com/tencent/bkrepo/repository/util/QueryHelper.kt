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

package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.repository.model.TNode
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.LocalDateTime

/**
 * 查询条件构造工具
 */
object QueryHelper {

    fun nodeQuery(projectId: String, repoName: String, fullPath: String? = null): Query {
        val criteria = Criteria.where(TNode::projectId.name).`is`(projectId)
            .and(TNode::repoName.name).`is`(repoName)
            .and(TNode::deleted.name).`is`(null)
        val query = Query(criteria)
        fullPath?.run { criteria.and(TNode::fullPath.name).`is`(fullPath) }

        return query
    }

    fun nodeListQuery(projectId: String, repoName: String, fullPathList: List<String>): Query {
        val criteria = Criteria.where(TNode::projectId.name).`is`(projectId)
            .and(TNode::repoName.name).`is`(repoName)
            .and(TNode::deleted.name).`is`(null)
            .and(TNode::fullPath.name).`in`(fullPathList)

        return Query(criteria)
    }

    fun nodeListCriteria(projectId: String, repoName: String, path: String, includeFolder: Boolean, deep: Boolean): Criteria {
        val formattedPath = NodeUtils.formatPath(path)
        val escapedPath = NodeUtils.escapeRegex(formattedPath)
        val criteria = Criteria.where(TNode::projectId.name).`is`(projectId)
            .and(TNode::repoName.name).`is`(repoName)
            .and(TNode::deleted.name).`is`(null)
            .and(TNode::name.name).ne(StringPool.EMPTY)

        if (deep) {
            criteria.and(TNode::fullPath.name).regex("^$escapedPath")
        } else {
            criteria.and(TNode::path.name).`is`(formattedPath)
        }

        if (!includeFolder) { criteria.and(TNode::folder.name).`is`(false) }

        return criteria
    }

    fun nodeListQuery(projectId: String, repoName: String, path: String, includeFolder: Boolean, includeMetadata: Boolean, deep: Boolean): Query {
        return Query.query(nodeListCriteria(projectId, repoName, path, includeFolder, deep))
            .with(Sort.by(TNode::fullPath.name))
            .apply {
                // 强制使用fullPath索引，否则mongodb可能会使用path索引，不能达到最优索引
                if (deep) {
                    this.withHint(TNode.FULL_PATH_IDX_DEF)
                }
                // 查询元数据
                if (!includeMetadata) {
                    this.fields().exclude(TNode::metadata.name)
                }
            }
    }

    fun nodePathUpdate(path: String, name: String, operator: String): Update {
        return update(operator)
            .set(TNode::path.name, path)
            .set(TNode::name.name, name)
            .set(TNode::fullPath.name, path + name)
    }

    fun nodeExpireDateUpdate(expireDate: LocalDateTime?, operator: String): Update {
        return update(operator).apply {
            expireDate?.let {
                set(TNode::expireDate.name, expireDate)
            } ?: run { unset(TNode::expireDate.name) }
        }
    }

    fun nodeDeleteUpdate(operator: String): Update {
        return update(operator).set(TNode::deleted.name, LocalDateTime.now())
    }

    private fun update(operator: String): Update {
        return Update()
            .set(TNode::lastModifiedDate.name, LocalDateTime.now())
            .set(TNode::lastModifiedBy.name, operator)
    }
}
