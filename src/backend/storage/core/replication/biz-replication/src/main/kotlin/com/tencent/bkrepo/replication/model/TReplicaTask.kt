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

import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeName
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.request.ReplicaObjectType
import com.tencent.bkrepo.replication.pojo.request.ReplicaType
import com.tencent.bkrepo.replication.pojo.task.ReplicaStatus
import com.tencent.bkrepo.replication.pojo.task.setting.ReplicaSetting
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 同步任务
 */
@Document("replica_task")
data class TReplicaTask(
    var id: String? = null,
    /**
     * 任务唯一key
     * 任务更新后删除旧的数据，插入新的task，保持key不变
     */
    @Indexed(unique = true)
    val key: String,
    /**
     * 任务名称
     */
    var name: String,
    /**
     * 项目id
     */
    @Indexed
    val projectId: String,
    /**
     * 同步对象类型
     */
    val replicaObjectType: ReplicaObjectType,
    /**
     * 同步类型
     */
    val replicaType: ReplicaType,
    /**
     * 任务设置
     */
    val setting: ReplicaSetting,
    /**
     * 远程集群集合
     */
    val remoteClusters: Set<ClusterNodeName>,
    /**
     * 任务状态
     */
    var status: ReplicaStatus,
    /**
     * 任务描述
     */
    val description: String?,
    /**
     * 上次执行状态
     */
    var lastExecutionStatus: ExecutionStatus?,
    /**
     * 上次执行时间
     */
    var lastExecutionTime: LocalDateTime?,
    /**
     * 下次执行时间
     */
    var nextExecutionTime: LocalDateTime?,
    /**
     * 执行次数
     */
    var executionTimes: Long,
    /**
     * 是否启用
     */
    var enabled: Boolean = true,
    /**
     * 审计信息
     */
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime
)
