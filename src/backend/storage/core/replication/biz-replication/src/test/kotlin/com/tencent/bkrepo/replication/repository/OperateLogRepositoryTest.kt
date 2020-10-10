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

package com.tencent.bkrepo.replication.repository

import com.tencent.bkrepo.replication.model.TOperateLog
import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.pojo.log.ResourceType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import java.time.LocalDateTime

@DataMongoTest
internal class OperateLogRepositoryTest {

    @Autowired
    private lateinit var opLogRepository: OperateLogRepository

    @BeforeEach
    private fun beforeEach() {
        opLogRepository.deleteAll()
    }

    @Test
    fun testDelete() {
        Assertions.assertEquals(0, opLogRepository.findAll().size)
    }

    @Test
    fun testInsert() {
        Assertions.assertEquals(0, opLogRepository.findAll().size)
        opLogRepository.insert(createLog("test", "test", "/test/index.txt"))
        opLogRepository.insert(createLog("test", "test", "/test/index2.txt"))
        opLogRepository.insert(createLog("test", "test", "/test/index3.txt"))
        Assertions.assertEquals(3, opLogRepository.findAll().size)
    }

    private fun createLog(projectId: String, repoName: String, fullPath: String): TOperateLog {
        val description = mapOf("projectId" to projectId, "repoName" to repoName, "request" to "")
        return TOperateLog(
            createdDate = LocalDateTime.now(),
            resourceType = ResourceType.NODE,
            resourceKey = "/$projectId/$repoName/$fullPath",
            operateType = OperateType.CREATE,
            userId = "admin",
            clientAddress = "127.0.0.1",
            description = description
        )
    }
}
