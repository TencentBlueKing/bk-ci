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

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.plugin.api.ServiceMigCDNResource
import com.tencent.devops.plugin.pojo.migcdn.MigCDNUploadParam
import com.tencent.devops.common.pipeline.element.MigCDNPushFileElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultSuccessAtomResponse
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.CommonUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class MigCDNPushFileTaskAtom @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<MigCDNPushFileElement> {

    override fun getParamElement(task: PipelineBuildTask): MigCDNPushFileElement {
        return JsonUtil.mapTo(task.taskParams, MigCDNPushFileElement::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: MigCDNPushFileElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        val ticketId = parseVariable(param.ticketId, runVariables)
        val destFileDir = parseVariable(param.destFileDir, runVariables)
        val needUnzip = param.needUnzip
        val fileSource = parseVariable(param.fileSource, runVariables)
        val filePath = parseVariable(param.filePath, runVariables)

        val projectId = task.projectId
        val buildId = task.buildId
        val pipelineId = task.pipelineId
        val userId = task.starter

        val ticketsMap = CommonUtils.getCredential(client, projectId, ticketId, CredentialType.APPID_SECRETKEY)

        val uploadParams = MigCDNUploadParam(
            userId,
            MigCDNUploadParam.CommonParam(
                ticketsMap["v1"] as String,
                ticketsMap["v2"] as String,
                destFileDir,
                if (needUnzip) 1 else 0
            ),
            ArtifactorySearchParam(
                projectId,
                pipelineId,
                buildId,
                filePath,
                fileSource == "CUSTOMIZE",
                task.executeCount ?: 1,
                task.taskId
            )
        )

        buildLogPrinter.addLine(buildId, "开始上传对应文件到CDN...",
            task.taskId, task.containerHashId, task.executeCount ?: 1)
        buildLogPrinter.addLine(buildId, "匹配文件中: ${uploadParams.fileParams.regexPath}($fileSource)",
            task.taskId, task.containerHashId, task.executeCount ?: 1)
        val pushFile = client.get(ServiceMigCDNResource::class).pushFile(uploadParams)
        buildLogPrinter.addLine(buildId, "上传对应文件到CDN结束! result=$pushFile",
            task.taskId, task.containerHashId, task.executeCount ?: 1)
        return defaultSuccessAtomResponse
    }
}
