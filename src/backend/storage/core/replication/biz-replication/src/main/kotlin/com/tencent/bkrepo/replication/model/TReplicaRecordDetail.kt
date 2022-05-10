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

package com.tencent.bkrepo.replication.model

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.record.ReplicaProgress
import com.tencent.bkrepo.replication.pojo.task.objects.PackageConstraint
import com.tencent.bkrepo.replication.pojo.task.objects.PathConstraint
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 同步任务执行记录详情
 * 记录-详情：1 to N
 */
@Document("replica_record_detail")
data class TReplicaRecordDetail(
    var id: String? = null,
    /**
     * 关联的record id
     */
    @Indexed
    val recordId: String,
    /**
     * 本地cluster名称
     */
    val localCluster: String,
    /**
     * 远程cluster名称
     */
    val remoteCluster: String,
    /**
     * 本地仓库名称
     */
    val localRepoName: String,
    /**
     * 仓库类型
     */
    val repoType: RepositoryType,
    /**
     * 包限制
     */
    val packageConstraint: PackageConstraint? = null,
    /**
     * 路径限制
     */
    val pathConstraint: PathConstraint? = null,
    /**
     * 运行状态
     */
    var status: ExecutionStatus,
    /**
     * 同步进度
     */
    val progress: ReplicaProgress,
    /**
     * 开启时间
     */
    var startTime: LocalDateTime,
    /**
     * 结束时间
     */
    var endTime: LocalDateTime? = null,
    /**
     * 错误原因
     */
    var errorReason: String? = null
)
