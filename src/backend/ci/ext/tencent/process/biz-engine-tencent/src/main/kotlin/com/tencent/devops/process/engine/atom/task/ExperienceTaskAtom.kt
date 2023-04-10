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

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.ExperienceElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.experience.api.service.ServiceExperienceResource
import com.tencent.devops.experience.constant.ProductCategoryEnum
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import com.tencent.devops.experience.pojo.NotifyType
import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import com.tencent.devops.experience.pojo.enums.TimeType
import com.tencent.devops.process.constant.ProcessMessageCode.BK_EXPERIENCE_PATH_EMPTY
import com.tencent.devops.process.constant.ProcessMessageCode.BK_FILE_NOT_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.BK_INCORRECT_NOTIFICATION_METHOD
import com.tencent.devops.process.constant.ProcessMessageCode.BK_VERSION_EXPERIENCE_CREATED_SUCCESSFULLY
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ExperienceTaskAtom @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<ExperienceElement> {

    override fun getParamElement(task: PipelineBuildTask): ExperienceElement {
        return JsonUtil.mapTo(task.taskParams, ExperienceElement::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: ExperienceElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        val buildId = task.buildId
        val taskId = task.taskId
        val containerId = task.containerHashId
        if (param.path.isBlank()) {
            buildLogPrinter.addRedLine(buildId,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_EXPERIENCE_PATH_EMPTY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ), taskId, containerId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = MessageUtil.getMessageByLocale(
                    messageCode = BK_EXPERIENCE_PATH_EMPTY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
        }

        if (param.notifyTypes.isEmpty()) {
            buildLogPrinter.addRedLine(buildId,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_INCORRECT_NOTIFICATION_METHOD,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ), taskId, containerId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = MessageUtil.getMessageByLocale(
                    messageCode = BK_INCORRECT_NOTIFICATION_METHOD,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
        }

        val userId = runVariables[PIPELINE_START_USER_ID]!!
        val projectId = task.projectId
        val pipelineId = task.pipelineId

        val experienceGroups = if (param.experienceGroups.isEmpty()) {
            emptySet()
        } else {
            parseVariable(param.experienceGroups.joinToString(","), runVariables).split(",").toSet()
        }
        val innerUsers = if (param.innerUsers.isEmpty()) {
            emptySet()
        } else {
            parseVariable(param.innerUsers.joinToString(","), runVariables).split(",").toSet()
        }
        val outerUsers = if (param.outerUsers.isEmpty()) {
            ""
        } else {
            parseVariable(param.outerUsers, runVariables)
        }

        val path = parseVariable(param.path, runVariables)
        val customized = param.customized
        val timeType = TimeType.valueOf(param.timeType)

        val expireDate = if (timeType == TimeType.ABSOLUTE) {
            param.expireDate
        } else {
            LocalDateTime.now().plusDays(param.expireDate).timestamp()
        }
        val notifyTypes = parseVariable(param.notifyTypes.joinToString(","), runVariables).split(",")
        val enableGroupId = param.enableGroupId ?: true
        val groupId = param.groupId

        val realPath =
            if (customized) "/${path.removePrefix("/")}" else "/$pipelineId/$buildId/${path.removePrefix("/")}"
        val fileName = File(realPath).name

        val artifactoryType = if (customized) com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.CUSTOM_DIR
        else com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.PIPELINE
        if (!client.get(ServiceArtifactoryResource::class).check(userId, projectId, artifactoryType, realPath).data!!) {
            buildLogPrinter.addRedLine(buildId,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_FILE_NOT_EXIST,
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf(path)
                ), taskId, containerId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = MessageUtil.getMessageByLocale(
                    messageCode = BK_FILE_NOT_EXIST,
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf(path)
                )
            )
        }

        val expArtifactoryType = if (customized) ArtifactoryType.CUSTOM_DIR else ArtifactoryType.PIPELINE
        val notifyTypeSet = notifyTypes.map { NotifyType.valueOf(it) }.toSet()
        val experience = ExperienceServiceCreate(
            path = realPath,
            artifactoryType = expArtifactoryType,
            expireDate = expireDate,
            experienceGroups = experienceGroups,
            innerUsers = innerUsers,
            outerUsers = emptySet(),
            notifyTypes = notifyTypeSet,
            enableWechatGroups = enableGroupId,
            wechatGroups = groupId,
            description = "",
            experienceName = projectId,
            versionTitle = projectId,
            categoryId = ProductCategoryEnum.LIFE.id,
            productOwner = emptyList()
        )
        client.get(ServiceExperienceResource::class).create(userId, projectId, experience)

        buildLogPrinter.addLine(buildId,
            MessageUtil.getMessageByLocale(
                messageCode = BK_VERSION_EXPERIENCE_CREATED_SUCCESSFULLY,
                language = I18nUtil.getDefaultLocaleLanguage(),
                params = arrayOf(fileName)
            ), taskId, containerId, task.executeCount ?: 1)

        return AtomResponse(BuildStatus.SUCCEED)
    }
}
