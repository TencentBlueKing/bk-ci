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

package com.tencent.devops.process.api.service.callback

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.StopWatch
import java.time.LocalDateTime
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@RestResource
class ServicePipelineCallbackResourceImpl @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService
) : ServicePipelineCallbackResource {
    private val logger = LoggerFactory.getLogger(ServicePipelineCallbackResourceImpl::class.java)

    @Value("\${deletedPipelineStoreTime:30}")
    private val deletedPipelineStoreTime: Int = 30

    //最多5线程，用完立即销毁
    private val executorService = ThreadPoolExecutor(0, 5, 0, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

    override fun clear(): Result<Boolean> {
        executorService.submit {
            val watch = StopWatch("clear deleted Task")
            try {
                logger.info("clear pipelines deleted before $deletedPipelineStoreTime days")
                val deleteTime = LocalDateTime.now().minusDays(deletedPipelineStoreTime.toLong())
                //查出所有被删除超过过期时间的流水线
                val deletedPipelines = pipelineRepositoryService.listDeletePipelineBefore(deleteTime)
                val deletedPipelineIds = deletedPipelines.map { it.pipelineId }
                logger.info("deletedPipelineIds=${deletedPipelineIds.size},(${deletedPipelineIds.joinToString()})")
                //依次删除
                deletedPipelines.forEach {
                    watch.start("${it.pipelineId} delete Task")
                    pipelineRepositoryService.deletePipelineHardly(it.creator, it.projectId, it.pipelineId, ChannelCode.BS)
                    watch.stop()
                }
            } catch (e: Exception) {
                logger.error("fail to clear deleted pipelines", e)
            } finally {
                logger.info("Clear Deleted Task Time Consuming:$watch")
            }
        }
        return Result(true)
    }
}
