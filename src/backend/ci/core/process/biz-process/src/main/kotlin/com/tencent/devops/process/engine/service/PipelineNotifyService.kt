package com.tencent.devops.process.engine.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.NotifyCmd
import com.tencent.devops.process.notify.command.NotifyCmdChain
import com.tencent.devops.process.notify.command.impl.NotifyPipelineCmd
import com.tencent.devops.process.notify.command.impl.NotifySendCmd
import com.tencent.devops.process.notify.command.impl.NotifyUrlBuildCmd
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineContextService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineNotifyService @Autowired constructor(
    val buildVariableService: BuildVariableService,
    val pipelineRuntimeService: PipelineRuntimeService,
    val pipelineContextService: PipelineContextService,
    val pipelineRepositoryService: PipelineRepositoryService
) {

    private val commandCache: LoadingCache<Class<out NotifyCmd>, NotifyCmd> = CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE).build(
            object : CacheLoader<Class<out NotifyCmd>, NotifyCmd>() {
                override fun load(clazz: Class<out NotifyCmd>): NotifyCmd {
                    return SpringContextUtil.getBean(clazz)
                }
            }
        )

    fun onPipelineShutdown(
        pipelineId: String,
        buildId: String,
        projectId: String,
        buildStatus: BuildStatus
    ) {
        logger.info("onPipelineShutdown new $pipelineId|$buildId|$buildStatus")

        val vars = pipelineContextService.getAllBuildContext(
            buildVariableService.getAllVariable(projectId, pipelineId, buildId)
        ).toMutableMap()

        // #8161 调试中生效调试版本的通知配置
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, pipelineId, buildId) ?: return
        val setting = if (buildInfo.debug) {
            pipelineRepositoryService.getSettingByPipelineVersion(
                projectId, pipelineId, buildInfo.version
            ) ?: return
        } else {
            pipelineRepositoryService.getSetting(projectId, pipelineId) ?: return
        }

        val context = BuildNotifyContext(
            buildId = buildId,
            projectId = projectId,
            pipelineId = pipelineId,
            variables = vars,
            notifyValue = mutableMapOf(),
            buildStatus = buildStatus,
            cmdFlowSeq = 0,
            pipelineSetting = setting,
            watcher = Watcher("buildNotify")
        )

        val commandList = mutableListOf(
            commandCache.get(NotifyUrlBuildCmd::class.java), // 构建发送url相关信息
            commandCache.get(NotifyPipelineCmd::class.java), // 构建流水线相关相关信息
            commandCache.get(NotifySendCmd::class.java) // 发送消息
        )

        NotifyCmdChain(commandList).doCommand(context)
    }

    companion object {
        val logger = LoggerFactory.getLogger(PipelineNotifyService::class.java)
        private const val CACHE_SIZE = 500L
    }
}
