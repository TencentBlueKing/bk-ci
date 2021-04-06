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

package com.tencent.bkrepo.replication.repository

import com.tencent.bkrepo.replication.model.TReplicationTaskLog
import com.tencent.bkrepo.replication.pojo.task.ReplicationProgress
import com.tencent.bkrepo.replication.pojo.task.ReplicationStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import java.time.LocalDateTime

@DataMongoTest
internal class TaskLogRepositoryTest {

    @Autowired
    private lateinit var taskLogRepository: TaskLogRepository

    @BeforeEach
    private fun beforeEach() {
        taskLogRepository.deleteAll()
    }

    @Test
    fun testDeleteByTaskKey() {
        Assertions.assertEquals(0, taskLogRepository.findAll().size)
        taskLogRepository.insert(createLog(taskKey = TEST_KEY))
        taskLogRepository.insert(createLog(taskKey = TEST_KEY))
        taskLogRepository.insert(createLog(taskKey = TEST_KEY))
        taskLogRepository.insert(createLog(taskKey = "another"))
        Assertions.assertEquals(4, taskLogRepository.findAll().size)
        taskLogRepository.deleteByTaskKey("non-exist")
        Assertions.assertEquals(4, taskLogRepository.findAll().size)
        taskLogRepository.deleteByTaskKey(TEST_KEY)
        Assertions.assertEquals(1, taskLogRepository.findAll().size)
        taskLogRepository.deleteByTaskKey("another")
        Assertions.assertEquals(0, taskLogRepository.findAll().size)
    }

    @Test
    fun testFindFirstByTaskKeyOrderByStartTimeDesc() {
        taskLogRepository.insert(createLog(startTime = LocalDateTime.now().plusDays(1)))
        taskLogRepository.insert(createLog(startTime = LocalDateTime.now().plusDays(2)))
        val log3 = taskLogRepository.insert(createLog(startTime = LocalDateTime.now().plusDays(3)))
        val log = taskLogRepository.findFirstByTaskKeyOrderByStartTimeDesc(TEST_KEY)
        val anotherLog = taskLogRepository.findFirstByTaskKeyOrderByStartTimeDesc("another")
        Assertions.assertNull(anotherLog)
        Assertions.assertEquals(log3.id!!, log!!.id)
    }

    @Test
    fun testFindByTaskKeyOrderByStartTimeDesc() {
        val log1 = taskLogRepository.insert(createLog(startTime = LocalDateTime.now().plusDays(1)))
        val log2 = taskLogRepository.insert(createLog(startTime = LocalDateTime.now().plusDays(2)))
        val log3 = taskLogRepository.insert(createLog(startTime = LocalDateTime.now().plusDays(3)))
        taskLogRepository.insert(createLog(taskKey = "another"))
        val logList = taskLogRepository.findByTaskKeyOrderByStartTimeDesc(TEST_KEY)
        val anotherLogList = taskLogRepository.findByTaskKeyOrderByStartTimeDesc("another")
        val nonExistLogList = taskLogRepository.findByTaskKeyOrderByStartTimeDesc("non-exist")
        Assertions.assertEquals(3, logList.size)
        Assertions.assertEquals(1, anotherLogList.size)
        Assertions.assertEquals(0, nonExistLogList.size)
        Assertions.assertEquals(log3.id!!, logList[0].id!!)
        Assertions.assertEquals(log2.id!!, logList[1].id!!)
        Assertions.assertEquals(log1.id!!, logList[2].id!!)
    }

    private fun createLog(
        taskKey: String = TEST_KEY,
        status: ReplicationStatus = ReplicationStatus.SUCCESS,
        startTime: LocalDateTime = LocalDateTime.now()
    ): TReplicationTaskLog {
        return TReplicationTaskLog(
            taskKey = taskKey,
            status = status,
            replicationProgress = ReplicationProgress(),
            startTime = startTime,
            endTime = LocalDateTime.now()
        )
    }

    companion object {
        private const val TEST_KEY = "testKey"
    }
}
