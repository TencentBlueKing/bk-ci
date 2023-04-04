package com.tencent.devops.dispatch.codecc.listener

import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.environment.agent.pojo.devcloud.Credential
import com.tencent.devops.common.environment.agent.pojo.devcloud.Pool
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.codecc.pojo.CodeccDispatchMessage
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CodeCCDispatchListener @Autowired constructor(
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val buildLogPrinter: BuildLogPrinter,
    private val dslContext: DSLContext,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : BuildListener {

    companion object {
        private val logger = LoggerFactory.getLogger(CodeCCDispatchListener::class.java)
    }

    override fun getShutdownQueue(): String {
        return ".codecc.scan"
    }

    override fun getStartupDemoteQueue(): String {
        return ".codecc.scan.demote"
    }

    override fun getStartupQueue(): String {
        return ".codecc.scan"
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.OTHER
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("On shutdown - ($event|$)")
        val buildHistory = pipelineDockerBuildDao.getBuild(dslContext, event.buildId, event.vmSeqId?.toInt() ?: 1)
        // 判断是否有docker vm构建记录，不存在则默认为devcloud构建
        if (buildHistory != null) {
            pipelineEventDispatcher.dispatch(
                event.copy(routeKeySuffix = DispatchRouteKeySuffix.DOCKER_VM.routeKeySuffix)
            )
            return
        }

        pipelineEventDispatcher.dispatch(event.copy(routeKeySuffix = DispatchRouteKeySuffix.DEVCLOUD.routeKeySuffix))
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        logger.info("CodeCC dispatcher startUp dispatchMessage: $dispatchMessage")

        val codeccDispatchMessage = JsonUtil.to(dispatchMessage.dispatchMessage, CodeccDispatchMessage::class.java)

        // 判断是否为devcloud构建
        if (codeccDispatchMessage.codeccTaskId == -101L || codeccDispatchMessage.codeccTaskId == -3L) {
            printLogs(dispatchMessage, "Preparing container...")

            val containerPool = Pool(
                container = "mirrors.tencent.com/ci/tlinux3_ci:2.3.0",
                credential = Credential("", "")
            )

            with(dispatchMessage) {
                pipelineEventDispatcher.dispatch(
                    PipelineAgentStartupEvent(
                        source = "vmStartupTaskAtom",
                        projectId = projectId,
                        pipelineId = pipelineId,
                        pipelineName = "",
                        userId = userId,
                        buildId = buildId,
                        buildNo = 0,
                        vmSeqId = containerId,
                        taskName = "",
                        os = "",
                        vmNames = vmNames,
                        startTime = System.currentTimeMillis(),
                        channelCode = channelCode,
                        dispatchType = PublicDevCloudDispathcType(
                            image = JsonUtil.toJson(containerPool),
                            imageType = ImageType.THIRD,
                            performanceConfigId = "0"
                        ),
                        atoms = atoms,
                        executeCount = executeCount,
                        routeKeySuffix = DispatchRouteKeySuffix.DEVCLOUD.routeKeySuffix,
                        containerId = containerId,
                        containerHashId = containerHashId,
                        customBuildEnv = customBuildEnv
                    )
                )
            }

            return
        }

        // 记录构建历史
        with(dispatchMessage) {
            pipelineDockerBuildDao.startBuild(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId.toInt(),
                secretKey = "",
                zone = Zone.SHENZHEN.name,
                dockerIp = "",
                poolNo = 0
            )

            pipelineEventDispatcher.dispatch(
                PipelineAgentStartupEvent(
                    source = "vmStartupTaskAtom",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = "",
                    userId = userId,
                    buildId = buildId,
                    buildNo = 0,
                    vmSeqId = containerId,
                    taskName = "",
                    os = "",
                    vmNames = vmNames,
                    startTime = System.currentTimeMillis(),
                    channelCode = channelCode,
                    dispatchType = DockerDispatchType(
                        dockerBuildVersion = "mirrors.tencent.com/ci/tlinux3_ci:2.3.0",
                        imageType = ImageType.THIRD
                    ),
                    atoms = atoms,
                    executeCount = executeCount,
                    routeKeySuffix = DispatchRouteKeySuffix.DOCKER_VM.routeKeySuffix,
                    containerId = containerId,
                    containerHashId = containerHashId,
                    customBuildEnv = customBuildEnv
                )
            )
        }

        return
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        onStartup(dispatchMessage)
    }

    private fun printLogs(dispatchMessage: DispatchMessage, message: String) {
        try {
            log(
                buildLogPrinter,
                dispatchMessage.buildId,
                dispatchMessage.containerHashId,
                dispatchMessage.vmSeqId,
                message,
                dispatchMessage.executeCount
            )
        } catch (e: Throwable) {
            // 日志有问题就不打日志了，不能影响正常流程
            logger.error("", e)
        }
    }
}
