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

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.client.JfrogClient
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.element.CloudStoneElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.service.CloudStoneService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class CloudStoneTaskAtom @Autowired constructor(
    private val cloudStoneService: CloudStoneService,
    private val buildLogPrinter: BuildLogPrinter,
    private val client: Client,
    private val commonConfig: CommonConfig,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray,
    private val bkRepoClient: BkRepoClient
) : IAtomTask<CloudStoneElement> {
    override fun getParamElement(task: PipelineBuildTask): CloudStoneElement {
        return JsonUtil.mapTo(task.taskParams, CloudStoneElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: CloudStoneElement, runVariables: Map<String, String>): AtomResponse {
        val executeCount = task.executeCount ?: 1
        val sourcePath = parseVariable(param.sourcePath, runVariables)
        val isCustom = param.sourceType.name == "CUSTOMIZE"
        val releaseNote = if (param.releaseNote != null) parseVariable(param.releaseNote, runVariables) else ""
        val targetPath = parseVariable(param.targetPath, runVariables)
        val versionId = parseVariable(param.versionId, runVariables)
        val fileType = parseVariable(param.fileType, runVariables)
        val customFiled = if (param.customFiled != null) parseVariable(param.customFiled!!.joinToString(","), runVariables) else ""

        val projectId = task.projectId
        val buildId = task.buildId
        val taskId = task.taskId
        val pipelineId = task.pipelineId
        val buildNo = runVariables[PIPELINE_BUILD_NUM]!!.toInt()
        val userId = runVariables[PIPELINE_START_USER_ID]!!

        val destPath = Files.createTempDirectory("cloudStone_").toAbsolutePath().toString()
        val useBkRepo = repoGray.isGray(projectId, redisOperation)
        logger.info("use bkrepo: $useBkRepo")
        val matchFiles = if (useBkRepo) {
            bkRepoClient.downloadFileByPattern(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                repoName = if (isCustom) "custom" else "pipeline",
                pathPattern = sourcePath,
                destPath = destPath
            )
        } else {
            JfrogClient(commonConfig.devopsHostGateway!!, projectId, pipelineId, buildId).downloadFile(sourcePath, isCustom, destPath)
        }
        if (matchFiles.isEmpty()) throw OperationException("There is 0 file find in $sourcePath(custom: $isCustom)")
        val appId = client.get(ServiceProjectResource::class).get(task.projectId).data?.ccAppId?.toInt()
            ?: run {
                buildLogPrinter.addLine(task.buildId, "找不到绑定配置平台的业务ID/can not found CC Business ID", task.taskId,
                    task.containerHashId,
                    executeCount
                )
                return defaultFailAtomResponse
            }
        matchFiles.forEach { file ->
            val result = cloudStoneService.postFile(userId,
                appId, pipelineId, buildNo, releaseNote, file, targetPath, versionId, fileType, customFiled)
            if (result.first) {
                logger.info("Upload to cloudStone success. file:${file.name}")
                buildLogPrinter.addLine(buildId, "上传云石成功，文件：${result.second}",
                    taskId, task.containerHashId, task.executeCount ?: 1)
            } else {
                logger.info("Upload to cloudStone failed. msg:${result.second}")
                buildLogPrinter.addRedLine(buildId, "上传云石失败: ${result.second}",
                    taskId, task.containerHashId, task.executeCount ?: 1)
                return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorMsg = "上传云石失败: ${result.second}"
                )
            }
        }
        return AtomResponse(BuildStatus.SUCCEED)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CloudStoneTaskAtom::class.java)
    }
}
