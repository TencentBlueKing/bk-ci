package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.shorturl.ShortUrlApi
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.element.SendSmsNotifyElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PROJECT_NAME
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class SmsTaskAtom @Autowired constructor(
    private val client: Client,
    private val shortUrlApi: ShortUrlApi,
    private val rabbitTemplate: RabbitTemplate
)
    : IAtomTask<SendSmsNotifyElement> {

    override fun getParamElement(task: PipelineBuildTask): SendSmsNotifyElement {
        return JsonUtil.mapTo(task.taskParams, SendSmsNotifyElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: SendSmsNotifyElement, runVariables: Map<String, String>): AtomResponse {
        val buildId = task.buildId
        val taskId = task.taskId
        if (param.receivers.isEmpty()) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "The receivers is not init of build", taskId, task.containerHashId, task.executeCount ?: 1)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "The receivers is not init of build"
            )
        }

        val sendDetailFlag = param.detailFlag ?: false

        var bodyStr = parseVariable(param.body, runVariables)
        // 启动短信的查看详情,短信必须是短连接
        if (sendDetailFlag) {
            val shortUrl = shortUrlApi.getShortUrl("${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive&" +
                    "projectId=${runVariables[PROJECT_NAME]}&" +
                    "pipelineId=${runVariables[PIPELINE_ID]}&" +
                    "buildId=$buildId",
                    24 * 3600 * 180)
            bodyStr = "$bodyStr\n\n 查看详情：$shortUrl"
        }
        val message = SmsNotifyMessage().apply {
            body = bodyStr
        }
        val receiversStr = parseVariable(param.receivers.joinToString(","), runVariables)
        LogUtils.addLine(rabbitTemplate, buildId, "send SMS message (${message.body}) to $receiversStr", taskId, task.containerHashId, task.executeCount ?: 1)

        message.addAllReceivers(receiversStr.split(",").toSet())
        client.get(ServiceNotifyResource::class).sendSmsNotify(message)
        return AtomResponse(BuildStatus.SUCCEED)
    }
}
