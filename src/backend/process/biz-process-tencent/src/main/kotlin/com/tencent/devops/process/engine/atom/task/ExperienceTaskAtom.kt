package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.experience.api.service.ServiceExperienceResource
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import com.tencent.devops.experience.pojo.NotifyType
import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import com.tencent.devops.experience.pojo.enums.TimeType
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.common.pipeline.element.ExperienceElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import org.springframework.amqp.rabbit.core.RabbitTemplate
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
    private val rabbitTemplate: RabbitTemplate
) : IAtomTask<ExperienceElement> {

    override fun getParamElement(task: PipelineBuildTask): ExperienceElement {
        return JsonUtil.mapTo(task.taskParams, ExperienceElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: ExperienceElement, runVariables: Map<String, String>): AtomResponse {
        val buildId = task.buildId
        val taskId = task.taskId
        if (param.path.isBlank()) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "体验路径为空", taskId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "体验路径为空"
            )
        }

        if (param.notifyTypes.isEmpty()) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "通知方式不正确", taskId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "通知方式不正确"
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

        val realPath = if (customized) "/${path.removePrefix("/")}" else "/$pipelineId/$buildId/${path.removePrefix("/")}"
        val fileName = File(realPath).name

        val artifactoryType = if (customized) com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.CUSTOM_DIR
        else com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.PIPELINE
        if (!client.get(ServiceArtifactoryResource::class).check(projectId, artifactoryType, realPath).data!!) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "文件($path)不存在", taskId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL,
                errorMsg = "文件($path)不存在"
            )
        }

        val expArtifactoryType = if (customized) ArtifactoryType.CUSTOM_DIR else ArtifactoryType.PIPELINE
        val notifyTypeSet = notifyTypes.map { NotifyType.valueOf(it) }.toSet()
        val experience = ExperienceServiceCreate(realPath, expArtifactoryType, expireDate, experienceGroups, innerUsers, outerUsers, notifyTypeSet, enableGroupId, groupId)
        client.get(ServiceExperienceResource::class).create(userId, projectId, experience)

        LogUtils.addLine(rabbitTemplate, buildId, "版本体验($fileName)创建成功", taskId, task.executeCount ?: 1)

        return AtomResponse(BuildStatus.SUCCEED)
    }
}
