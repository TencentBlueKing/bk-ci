package com.tencent.devops.process.engine.atom.task.deploy

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.gcloud.DynamicGcloudClient
import com.tencent.devops.common.gcloud.api.pojo.CommonParam
import com.tencent.devops.common.gcloud.api.pojo.dyn.DynUpdateVerParam
import com.tencent.devops.common.pipeline.element.GcloudPufferUpdateVersionElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.plugin.api.ServiceGcloudConfResource
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.gcloud.TicketUtil
import org.apache.commons.lang3.math.NumberUtils
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class GcloudPufferUpdateVersionTaskAtom @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val rabbitTemplate: RabbitTemplate
) : IAtomTask<GcloudPufferUpdateVersionElement> {

    override fun execute(task: PipelineBuildTask, param: GcloudPufferUpdateVersionElement, runVariables: Map<String, String>): AtomResponse {
        parseParam(param, runVariables)
        LogUtils.addLine(rabbitTemplate, task.buildId, "gcloud element params:\n $param", task.taskId, task.containerHashId, task.executeCount ?: 1)

        val gcloudUtil = TicketUtil(client)

        with(param) {
            val elementId = task.taskId
            LogUtils.addLine(rabbitTemplate, task.buildId, "正在开始更新gcloud版本配置信息，结果可以稍后前往http://console.gcloud.oa.com或者http://console.gcloud.qq.com查看\n", elementId, task.containerHashId, task.executeCount ?: 1)

            val projectId = task.projectId
            val buildId = task.buildId
            val userId = task.starter

            // 获取accessId和accessKey
            val ketPair = gcloudUtil.getAccesIdAndToken(projectId, ticketId)
            val accessId = ketPair.first
            val accessKey = ketPair.second

            val commonParam = CommonParam(gameId, accessId, accessKey)
            val host = client.get(ServiceGcloudConfResource::class).getByConfigId(configId.toInt()).data
                    ?: throw RuntimeException("unknown configId($configId)")
            val gcloudClient = DynamicGcloudClient(objectMapper, host.address, host.fileAddress)

            // step1
            val updateVerParam = DynUpdateVerParam(userId, productId.toInt(), versionStr,
                    if (NumberUtils.isDigits(versionType)) versionType?.toInt() else null,
                    versionDes, customStr)
            LogUtils.addLine(rabbitTemplate, buildId, "更新的配置信息：\n$updateVerParam", elementId, task.containerHashId, task.executeCount ?: 1)

            gcloudClient.updateVersion(updateVerParam, commonParam)
            LogUtils.addLine(rabbitTemplate, buildId, "更新gcloud配置成功!(gameId: $gameId, productId: $productId)", elementId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(BuildStatus.SUCCEED)
        }
    }

    override fun getParamElement(task: PipelineBuildTask): GcloudPufferUpdateVersionElement {
        return JsonUtil.mapTo(task.taskParams, GcloudPufferUpdateVersionElement::class.java)
    }

    private fun parseParam(param: GcloudPufferUpdateVersionElement, runVariables: Map<String, String>) {
        param.configId = parseVariable(param.configId, runVariables)
        param.productId = parseVariable(param.productId, runVariables)
        param.gameId = parseVariable(param.gameId, runVariables)
        param.versionStr = parseVariable(param.versionStr, runVariables)
        param.ticketId = parseVariable(param.ticketId, runVariables)
        param.versionType = parseVariable(param.versionType, runVariables)
        param.versionDes = parseVariable(param.versionDes, runVariables)
        param.customStr = parseVariable(param.customStr, runVariables)
    }
}
