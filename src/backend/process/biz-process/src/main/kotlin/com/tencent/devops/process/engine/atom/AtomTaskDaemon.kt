/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.ErrorType
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

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
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = AtomErrorCode.SYSTEM_DAEMON_INTERRUPTED,
                errorMsg = "守护进程启动出错"
            )
        } catch (e: BuildTaskException) {
            logger.error("Backend BuildTaskException", e)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = e.errorType,
                errorCode = e.errorCode,
                errorMsg = "后台服务任务执行出错"
            )
        } catch (e: Throwable) {
            logger.error("Backend RuntimeException", e)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = AtomErrorCode.SYSTEM_DAEMON_INTERRUPTED,
                errorMsg = "后台服务运行出错"
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AtomTaskDaemon::class.java)
    }
}