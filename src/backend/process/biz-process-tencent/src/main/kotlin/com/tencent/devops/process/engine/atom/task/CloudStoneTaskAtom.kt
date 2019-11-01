package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.client.JfrogClient
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.common.pipeline.element.CloudStoneElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.service.CloudStoneService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class CloudStoneTaskAtom @Autowired constructor(
    private val cloudStoneService: CloudStoneService,
    private val rabbitTemplate: RabbitTemplate,
    private val client: Client
) : IAtomTask<CloudStoneElement> {
    override fun getParamElement(task: PipelineBuildTask): CloudStoneElement {
        return JsonUtil.mapTo(task.taskParams, CloudStoneElement::class.java)
    }

    @Value("\${gateway.url:#{null}}")
    private val gatewayUrl: String? = null

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
        val matchFiles = JfrogClient(gatewayUrl!!, projectId, pipelineId, buildId).downloadFile(sourcePath, isCustom, destPath)
        if (matchFiles.isEmpty()) throw OperationException("There is 0 file find in $sourcePath(custom: $isCustom)")
        val appId = client.get(ServiceProjectResource::class).get(task.projectId).data?.ccAppId?.toInt()
            ?: run {
                LogUtils.addLine(rabbitTemplate, task.buildId, "找不到绑定配置平台的业务ID/can not found CC Business ID", task.taskId,
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

                LogUtils.addLine(rabbitTemplate, buildId, "上传云石成功/Upload to cloudStone success ：${result.second}", taskId, task.containerHashId, executeCount)
            } else {
                logger.info("Upload to cloudStone failed. msg:${result.second}")
                LogUtils.addRedLine(rabbitTemplate, buildId, "上传云石失败/Upload to cloudStone fail : ${result.second}", taskId, task.containerHashId, executeCount)
                return return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL,
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
