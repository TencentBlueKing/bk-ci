package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.client.Client
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.plugin.api.ServiceLunaResource
import com.tencent.devops.plugin.pojo.luna.LunaUploadParam
import com.tencent.devops.common.pipeline.element.LunaPushFileElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultSuccessAtomResponse
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.CommonUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class LunaPushFileTaskAtom @Autowired constructor(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate
) : IAtomTask<LunaPushFileElement> {

    override fun getParamElement(task: PipelineBuildTask): LunaPushFileElement {
        return JsonUtil.mapTo(task.taskParams, LunaPushFileElement::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: LunaPushFileElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        val ticketId = parseVariable(param.ticketId, runVariables)
        val destFileDir = parseVariable(param.destFileDir, runVariables)
        val fileSource = parseVariable(param.fileSource, runVariables)
        val filePath = parseVariable(param.filePath, runVariables)

        val projectId = task.projectId
        val buildId = task.buildId
        val pipelineId = task.pipelineId
        val userId = task.starter

        val ticketsMap = CommonUtils.getCredential(client, projectId, ticketId, CredentialType.APPID_SECRETKEY)

        val uploadParams = LunaUploadParam(
            userId,
            LunaUploadParam.CommonParam(
                ticketsMap["v1"] as String,
                ticketsMap["v2"] as String,
                destFileDir
            ),
            ArtifactorySearchParam(
                projectId,
                pipelineId,
                buildId,
                filePath,
                fileSource == "CUSTOMIZE",
                task.executeCount ?: 1,
                task.taskId
            )
        )

        LogUtils.addLine(rabbitTemplate, buildId, "开始上传文件到LUNA...", task.taskId, task.containerHashId, task.executeCount ?: 1)
        LogUtils.addLine(rabbitTemplate, buildId, "匹配文件中: ${uploadParams.fileParams.regexPath}($fileSource)", task.taskId, task.containerHashId, task.executeCount ?: 1)
        client.get(ServiceLunaResource::class).pushFile(uploadParams)
        LogUtils.addLine(rabbitTemplate, buildId, "上传文件到LUNA结束! 请到LUNA平台->托管源站->托管列表->文件管理中查看【<a target='_blank' href='http://luna.oa.com/homepage'>传送门</a>】",
            task.taskId, task.containerHashId, task.executeCount ?: 1)
        return defaultSuccessAtomResponse
    }
}
