package com.tencent.bk.codecc.defect.service

import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository
import com.tencent.bk.codecc.defect.model.MetricsEntity
import com.tencent.bk.codecc.defect.pojo.StandardScoringConfig
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.redis.lock.RedisLock
import com.tencent.devops.common.util.JsonUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
abstract class AbstractCodeScoringService @Autowired constructor(
        private val redisTemplate: RedisTemplate<String, String>,
        private val taskLogService: TaskLogService,
        private val client: Client,
        private val taskLogOverviewService: TaskLogOverviewService,
        private val clocStatisticRepository: CLOCStatisticRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    abstract fun scoring(taskDetailVO: TaskDetailVO, buildId: String): MetricsEntity?

    /**
     * 通用代码度量计算逻辑，普通扫描和超快增量都走这里
     * 当本次工具上报完成后其他工具上报未完成时不执行度量计算
     * 必须等所有工具执行完成并成功才执行度量计算逻辑
     *
     * @param taskDetailVO 任务信息实体
     * @param buildId 构建号
     * @param toolName 工具名称
     * @param type 标记是 普通扫描还是超快增量扫描
     */
    fun scoring(taskDetailVO: TaskDetailVO, buildId: String, toolName: String, type: String) {

        val taskStatus = getTaskStatus(taskDetailVO.taskId, buildId)
        // 不等待正在执行的任务,若任务还在执行中则不计算度量信息
        if (taskStatus.second != ComConstants.StepFlag.SUCC.value()) {
            logger.info(taskStatus.first)
            return
        }

        val redisLock = RedisLock(
                redisTemplate = redisTemplate,
                lockKey = "${RedisKeyConstants.TASK_CODE_SCORING}${taskDetailVO.taskId}:$buildId",
                expiredTimeInSeconds = 5
        )
        try {
            if (redisLock.tryLock()) {
                logger.info(
                        "get redis lock: taskId: {} | buildId: {} | toolName: {}, type: {}",
                        taskDetailVO.taskId,
                        buildId,
                        toolName,
                        type
                )
                val metricsEntity = scoring(taskDetailVO, buildId) ?: return
            }
        } catch (e: Exception) {
            logger.error("", e)
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 取当前任务设置的语言集合,
     * 根据 codeLang 与各语言数值 按位与 大于0的为当前任务绑定的语言
     * @param taskDetailVO
     */
    fun initScoringConfigs(
            taskDetailVO: TaskDetailVO,
            lines: MutableMap<String, Long>
    ): MutableMap<ComConstants.CodeLang, StandardScoringConfig> {
        logger.info("init scoring configurations:taskId:${taskDetailVO.taskId}")
        val languageList = mutableListOf<ComConstants.CodeLang>()
        ComConstants.CodeLang.values().filter {
            it != ComConstants.CodeLang.OTHERS
        }.forEach {
            if (taskDetailVO.codeLang.and(it.langValue()) > 0) {
                languageList.add(it)
            }
        }

        val scoringConfigs = mutableMapOf<ComConstants.CodeLang, StandardScoringConfig>()
        val iterator = languageList.iterator()
        while (iterator.hasNext()) {
            val lang = iterator.next()
            val configJsonStr: String? = redisTemplate.opsForHash<String, String>()
                    .get(RedisKeyConstants.STANDARD_LANG, lang.langName())
            if (!configJsonStr.isNullOrBlank()) {
                val standardScoringConfig = JsonUtil.to(
                        configJsonStr,
                        StandardScoringConfig::class.java
                )
                val clocLangList = lines.filter {
                    standardScoringConfig.clocLanguage.contains(it.key)
                }
                if (clocLangList.isNotEmpty()) {
                    standardScoringConfig.lineCount = clocLangList.map { it.value }.sum()
                    scoringConfigs[lang] = standardScoringConfig
                    clocLangList.forEach { (t, _) ->
                        lines.remove(t)
                    }
                }
                iterator.remove()
            }
        }

        // 当用户选了七种语言之外的语言并且cloc中有七种语言之外的数据时，把这些数据统一到 Others 中
        logger.info("init other before: $languageList $lines")
        if (languageList.size > 0 && lines.size > 0) {
            val othersConfig = StandardScoringConfig()
            languageList.forEach { lang ->
                othersConfig.clocLanguage.add(lang.langName())
            }
            othersConfig.lineCount = lines.map { it.value }.sum()
            scoringConfigs[ComConstants.CodeLang.OTHERS] = othersConfig
        }
        return scoringConfigs
    }

    /**
     * 获取任务执行状态
     * 当所有工具都执行成功时才标记成功
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
                    ComConstants.StepFlag.FAIL.value()
            )
        }

        val toolList = result.data
        // 获取任务的实际执行工具
        val actualExeTools = taskLogOverviewService.getActualExeTools(taskId, buildId)
        // 判断任务是否执行完毕的时候根据任务设置的扫描工具和实际扫描的工具决定
        toolList?.enableToolList?.filter { toolConfigBaseVO ->
            actualExeTools?.contains(toolConfigBaseVO.toolName) ?: true
        }
                ?.forEach { tool ->
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
     * 获取任务对应代码行告警
     * 按语言区分
     * @param taskId
     * @param buildId
     */
     fun getCLOCDefectNum(taskId: Long, toolName: String, buildId: String): MutableMap<String, Long> {
        val res = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
        val pairs = mutableMapOf<String, Long>()
        res.forEach {
            val totalLine = it.sumCode
            pairs[it.language] = totalLine
        }
        return pairs
    }
}
