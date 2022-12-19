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
import com.tencent.devops.process.notify.command.impl.NotifyContentCmd
import com.tencent.devops.process.notify.command.impl.NotifyPipelineCmd
import com.tencent.devops.process.notify.command.impl.NotifyReceiversCmd
import com.tencent.devops.process.notify.command.impl.NotifySendCmd
import com.tencent.devops.process.notify.command.impl.NotifyUrlBuildCmd
import com.tencent.devops.process.service.BuildVariableService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class PipelineNotifyService @Autowired constructor(
    open val buildVariableService: BuildVariableService,
    open val pipelineRepositoryService: PipelineRepositoryService
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

        val vars = buildVariableService.getAllVariable(projectId, pipelineId, buildId).toMutableMap()

        val setting = pipelineRepositoryService.getSetting(projectId, pipelineId) ?: return

        val context = BuildNotifyContext(
            buildId = buildId,
            projectId = projectId,
            pipelineId = pipelineId,
            variables = vars,
            notifyValue = mutableMapOf(),
            buildStatus = buildStatus,
            cmdFlowSeq = 0,
            pipelineSetting = setting,
            receivers = mutableSetOf(),
            watcher = Watcher("buildNotify")
        )

        val commandList = mutableListOf(
            commandCache.get(NotifyUrlBuildCmd::class.java), // 构建发送url相关信息
            commandCache.get(NotifyPipelineCmd::class.java), // 构建流水线相关相关信息
            commandCache.get(NotifyContentCmd::class.java), // 构建发送内容相关信息
            commandCache.get(NotifyReceiversCmd::class.java), // 构建发送人相关信息
            commandCache.get(NotifySendCmd::class.java) // 发送消息
        )
        // 添加自定义扩展
        if (addExtCmd() != null) {
            commandList.addAll(addExtCmd()!!)
        }

        NotifyCmdChain(commandList).doCommand(context)
    }

    abstract fun addExtCmd(): MutableList<NotifyCmd>?

    companion object {
        val logger = LoggerFactory.getLogger(PipelineNotifyService::class.java)
        private const val CACHE_SIZE = 500L
    }
}
