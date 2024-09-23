package com.tencent.devops.remotedev.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.remotedev.config.async.AsyncExecuteEvent
import com.tencent.devops.remotedev.config.async.AsyncExecuteEventType
import com.tencent.devops.remotedev.pojo.async.AsyncJobEndEvent
import com.tencent.devops.remotedev.pojo.async.AsyncJobPipeline
import com.tencent.devops.remotedev.pojo.async.AsyncNotify
import com.tencent.devops.remotedev.pojo.async.AsyncPipelineEvent
import com.tencent.devops.remotedev.pojo.async.AsyncTCloudCfs
import com.tencent.devops.remotedev.pojo.async.AsyncTGitAclIp
import com.tencent.devops.remotedev.pojo.async.AsyncTGitAclUser
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import com.tencent.devops.remotedev.service.job.RemoteDevJobActionService
import com.tencent.devops.remotedev.service.job.RemoteDevJobService
import com.tencent.devops.remotedev.service.tcloud.TCloudCfsService
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AsyncExecuteListener @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val remoteDevJobService: RemoteDevJobService,
    private val tCloudCfsService: TCloudCfsService,
    private val gitProxyTGitService: GitProxyTGitService,
    private val jobActionService: RemoteDevJobActionService,
    private val notifyControl: NotifyControl
) {
    fun listenAsyncExecuteEvent(event: AsyncExecuteEvent) {
        logger.debug("listenAsyncExecuteEvent|$event")
        try {
            doExecute(event)
        } catch (e: Throwable) {
            logger.error("listenAsyncExecuteEvent|${event.type}|${event.eventStr}|error", e)
        }
    }

    private fun doExecute(event: AsyncExecuteEvent) {
        when (event.type) {
            AsyncExecuteEventType.ASYNC_PIPELINE -> {
                val data = objectMapper.readValue<AsyncPipelineEvent>(event.eventStr)
                client.get(ServiceBuildResource::class).manualStartupNew(
                    userId = data.userId,
                    projectId = data.projectId,
                    pipelineId = data.pipelineId,
                    values = data.values,
                    channelCode = data.channelCode,
                    buildNo = data.buildNo,
                    startType = data.startType
                )
            }

            AsyncExecuteEventType.ASYNC_JOB_END -> {
                val data = objectMapper.readValue<AsyncJobEndEvent>(event.eventStr)
                remoteDevJobService.doPipelineJobEnd(data.id)
            }

            AsyncExecuteEventType.ASYNC_TCLOUD_CFS -> {
                val data = objectMapper.readValue<AsyncTCloudCfs>(event.eventStr)
                tCloudCfsService.doCreateOrDeleteCfsRule(
                    pgId = data.pgId,
                    ip = data.ip,
                    ruleId = data.ruleId,
                    region = data.region,
                    delete = data.delete
                )
            }

            AsyncExecuteEventType.ASYNC_TGIT_ACL_IP -> {
                val data = objectMapper.readValue<AsyncTGitAclIp>(event.eventStr)
                gitProxyTGitService.doAddOrRemoveAclIp(
                    projectId = data.projectId,
                    ips = data.ips,
                    remove = data.remove,
                    tgitId = data.tgitId
                )
            }

            AsyncExecuteEventType.ASYNC_TGIT_ACL_USER -> {
                val data = objectMapper.readValue<AsyncTGitAclUser>(event.eventStr)
                gitProxyTGitService.doRefreshProjectTGitSpecUser(
                    projectId = data.projectId,
                    tgitId = data.tgitId
                )
            }

            AsyncExecuteEventType.ASYNC_JOB_PIPELINE -> {
                val data = objectMapper.readValue<AsyncJobPipeline>(event.eventStr)
                jobActionService.doStartPipeline(data.projectId, data.id, data.param)
            }

            AsyncExecuteEventType.ASYNC_NOTIFY -> {
                val data = objectMapper.readValue<AsyncNotify>(event.eventStr)
                notifyControl.notifyWorkspaceInfo(
                    userId = data.operator,
                    notifyData = data.notifyData
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncExecuteListener::class.java)
    }
}
