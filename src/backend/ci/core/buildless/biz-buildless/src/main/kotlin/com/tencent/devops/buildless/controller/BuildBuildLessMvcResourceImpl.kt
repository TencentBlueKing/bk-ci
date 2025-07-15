/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.buildless.controller

import com.tencent.devops.buildless.api.builds.BuildBuildLessMvcResource
import com.tencent.devops.buildless.config.BuildLessConfig
import com.tencent.devops.buildless.pojo.BuildLessTask
import com.tencent.devops.buildless.service.BuildLessTaskService
import com.tencent.devops.buildless.utils.ThreadPoolName
import com.tencent.devops.buildless.utils.ThreadPoolUtils
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.async.DeferredResult

@RestResource
class BuildBuildLessMvcResourceImpl @Autowired constructor(
    private val buildLessTaskService: BuildLessTaskService,
    private val buildLessConfig: BuildLessConfig
) : BuildBuildLessMvcResource {

    override fun claimBuildLessTask(containerId: String): DeferredResult<BuildLessTask?> {
        // deferredResult异步非阻塞返回
        val deferredResult = DeferredResult<BuildLessTask?>(30000L)
        deferredResult.onCompletion {
            logger.info("****> container: $containerId claim task deferredResult: ${deferredResult.result}")
        }

        // 异步线程执行业务逻辑
        ThreadPoolUtils.getInstance().getThreadPool(
            poolName = ThreadPoolName.CLAIM_TASK.name,
            corePoolSize = buildLessConfig.maxContainerPool + 10,
            maxPoolSize = buildLessConfig.maxContainerPool + 10
        ).submit {
            buildLessTaskService.claimBuildLessTaskDeferred(containerId, deferredResult)
        }

        return deferredResult
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildBuildLessMvcResourceImpl::class.java)
    }
}
