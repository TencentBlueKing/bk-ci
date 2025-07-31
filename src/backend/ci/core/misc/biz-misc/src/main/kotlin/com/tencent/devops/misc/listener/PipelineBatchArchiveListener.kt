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

package com.tencent.devops.misc.listener

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_CANCEL_FLAG
import com.tencent.devops.common.api.constant.KEY_SEND_MSG_FLAG
import com.tencent.devops.common.api.constant.KEY_USER_ID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.event.listener.EventListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBatchArchiveEvent
import com.tencent.devops.common.service.utils.BkServiceUtil
import com.tencent.devops.common.task.pojo.TaskTypeEnum
import com.tencent.devops.common.task.service.TaskPublishService
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_PROJECT_ID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineBatchArchiveListener @Autowired constructor(
    private val taskPublishService: TaskPublishService
) : EventListener<PipelineBatchArchiveEvent> {

    override fun execute(event: PipelineBatchArchiveEvent) {
        val userId = event.userId
        val projectId = event.projectId
        val pipelineIds = event.pipelineIds
        val cancelFlag = event.cancelFlag
        // 为每个流水线构建任务参数映射表
        val dataList = pipelineIds.map { pipelineId ->
            mapOf(
                KEY_USER_ID to userId,
                KEY_PROJECT_ID to projectId,
                KEY_CANCEL_FLAG to cancelFlag,
                KEY_PIPELINE_ID to pipelineId,
                KEY_SEND_MSG_FLAG to false
            )
        }
        try {
            // 批量发布流水线归档任务
            taskPublishService.publishTasks(
                userId = userId,
                taskType = TaskTypeEnum.PIPELINE_ARCHIVE,
                dataList = dataList,
                expiredInHour = event.expiredInHour,
                targetService = BkServiceUtil.getApplicationName()
            )
        } catch (ignored: Throwable) {
            val msg = "Fail to batch migrate project[$projectId] pipeline data"
            logger.warn(msg, ignored)
            throw ErrorCodeException(
                errorCode = CommonMessageCode.SYSTEM_ERROR,
                defaultMessage = msg
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBatchArchiveListener::class.java)
    }
}
