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

package com.tencent.devops.dispatch.bcs.common

import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.engine.common.VMUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class LogsPrinter @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter
) {

    companion object {
        private val logger = LoggerFactory.getLogger(LogsPrinter::class.java)
    }

    fun printLogs(dispatchMessage: DispatchMessage, message: String) {
        with(dispatchMessage) {
            try {
                log(
                    buildId = buildId,
                    builderHashId = containerHashId,
                    vmSeqId = vmSeqId,
                    message = message,
                    executeCount = executeCount
                )
            } catch (e: Throwable) {
                // 日志有问题就不打日志了，不能影响正常流程
                logger.error("", e)
            }
        }
    }

    fun log(buildId: String, builderHashId: String?, vmSeqId: String, message: String, executeCount: Int?) {
        buildLogPrinter.addLine(
            buildId = buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(vmSeqId),
            jobId = builderHashId,
            executeCount = executeCount ?: 1
        )
    }

    fun logRed(buildId: String, builderHashId: String?, vmSeqId: String, message: String, executeCount: Int?) {
        buildLogPrinter.addRedLine(
            buildId = buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(vmSeqId),
            jobId = builderHashId,
            executeCount = executeCount ?: 1
        )
    }
}
