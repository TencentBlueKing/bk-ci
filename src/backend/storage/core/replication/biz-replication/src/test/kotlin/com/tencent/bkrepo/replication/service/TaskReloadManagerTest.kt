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

package com.tencent.bkrepo.replication.service

import com.tencent.bkrepo.common.job.JobAutoConfiguration
import com.tencent.bkrepo.common.service.async.AsyncConfiguration
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.replication.config.ReplicationConfigurer
import com.tencent.bkrepo.replication.dao.ReplicaTaskDao
import com.tencent.bkrepo.replication.pojo.request.ReplicaObjectType
import com.tencent.bkrepo.replication.pojo.request.ReplicaType
import com.tencent.bkrepo.replication.pojo.task.ReplicaStatus
import com.tencent.bkrepo.replication.pojo.task.ReplicaTaskInfo
import com.tencent.bkrepo.replication.pojo.task.request.ReplicaTaskCreateRequest
import com.tencent.bkrepo.replication.pojo.task.setting.ExecutionPlan
import com.tencent.bkrepo.replication.pojo.task.setting.ReplicaSetting
import com.tencent.bkrepo.replication.replica.schedule.ReplicaTaskScheduler
import com.tencent.bkrepo.replication.replica.schedule.ScheduledReplicaJobExecutor
import com.tencent.bkrepo.replication.replica.schedule.TaskReloadManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.query.Query
import java.time.LocalDateTime

@DataMongoTest(properties = ["logging.level.com.tencent=DEBUG"])
@Import(
    ReplicaTaskService::class,
    ReplicaTaskScheduler::class,
    TaskReloadManager::class,
    SpringContextUtils::class,
    JobAutoConfiguration::class,
    ReplicationConfigurer::class,
    AsyncConfiguration::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
private class TaskReloadManagerTest {

    @Autowired
    private lateinit var replicaTaskService: ReplicaTaskService

    @Autowired
    private lateinit var replicaTaskDao: ReplicaTaskDao

    @MockBean
    private lateinit var scheduledReplicaJobExecutor: ScheduledReplicaJobExecutor

    @BeforeAll
    fun setUp() {
        `when`(scheduledReplicaJobExecutor.execute(ArgumentMatchers.anyString())).then {
            println("job execute")
        }
    }

    @BeforeEach
    fun beforeEach() {
        replicaTaskDao.remove(Query())
    }

    @Test
    fun `should execute immediately`() {
        val task = createTask(ReplicaType.SCHEDULED, ExecutionPlan(executeImmediately = true))
        Assertions.assertEquals(
            ReplicaStatus.WAITING,
            replicaTaskService.getByTaskKey(task.key).lastExecutionStatus
        )
        verify(scheduledReplicaJobExecutor, times(0)).execute(task.id)
        Thread.sleep(10 * 1000)
        verify(scheduledReplicaJobExecutor, times(1)).execute(task.id)
    }

    @Test
    fun `should execute at specific time`() {
        val executeTime = LocalDateTime.now().plusSeconds(10)
        val executionPlan = ExecutionPlan(executeImmediately = false, executeTime = executeTime)
        val task = createTask(ReplicaType.SCHEDULED, executionPlan)
        Thread.sleep(9 * 1000)
        verify(scheduledReplicaJobExecutor, times(0)).execute(task.id)
        Thread.sleep(1 * 1000)
        verify(scheduledReplicaJobExecutor, times(1)).execute(task.id)
    }

    @Test
    fun `should not execute after delete task`() {
        val executeTime = LocalDateTime.now().plusSeconds(25)
        val executionPlan = ExecutionPlan(executeImmediately = false, executeTime = executeTime)
        val task = createTask(ReplicaType.SCHEDULED, executionPlan)
        Thread.sleep(11 * 1000)
        replicaTaskService.deleteByTaskKey(task.key)
        Thread.sleep(11 * 1000)
        verify(scheduledReplicaJobExecutor, times(0)).execute(task.id)
    }

    @Test
    fun `should execute repeat by cron expression`() {
        val cronExpression = "0/1 * * * * ?"
        val executionPlan = ExecutionPlan(executeImmediately = false, cronExpression = cronExpression)
        val task = createTask(ReplicaType.SCHEDULED, executionPlan)
        Thread.sleep(16 * 1000)
        verify(scheduledReplicaJobExecutor, atLeast(5)).execute(task.id)
    }

    private fun createTask(type: ReplicaType, executionPlan: ExecutionPlan = ExecutionPlan()): ReplicaTaskInfo {
        val setting = ReplicaSetting(executionPlan = executionPlan)
        val request = ReplicaTaskCreateRequest(
            name = "123",
            localProjectId = "111",
            replicaObjectType = ReplicaObjectType.PACKAGE,
            replicaTaskObjects = listOf(),
            replicaType = type,
            setting = setting,
            remoteClusterIds = setOf(),
            enabled = true
        )
        return replicaTaskService.create(request) as ReplicaTaskInfo
    }
}
