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

package com.tencent.devops.process.util

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory

object TempNotifyTemplateUtils {

    private val logger = LoggerFactory.getLogger(TempNotifyTemplateUtils::class.java)

    @Suppress("ALL")
    fun sendUpdateTemplateInstanceNotify(
        client: Client,
        projectId: String,
        receivers: MutableSet<String>,
        instanceListUrl: String,
        successPipelines: List<String>,
        failurePipelines: List<String>
    ) {
        try {
            val projectName = client.get(ServiceProjectResource::class).get(projectId).data!!.projectName
            val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
                templateCode = PipelineNotifyTemplateEnum
                    .PIPELINE_UPDATE_TEMPLATE_INSTANCE_NOTIFY_TEMPLATE.templateCode,
                receivers = receivers,
                cc = receivers,
                titleParams = mapOf(
                    "projectName" to projectName
                ),
                bodyParams = mapOf(
                    "projectName" to projectName,
                    "successPipelineNum" to successPipelines.size.toString(),
                    "successPipelineMsg" to getPipelineShowMsg(successPipelines),
                    "failurePipelineNum" to failurePipelines.size.toString(),
                    "failurePipelineMsg" to getPipelineShowMsg(failurePipelines),
                    "instanceListUrl" to instanceListUrl
                )
            )
            client.get(ServiceNotifyMessageTemplateResource::class)
                .sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
        } catch (ignored: Exception) {
            logger.error("fail to send updateTemplateInstance notify to $receivers : ", ignored)
        }
    }

    private fun getPipelineShowMsg(pipelines: List<String>): String {
        return if (pipelines.size > MAX_PID) {
            JsonUtil.toJson(pipelines.subList(0, MAX_PID))
                .removePrefix(LEFT_BRACE).removeSuffix(RIGHT_BRACE).plus("...")
        } else {
            JsonUtil.toJson(pipelines).removePrefix(LEFT_BRACE).removeSuffix(RIGHT_BRACE)
        }
    }

    private const val MAX_PID = 50
    private const val LEFT_BRACE = "["
    private const val RIGHT_BRACE = "]"
}
