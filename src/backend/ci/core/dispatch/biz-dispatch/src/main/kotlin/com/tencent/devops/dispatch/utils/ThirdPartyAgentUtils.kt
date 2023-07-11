package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.agent.Credential
import com.tencent.devops.process.api.service.ServiceTemplateAcrossResource
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.ticket.pojo.enums.CredentialType

object ThirdPartyAgentUtils {
    // 获取凭据，同时存在stream中跨项目引用凭据的情况
    // 先获取当前项目凭据，如果当前项目没有凭据则判断跨项目引用来获取跨项目凭据
    fun getTicket(client: Client, projectId: String, credInfo: Credential): Pair<String?, String?> {
        if (credInfo.credentialId.isNullOrBlank()) {
            return Pair(null, null)
        }

        val tickets = try {
            CommonUtils.getCredential(
                client = client,
                projectId = projectId,
                credentialId = credInfo.credentialId!!,
                type = CredentialType.USERNAME_PASSWORD
            )
        } catch (ignore: Exception) {
            // 没有跨项目的模板引用就直接扔出错误
            if (credInfo.acrossTemplateId.isNullOrBlank() || credInfo.jobId.isNullOrBlank()) {
                throw ignore
            } else {
                emptyMap()
            }
        }

        if (!tickets["v1"].isNullOrBlank() && !tickets["v2"].isNullOrBlank()) {
            return Pair(tickets["v1"], tickets["v2"])
        }

        // 校验跨项目信息可能
        if (credInfo.acrossTemplateId.isNullOrBlank() || credInfo.jobId.isNullOrBlank()) {
            return Pair(null, null)
        }
        val result = client.get(ServiceTemplateAcrossResource::class).getBuildAcrossTemplateInfo(
            projectId = projectId,
            templateId = credInfo.acrossTemplateId!!
        ).data ?: return Pair(null, null)

        val across = result.firstOrNull {
            it.templateType == TemplateAcrossInfoType.JOB &&
                    it.templateInstancesIds.contains(credInfo.jobId)
        } ?: return Pair(null, null)

        // 校验成功后获取跨项目的凭据
        val acrossTickets = CommonUtils.getCredential(
            client = client,
            projectId = across.targetProjectId,
            credentialId = credInfo.credentialId!!,
            type = CredentialType.USERNAME_PASSWORD,
            acrossProject = true
        )
        return Pair(acrossTickets["v1"], acrossTickets["v2"])
    }
}
