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

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.artifactory.api.service.ServiceShortUrlResource
import com.tencent.devops.artifactory.pojo.CreateShortUrlRequest
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.element.SendSmsNotifyElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PROJECT_NAME
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class SmsTaskAtom @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val client: Client
) : IAtomTask<SendSmsNotifyElement> {

    override fun getParamElement(task: PipelineBuildTask): SendSmsNotifyElement {
        return JsonUtil.mapTo(task.taskParams, SendSmsNotifyElement::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: SendSmsNotifyElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        val buildId = task.buildId
        val taskId = task.taskId
        if (param.receivers.isEmpty()) {
            buildLogPrinter.addRedLine(
                buildId = buildId,
                message = "The receivers is not init of build",
                tag = taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "The receivers is not init of build"
            )
        }

        val sendDetailFlag = param.detailFlag ?: false

        var bodyStr = parseVariable(param.body, runVariables)
        // 启动短信的查看详情,短信必须是短连接
        if (sendDetailFlag) {
            val url = "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html" +
                "?flag=buildArchive&projectId=${runVariables[PROJECT_NAME]}" +
                "&pipelineId=${runVariables[PIPELINE_ID]}&buildId=$buildId"
            val shortUrl = client.get(ServiceShortUrlResource::class)
                .createShortUrl(CreateShortUrlRequest(url, 24 * 3600 * 30)).data!!
            bodyStr = "$bodyStr\n\n 查看详情：$shortUrl"
        }
        val message = SmsNotifyMessage().apply {
            body = bodyStr
        }
        val receiversStr = parseVariable(param.receivers.joinToString(","), runVariables)
        buildLogPrinter.addLine(
            buildId = buildId,
            message = "send SMS message (${message.body}) to $receiversStr",
            tag = taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )

        message.addAllReceivers(receiversStr.split(",").toSet())
        client.get(ServiceNotifyResource::class).sendSmsNotify(message)
        return AtomResponse(BuildStatus.SUCCEED)
    }
}
