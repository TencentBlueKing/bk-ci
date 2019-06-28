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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.atom

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 线程池只是做防止出现意外情况长时间运行，实际上并不能真正终止线程
 */
class AtomTaskDaemon(
    private val task: PipelineBuildTask,
    private val buildVariables: Map<String, String>
) : Callable<AtomResponse> {
    override fun call(): AtomResponse {
        return try {
            SpringContextUtil.getBean(IAtomTask::class.java, task.taskAtom).execute(task, buildVariables)
        } catch (e: InterruptedException) {
            logger.error("AtomTaskDaemon InterruptedException", e)
            AtomResponse(BuildStatus.EXEC_TIMEOUT)
        }
    }

    fun run(): AtomResponse {
        val timeout = task.additionalOptions?.timeout
        return if (timeout == null) {
            SpringContextUtil.getBean(IAtomTask::class.java, task.taskAtom).execute(task, buildVariables)
        } else {
            val taskDaemon = AtomTaskDaemon(task, buildVariables)
            val executor = Executors.newCachedThreadPool()
            val f1 = executor.submit(taskDaemon)
            try {
                f1.get(timeout, TimeUnit.MINUTES)
            } catch (e: TimeoutException) {
                logger.error("AtomTaskDaemon run timeout, timeout:$timeout", e)
                throw TimeoutException("插件执行超时, 超时时间:${timeout}分钟")
            } finally {
                executor.shutdownNow()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AtomTaskDaemon::class.java)
    }
}