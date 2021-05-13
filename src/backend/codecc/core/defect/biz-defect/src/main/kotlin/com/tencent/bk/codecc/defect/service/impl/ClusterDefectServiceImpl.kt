package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.ToolMetaCacheServiceImpl
import com.tencent.bk.codecc.defect.service.ClusterDefectService
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService
import com.tencent.bk.codecc.defect.service.TaskLogService
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.util.stream.Collectors


@Component
class ClusterDefectServiceImpl @Autowired constructor(
        private var applicationContext: ApplicationContext,
        private val client: Client,
        private val taskLogOverviewService: TaskLogOverviewService,
        private val taskLogService: TaskLogService,
        private val toolMetaCacheServiceImpl: ToolMetaCacheServiceImpl
) : ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    /**
     * 以工具类型为维度，聚类告警
     * 聚类逻辑执行需要满足几个前提条件：
     * 1. 所有工具执行完成并成功，通过 getTaskStatus 方法判断工具状态，非成功状态不执行聚类
     * 2. 聚类逻辑必须只执行一次，现有逻辑是由最后一个完成的工具去执行，由 getTaskStatus 方法实现
     * 如果聚类逻辑执行多次，会造成聚类数据不准确
     *
     * @param taskId
     * @param buildId
     * @param toolName
     */
    fun cluster(taskId: Long, buildId: String, toolName: String) {
        logger.info("$toolName trigger cluster for $taskId $buildId")
        val taskStatus = getTaskStatus(taskId, buildId)
        // 不等待正在执行的任务,若任务还在执行中则不聚类信息
        if (taskStatus.second != ComConstants.StepFlag.SUCC.value()) {
            logger.info(taskStatus.first)
            return
        }

        // 聚类逻辑，由通过上面判断的线程来执行
        logger.info("$toolName trigger cluster for $taskId $buildId success")
        // 工具根据 Type 分类
        val taskLogVOList = taskLogService.getCurrBuildInfo(taskId, buildId)
        val toolMap = taskLogVOList.stream()
                .map { t ->
                    t.toolName
                }.collect(Collectors.toList()).groupBy {
                    toolMetaCacheServiceImpl.getToolBaseMetaCache(it).type
                }

        val clusterBeans =
                applicationContext.getBeansOfType(ClusterDefectService::class.java)
        toolMap.forEach { (type, toolList) ->
            clusterBeans[type]?.cluster(
                    taskId = taskId,
                    buildId = buildId,
                    toolList = toolList)
        }
    }

    /**
     * 获取任务执行状态
     * 当所有工具都执行成功时才标记成功
     *
     * @param taskId
     * @param buildId
     */
    private fun getTaskStatus(taskId: Long, buildId: String): Pair<String, Int> {
        val taskLogVOList = taskLogService.getCurrBuildInfo(taskId, buildId)
        // 获取任务扫描工具
        val result = client.get(ServiceTaskRestResource::class.java).getTaskToolList(taskId)
        if (result.isNotOk() || result.data == null) {
            // 远程调用失败标记为为执行，不再计算度量信息
            return Pair(
                    "get task tool config info from remote fail! message: ${result.message} taskId: $taskId | buildId: $buildId",
                    ComConstants.StepFlag.FAIL.value())
        }

        val toolList = result.data
        // 获取任务的实际执行工具
        val actualExeTools = taskLogOverviewService.getActualExeTools(taskId, buildId)
        // 判断任务是否执行完毕的时候根据任务设置的扫描工具和实际扫描的工具决定
        toolList?.enableToolList?.filter { toolConfigBaseVO ->
            actualExeTools?.contains(toolConfigBaseVO.toolName) ?: true }
                ?.forEach { tool ->
                    logger.info("cal status ${tool.toolName}")
                    val taskLog = taskLogVOList.find { taskLogVO ->
                        taskLogVO.toolName.equals(tool.toolName, true)
                    } ?: return Pair(
                            "${tool.toolName} not found! taskId: $taskId | buildId: $buildId",
                            ComConstants.StepFlag.FAIL.value()
                    )

                    // 执行成功则继续分析
                    if (taskLog.flag != ComConstants.StepFlag.SUCC.value()) {
                        return Pair(
                                "${taskLog.toolName} execute not success! taskId: $taskId | buildId: $buildId",
                                taskLog.flag
                        )
                    }
                }

        return Pair("", ComConstants.StepFlag.SUCC.value())
    }

    /**
     * 获取指定构建的聚类信息，用于前端概览页面展示
     *
     * @param taskId
     * @param buildId
     */
    fun getClusterStatistic(taskId: Long, buildId: String): List<BaseClusterResultVO> {
        logger.info("get cluster statistic: $taskId $buildId")
        // 通过接口实现类遍历拿到所有工具的聚类信息
        val clusterBeans =
                applicationContext.getBeansOfType(ClusterDefectService::class.java)
        val clusterResultVOList = mutableListOf<BaseClusterResultVO>()
        clusterBeans.forEach { (_, clusterService) ->
            clusterResultVOList.add(clusterService.getClusterStatistic(taskId, buildId))
        }

        return clusterResultVOList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterDefectServiceImpl::class.java)
    }
}
