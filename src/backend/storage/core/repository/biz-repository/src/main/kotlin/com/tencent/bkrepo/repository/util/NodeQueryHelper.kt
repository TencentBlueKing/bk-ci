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

package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.artifact.path.PathUtils.escapeRegex
import com.tencent.bkrepo.common.artifact.path.PathUtils.toFullPath
import com.tencent.bkrepo.common.artifact.path.PathUtils.toPath
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import java.time.LocalDateTime

/**
 * 查询条件构造工具
 */
object NodeQueryHelper {

    fun nodeQuery(projectId: String, repoName: String, fullPath: String? = null): Query {
        val criteria = where(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::deleted).isEqualTo(null)
            .apply { fullPath?.run { and(TNode::fullPath).isEqualTo(fullPath) } }
        return Query(criteria)
    }

    fun nodeQuery(projectId: String, repoName: String, fullPath: List<String>): Query {
        val criteria = where(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::fullPath).inValues(fullPath)
            .and(TNode::deleted).isEqualTo(null)
        return Query(criteria)
    }

    fun nodeListCriteria(projectId: String, repoName: String, path: String, option: NodeListOption): Criteria {
        val nodePath = toPath(path)
        val criteria = where(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::deleted).isEqualTo(null)
        if (option.deep) {
            criteria.and(TNode::fullPath).regex("^${escapeRegex(nodePath)}")
        } else {
            criteria.and(TNode::path).isEqualTo(nodePath)
        }
        if (!option.includeFolder) {
            criteria.and(TNode::folder).isEqualTo(false)
        }
        return criteria
    }

    fun nodeListQuery(
        projectId: String,
        repoName: String,
        path: String,
        option: NodeListOption
    ): Query {
        val query = Query(nodeListCriteria(projectId, repoName, path, option))
        if (option.sort) {
            query.with(Sort.by(Sort.Direction.ASC, TNode::fullPath.name))
            if (option.includeFolder) {
                query.with(Sort.by(Sort.Direction.DESC, TNode::folder.name))
            }
        }
        if (!option.includeMetadata) {
            query.fields().exclude(TNode::metadata.name)
        }
        return query
    }

    /**
     * 查询节点被删除的记录
     */
    fun nodeDeletedPointListQuery(projectId: String, repoName: String, fullPath: String): Query {
        val criteria = where(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::fullPath).isEqualTo(toFullPath(fullPath))
            .and(TNode::deleted).ne(null)
        return Query(criteria).with(Sort.by(Sort.Direction.DESC, TNode::deleted.name))
    }

    /**
     * 查询单个被删除节点
     */
    fun nodeDeletedPointQuery(projectId: String, repoName: String, fullPath: String, deleted: LocalDateTime): Query {
        val criteria = where(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::fullPath).isEqualTo(toFullPath(fullPath))
            .and(TNode::deleted).isEqualTo(deleted)
        return Query(criteria)
    }

    /**
     * 查询被删除的目录以及子节点
     */
    fun nodeDeletedFolderQuery(
        projectId: String,
        repoName: String,
        path: String,
        deleted: LocalDateTime,
        deep: Boolean
    ): Query {
        val nodePath = toPath(path)
        val criteria = where(TNode::projectId).isEqualTo(projectId)
            .and(TNode::repoName).isEqualTo(repoName)
            .and(TNode::deleted).isEqualTo(deleted)
        if (deep) {
            criteria.and(TNode::fullPath).regex("^${escapeRegex(nodePath)}")
        } else {
            criteria.and(TNode::path).isEqualTo(nodePath)
        }
        return Query(criteria)
    }

    fun nodeRestoreUpdate(): Update {
        return Update().unset(TNode::deleted.name)
    }

    fun nodePathUpdate(path: String, name: String, operator: String): Update {
        return update(operator)
            .set(TNode::path.name, path)
            .set(TNode::name.name, name)
            .set(TNode::fullPath.name, path + name)
    }

    fun nodeExpireDateUpdate(expireDate: LocalDateTime?, operator: String): Update {
        return update(operator).apply {
            expireDate?.let { set(TNode::expireDate.name, expireDate) } ?: run { unset(TNode::expireDate.name) }
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
