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

import com.tencent.devops.common.api.constant.I18NConstant.BK_MATCHING_FILE
import com.tencent.devops.common.api.constant.I18NConstant.BK_START_UPLOADING_CORRESPONDING_FILES
import com.tencent.devops.common.api.constant.I18NConstant.BK_UPLOAD_CORRESPONDING_FILE
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.ZhiyunPushFileElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.plugin.api.ServiceZhiyunResource
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunUploadParam
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_URL
import org.apache.poi.util.StringUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ZhiYunPushFileTaskAtom @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<ZhiyunPushFileElement> {
    override fun getParamElement(task: PipelineBuildTask): ZhiyunPushFileElement {
        return JsonUtil.mapTo(task.taskParams, ZhiyunPushFileElement::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: ZhiyunPushFileElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        val product = parseVariable(param.product, runVariables)
        val packageName = parseVariable(param.packageName, runVariables)
        val description = parseVariable(param.description, runVariables)
        val clean = param.clean
        val fileSource = parseVariable(param.fileSource, runVariables)
        val filePath = parseVariable(param.filePath, runVariables)

        val projectId = task.projectId
        val buildId = task.buildId
        val pipelineId = task.pipelineId
        val userId = task.starter
        val codeRepoUrl = getCodeRepoUrl(runVariables)
        logger.info("codeRepoUrl is $codeRepoUrl")
        val uploadParams = ZhiyunUploadParam(
            userId,
            ZhiyunUploadParam.CommonParam(
                product = product,
                name = packageName,
                author = userId,
                description = description,
                clean = clean.toString(),
                buildId = buildId,
                codeUrl = codeRepoUrl
            ),
            ArtifactorySearchParam(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                regexPath = filePath,
                custom = fileSource == "CUSTOMIZE",
                executeCount = task.executeCount ?: 1,
                elementId = task.taskId
            )
        )

        buildLogPrinter.addLine(
            buildId = buildId,
            message = MessageUtil.getMessageByLocale(
                messageCode = BK_START_UPLOADING_CORRESPONDING_FILES,
                language = I18nUtil.getDefaultLocaleLanguage()
            ) + "【<a target='_blank' href='http://ccc.oa.com/package/versions?innerurl=${URLEncoder.encode(
                "http://yun.ccc.oa.com/index.php/package/versions?product=$product&package=$packageName",
                "UTF-8"
            )}'>查看详情</a>】",
            tag = task.taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = buildId,
            message = MessageUtil.getMessageByLocale(
                messageCode = BK_MATCHING_FILE,
                language = I18nUtil.getDefaultLocaleLanguage()
            ) + "${uploadParams.fileParams.regexPath}($fileSource)",
            tag = task.taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
        val versions = client.getWithoutRetry(ServiceZhiyunResource::class).pushFile(uploadParams).data
            ?: throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "0 file send to zhiyun"
            )
        val ver = versions.lastOrNull() ?: throw TaskExecuteException(
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
            errorType = ErrorType.USER,
            errorMsg = "0 file send to zhiyun"
        )
        buildLogPrinter.addLine(
            buildId = buildId,
            message = MessageUtil.getMessageByLocale(
                messageCode = BK_UPLOAD_CORRESPONDING_FILE,
                language = I18nUtil.getLanguage(userId)
            ),
            tag = task.taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
        return AtomResponse(BuildStatus.SUCCEED, mapOf("bk_zhiyun_version_$packageName" to ver))
    }

    private fun getCodeRepoUrl(runVariables: Map<String, String>): String? {
        val codeRepoUrlMap = runVariables.filter { it.key.startsWith(PIPELINE_MATERIAL_URL) }.toMap()
        if (codeRepoUrlMap.isEmpty()) {
            logger.info("No code repo url in variables")
            return ""
        }
        return StringUtil.join(codeRepoUrlMap.values.toTypedArray(), ";")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ZhiYunPushFileTaskAtom::class.java)
    }
}
