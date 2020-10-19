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

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.constant.StringPool.uniqueId
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import

@DisplayName("文件引用计数测试")
@DataMongoTest
@Import(
    FileReferenceDao::class
)
class FileReferenceServiceTest @Autowired constructor(
    private val fileReferenceService: FileReferenceService
) : ServiceBaseTest() {

    @MockBean
    private lateinit var repositoryService: RepositoryService

    @Test
    fun testIncrementAndDecrement() {
        val sha256 = uniqueId()
        Assertions.assertEquals(0, fileReferenceService.count(sha256, null))
        Assertions.assertTrue(fileReferenceService.increment(sha256, null))
        Assertions.assertEquals(1, fileReferenceService.count(sha256, null))
        Assertions.assertTrue(fileReferenceService.increment(sha256, null))
        Assertions.assertEquals(2, fileReferenceService.count(sha256, null))

        Assertions.assertTrue(fileReferenceService.decrement(sha256, null))
        Assertions.assertEquals(1, fileReferenceService.count(sha256, null))
        Assertions.assertTrue(fileReferenceService.decrement(sha256, null))
        Assertions.assertEquals(0, fileReferenceService.count(sha256, null))
        Assertions.assertFalse(fileReferenceService.decrement(sha256, null))
        Assertions.assertEquals(0, fileReferenceService.count(sha256, null))
    }
}
