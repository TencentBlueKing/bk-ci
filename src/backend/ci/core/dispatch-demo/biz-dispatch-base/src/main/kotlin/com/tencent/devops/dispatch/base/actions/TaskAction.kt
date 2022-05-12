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

package com.tencent.devops.dispatch.base.actions

import com.tencent.devops.dispatch.base.client.BcsTaskClient
import com.tencent.devops.dispatch.base.pojo.DispatchBuildStatusEnum
import com.tencent.devops.dispatch.base.pojo.DispatchBuildStatusResp
import com.tencent.devops.dispatch.base.pojo.bcs.BcsTaskStatusEnum
import com.tencent.devops.dispatch.base.pojo.bcs.getCodeMessage
import com.tencent.devops.dispatch.base.pojo.bcs.isFailed
import com.tencent.devops.dispatch.base.pojo.bcs.isRunning
import com.tencent.devops.dispatch.base.pojo.bcs.isSuccess
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TaskAction @Autowired constructor(
    private val bcsTaskClient: BcsTaskClient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TaskAction::class.java)
    }

    fun getTaskStatus(userId: String, taskId: String): com.tencent.devops.dispatch.base.pojo.DispatchBuildStatusResp {
        val taskResponse = bcsTaskClient.getTasksStatus(userId, taskId)
        val status = BcsTaskStatusEnum.realNameOf(taskResponse.data?.status)
        if (taskResponse.isNotOk() || taskResponse.data == null) {
            // 创建失败
            val msg = "${taskResponse.message ?: taskResponse.getCodeMessage()}"
            logger.error("Execute task: $taskId failed, actionCode is ${taskResponse.code}, msg: $msg")
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, msg)
        }
        // 请求成功但是任务失败
        if (status != null && status.isFailed()) {
            return DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, taskResponse.data!!.message)
        }
        return when {
            status!!.isRunning() -> DispatchBuildStatusResp(DispatchBuildStatusEnum.running.name)
            status.isSuccess() -> {
                DispatchBuildStatusResp(DispatchBuildStatusEnum.succeeded.name)
            }
            else -> DispatchBuildStatusResp(DispatchBuildStatusEnum.failed.name, status.message)
        }
    }
}
