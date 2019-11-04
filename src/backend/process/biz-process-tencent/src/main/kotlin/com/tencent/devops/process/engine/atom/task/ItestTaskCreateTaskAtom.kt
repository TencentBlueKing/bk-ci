@file:Suppress("UNCHECKED_CAST")

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.common.pipeline.element.ItestTaskCreateElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.common.itest.api.ITestClient
import com.tencent.devops.common.itest.api.request.TaskCreateRequest
import com.tencent.devops.process.util.CommonUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType
import net.sf.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(SCOPE_PROTOTYPE)
class ItestTaskCreateTaskAtom @Autowired constructor(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate
) : IAtomTask<ItestTaskCreateElement> {

    override fun getParamElement(task: PipelineBuildTask): ItestTaskCreateElement {
        return JsonUtil.mapTo(task.taskParams, ItestTaskCreateElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: ItestTaskCreateElement, runVariables: Map<String, String>): AtomResponse {
        val buildId = task.buildId
        val taskId = task.taskId
        val containerId = task.containerHashId

        val userId = task.starter
        val itestProjectIdValue = parseVariable(param.itestProjectId, runVariables)
        val ticketIdValue = parseVariable(param.ticketId, runVariables)

        // 从ticket服务拿到itest api的用户名和token
        val projectId = task.projectId
        val ticketsMap = CommonUtils.getCredential(client, projectId, ticketIdValue, CredentialType.USERNAME_PASSWORD)
        val apiUser = ticketsMap["v1"].toString()
        val token = ticketsMap["v2"].toString()

        val itestClient = ITestClient(apiUser, token)

        // TODO 根据svn或者git的changelog创建自测任务
        val changelog = "hello changelog"
        val commitUser = "johuang"

        val taskCreateRequest = TaskCreateRequest(if (changelog.length > 200) {
            changelog.substring(0, 200)
        } else changelog, itestProjectIdValue,
                changelog, "7", userId, "",
                commitUser, "", "")
        val taskCreateResponse = itestClient.createTask(taskCreateRequest)

        logger.info("Create task for itest success!")
        val processJson = JSONObject.fromObject(taskCreateResponse.data).toString()
        LogUtils.addLine(rabbitTemplate, buildId, "创建itest自测任务成功, 详情：$processJson", taskId, containerId, task.executeCount ?: 1)
        return AtomResponse(BuildStatus.SUCCEED)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItestTaskCreateTaskAtom::class.java)
    }
}
