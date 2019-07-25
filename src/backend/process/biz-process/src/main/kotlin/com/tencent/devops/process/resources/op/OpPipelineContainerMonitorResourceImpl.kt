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

package com.tencent.devops.process.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.op.OpPipelineContainerMonitorResource
import com.tencent.devops.process.engine.service.PipelineContainerMonitorService
import com.tencent.devops.process.pojo.PipelineContainerMonitor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * deng
 * 2019-01-11
 */
@RestResource
class OpPipelineContainerMonitorResourceImpl @Autowired constructor(private val monitorService: PipelineContainerMonitorService) :
    OpPipelineContainerMonitorResource {
    override fun update(userId: String, monitor: PipelineContainerMonitor): Result<Boolean> {
        logger.info("Update the monitor $monitor by user $userId")
        return Result(monitorService.update(monitor))
    }

    override fun delete(userId: String, osType: VMBaseOS, buildType: BuildType): Result<Boolean> {
        logger.info("Delete the monitor [$osType|$buildType] by user $userId")
        return Result(monitorService.delete(osType, buildType))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpPipelineContainerMonitorResourceImpl::class.java)
    }
}