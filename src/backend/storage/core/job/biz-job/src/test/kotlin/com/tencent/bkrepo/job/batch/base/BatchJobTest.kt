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

package com.tencent.bkrepo.job.batch.base

import com.tencent.bkrepo.job.executor.BlockThreadPoolTaskExecutorDecorator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@Import(
    TaskExecutionAutoConfiguration::class
)
@TestPropertySource(locations = ["classpath:bootstrap.properties"])
class BatchJobTest {

    @Autowired
    private lateinit var executor: BlockThreadPoolTaskExecutorDecorator

    @DisplayName("测试最大任务数")
    @Test
    fun maxAvailable() {

 /*       // 启动一个线程占满配额
        thread {
            job.runAsync((0 until executor.maxAvailable), executor = executor) {
                TimeUnit.MILLISECONDS.sleep(100)
            }
        }
        // 停止100ms，让前面的线程有足够的时间去占满配额
        TimeUnit.MILLISECONDS.sleep(100)
        val putOk = AtomicInteger()
        val putNum = 8
        // 启动多个线程去跑异步任务
        repeat(putNum) {
            thread {
                job.runAsync((100000 + it until 100001 + it), false, executor = executor) {}
                putOk.incrementAndGet()
            }
        }
        // 因为前面的任务还未执行完，没有获取到配额，所以此时任务放进队列的数量应该为0
        Assertions.assertEquals(0, putOk.get())
        TimeUnit.MILLISECONDS.sleep(100)
        // 等待一段时间后，任务全部获取到配额
        Assertions.assertEquals(putNum, putOk.get())*/
    }
}
