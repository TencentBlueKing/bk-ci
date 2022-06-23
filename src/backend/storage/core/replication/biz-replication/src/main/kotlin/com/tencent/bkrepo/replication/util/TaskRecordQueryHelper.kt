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

import com.tencent.bkrepo.replication.model.TReplicaRecord
import com.tencent.bkrepo.replication.model.TReplicaRecordDetail
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordDetailListOption
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordListOption
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where

/**
 * 执行日志查询条件构造工具
 */
object TaskRecordQueryHelper {

    fun recordDetailListQuery(
        recordId: String,
        option: ReplicaRecordDetailListOption
    ): Query {
        with(option) {
            val criteria = where(TReplicaRecordDetail::recordId).isEqualTo(recordId)
                .apply {
                    packageName?.let { and("packageConstraint.packageKey").regex("^$it") }
                }.apply {
                    repoName?.let { and(TReplicaRecordDetail::localRepoName).isEqualTo(it) }
                }.apply {
                    clusterName?.let { and(TReplicaRecordDetail::remoteCluster).isEqualTo(it) }
                }.apply {
                    path?.let { and("pathConstraint.path").isEqualTo("^$it") }
                }.apply {
                    status?.let { and(TReplicaRecordDetail::status).isEqualTo(it) }
                }
            return Query(criteria)
                .with(Sort.by(Sort.Order(Sort.Direction.DESC, TReplicaRecordDetail::startTime.name)))
        }
    }

    fun recordListQuery(key: String, option: ReplicaRecordListOption): Query {
        val criteria = with(option) {
            where(TReplicaRecord::taskKey).isEqualTo(key)
                .apply {
                    status?.let { and(TReplicaRecord::status).isEqualTo(it) }
                }
        }
        return Query(criteria)
            .with(Sort.by(Sort.Order(Sort.Direction.DESC, TReplicaRecord::startTime.name)))
    }
}
