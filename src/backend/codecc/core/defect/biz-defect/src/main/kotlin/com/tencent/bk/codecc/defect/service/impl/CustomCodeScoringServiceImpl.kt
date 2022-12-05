package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.constant.DefectConstants
import com.tencent.bk.codecc.defect.dao.ToolMetaCacheServiceImpl
import com.tencent.bk.codecc.defect.dao.mongorepository.*
import com.tencent.bk.codecc.defect.model.MetricsEntity
import com.tencent.bk.codecc.defect.model.TaskLogEntity
import com.tencent.bk.codecc.defect.pojo.StandardScoringConfig
import com.tencent.bk.codecc.defect.service.AbstractCodeScoringService
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService
import com.tencent.bk.codecc.defect.service.TaskLogService
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.service.annotation.CCN
import com.tencent.devops.common.service.annotation.tool_type.DEFECT
import com.tencent.devops.common.service.annotation.tool_type.SECURITY
import com.tencent.devops.common.service.annotation.tool_type.STANDARD
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

/**
 * 自定义规则集扫描结果打分逻辑，在当前扫描任务的扫描环境不符合开源扫描条件时触发当前打分逻辑
 * 对于自定义的打分逻辑，参考所有工具的扫描结果
 * 打分逻辑分三类组成：
 * 1. 代码规范得分：规范得分根据当前任务中所有的代码规范工具扫描的告警为参考
 * 2. 代码度量得分：度量得分分为 圈复杂度 和 所有缺陷类工具的扫描结果两个维度，圈复杂度按"千行超标数"计算，
 *    缺陷类工具扫描结果还包含安全告警，需要筛选出缺陷告警再计算，整体度量得分按照 圈复杂度：缺陷类工具 9：1 的比例计算
 * 3. 代码安全得分：安全得分根据当前任务中所有的代码安全工具扫描的告警为参考
 */
@Service("Custom")
class CustomCodeScoringServiceImpl @Autowired constructor(
        private val toolMetaCacheServiceImpl: ToolMetaCacheServiceImpl,
        private val taskLogRepository: TaskLogRepository,
        private val defectRepository: DefectRepository,
        private val lintDefectV2Repository: LintDefectV2Repository,
        private val ccnStatisticRepository: CCNStatisticRepository,
        private val metricsRepository: MetricsRepository,
        clocStatisticRepository: CLOCStatisticRepository,
        redisTemplate: RedisTemplate<String, String>,
        client: Client,
        taskLogService: TaskLogService,
        taskLogOverviewService: TaskLogOverviewService
): AbstractCodeScoringService(
        redisTemplate,
        taskLogService,
        client,
        taskLogOverviewService,
        clocStatisticRepository
) {

    override fun scoring(taskDetailVO: TaskDetailVO, buildId: String): MetricsEntity? {
        logger.info("start to scoring custom: ${taskDetailVO.taskId} $buildId")
        val taskId = taskDetailVO.taskId
        val taskLogList: MutableList<TaskLogEntity> =
                taskLogRepository.findByTaskIdAndBuildId(taskId, buildId)
                        .filter { it.toolName != ComConstants.Tool.SCC.name }.toMutableList()

        // 没有 CLOC 工具的话不进行度量计算
        val isLegal = taskLogList.stream()
                .anyMatch { taskLog -> taskLog.toolName == ComConstants.Tool.CLOC.name }
        if (!isLegal) {
            logger.error("fail to scoring: task {} no match CLOC", taskId)
            return null
        }

        // 获取 CLOC 扫描各语言代码行信息
        val lines = getCLOCDefectNum(taskId, ComConstants.Tool.CLOC.name, buildId)

        val metricsEntity = MetricsEntity()
        metricsEntity.taskId = taskId
        metricsEntity.buildId = buildId
        metricsEntity.isOpenScan = false
        // 工具按类型分类
        val toolTypeMap = taskLogList.groupBy {
            val tool = toolMetaCacheServiceImpl.getToolBaseMetaCache(it.toolName)
            tool.type
        }

        // 遍历当次构建包含的所有工具类型，按照类型触发相应的方法
        toolTypeMap.forEach { (type, taskLogList) ->
            val toolNameList = taskLogList.map { it.toolName }
            // 遍历方法匹配查询逻辑
            this::class.java.methods.find { method ->
                method.declaredAnnotations.find {
                    it.annotationClass.simpleName == type} != null
            }?.invoke(this, taskDetailVO, metricsEntity, toolNameList, lines)
        }

        // 最后根据 圈复杂度得分 和 缺陷工具得分计算 度量得分
        calCodeMeasureScore(metricsEntity)
        // 计算总分数
        calRDIndicatorsScore(metricsEntity)
        metricsRepository.save(metricsEntity)
        return metricsEntity
    }

    @DEFECT
    fun scoringDefect(taskDetailVO: TaskDetailVO, metricsEntity: MetricsEntity, toolNameList: List<String>, lines: MutableMap<String, Long>) {
        val taskId = metricsEntity.taskId
        val seriousCount = defectRepository.countByTaskIdAndToolNameInAndStatusAndSeverity(
                taskId,
                toolNameList,
                ComConstants.DefectStatus.NEW.value(),
                DefectConstants.DefectSeverity.SERIOUS.value()
        )
        val normalCount = defectRepository.countByTaskIdAndToolNameInAndStatusAndSeverity(
                taskId,
                toolNameList,
                ComConstants.DefectStatus.NEW.value(),
                DefectConstants.DefectSeverity.NORMAL.value()
        )

        val totalLine = lines.map { it.value }.sum()

        // 计算严重告警数得分
        val seriousWaringScore = if (seriousCount > 0)
            0
        else
            100
        // 计算严重告警数千行均值
        val thousandSeriousWaringCount = if (totalLine == 0L)
            0.toDouble()
        else
            (1.0 * 1000 * (seriousCount.toDouble() / totalLine.toDouble()))

        // 计算一般告警数千行均值
        val thousandNormalWaringCount = if (totalLine == 0L)
            0.toDouble()
        else
            (1.0 * 1000 * (normalCount.toDouble() / totalLine.toDouble()))
        // 计算一般告警数得分评分
        val normalWaringScore = 60 - 40 * (thousandNormalWaringCount - 0.035) / (0.035 - 0)
        val defectScore = BigDecimal(0.8 * seriousWaringScore + 0.2 * normalWaringScore)
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .toDouble()

        metricsEntity.codeDefectScore = if (defectScore >= 0) {
            defectScore
        } else {
            0.toDouble()
        }
        metricsEntity.codeDefectNormalDefectCount = normalCount
        metricsEntity.codeDefectSeriousDefectCount = seriousCount
        metricsEntity.averageNormalDefectThousandDefect = BigDecimal(thousandNormalWaringCount)
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .toDouble()
        metricsEntity.averageSeriousDefectThousandDefect = BigDecimal(thousandSeriousWaringCount)
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .toDouble()
    }

    @SECURITY
    fun scoringSecurity(taskDetailVO: TaskDetailVO, metricsEntity: MetricsEntity, toolNameList: List<String>, lines: MutableMap<String, Long>) {
        val taskId = metricsEntity.taskId
        val seriousCount = lintDefectV2Repository.countByTaskIdAndToolNameInAndStatusAndSeverity(
                taskId,
                toolNameList,
                ComConstants.DefectStatus.NEW.value(),
                ComConstants.SERIOUS
        )
        val normalCount = lintDefectV2Repository.countByTaskIdAndToolNameInAndStatusAndSeverity(
                taskId,
                toolNameList,
                ComConstants.DefectStatus.NEW.value(),
                ComConstants.NORMAL
        )
        val score = if (seriousCount > 0) {
            0
        } else if (seriousCount == 0 && normalCount > 0) {
            50
        } else if (seriousCount == 0 && normalCount == 0) {
            100
        } else {
            logger.error(
                    "error CodeSecurity defect num, serious: {} | normal {}",
                    seriousCount,
                    normalCount
            )
            -1
        }
        metricsEntity.codeSecurityNormalDefectCount = normalCount
        metricsEntity.codeSecuritySeriousDefectCount = seriousCount
        metricsEntity.codeSecurityScore = if (score >= 0) {
            score.toDouble()
        } else {
            0.toDouble()
        }
    }

    @CCN
    fun scoringCcn(taskDetailVO: TaskDetailVO, metricsEntity: MetricsEntity, toolNameList: List<String>, lines: MutableMap<String, Long>) {
        val taskId = metricsEntity.taskId
        val buildId = metricsEntity.buildId
        val ccnStatistic = ccnStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
        val totalLine = lines.map { it.value }.sum()
        val totalCcnExceedNum = ccnStatistic.ccnBeyondThresholdSum.toDouble()
        // 计算圈复杂度千行平均超标数
        val thousandCcnCount = if (totalLine == 0L)
            0.toDouble()
        else
            (1000 * (totalCcnExceedNum / totalLine.toDouble()))
        // 计算圈复杂度得分
        val ccnScore = 60.toDouble() - 40.toDouble() * (thousandCcnCount - 3) / (3 - 0)

        metricsEntity.averageThousandDefect = thousandCcnCount
        metricsEntity.codeCcnScore = if (ccnScore >= 0 ) {
            BigDecimal(ccnScore).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
        } else {
            0.toDouble()
        }
    }

    // @STANDARD
    @Deprecated(message = "旧的代码规范不支持配置化计分", replaceWith = ReplaceWith("newScoringStandard"))
    fun scoringStandard(metricsEntity: MetricsEntity, toolNameList: List<String>, lines: MutableMap<String, Long>) {
        val taskId = metricsEntity.taskId
        var score = 0.toDouble()
        val totalLines = lines.map { it.value }.sum()
        var otherLines = totalLines
        var pyDefect = 0.toDouble()
        var languageWaringConfigCount: Double
        var otherDefect = 0.toDouble()
        var totalNormalDefectNum = 0
        var totalSeriousDefectNum = 0
        toolNameList.forEach{ toolName ->
            val seriousCount = lintDefectV2Repository.countByTaskIdAndToolNameAndStatusAndSeverity(
                    taskId,
                    toolName,
                    ComConstants.DefectStatus.NEW.value(),
                    ComConstants.SERIOUS
            )
            val normalCount = lintDefectV2Repository.countByTaskIdAndToolNameAndStatusAndSeverity(
                    taskId,
                    toolName,
                    ComConstants.DefectStatus.NEW.value(),
                    ComConstants.NORMAL
            )
            val totalLine: Long
            totalNormalDefectNum += normalCount
            totalSeriousDefectNum += seriousCount
            when (toolName) {
                ComConstants.Tool.GOML.name -> {
                    languageWaringConfigCount = 0.1
                    totalLine = lines["Go"] ?: 0L
                }
                ComConstants.Tool.PYLINT.name -> {
                    pyDefect += seriousCount * 1.0 + normalCount * 0.5
                    return@forEach
                }
                ComConstants.Tool.FLAKE8.name -> {
                    pyDefect += seriousCount * 1.0 + normalCount * 0.5
                    return@forEach
                }
                ComConstants.Tool.ESLINT.name -> {
                    languageWaringConfigCount = 7.toDouble()
                    totalLine = lines["JavaScript"] ?: 0L
                }
                ComConstants.Tool.STYLECOP.name -> {
                    languageWaringConfigCount = 8.0
                    totalLine = lines["C#"] ?: 0L
                }
                ComConstants.Tool.CHECKSTYLE.name -> {
                    languageWaringConfigCount = 6.0
                    totalLine = lines["Java"] ?: 0L
                }
                ComConstants.Tool.CPPLINT.name -> {
                    languageWaringConfigCount = 7.0
                    totalLine = (lines["C"] ?: 0L) +
                            (lines["C++"] ?: 0L) + (lines["C/C++ Header"] ?: 0L)
                }
                "BKCHECK-OC" -> {
                    languageWaringConfigCount = 3.0
                    totalLine = (lines["Objective C"] ?: 0L) +
                            (lines["Objective C++"] ?: 0L)
                }
                else -> {
                    otherDefect += seriousCount * 1.0 + normalCount * 0.5
                    return@forEach
                }
            }
            if (totalLine == 0L) {
                return@forEach
            }

            otherLines -= totalLine
            // 计算百行告警数，百行告警数=（严重告警数*1+一般告警数*0.5）/代码行数*100
            val totalDefect = seriousCount * 1.0 + normalCount * 0.5
            logger.info("cal $toolName code style score, totalDefect: $totalDefect | line: $totalLine")
            score += calCodeStyleScore(totalDefect, languageWaringConfigCount, totalLine, totalLines)
        }
        // 单独计算 python 的规范分数
        if ((lines["Python"] ?: 0L) != 0L || pyDefect != 0.0) {
            logger.info("cal python code style score, totalDefect: $pyDefect | line: ${lines["Python"] ?: 0} | totalLine: $totalLines")
            otherLines -= (lines["Python"] ?: 0L)
            score += calCodeStyleScore(pyDefect, 4.toDouble(), (lines["Python"] ?: 0L), totalLines)
        }
        logger.info("cal other code style score, totalDefect: $otherDefect | line: $otherLines | totalLine: $totalLines")
        // 计算规范总分数
        score += calCodeStyleScore(otherDefect, 6.toDouble(), otherLines, totalLines)
        // 计算总告警行占比
        val averageNormalStandardThousandDefect = BigDecimal(1000.toDouble() *
                (totalNormalDefectNum.toDouble() / totalLines.toDouble()))
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .toDouble()

        val averageSeriousStandardThousandDefect = BigDecimal(1000.toDouble() *
                (totalSeriousDefectNum.toDouble() / totalLines.toDouble()))
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .toDouble()

        metricsEntity.codeStyleScore = if (score >= 0) {
            BigDecimal(score).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
        } else {
            0.toDouble()
        }
        metricsEntity.averageNormalStandardThousandDefect = averageNormalStandardThousandDefect
        metricsEntity.averageSeriousStandardThousandDefect = averageSeriousStandardThousandDefect
        metricsEntity.codeStyleNormalDefectCount = totalNormalDefectNum
        metricsEntity.codeStyleSeriousDefectCount = totalSeriousDefectNum
    }

    @STANDARD
    fun newScoringStandard(
            taskDetailVO: TaskDetailVO,
            metricsEntity: MetricsEntity,
            actualExeTools: MutableList<String>,
            lines: MutableMap<String, Long>
    ) {
        val scoringConfigs = initScoringConfigs(taskDetailVO, lines)
        if (scoringConfigs.isEmpty()) {
            return
        }

        val totalLine = scoringConfigs.filter {
            if (it.value.clocLanguage.isEmpty()) {
                it.key != ComConstants.CodeLang.OTHERS
            } else {
                true
            }
        }.map { it.value.lineCount }.sum()
        scoringConfigs.filter {
            it.key != ComConstants.CodeLang.OTHERS
        }.forEach { (lang, config) ->
            val matchingTools = actualExeTools.filter {
                lang.langValue().and(toolMetaCacheServiceImpl.getToolBaseMetaCache(it).lang) > 0
            }.toMutableList()
            if (matchingTools.isEmpty()) {
                logger.info("${lang.langName()} has not install standard tool")
                return@forEach
            }
            scoringConfigLangStandard(
                    metricsEntity,
                    matchingTools,
                    config,
                    totalLine
            )
            actualExeTools.removeAll(matchingTools)
        }

        if (actualExeTools.isNotEmpty() && scoringConfigs[ComConstants.CodeLang.OTHERS] != null) {
            scoringConfigLangStandard(
                    metricsEntity,
                    actualExeTools,
                    scoringConfigs[ComConstants.CodeLang.OTHERS]!!,
                    totalLine
            )
        }
        metricsEntity.averageSeriousStandardThousandDefect =
                BigDecimal(1000 * metricsEntity.codeStyleSeriousDefectCount.toDouble()
                        / totalLine.toDouble())
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble()
        metricsEntity.averageNormalStandardThousandDefect =
                BigDecimal(1000 * metricsEntity.codeStyleNormalDefectCount.toDouble()
                        / totalLine.toDouble())
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble()
    }

    fun scoringConfigLangStandard(
            metricsEntity: MetricsEntity,
            matchingTools: MutableList<String>,
            config: StandardScoringConfig,
            totalLine: Long
    ) {
        val seriousCount = lintDefectV2Repository.countByTaskIdAndToolNameInAndStatusAndSeverity(
                metricsEntity.taskId,
                matchingTools,
                ComConstants.DefectStatus.NEW.value(),
                ComConstants.SERIOUS
        )
        val normalCount = lintDefectV2Repository.countByTaskIdAndToolNameInAndStatusAndSeverity(
                metricsEntity.taskId,
                matchingTools,
                ComConstants.DefectStatus.NEW.value(),
                ComConstants.NORMAL
        )
        // 计算行占比
        val totalDefect = seriousCount * 1.0 + normalCount * 0.5
        val currScore = calCodeStyleScore(
                totalDefect,
                config.coefficient,
                config.lineCount,
                totalLine
        ) + metricsEntity.codeStyleScore
        logger.info("cal ${config.clocLanguage} code style score, totalDefect: $totalDefect " +
                "| line: ${config.lineCount} | totalLine: $totalLine | score: $currScore")
        metricsEntity.codeStyleScore = currScore
        metricsEntity.codeStyleSeriousDefectCount += seriousCount
        metricsEntity.codeStyleNormalDefectCount += normalCount
    }

    /**
     * 计算代码规范评分，多语言项目按照代码行比例计算分数
     * @param totalDefect
     * @param line
     * @param totalLine
     * @param languageWaringConfigCount
     */
    private fun calCodeStyleScore(totalDefect: Double, languageWaringConfigCount: Double, line: Long, totalLine: Long): Double {
        val hundredWaringCount = if (line == 0L) {
            0.toDouble()
        } else {
            (totalDefect / line) * 100.toDouble()
        }
        // 计算行占比
        val linePercentage = line.toDouble() / totalLine.toDouble()
        // 计算代码规范评分
        val styleScore =  BigDecimal(100 * linePercentage * ((0.6.pow(1.toDouble() / languageWaringConfigCount)).pow(hundredWaringCount)))
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .toDouble()
        return if (styleScore >= 0) {
            styleScore
        } else {
            0.toDouble()
        }
    }

    fun calCodeMeasureScore(metricsEntity: MetricsEntity) {
        val measureScore = BigDecimal(0.9 * metricsEntity.codeCcnScore + 0.1 * metricsEntity.codeDefectScore)
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .toDouble()
        metricsEntity.codeMeasureScore = if (measureScore >= 0) {
            measureScore
        } else {
            0.toDouble()
        }
    }

    /**
     * 计算研发指标分数
     * @param metricsEntity
     */
    private fun calRDIndicatorsScore(metricsEntity: MetricsEntity) {
        val rd = (metricsEntity.codeStyleScore + metricsEntity.codeSecurityScore + metricsEntity.codeMeasureScore)*
                0.25 + 25
        val bigDecimal = BigDecimal(rd)
        metricsEntity.rdIndicatorsScore = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CustomCodeScoringServiceImpl::class.java)
    }
}
