package com.tencent.devops.dispatch.devcloud.listener

import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.devcloud.constant.DispatchDevcloudMessageCode.BK_PREPARE_CREATE_TENCENT_CLOUD_BUILD_MACHINE
import com.tencent.devops.dispatch.devcloud.service.DcContainerPrepareHandler
import com.tencent.devops.dispatch.devcloud.service.DcContainerShutdownHandler
import com.tencent.devops.dispatch.devcloud.service.DispatchDevcloudService
import com.tencent.devops.dispatch.devcloud.service.context.DcShutdownHandlerContext
import com.tencent.devops.dispatch.devcloud.service.context.DcStartupHandlerContext
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DevCloudBuildListener @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val dispatchDevcloudService: DispatchDevcloudService,
    private val dcContainerPrepareHandler: DcContainerPrepareHandler,
    private val dcContainerShutdownHandler: DcContainerShutdownHandler
) : BuildListener {

    override fun getShutdownQueue(): String {
        return ".devcloud.public"
    }

    override fun getStartupDemoteQueue(): String {
        return ".devcloud.public.demote"
    }

    override fun getStartupQueue(): String {
        return ".devcloud.public"
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.DOCKER_DEVCLOUD
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        startUp(dispatchMessage)
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        startUp(dispatchMessage)
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        dcContainerShutdownHandler.handlerRequest(
            DcShutdownHandlerContext(
                userId = event.userId,
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                executeCount = event.executeCount,
                shutdownEvent = event
            )
        )
    }

    private fun startUp(dispatchMessage: DispatchMessage) {
        with(dispatchMessage.event) {
            // 打印启动日志
            printLogs(
                dispatchMessage,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_PREPARE_CREATE_TENCENT_CLOUD_BUILD_MACHINE,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )

            // 判断是否需要事件重试
            if (dispatchDevcloudService.needRetry(buildId, vmSeqId, executeCount)) {
                retry()
                return
            }

            dcContainerPrepareHandler.handlerRequest(
                DcStartupHandlerContext(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    executeCount = executeCount,
                    agentId = dispatchMessage.id,
                    secretKey = dispatchMessage.secretKey,
                    gateway = dispatchMessage.gateway,
                    dispatchMessage = dispatchType.value,
                    atoms = atoms,
                    dispatchType = dispatchType,
                    customBuildEnv = dispatchMessage.customBuildEnv,
                    containerHashId = containerHashId,
                    persistence = (dispatchType as PublicDevCloudDispathcType).persistence ?: false
                )
            )
        }
    }

    private fun printLogs(dispatchMessage: DispatchMessage, message: String) {
        log(
            buildLogPrinter,
            dispatchMessage.event.buildId,
            dispatchMessage.event.containerHashId,
            dispatchMessage.event.vmSeqId,
            message,
            dispatchMessage.event.executeCount
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudBuildListener::class.java)
    }
}
