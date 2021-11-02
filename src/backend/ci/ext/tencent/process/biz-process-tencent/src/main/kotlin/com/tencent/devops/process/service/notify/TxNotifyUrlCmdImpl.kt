package com.tencent.devops.process.service.notify

import com.tencent.devops.artifactory.api.service.ServiceShortUrlResource
import com.tencent.devops.artifactory.pojo.CreateShortUrlRequest
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.impl.NotifyUrlBuildCmd
import com.tencent.devops.process.util.ServiceHomeUrlUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxNotifyUrlCmdImpl @Autowired constructor(
    val client: Client
) : NotifyUrlBuildCmd() {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val detailUrl = detailUrl(commandContext.projectId, commandContext.pipelineId, commandContext.buildId)
        val detailOuterUrl = detailOuterUrl(commandContext.projectId, commandContext.pipelineId, commandContext.buildId)
        val detailShortOuterUrl = client.get(ServiceShortUrlResource::class).createShortUrl(
            CreateShortUrlRequest(url = detailOuterUrl, ttl = SHORT_URL_TTL)).data!!

        val urlMap = mutableMapOf(
            "detailUrl" to detailUrl,
            "detailOuterUrl" to detailOuterUrl,
            "detailShortOuterUrl" to detailShortOuterUrl
        )
        commandContext.notifyValue.putAll(urlMap)
    }

    private fun detailUrl(projectId: String, pipelineId: String, processInstanceId: String) =
        "${ServiceHomeUrlUtils.server()}/console/pipeline/$projectId/$pipelineId/detail/$processInstanceId"

    private fun detailOuterUrl(projectId: String, pipelineId: String, processInstanceId: String) =
        "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html" +
            "?flag=buildArchive&projectId=$projectId&pipelineId=$pipelineId&buildId=$processInstanceId"

    companion object {
        private const val SHORT_URL_TTL = 24 * 3600 * 180
        val logger = LoggerFactory.getLogger(TxNotifyUrlCmdImpl::class.java)
    }
}
