package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.client.Client
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.plugin.api.ServiceMigCDNResource
import com.tencent.devops.plugin.pojo.migcdn.MigCDNUploadParam
import com.tencent.devops.common.pipeline.element.MigCDNPushFileElement
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
class MigCDNPushFileTaskAtom @Autowired constructor(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate
) : IAtomTask<MigCDNPushFileElement> {

    override fun getParamElement(task: PipelineBuildTask): MigCDNPushFileElement {
        return JsonUtil.mapTo(task.taskParams, MigCDNPushFileElement::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: MigCDNPushFileElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        val ticketId = parseVariable(param.ticketId, runVariables)
        val destFileDir = parseVariable(param.destFileDir, runVariables)
        val needUnzip = param.needUnzip
        val fileSource = parseVariable(param.fileSource, runVariables)
        val filePath = parseVariable(param.filePath, runVariables)

        val projectId = task.projectId
        val buildId = task.buildId
        val pipelineId = task.pipelineId
        val userId = task.starter

        val ticketsMap = CommonUtils.getCredential(client, projectId, ticketId, CredentialType.APPID_SECRETKEY)

        val uploadParams = MigCDNUploadParam(
            userId,
            MigCDNUploadParam.CommonParam(
                ticketsMap["v1"] as String,
                ticketsMap["v2"] as String,
                destFileDir,
                if (needUnzip) 1 else 0
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

        LogUtils.addLine(rabbitTemplate, buildId, "开始上传对应文件到CDN...", task.taskId, task.executeCount ?: 1)
        LogUtils.addLine(rabbitTemplate, buildId, "匹配文件中: ${uploadParams.fileParams.regexPath}($fileSource)", task.taskId, task.executeCount ?: 1)
        val pushFile = client.get(ServiceMigCDNResource::class).pushFile(uploadParams)
        LogUtils.addLine(rabbitTemplate, buildId, "上传对应文件到CDN结束! result=$pushFile", task.taskId, task.executeCount ?: 1)
        return defaultSuccessAtomResponse
    }
}
