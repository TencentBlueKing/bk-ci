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

package com.tencent.bkrepo.replication.service

import com.tencent.bkrepo.common.job.JobAutoConfiguration
import com.tencent.bkrepo.common.service.async.AsyncConfiguration
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.replication.config.ReplicationConfiguration
import com.tencent.bkrepo.replication.job.ReplicationJobBean
import com.tencent.bkrepo.replication.pojo.request.ReplicationTaskCreateRequest
import com.tencent.bkrepo.replication.pojo.setting.ExecutionPlan
import com.tencent.bkrepo.replication.pojo.setting.RemoteClusterInfo
import com.tencent.bkrepo.replication.pojo.setting.ReplicationSetting
import com.tencent.bkrepo.replication.pojo.task.ReplicationStatus
import com.tencent.bkrepo.replication.pojo.task.ReplicationTaskInfo
import com.tencent.bkrepo.replication.pojo.task.ReplicationType
import com.tencent.bkrepo.replication.repository.TaskRepository
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
import java.time.LocalDateTime

@DataMongoTest(properties = ["logging.level.com.tencent=DEBUG"])
@Import(
    TaskService::class,
    ScheduleService::class,
    TaskReloadService::class,
    SpringContextUtils::class,
    JobAutoConfiguration::class,
    ReplicationConfiguration::class,
    AsyncConfiguration::class,
    TaskLogService::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
private class TaskReloadServiceTest {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @MockBean
    private lateinit var replicationJobBean: ReplicationJobBean

    @BeforeAll
    fun setUp() {
        `when`(replicationJobBean.execute(ArgumentMatchers.anyString())).then {
            println("job execute")
        }
    }

    @BeforeEach
    fun beforeEach() {
        taskRepository.deleteAll()
    }

    @Test
    fun `should execute immediately`() {
        val task = createTask(ReplicationType.FULL, ExecutionPlan(executeImmediately = true))
        Assertions.assertEquals(ReplicationStatus.WAITING, taskService.detail(task.key)!!.status)
        verify(replicationJobBean, times(0)).execute(task.id)
        Thread.sleep(10 * 1000)
        verify(replicationJobBean, times(1)).execute(task.id)
    }

    @Test
    fun `should execute at specific time`() {
        val executeTime = LocalDateTime.now().plusSeconds(10)
        val executionPlan = ExecutionPlan(executeImmediately = false, executeTime = executeTime)
        val task = createTask(ReplicationType.FULL, executionPlan)
        Thread.sleep(9 * 1000)
        verify(replicationJobBean, times(0)).execute(task.id)
        Thread.sleep(1 * 1000)
        verify(replicationJobBean, times(1)).execute(task.id)
    }

    @Test
    fun `should not execute after delete task`() {
        val executeTime = LocalDateTime.now().plusSeconds(25)
        val executionPlan = ExecutionPlan(executeImmediately = false, executeTime = executeTime)
        val task = createTask(ReplicationType.FULL, executionPlan)
        Thread.sleep(11 * 1000)
        taskService.delete(task.key)
        Thread.sleep(11 * 1000)
        verify(replicationJobBean, times(0)).execute(task.id)
    }

    @Test
    fun `should execute repeat by cron expression`() {
        val cronExpression = "0/1 * * * * ?"
        val executionPlan = ExecutionPlan(executeImmediately = false, cronExpression = cronExpression)
        val task = createTask(ReplicationType.FULL, executionPlan)
        Thread.sleep(16 * 1000)
        verify(replicationJobBean, atLeast(5)).execute(task.id)
    }

    private fun createTask(type: ReplicationType, executionPlan: ExecutionPlan = ExecutionPlan()): ReplicationTaskInfo {
        val remoteClusterInfo = RemoteClusterInfo(url = "", username = "", password = "")
        val request = ReplicationTaskCreateRequest(
            type = type,
            includeAllProject = true,
            localProjectId = "localProjectId",
            localRepoName = "localRepoName",
            remoteProjectId = "remoteProjectId",
            remoteRepoName = "remoteRepoName",
            setting = ReplicationSetting(remoteClusterInfo = remoteClusterInfo, executionPlan = executionPlan),
            validateConnectivity = false
        )
        return taskService.create("system", request)
    }
}
