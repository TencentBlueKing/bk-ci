package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthResourceApi
import com.tencent.devops.common.auth.code.VSAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.plugin.api.ServiceJinGangAppResource
import com.tencent.devops.common.pipeline.element.JinGangAppElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class JinGangAppTaskAtom @Autowired constructor(
    private val bkAuthResourceApi: BSAuthResourceApi,
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate,
    private val serviceCode: VSAuthServiceCode
) : IAtomTask<JinGangAppElement> {

    override fun getParamElement(task: PipelineBuildTask): JinGangAppElement {
        return JsonUtil.mapTo(task.taskParams, JinGangAppElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: JinGangAppElement, runVariables: Map<String, String>): AtomResponse {
        val srcType = parseVariable(param.srcType, runVariables)
        val files = parseVariable(param.files, runVariables)
        val runType = parseVariable(param.runType, runVariables)

        val buildId = task.buildId
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val taskId = task.taskId
        val userId = task.starter
        val buildNo = runVariables[PIPELINE_BUILD_NUM]?.toInt() ?: 0

        val isCustom = when (srcType) {
            "PIPELINE" -> false
            "CUSTOMIZE" -> true
            else -> return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "unsupported srcType : $srcType"
            )
        }
        files.split(",").map { it.trim() }.forEach {
            LogUtils.addLine(rabbitTemplate, buildId, "jin gang start scan file: $it", taskId, task.executeCount ?: 1)
            val path = if (isCustom) "/${it.removePrefix("/")}" else "/$pipelineId/$buildId/${it.removePrefix("/")}"
            val data = client.get(ServiceJinGangAppResource::class).scanApp(userId, projectId, pipelineId, buildId, buildNo, taskId, path, isCustom, runType)
            val resourceName = File(it).name + "($buildNo)"
            val jinGangTaskId = data.data ?: throw RuntimeException("task id is null for ($it)")
            // 权限中心注册资源
            bkAuthResourceApi.createResource(userId, serviceCode, AuthResourceType.SCAN_TASK,
                    projectId, HashUtil.encodeLongId(jinGangTaskId.toLong()), resourceName)

            if (data.status != 0) {
                LogUtils.addLine(rabbitTemplate, buildId, "jin gang fail: $data", taskId, task.executeCount ?: 1)
            } else {
                LogUtils.addLine(rabbitTemplate, buildId, "jin gang success: $data", taskId, task.executeCount ?: 1)
            }
        }
        return AtomResponse(BuildStatus.SUCCEED)
    }
}
