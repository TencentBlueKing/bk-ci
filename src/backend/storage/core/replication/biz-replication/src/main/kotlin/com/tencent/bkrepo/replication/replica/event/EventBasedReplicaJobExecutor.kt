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

package com.tencent.bkrepo.replication.replica.event

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordInfo
import com.tencent.bkrepo.replication.pojo.task.ReplicaTaskDetail
import com.tencent.bkrepo.replication.replica.base.AbstractReplicaJobExecutor
import com.tencent.bkrepo.replication.service.ClusterNodeService
import com.tencent.bkrepo.replication.service.ReplicaRecordService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 基于事件消息的实时同步逻辑实现类
 */
@Suppress("TooGenericExceptionCaught")
@Component
class EventBasedReplicaJobExecutor(
    clusterNodeService: ClusterNodeService,
    localDataManager: LocalDataManager,
    replicaService: EventBasedReplicaService,
    private val replicaRecordService: ReplicaRecordService
) : AbstractReplicaJobExecutor(clusterNodeService, localDataManager, replicaService) {

    /**
     * 执行同步
     */
    fun execute(taskDetail: ReplicaTaskDetail, event: ArtifactEvent) {
        val task = taskDetail.task
        val taskRecord: ReplicaRecordInfo = replicaRecordService.findOrCreateLatestRecord(task.key)
        try {
            task.remoteClusters.map { submit(taskDetail, taskRecord, it, event) }.map { it.get() }
            logger.info("Replica ${event.getFullResourceKey()} completed.")
        } catch (exception: Exception) {
            logger.error("Replica ${event.getFullResourceKey()}} failed: $exception", exception)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EventBasedReplicaJobExecutor::class.java)
    }
}
