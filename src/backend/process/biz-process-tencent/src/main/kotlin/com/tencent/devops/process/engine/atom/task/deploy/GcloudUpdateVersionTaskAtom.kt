package com.tencent.devops.process.engine.atom.task.deploy

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.gcloud.GcloudClient
import com.tencent.devops.common.gcloud.api.pojo.CommonParam
import com.tencent.devops.common.gcloud.api.pojo.UpdateVerParam
import com.tencent.devops.common.pipeline.element.GcloudUpdateVersionElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.plugin.api.ServiceGcloudConfResource
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.gcloud.TicketUtil
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class GcloudUpdateVersionTaskAtom @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val rabbitTemplate: RabbitTemplate
) : IAtomTask<GcloudUpdateVersionElement> {

    override fun execute(task: PipelineBuildTask, param: GcloudUpdateVersionElement, runVariables: Map<String, String>): AtomResponse {
        parseParam(param, runVariables)
        LogUtils.addLine(rabbitTemplate, task.buildId, "gcloud element params:\n $param", task.taskId, task.containerHashId, task.executeCount ?: 1)

        val ticketUtil = TicketUtil(client)
        with(param) {
            val elementId = task.taskId
            LogUtils.addLine(rabbitTemplate, task.buildId, "正在开始更新gcloud版本配置信息，结果可以稍后前往查看：\n", elementId, task.containerHashId, task.executeCount ?: 1)
            LogUtils.addLine(rabbitTemplate, task.buildId, "<a target='_blank' href='http://console.gcloud.oa.com/dolphin/edit/$gameId/$productId/$versionStr'>查看详情</a>", elementId, task.containerHashId, task.executeCount ?: 1)

            val projectId = task.projectId
            val buildId = task.buildId
            val userId = task.starter

            /*
            *
            * 版本目标用户。可选值：
            * 0 - 不可用，1 - 普通用户可用，2 - 灰度用户可用，
            * 3 - 普通用户和灰度用户都可用，4 - 审核版本，缺省 0
            *
            * */
            val availableType = if (versionType == "0") {
                if (normalUserCanUse == "1" && grayUserCanUse == "0") {
                    1
                } else if (normalUserCanUse == "0" && grayUserCanUse == "1") {
                    2
                } else if (normalUserCanUse == "1" && grayUserCanUse == "1") {
                    3
                } else {
                    0
                }
            } else {
                4
            }

            // 获取accessId和accessKey
            val ketPair = ticketUtil.getAccesIdAndToken(projectId, ticketId)
            val accessId = ketPair.first
            val accessKey = ketPair.second

            val commonParam = CommonParam(gameId, accessId, accessKey)
            val host = client.get(ServiceGcloudConfResource::class).getByConfigId(configId.toInt()).data
                    ?: throw RuntimeException("unknown configId($configId)")
            val gcloudClient = GcloudClient(objectMapper, host.address, host.fileAddress)

            // step1
            val updateVerParam = UpdateVerParam(userId, productId.toInt(), versionStr, availableType, grayRuleId, versionDes, customStr)
            LogUtils.addLine(rabbitTemplate, buildId, "更新的配置信息：\n$updateVerParam", elementId, task.containerHashId, task.executeCount ?: 1)

            LogUtils.addLine(rabbitTemplate, buildId, "updateVerParam", elementId, task.containerHashId, task.executeCount ?: 1)
            gcloudClient.updateVersion(updateVerParam, commonParam)
            LogUtils.addLine(rabbitTemplate, buildId, "更新gcloud配置成功!(gameId: $gameId, productId: $productId)", elementId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(BuildStatus.SUCCEED)
        }
    }

    override fun getParamElement(task: PipelineBuildTask): GcloudUpdateVersionElement {
        return JsonUtil.mapTo(task.taskParams, GcloudUpdateVersionElement::class.java)
    }

    private fun parseParam(param: GcloudUpdateVersionElement, runVariables: Map<String, String>) {
        param.configId = parseVariable(param.configId, runVariables)
        param.productId = parseVariable(param.productId, runVariables)
        param.gameId = parseVariable(param.gameId, runVariables)
        param.versionStr = parseVariable(param.versionStr, runVariables)
        param.ticketId = parseVariable(param.ticketId, runVariables)
        param.versionType = parseVariable(param.versionType, runVariables)
        param.normalUserCanUse = parseVariable(param.normalUserCanUse, runVariables)
        param.grayUserCanUse = parseVariable(param.grayUserCanUse, runVariables)
        param.grayRuleId = parseVariable(param.grayRuleId, runVariables)
        param.versionDes = parseVariable(param.versionDes, runVariables)
        param.customStr = parseVariable(param.customStr, runVariables)
    }
}
