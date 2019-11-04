package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.common.pipeline.element.AcrossProjectDistributionElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.pojo.AtomErrorCode.SYSTEM_SERVICE_ERROR
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class AcrossProjectDistributionAtom @Autowired constructor(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate
)
    : IAtomTask<AcrossProjectDistributionElement> {

    override fun getParamElement(task: PipelineBuildTask): AcrossProjectDistributionElement {
        return JsonUtil.mapTo(task.taskParams, AcrossProjectDistributionElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: AcrossProjectDistributionElement, runVariables: Map<String, String>): AtomResponse {
        val pipelineId = task.pipelineId
        val projectId = task.projectId
        val buildId = task.buildId
//        val param: AcrossProjectDistributionElement = JsonUtil.mapTo(task.taskParams)
        val path = parseVariable(param.path, runVariables) // parseVariable(getTaskParams("path", task), task)
        val customized = param.customized // parseVariable(getTaskParams("customized", task), task)
        val targetProjectId = parseVariable(param.targetProjectId, runVariables) // getTaskParams("targetProjectId", task), task)
        val targetPath = if (param.targetPath.isNotEmpty()) {
            parseVariable(param.targetPath, runVariables)
        } else {
            "/"
        }

        val artifactoryType = if (customized) ArtifactoryType.CUSTOM_DIR else ArtifactoryType.PIPELINE
        val relativePath = if (customized) path else "/$pipelineId/$buildId/${path.removePrefix("/")}"

        logger.info("[$buildId]|param=$param")

        val result =
                client.get(ServiceArtifactoryResource::class)
                        .acrossProjectCopy(projectId, artifactoryType, relativePath, targetProjectId, targetPath)

        return if (result.isOk()) {
            LogUtils.addLine(rabbitTemplate, buildId, "跨项目构件分发成功，共分发了${result.data}个文件", task.taskId, task.containerHashId, task.executeCount ?: 1)
            AtomResponse(BuildStatus.SUCCEED)
        } else {
            LogUtils.addRedLine(rabbitTemplate, buildId, "跨项目构件分发失败，$result", task.taskId, task.containerHashId, task.executeCount ?: 1)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = SYSTEM_SERVICE_ERROR,
                errorMsg = "跨项目构件分发失败，$result"
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
