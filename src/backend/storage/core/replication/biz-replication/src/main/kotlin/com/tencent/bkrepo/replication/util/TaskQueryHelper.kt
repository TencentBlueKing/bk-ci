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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.replication.util

import com.tencent.bkrepo.replication.model.TReplicaObject
import com.tencent.bkrepo.replication.model.TReplicaTask
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.request.ReplicaType
import com.tencent.bkrepo.replication.pojo.task.ReplicaStatus
import com.tencent.bkrepo.replication.pojo.task.TaskSortType
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where

/**
 * 任务查询条件构造工具
 */
object TaskQueryHelper {

    /**
     * 构造list查询条件
     */
    fun buildListQuery(
        projectId: String,
        name: String? = null,
        lastExecutionStatus: ExecutionStatus? = null,
        enabled: Boolean? = null,
        sortType: TaskSortType?
    ): Query {
        val criteria = where(TReplicaTask::projectId).isEqualTo(projectId)
        name?.takeIf { it.isNotBlank() }?.apply { criteria.and(TReplicaTask::name).regex("^$this") }
        lastExecutionStatus?.apply { criteria.and(TReplicaTask::lastExecutionStatus).isEqualTo(this) }
        enabled?.apply { criteria.and(TReplicaTask::enabled).isEqualTo(this) }
        return Query(criteria).with(Sort.by(Sort.Direction.DESC, sortType?.key))
    }

    fun undoScheduledTaskQuery(): Query {
        val criteria = Criteria.where(TReplicaTask::replicaType.name).`is`(ReplicaType.SCHEDULED)
            .and(TReplicaTask::status.name).`in`(ReplicaStatus.UNDO_STATUS)
            .and(TReplicaTask::enabled.name).`is`(true)
        return Query(criteria)
    }

    fun realTimeTaskQuery(taskKeyList: List<String>): Query {
        val criteria = where(TReplicaTask::replicaType).isEqualTo(ReplicaType.REAL_TIME)
            .and(TReplicaTask::key).inValues(taskKeyList)
            .and(TReplicaTask::enabled).isEqualTo(true)
        return Query(criteria)
    }

    fun taskObjectQuery(projectId: String, repoName: String): Query {
        val criteria = where(TReplicaObject::localProjectId).isEqualTo(projectId)
            .and(TReplicaObject::localRepoName).isEqualTo(repoName)
        return Query(criteria)
    }
}
