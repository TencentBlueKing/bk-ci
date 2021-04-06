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

package com.tencent.bkrepo.replication.service

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.replication.model.TReplicationTask
import com.tencent.bkrepo.replication.pojo.request.ReplicationTaskCreateRequest
import com.tencent.bkrepo.replication.pojo.setting.RemoteClusterInfo
import com.tencent.bkrepo.replication.pojo.setting.ReplicationSetting
import com.tencent.bkrepo.replication.pojo.task.ReplicationStatus
import com.tencent.bkrepo.replication.pojo.task.ReplicationType
import com.tencent.bkrepo.replication.repository.TaskRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

@DataMongoTest
@Import(TaskService::class)
class TaskServiceTest {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @BeforeEach
    fun beforeEach() {
        taskRepository.deleteAll()
    }

    @Test
    fun `list relative task`() {
        createIncrementalTask()
        createIncrementalTask()
        createIncrementalTask(false, "p1")
        createIncrementalTask(false, "p1")
        createIncrementalTask(false, "p2")
        createIncrementalTask(false, "p1", "r1")
        createIncrementalTask(false, "p1", "r2")
        createIncrementalTask(false, "p2", "r1")

        val list1 = taskService.listRelativeTask(
            type = ReplicationType.INCREMENTAL,
            localProjectId = "p1",
            localRepoName = "r1"
        )
        Assertions.assertEquals(5, list1.size)

        val list2 = taskService.listRelativeTask(
            type = ReplicationType.FULL,
            localProjectId = "p1",
            localRepoName = "r1"
        )
        Assertions.assertEquals(0, list2.size)
    }

    @Test
    fun `list undo full task`() {
        createIncrementalTask()
        insertFullTask("testTask1", ReplicationStatus.WAITING)
        insertFullTask("testTask2", ReplicationStatus.SUCCESS)
        insertFullTask("testTask3", ReplicationStatus.INTERRUPTED)
        insertFullTask("testTask4", ReplicationStatus.PAUSED)
        insertFullTask("testTask5", ReplicationStatus.REPLICATING)
        insertFullTask("testTask6", ReplicationStatus.FAILED)

        val undoFullTaskList = taskService.listUndoFullTask()
        Assertions.assertEquals(2, undoFullTaskList.size)
        undoFullTaskList.forEach {
            Assertions.assertTrue(it.status in ReplicationStatus.UNDO_STATUS_SET)
            Assertions.assertEquals(it.type, ReplicationType.FULL)
        }
    }

    @Test
    fun `delete task`() {
        insertFullTask("test1", ReplicationStatus.WAITING)

        val dbTask = taskService.detail("test1")
        Assertions.assertNotNull(dbTask)
        Assertions.assertNull(taskService.detail("test2"))

        taskService.delete("test1")
        Assertions.assertNull(taskService.detail("test1"))

        assertThrows<ErrorCodeException> {
            taskService.delete("test1")
        }
    }

    @Test
    fun `should throw exception when delete non exist task`() {
        assertThrows<ErrorCodeException> {
            taskService.delete("test1")
        }
    }

    private fun createIncrementalTask(
        includeAllProject: Boolean = true,
        localProjectId: String? = null,
        localRepoName: String? = null,
        remoteProjectId: String? = null,
        remoteRepoName: String? = null
    ) {
        val remoteClusterInfo = RemoteClusterInfo(url = "", username = "", password = "")
        val request = ReplicationTaskCreateRequest(
            type = ReplicationType.INCREMENTAL,
            includeAllProject = includeAllProject,
            localProjectId = localProjectId,
            localRepoName = localRepoName,
            remoteProjectId = remoteProjectId,
            remoteRepoName = remoteRepoName,
            setting = ReplicationSetting(remoteClusterInfo = remoteClusterInfo),
            validateConnectivity = false
        )
        taskService.create("system", request)
    }

    private fun insertFullTask(
        taskKey: String,
        status: ReplicationStatus = ReplicationStatus.WAITING
    ): TReplicationTask {
        val remoteClusterInfo = RemoteClusterInfo(url = "", username = "", password = "")
        val task = TReplicationTask(
            key = taskKey,
            type = ReplicationType.FULL,
            createdBy = "system",
            createdDate = LocalDateTime.now(),
            lastModifiedBy = "system",
            lastModifiedDate = LocalDateTime.now(),
            includeAllProject = true,
            localProjectId = "localProjectId",
            localRepoName = "localRepoName",
            remoteProjectId = "remoteProjectId",
            remoteRepoName = "remoteRepoName",
            setting = ReplicationSetting(remoteClusterInfo = remoteClusterInfo),
            status = status
        )
        return taskRepository.insert(task)
    }
}
