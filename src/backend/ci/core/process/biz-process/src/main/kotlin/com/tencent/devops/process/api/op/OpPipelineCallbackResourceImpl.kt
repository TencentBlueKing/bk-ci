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

package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@RestResource
class OpPipelineCallbackResourceImpl @Autowired constructor(
    private val projectPipelineCallBackService: ProjectPipelineCallBackService
) : OpPipelineCallbackResource {

    override fun enableCallback(projectId: String, events: String?) {
        if (projectId.isEmpty()){
            throw ParamBlankException("Invalid projectId")
        }
        val threadPoolExecutor = ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(1),
            Executors.defaultThreadFactory(),
            ThreadPoolExecutor.AbortPolicy()
        )
        threadPoolExecutor.submit {
            logger.info("OpPipelineCallbackResource:begin enableCallback-----------")
            try {
                batchEnableCallback(projectId = projectId, events = events)
            } catch (e: Exception) {
                logger.warn("OpPipelineCallbackResource：enableCallback failed | $e ")
            } finally {
                threadPoolExecutor.shutdown()
            }
        }
    }

    fun batchEnableCallback(projectId: String?, events: String?) {
        val limit = 1000
        var offset = 0
        do {
            val disableCallbackList = projectPipelineCallBackService.getDisableCallbackList(
                limit = limit,
                offset = offset,
                projectId = projectId,
                events = events
            )
            val pageSize = disableCallbackList.size
            disableCallbackList.forEach {
                projectPipelineCallBackService.enable(it)
            }
            offset += limit
            // 一秒休眠时间
            Thread.sleep(1 * 1000)
        } while (pageSize == limit)
        logger.info("OpPipelineCallbackResource:end enableCallback-----------")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpPipelineCallbackResourceImpl::class.java)
    }
}
