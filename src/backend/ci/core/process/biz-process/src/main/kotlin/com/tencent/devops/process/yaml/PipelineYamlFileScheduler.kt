package com.tencent.devops.process.yaml

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.pojo.pipeline.enums.YamDiffFileStatus
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.process.yaml.mq.PipelineYamlFileExecutorEvent
import com.tencent.devops.process.yaml.mq.PipelineYamlFileSchedulerEvent
import com.tencent.devops.process.yaml.pojo.PipelineYamlSchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * yaml文件调度器
 */
@Service
class PipelineYamlFileScheduler @Autowired constructor(
    private val pipelineYamlDiffService: PipelineYamlDiffService,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val redisOperation: RedisOperation
) {

    fun schedule(event: PipelineYamlFileSchedulerEvent) {
        with(event) {
            logger.info(
                "[PAC_PIPELINE]|Start to schedule yaml file|$eventId|$projectId|${repository.repoHashId}|$filePath"
            )
            val lock = PipelineYamlSchedulerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                eventId = eventId
            )
            try {
                if (!lock.tryLock()) {
                    logger.info(
                        "[PAC_PIPELINE] scheduler|$projectId|$eventId|$filePath|YamlSchedulerLock try lock fail"
                    )
                    retry()
                    return
                }
                doHandle()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineYamlFileSchedulerEvent.retry() {
        logger.info("[PAC_PIPELINE] scheduler|$projectId|$eventId|$filePath|RETRY_TO_EXECUTOR_LOCK")
        this.delayMills = DEFAULT_DELAY
        sampleEventDispatcher.dispatch(this)
    }

    private fun PipelineYamlFileSchedulerEvent.doHandle() {
        val yamlDiffs = pipelineYamlDiffService.listYamlDiffs(
            projectId = projectId,
            eventId = eventId
        )
        val dependencyTypeStatusMap = yamlDiffs.groupBy { it.fileType }.mapValues { (_, events) ->
            events.all { it.status.isFinished() }
        }
        val dependencyFileStatusMap = yamlDiffs.associate { it.filePath to it.status.isFinished() }

        logger.info(
            "[PAC_PIPELINE] pipeline yaml diff scheduler| dependency type status map: $dependencyTypeStatusMap"
        )

        yamlDiffs.filter {
            // 1. 不是回调回来的事件,触发所有的文件
            // 2. 如果是回调回来的事件,只需要通知指定的文件类型
            fileType == null || fileType.notifyType().contains(it.fileType)
        }.forEach { yamlDiff ->
            // 不是准备运行中的状态,不处理
            if (!yamlDiff.status.isPending()) {
                logger.info("[PAC_PIPELINE] pipeline yaml diff scheduler|status is not pending|${yamlDiff.status}")
                return@forEach
            }
            val canDispatch = when (yamlDiff.actionType) {
                // 当新增和修改时,这里还无法判断依赖了哪些文件,所以需要等待所有依赖的文件类型都执行完成后,才执行
                YamlFileActionType.CREATE, YamlFileActionType.UPDATE -> {
                    isDependencyTypeAllFinished(
                        fileType = yamlDiff.fileType,
                        dependencyTypeStatusMap = dependencyTypeStatusMap
                    )
                }
                // 依赖更新,需要判断依赖的文件是否已经执行完成
                YamlFileActionType.DEPENDENCY_UPGRADE, YamlFileActionType.DEPENDENCY_UPGRADE_AND_TRIGGER -> {
                    yamlDiff.dependentFilePath?.let { dependencyFileStatusMap[it] ?: true } ?: true
                }

                else -> {
                    true
                }
            }
            if (canDispatch) {
                val yamlFileExecutorEvent = PipelineYamlFileExecutorEvent(
                    projectId = projectId,
                    repository = repository,
                    eventId = eventId,
                    filePath = yamlDiff.filePath,
                    ref = yamlDiff.ref
                )
                sampleEventDispatcher.dispatch(yamlFileExecutorEvent)
                pipelineYamlDiffService.updateStatus(
                    projectId = projectId,
                    eventId = eventId,
                    filePath = yamlDiff.filePath,
                    ref = yamlDiff.ref,
                    status = YamDiffFileStatus.RUNNING
                )
            }
        }
    }

    /**
     * 判断依赖的类型文件是否都已经执行完成
     */
    private fun isDependencyTypeAllFinished(
        fileType: YamlFileType,
        dependencyTypeStatusMap: Map<YamlFileType, Boolean>
    ): Boolean {
        val dependencyFileType = fileType.dependencyType()
        // 如果依赖文件类型为空,表示没有依赖,可以直接触发
        return if (dependencyFileType.isEmpty()) {
            true
        } else {
            // 如果有依赖,需要判断依赖的文件是否全部执行完成,如果全部执行完成,则可以触发
            dependencyFileType.all { dependencyTypeStatusMap[it] ?: true }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlFileScheduler::class.java)
        private const val DEFAULT_DELAY = 1000
    }
}
