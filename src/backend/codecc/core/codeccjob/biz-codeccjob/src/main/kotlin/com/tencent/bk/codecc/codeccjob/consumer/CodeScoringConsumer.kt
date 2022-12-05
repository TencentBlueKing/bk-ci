package com.tencent.bk.codecc.codeccjob.consumer

import com.google.common.collect.Lists
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.CCNDefectRepository
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.CCNStatisticRepository
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.CLOCStatisticRepository
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintDefectV2Repository
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintStatisticRepository
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.MetricsRepository
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.TaskLogRepository
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.MetricsDao
import com.tencent.bk.codecc.defect.model.CCNDefectEntity
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity
import com.tencent.bk.codecc.defect.model.LintStatisticEntity
import com.tencent.bk.codecc.defect.model.MetricsEntity
import com.tencent.bk.codecc.defect.model.TaskLogEntity
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.pojo.EmailMessageModel
import com.tencent.bk.codecc.task.pojo.WeChatMessageModel
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.StepFlag
import com.tencent.devops.common.web.mq.EXCHANGE_CODECC_GENERAL_NOTIFY
import com.tencent.devops.common.web.mq.EXCHANGE_SCORING_OPENSOURCE
import com.tencent.devops.common.web.mq.QUEUE_SCORING_OPENSOURCE
import com.tencent.devops.common.web.mq.ROUTE_CODECC_BKPLUGINEMAIL_NOTIFY
import com.tencent.devops.common.web.mq.ROUTE_CODECC_BKPLUGINWECHAT_NOTIFY
import com.tencent.devops.common.web.mq.ROUTE_SCORING_OPENSOURCE
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import java.math.BigDecimal
import java.util.Arrays
import java.util.Comparator
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.math.pow

@Service
class CodeScoringConsumer @Autowired constructor(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate,
    private val taskLogRepository: TaskLogRepository,
    private val ccnStatisticRepository: CCNStatisticRepository,
    private val lintStatisticRepository: LintStatisticRepository,
    private val clocStatisticRepository: CLOCStatisticRepository,
    private val lintDefectV2Repository: LintDefectV2Repository,
    private val ccnDefectRepository: CCNDefectRepository,
    private val metricsRepository: MetricsRepository,
    private val metricsDao: MetricsDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CodeScoringConsumer::class.java)
    }

    @Value("\${codecc.public.url:#{null}}")
    private val codeccGateWay: String? = null

    /**
     * 对蓝盾插件代码打分
     * 不合格的发送邮件提醒给代码告警人
     */
    @RabbitListener(bindings = [QueueBinding(key = [ROUTE_SCORING_OPENSOURCE],
            value = Queue(value = QUEUE_SCORING_OPENSOURCE, durable = "true"),
            exchange = Exchange(value = EXCHANGE_SCORING_OPENSOURCE, durable = "true", delayed = "true"))])
    fun scoring() {
        val exeNum = AtomicInteger(0)
        try {
            logger.info("scoring begin")
            val res = client.get(ServiceTaskRestResource::class.java).bkPluginTaskIds
            if (res == null || res.isNotOk() || res.data == null) {
                logger.error("fail to get bk plugin taskIds, res: {}", res)
                return
            }

            val taskIdList = res.data
            val metricsMap = ConcurrentHashMap<Long, MetricsEntity>()
            taskIdList?.parallelStream()?.forEach { taskId ->
                try {
                    val taskLogList: MutableList<TaskLogEntity> = findLastBuildInfo(taskId)
                            .stream()
                            .sorted(Comparator.comparing(TaskLogEntity::getStartTime).reversed())
                            .collect(Collectors.toList())

                    val currBuildLog = taskLogList.firstOrNull()
                    // 当前任务最近没有被扫描过，不再邮件提醒用户
                    if (currBuildLog != null
                            && metricsRepository.findFirstByTaskIdAndBuildId(taskId, currBuildLog.buildId) != null) {
                        logger.info(
                                "scoring abort because of not new build, taskId: {}, buildId: {}",
                                taskId,
                                currBuildLog.buildId
                        )
                        return@forEach
                    }

                    var totalLine = 0
                    var language = "other"
                    var totalCcnExceedNum = 0.0
                    var totalSeriousRiskCount = 0
                    var totalNormalRiskCount = 0
                    var totalSeriousWaringCount = 0
                    var totalNormalWaringCount = 0
                    // 没有CLOC工具的任务不合法，无法计算分数
                    val isLegal = taskLogList.stream().anyMatch { taskLog -> taskLog.toolName == "CLOC" }
                    if (!isLegal) {
                        logger.error("fail to scoring: no match CLOC")
                        return@forEach
                    }

                    for (taskLog in taskLogList) {
                        logger.info("scoring toolName: {}", taskLog.toolName)
                        when (taskLog.toolName) {
                            ComConstants.Tool.CCN.name -> {
                                totalCcnExceedNum = getDefectNum(taskLog.toolName, taskId, taskLog.buildId).first
                            }
                            ComConstants.Tool.WOODPECKER_SENSITIVE.name -> {
                                val pair = getDefectNum(taskLog.toolName, taskId, taskLog.buildId)
                                totalNormalRiskCount = pair.second.toInt()
                                totalSeriousRiskCount = pair.first.toInt()
                            }
                            ComConstants.Tool.CLOC.name -> {
                                val pair = getDefectNum(taskLog.toolName, taskId, taskLog.buildId)
                                totalLine = pair.first.toInt()
                                if (pair.second.toInt() == 1)
                                    language = "JavaScript"
                                if (pair.second.toInt() == 2)
                                    language = "Python"
                                if (pair.second.toInt() == 3)
                                    language = "Go"
                                if (pair.second.toInt() == 0)
                                    language = "Java"
                            }
                            ComConstants.Tool.DUPC.name -> {
                            }
                            else -> {
                                val pair = getDefectNum(taskLog.toolName, taskId, taskLog.buildId)
                                totalSeriousWaringCount += pair.first.toInt()
                                totalNormalWaringCount += pair.second.toInt()
                            }
                        }
                    }

                    // 计算得分，不合格的根据告警作者发送邮件提醒
                    var authorSet = mutableSetOf<String>()
                    val codeStyleScore = calCodeStyleScore(totalSeriousWaringCount, totalNormalWaringCount, totalLine, language)
                    val codeSecurityScore = calCodeSecurityScore(totalSeriousRiskCount, totalNormalRiskCount)
                    val codeMeasureScore = calCodeMeasureScore(0, 0, totalLine, totalCcnExceedNum)
                    val rdIndicatorsScore = calRDIndicatorsScore(codeStyleScore, codeSecurityScore, codeMeasureScore)
                    if (rdIndicatorsScore < 85.26) {
                        exeNum.incrementAndGet()
                        if (codeStyleScore < 95) {
                            val defectList = lintDefectV2Repository.findByTaskIdAndStatus(taskId, 1)
                            authorSet.addAll(defectList.stream().map(LintDefectV2Entity::getAuthor).collect(Collectors.toSet()))
                        }
                        if (codeSecurityScore < 95) {
                            val defectList = lintDefectV2Repository.findByTaskIdAndToolNameAndStatus(taskId, ComConstants.Tool.WOODPECKER_SENSITIVE.name, 1)
                            authorSet.addAll(defectList.stream().map(LintDefectV2Entity::getAuthor).collect(Collectors.toSet()))
                        }
                        if (codeMeasureScore < 90) {
                            val defectList = ccnDefectRepository.findByTaskIdAndStatus(taskId, 1)
                            authorSet.addAll(defectList.stream().map(CCNDefectEntity::getAuthor).collect(Collectors.toSet()))
                        }

                        authorSet = authorSet.stream().filter { author -> StringUtils.isNotBlank(author) }.collect(Collectors.toSet())

                        authorSet.add("admin")
                        val repoMap = getLastAnalyzeRepoInfo(taskId)
                        val repoInfo = repoMap[repoMap.keys.stream().findFirst().get()]
                        logger.info("scoring send email {} | {} | {} | {} | {} | {}", taskId, codeStyleScore, codeSecurityScore, codeMeasureScore, rdIndicatorsScore, authorSet.size)
                        if (authorSet.isNotEmpty()) {
                            val bodyParams = mapOf(
                                    "codeStyleScore" to "${BigDecimal(codeStyleScore).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()}",
                                    "codeSecurityScore" to "${BigDecimal(codeSecurityScore).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()}",
                                    "codeMeasureScore" to "${BigDecimal(codeMeasureScore).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()}",
                                    "rdIndicatorsScore" to "$rdIndicatorsScore",
                                    "codeccDetailUrl" to "$codeccGateWay/codecc/codecc/task/$taskId/detail",
                                    "repoUrl" to "${repoInfo?.repoUrl} # ${repoInfo?.branch}"
                            )

                            val emailMessageModel = EmailMessageModel()
                            with(emailMessageModel) {
                                this.receivers = authorSet
                                this.cc = emptySet()
                                this.bcc = emptySet()
                                this.contentParam = bodyParams
                                this.template = ComConstants.EmailNotifyTemplate.BK_PLUGIN_FAILED_TEMPLATE
                                this.priority = "-1"
                                rabbitTemplate.convertAndSend(EXCHANGE_CODECC_GENERAL_NOTIFY, ROUTE_CODECC_BKPLUGINEMAIL_NOTIFY, this)
                            }
                        }
                    }

                    // 记录度量记录
                    metricsMap[taskId] = MetricsEntity(
                            taskId,
                            currBuildLog?.buildId,
                            codeStyleScore,
                            codeSecurityScore,
                            codeMeasureScore,
                            rdIndicatorsScore
                    )
                } catch (e: Throwable) {
                    logger.info("codecc scoring exception: ", e)
                }
            }
            // 保存度量记录
            val metricsList = mutableListOf<MetricsEntity>()
            metricsList.addAll(metricsMap.values)
            metricsDao.batchUpsert(metricsList)

            val unqualifiedList = metricsList.stream()
                    .filter { metrics -> metrics.codeSecurityScore < 100 }
                    .collect(Collectors.toList())

            logger.info("scoring unqualifiedList {}", unqualifiedList.size)
            if (!CollectionUtils.isEmpty(unqualifiedList)) {
                // 发送告警邮件给蓝盾插件商店负责人
                sendWeChat(unqualifiedList)
            }
        } catch (e: Throwable) {
            logger.info("", e)
        } finally {
            logger.info("exe num: {}", exeNum.get())
        }
    }

    /**
     * 获取任务对应告警
     * @param toolName
     * @param taskId
     * @param buildId
     */
    private fun getDefectNum(toolName: String, taskId: Long, buildId: String): Pair<Double, Double> {
        val pair: Pair<Double, Double>
        when (toolName) {
            "CCN" -> {
                val res = ccnStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
                pair = Pair(res.ccnBeyondThresholdSum.toDouble(), 0.toDouble())
            }
            "WOODPECKER-SENSITIVE" -> {
                val res = lintStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
                pair = Pair(res.totalSerious.toDouble(), res.totalNormal.toDouble())
            }
            "CLOC" -> {
                val res = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
                var totalLine = 0L
                var language = -1
                res.forEach {
                    totalLine += it.sumBlank + it.sumCode + it.sumComment
                    if (it.language == "JavaScript")
                        language = 1
                    if (it.language == "Python")
                        language = 2
                    if (it.language == "Go")
                        language = 3
                    if (it.language == "Java")
                        language = 0
                }
                pair = Pair((totalLine).toDouble(), language.toDouble())
            }
            else -> {
                val res: LintStatisticEntity? = lintStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
                pair = if (res == null) {
                    logger.info("scoring defect statistic null {}", toolName)
                    Pair(0.toDouble(), 0.toDouble())
                } else {
                    logger.info("scoring defect num {} {} {}", toolName, res.totalSerious, res.totalNormal)
                    Pair(res.totalSerious.toDouble(), res.totalNormal.toDouble())
                }
            }
        }
        return pair
    }

    /**
     * 计算代码规范评分
     * @param totalSeriousWaringCount 总严重告警数
     * @param totalNormalWaringCount 总一般告警数
     * @param totalLine 代码总行数
     * @param language 开发语言
     */
    private fun calCodeStyleScore(
            totalSeriousWaringCount: Int,
            totalNormalWaringCount: Int,
            totalLine: Int,
            language: String
    ): Double {
        // 计算百行告警数，百行告警数=（严重告警数*1+一般告警数*0.5）/代码行数*100
        val hundredWaringCount = if (totalLine == 0)
            0.toDouble()
        else
            ((totalSeriousWaringCount * 1 + totalNormalWaringCount * 0.5) / totalLine) * 100
        // 从配置表中读取60分对应百行告警数配置
        val languageWaringConfigCount = when(language) {
            "Java" -> 6.toDouble()
            "JavaScript" -> 7.toDouble()
            "Python" -> 4.toDouble()
            "Go" -> 0.1
            else -> 6.toDouble()
        }
        // 计算代码规范评分
        return 100 * ((0.6.pow(1.toDouble() / languageWaringConfigCount)).pow(hundredWaringCount))
    }

    /**
     * 计算代码安全评分
     * @param totalSeriousRiskCount 总严重风险数
     * @param totalNormalRiskCount 总一般风险数
     */
    private fun calCodeSecurityScore(totalSeriousRiskCount: Int, totalNormalRiskCount: Int): Double {
        val score = if (totalSeriousRiskCount > 0) {
            0
        } else if (totalSeriousRiskCount == 0 && totalNormalRiskCount > 0) {
            50
        } else if (totalSeriousRiskCount == 0 && totalNormalRiskCount == 0) {
            100
        } else {
            logger.error("error CodeSecurity defect num, serious: {} | normal {}", totalSeriousRiskCount, totalNormalRiskCount)
            -1
        }
        return score.toDouble()
    }

    /**
     * 计算代码度量和检查评分
     * @param totalCoveritySeriousWaringCount 总严重告警数
     * @param totalCoverityNormalWaringCount 总一般告警数
     * @param totalLine 代码总行数
     * @param totalCcnExceedNum 代码圈复杂度总超标数
     */
    private fun calCodeMeasureScore(
            totalCoveritySeriousWaringCount: Int,
            totalCoverityNormalWaringCount: Int,
            totalLine: Int,
            totalCcnExceedNum: Double
    ): Double {
        // 计算圈复杂度千行平均超标数
        val thousandCcnCount = if(totalLine == 0)
            0.toDouble()
        else
            1000 * totalCcnExceedNum / totalLine
        // 计算圈复杂度得分
        val ccnScore = 60 - 40 * (thousandCcnCount - 3) / (3 - 0)
        // 计算严重告警数得分
        val coveritySeriousWaringScore = if (totalCoveritySeriousWaringCount > 0)
            0
        else
            100
        // 计算一般告警数千行均值
        val thousandCoverityNormalWaringCount = if(totalLine == 0)
            0.toDouble()
        else
            (1000 * totalCoverityNormalWaringCount / totalLine).toDouble()
        // 计算一般告警数得分评分
        val coverityNormalWaringScore = 60 - 40 * (thousandCoverityNormalWaringCount - 0.035) / (0.035 - 0)
        return 0.9 * ccnScore + 0.08 * coveritySeriousWaringScore + 0.02 * coverityNormalWaringScore
    }

    /**
     * 计算研发指标分数
     * @param codeMeasureScore
     * @param codeSecurityScore
     * @param codeStyleScore
     */
    private fun calRDIndicatorsScore(
            codeStyleScore: Double,
            codeSecurityScore: Double,
            codeMeasureScore: Double): Double {
        val rd = (codeStyleScore + codeSecurityScore + codeMeasureScore) * 0.25 + 25
        val bigDecimal = BigDecimal(rd)
        return bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
    }

    /**
     * 工具粒度获取最后一次成功分析记录
     * @param taskId
     */
    fun findLastBuildInfo(taskId: Long): MutableList<TaskLogEntity> {
        val lastTaskLog: TaskLogEntity = taskLogRepository.findFirstByTaskIdAndFlagOrderByStartTimeDesc(taskId, StepFlag.SUCC.value())
                ?: return Lists.newArrayList()
        var lastTaskLogList: MutableList<TaskLogEntity> = taskLogRepository.findByTaskIdAndBuildId(taskId, lastTaskLog.buildId)
        val failTaskLogList: MutableList<TaskLogEntity> = Lists.newLinkedList()
        for (taskLogEntity in lastTaskLogList) {
            if (taskLogEntity.flag != StepFlag.SUCC.value()) {
                val entity: TaskLogEntity? = taskLogRepository.findFirstByTaskIdAndToolNameAndFlagOrderByStartTimeDesc(taskId, taskLogEntity.toolName, StepFlag.SUCC.value())
                if (entity != null) {
                    failTaskLogList.add(entity)
                }
            }
        }
        lastTaskLogList.addAll(failTaskLogList)
        lastTaskLogList = lastTaskLogList.stream()
                .filter { taskLogEntity: TaskLogEntity? -> taskLogEntity != null && taskLogEntity.flag == StepFlag.SUCC.value() }
                .collect(Collectors.toList())
        return lastTaskLogList
    }

    /**
     * 获取最后一次分析的代码库信息
     * @param taskId
     */
    fun getLastAnalyzeRepoInfo(taskId: Long): Map<String, TaskLogRepoInfoVO> {
        val lastAnalyze = taskLogRepository.findFirstByTaskIdAndFlagOrderByStartTimeDesc(taskId,
                StepFlag.SUCC.value())
        if (lastAnalyze == null) {
            logger.warn("this task has been not ran, taskId: {}", taskId)
            return emptyMap()
        }
        val repoInfo: MutableMap<String, TaskLogRepoInfoVO> = HashMap()
        val buildId = lastAnalyze.buildId
        val lastAnalyzeList = taskLogRepository.findByTaskIdAndBuildId(taskId, buildId)
        lastAnalyzeList.forEach(Consumer { taskLogEntity: TaskLogEntity ->
            val steps = taskLogEntity.stepArray
            steps.forEach(Consumer { taskUnit: TaskLogEntity.TaskUnit ->
                val msg = taskUnit.msg
                if (org.apache.commons.lang.StringUtils.isNotBlank(msg) && msg.contains("代码库：")) {
                    val msgs = msg.split("\n").toTypedArray()
                    val msgList = Arrays.asList(*msgs)
                    msgList.stream().filter { m -> m.isNotBlank() }
                            .forEach(Consumer { s: String ->
                                try {
                                    val repoUrl = s.substring(s.indexOf("代码库：") + 4, s.indexOf("，版本号："))
                                    val revision = s.substring(s.indexOf("版本号：") + 4, s.indexOf("，提交时间"))
                                    val commitTime = s.substring(s.indexOf("提交时间：") + 5, s.indexOf("，提交人"))
                                    val commitUser = s.substring(s.indexOf("提交人：") + 4, s.indexOf("，分支"))
                                    val branch = s.substring(s.indexOf("分支：") + 3)
                                    val taskLogRepoInfoVO = TaskLogRepoInfoVO(repoUrl, revision, commitTime, commitUser, branch)
                                    repoInfo[repoUrl] = taskLogRepoInfoVO
                                } catch (e: Exception) {
                                    logger.error("代码库信息截取失败: {}", s)
                                    logger.info("{}", e)
                                }
                    })
                }
            })
        })
        return repoInfo
    }

    /**
     * 发送微信给蓝盾插件商店负责人
     * @param unqualifiedList
     */
    fun sendWeChat(unqualifiedList: MutableList<MetricsEntity>) {
        val taskIds = unqualifiedList.stream()
                .map(MetricsEntity::getTaskId)
                .collect(Collectors.toList())
        val result = client.get(ServiceTaskRestResource::class.java).getGongfengProjInfoByTaskId(taskIds)
        if (result.isNotOk() || result.data == null) {
            logger.error("soring get gongfeng project info fail")
        }

        val gongfengProject = result.data
        val unqualifiedRepo = StringBuilder()
        gongfengProject?.keys
                ?.stream()
                ?.forEach { taskId -> unqualifiedRepo.append("任务ID: $taskId 工蜂地址: ${gongfengProject[taskId]?.httpUrlToRepo}") }

        val bodyParams = mapOf(
                "repoUrl" to "$unqualifiedRepo"
        )
        val weChatMessageModel = WeChatMessageModel()
        with(weChatMessageModel) {
            this.template = ComConstants.WeChatNotifyTemplate.BK_PLUGIN_FAILED_TEMPLATE
            this.contentParam = bodyParams
            rabbitTemplate.convertAndSend(EXCHANGE_CODECC_GENERAL_NOTIFY, ROUTE_CODECC_BKPLUGINWECHAT_NOTIFY, this)
        }
    }
}
