package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.constant.DefectConstants
import com.tencent.bk.codecc.defect.dao.ToolMetaCacheServiceImpl
import com.tencent.bk.codecc.defect.dao.mongorepository.*
import com.tencent.bk.codecc.defect.model.LintStatisticEntity
import com.tencent.bk.codecc.defect.model.MetricsEntity
import com.tencent.bk.codecc.defect.model.TaskLogEntity
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity
import com.tencent.bk.codecc.defect.pojo.StandardScoringConfig
import com.tencent.bk.codecc.defect.service.AbstractCodeScoringService
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService
import com.tencent.bk.codecc.defect.service.TaskLogService
import com.tencent.bk.codecc.task.vo.GrayTaskStatVO
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

/**
 * 开源治理规则集扫描结果打分逻辑，在当前扫描任务的扫描环境符合开源扫描条件时触发当前打分逻辑
 * 对于开源治理打分逻辑，只参考特定工具的告警数据
 * 打分逻辑分三类组成：
 * 1. 代码规范得分：规范得分根据"腾讯代码规范"规则集中的代码规范工具扫描的告警为参考，当出现其他规则集以外的工具时直接忽略不参考
 * 2. 代码度量得分：度量得分分为 圈复杂度 和 Coverity 工具的扫描结果两个维度，圈复杂度按"千行超标数"计算，Coverity 工具扫描结果还包含安全告警，
 *    需要筛选出缺陷告警再计算，整体度量得分按照 圈复杂度：Coverity 9：1 的比例计算
 * 3. 代码安全得分：代码安全只包含 "啄木鸟" 相关工具和 Coverity 中的安全告警，当出现其他 "IP硬编码" 等工具时忽略不参考
 */
@Service("TStandard")
class CodeScoringServiceImpl @Autowired constructor(
        private val taskLogRepository: TaskLogRepository,
        private val ccnStatisticRepository: CCNStatisticRepository,
        private val lintStatisticRepository: LintStatisticRepository,
        private val defectRepository: DefectRepository,
        private val metricsRepository: MetricsRepository,
        private val lintDefectV2Repository: LintDefectV2Repository,
        private val toolMetaCacheServiceImpl: ToolMetaCacheServiceImpl,
        private val checkerSetProjectRelationshipRepository: CheckerSetProjectRelationshipRepository,
        private val checkerSetRepository: CheckerSetRepository,
        redisTemplate: RedisTemplate<String, String>,
        clocStatisticRepository: CLOCStatisticRepository,
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

    companion object {
        private val logger = LoggerFactory.getLogger(CodeScoringServiceImpl::class.java)
        private val languageList = listOf(
            "Java",
            "Go",
            "Python",
            "JavaScript",
            "C#",
            "C/C++ Header",
            "C",
            "C++",
            "Objective C",
            "Objective C++"
        )
    }

    /**
     * 通用代码度量计算逻辑，普通扫描和超快增量都走这里
     * 具体的计算逻辑都在这里
     *
     * @param taskId 任务ID
     * @param buildId 构建号
     */
    override fun scoring(taskDetailVO: TaskDetailVO, buildId: String): MetricsEntity? {
        try {
            val taskId = taskDetailVO.taskId
            val taskLogList: MutableList<TaskLogEntity> =
                    taskLogRepository.findByTaskIdAndBuildId(taskId, buildId)
                            .filter { it.toolName != ComConstants.Tool.SCC.name }.toMutableList()
            var lines = mutableMapOf<String, Long>()
            val tools = mutableMapOf<String, Pair<Double, Double>>()
            var totalCcnExceedNum = 0.0
            var totalSeriousRiskCount = 0
            var totalNormalRiskCount = 0
            var totalCoveritySeriousWaringCount = 0
            var totalCoverityNormalWaringCount = 0
            // 分别对应当前任务是否含有三个维度对应的工具，没有的话这个维度分数计为 100
            var styleExists = false
            var measureExists = false
            var securityExists = false
            var ccnExists = false
            var covExists = false
            // 没有CLOC工具的任务不合法，无法计算分数
            val isLegal = taskLogList.stream()
                .anyMatch { taskLog -> taskLog.toolName == ComConstants.Tool.CLOC.name }
            if (!isLegal) {
                logger.error("fail to scoring: task {} no match CLOC", taskId)
                return null
            }

            for (taskLog in taskLogList) {
                val tool = toolMetaCacheServiceImpl.getToolBaseMetaCache(taskLog.toolName)
                logger.info("scoring toolName: {} {}", tool.name, tool.type)
                if (taskLog.toolName == ComConstants.Tool.CLOC.name) {
                    // 代码行工具
                    lines = getCLOCDefectNum(taskId, taskLog.toolName, taskLog.buildId)
                } else if (taskLog.toolName == ComConstants.Tool.CCN.name) {
                    // 圈复杂度工具
                    totalCcnExceedNum = getCCNDefectNum(taskId, taskLog.buildId).first
                    measureExists = true
                    ccnExists = true
                } else if (taskLog.toolName == ComConstants.Tool.DUPC.name) {
                    // 重复率工具，不参与度量计算
                    continue
                } else if (taskLog.toolName == ComConstants.Tool.COVERITY.name) {
                    // Coverity工具需要同时获取安全告警和度量告警
                    val pair = getDefectNum(ComConstants.Tool.COVERITY.name, taskDetailVO)
                    totalSeriousRiskCount += pair.first.first
                    totalNormalRiskCount += pair.first.second
                    totalCoveritySeriousWaringCount += pair.second.first
                    totalCoverityNormalWaringCount += pair.second.second
                    measureExists = true
                    securityExists = true
                    covExists = true
                    continue
                } else if (tool.type == ComConstants.ToolType.SECURITY.name) {
                    // 安全工具
                    val pair = getSecurityDefectNum(taskId, taskLog.buildId, tool.name)
                    totalNormalRiskCount += (pair?.second?.toInt() ?: 0)
                    totalSeriousRiskCount += (pair?.first?.toInt() ?: 0)
                    securityExists = true
                } else if (tool.name != ComConstants.Tool.IP_CHECK.name
                    && tool.type == ComConstants.ToolType.STANDARD.name
                ) {
                    // 代码格式工具
                    tools[taskLog.toolName] = getLintDefectNum(
                        taskLog.toolName,
                        taskId,
                        taskLog.buildId
                    )
                    styleExists = true
                }
            }

            val totalLine = lines.values.stream().mapToLong { line -> line }.sum()
            val metricsEntity = MetricsEntity()
            metricsEntity.taskId = taskId
            metricsEntity.buildId = buildId
            metricsEntity.isOpenScan = true
            // 代码规范得分
            val scoringConfigs = initScoringConfigs(taskDetailVO, lines)
            if (scoringConfigs.isNullOrEmpty() || !styleExists) {
                metricsEntity.codeStyleScore = 100.toDouble()
            } else {
                newScoringStandard(metricsEntity, tools.keys.toMutableList(), scoringConfigs)
            }

            // 代码安全得分
            metricsEntity.codeSecurityScore = if (securityExists) {
                calCodeSecurityScore(
                    totalSeriousRiskCount = totalSeriousRiskCount,
                    totalNormalRiskCount = totalNormalRiskCount
                )
            } else {
                100.toDouble()
            }

            // 度量得分
            val codeMeasureScorePair = if (measureExists) {
                calCodeMeasureScore(
                    totalCoveritySeriousWaringCount = totalCoveritySeriousWaringCount,
                    totalCoverityNormalWaringCount = totalCoverityNormalWaringCount,
                    totalLine = totalLine,
                    totalCcnExceedNum = totalCcnExceedNum
                )
            } else {
                Pair(100.toDouble(), 0.toDouble())
            }

            metricsEntity.codeMeasureScore = codeMeasureScorePair.first
            // 圈复杂度千行超标数
            metricsEntity.averageThousandDefect = codeMeasureScorePair.second
            // 圈复杂度得分
            metricsEntity.codeCcnScore = if (ccnExists) {
                calCcnScore(
                    totalLine = totalLine,
                    totalCcnExceedNum = totalCcnExceedNum
                )
            } else {
                100.toDouble()
            }
            // 缺陷得分，开源治理只有 Coverity 工具
            metricsEntity.codeDefectScore = if (covExists) {
                calCoverityScore(
                    totalCoveritySeriousWaringCount,
                    totalCoverityNormalWaringCount,
                    totalLine
                )
            } else {
                100.toDouble()
            }

            // 总分
            metricsEntity.rdIndicatorsScore = calRDIndicatorsScore(
                codeStyleScore = metricsEntity.codeStyleScore,
                codeSecurityScore = metricsEntity.codeSecurityScore,
                codeMeasureScore = metricsEntity.codeMeasureScore
            )

            // 记录各个维度分数计算所需的告警数据，存表以便于概览页面直接显示
            val codeDefectNormalDefectCount = totalCoverityNormalWaringCount

            val codeDefectSeriousDefectCount = totalCoveritySeriousWaringCount

            metricsEntity.codeDefectNormalDefectCount = codeDefectNormalDefectCount
            metricsEntity.codeDefectSeriousDefectCount = codeDefectSeriousDefectCount
            metricsEntity.codeSecurityNormalDefectCount = totalNormalRiskCount
            metricsEntity.codeSecuritySeriousDefectCount = totalSeriousRiskCount
            metricsEntity.averageNormalDefectThousandDefect = BigDecimal(
                1000.toDouble() *
                    (codeDefectNormalDefectCount.toDouble() / totalLine.toDouble())
            )
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .toDouble()
            metricsEntity.averageSeriousDefectThousandDefect = BigDecimal(
                1000.toDouble() *
                    (codeDefectSeriousDefectCount.toDouble() / totalLine.toDouble())
            )
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .toDouble()

            // 保存得分结果
            /*val metricsEntity = MetricsEntity(
                taskId,
                buildId,
                true,
                codeStyleScore,
                codeSecurityScore,
                codeMeasureScore,
                ccnScore,
                coverityScore,
                rdIndicatorsScore,
                averageThousandDefect,
                codeStyleNormalDefectCount,
                averageNormalStandardThousandDefect,
                codeStyleSeriousDefectCount,
                averageSeriousStandardThousandDefect,
                codeDefectNormalDefectCount,
                averageNormalDefectThousandDefect,
                codeDefectSeriousDefectCount,
                averageSeriousDefectThousandDefect,
                codeSecurityNormalDefectCount,
                codeSecuritySeriousDefectCount
            )*/

            metricsRepository.save(metricsEntity)
            return metricsEntity
        } catch (e: Throwable) {
            logger.info("codecc scoring exception: ", e)
        }
        return null
    }

    /**
     * 获取任务对应圈复杂度告警
     * @param taskId
     * @param buildId
     */
    private fun getCCNDefectNum(taskId: Long, buildId: String): Pair<Double, Double> {
        val res = ccnStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
        return Pair(res.ccnBeyondThresholdSum.toDouble(), 0.toDouble())
    }

    /**
     * 获取任务对应啄木鸟告警
     * @param taskId
     * @param buildId
     */
    private fun getSecurityDefectNum(taskId: Long, buildId: String, toolName: String): Pair<Double, Double>? {
        val res = lintStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(
            taskId,
            toolName,
            buildId
        ) ?: return null
        return Pair(res.totalSerious.toDouble(), res.totalNormal.toDouble())
    }

    /**
     * 获取任务对应告警
     * @param toolName
     * @param taskId
     * @param buildId
     */
    private fun getLintDefectNum(toolName: String, taskId: Long, buildId: String): Pair<Double, Double> {
        val res: LintStatisticEntity? = lintStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(
            taskId,
            toolName,
            buildId
        )
        return if (res == null) {
            logger.info("scoring defect statistic null {}", toolName)
            Pair(0.toDouble(), 0.toDouble())
        } else {
            logger.info("scoring defect num {} {} {}", toolName, res.totalSerious, res.totalNormal)
            Pair(res.totalSerious.toDouble(), res.totalNormal.toDouble())
        }
    }

    /**
     * 获取度量计算相关的告警数据
     * 根据 Coverity 告警的规则集所属决定告警属于安全问题还是度量缺陷问题
     *
     * @param toolName
     * @param taskDetailVO
     */
    private fun getDefectNum(toolName: String, taskDetailVO: TaskDetailVO): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        val defectList = defectRepository.findByTaskIdAndToolNameAndStatus(
            taskDetailVO.taskId,
            toolName,
            ComConstants.DefectStatus.NEW.value()
        )
        val projectId = taskDetailVO.projectId
        val checkerSetProj = checkerSetProjectRelationshipRepository.findByProjectId(projectId)
        val peckerCheckerSet = mutableSetOf<CheckerPropsEntity>()
        checkerSetProj.filter { it.checkerSetId.matches(Regex("^pecker_.*_no_coverity")) }
            .forEach {
                val checkerSetEntity =
                    checkerSetRepository.findFirstByCheckerSetIdAndVersion(it.checkerSetId, it.version)
                peckerCheckerSet.addAll(checkerSetEntity.checkerProps)
            }

        val checkerNameList = peckerCheckerSet.filter { it.toolName == ComConstants.Tool.COVERITY.name }
            .map { it.checkerName }
            .toList()

        val securitySeriousCount = defectList.filter {
            checkerNameList.contains(it.checkerName)
                && it.severity == DefectConstants.DefectSeverity.SERIOUS.value()
        }.count()

        val securityNormalCount = defectList.filter {
            checkerNameList.contains(it.checkerName)
                && it.severity == DefectConstants.DefectSeverity.NORMAL.value()
        }.count()

        val seriousCount = defectList.filter { it.severity == DefectConstants.DefectSeverity.SERIOUS.value() }
            .count() - securitySeriousCount

        val normalCount = defectList.filter { it.severity == DefectConstants.DefectSeverity.NORMAL.value() }
            .count() - securityNormalCount

        return Pair(Pair(securitySeriousCount, securityNormalCount), Pair(seriousCount, normalCount))
    }

    /**
     * 计算代码规范评分，多语言项目按照代码行比例计算分数
     * python 代码规范由两种工具（PYLINT FLAKE8）的告警共同计算
     * @param tools 各工具告警数
     * @param lines 各语言行数
     */
    @Deprecated(message = "旧的代码规范不支持配置化计分", replaceWith = ReplaceWith("newScoringStandard"))
    private fun calCodeStyleScore(
        tools: Map<String, Pair<Double, Double>>,
        lines: Map<String, Long>
    ): Double {
        // 所有语言总行
        logger.info("all languages lines: $lines")
        var totalLines = 0L
        var otherDefect = 0.0
        var pyDefect = 0.0
        var score = 0.0
        for (tool in tools) {
            when (tool.key) {
                ComConstants.Tool.GOML.name -> {
                    totalLines += (lines["Go"] ?: 0)
                    logger.info("Go: ${(lines["Go"] ?: 0)} $totalLines")
                }
                ComConstants.Tool.PYLINT.name -> {
                    totalLines += (lines["Python"] ?: 0)
                    logger.info("Python: ${(lines["Python"] ?: 0)} $totalLines")
                }
                ComConstants.Tool.ESLINT.name -> {
                    totalLines += (lines["JavaScript"] ?: 0)
                    logger.info("JavaScript: ${(lines["JavaScript"] ?: 0)} $totalLines")
                }
                ComConstants.Tool.STYLECOP.name -> {
                    totalLines += (lines["C#"] ?: 0)
                    logger.info("C#: ${(lines["C#"] ?: 0)} $totalLines")
                }
                ComConstants.Tool.CHECKSTYLE.name -> {
                    totalLines += (lines["Java"] ?: 0)
                    logger.info("Java: ${(lines["Java"] ?: 0)} $totalLines")
                }
                ComConstants.Tool.CPPLINT.name -> {
                    totalLines += (lines["C"] ?: 0) + (lines["C++"] ?: 0) + (lines["C/C++ Header"]
                        ?: 0)
                    logger.info(
                        "CPPLINT: ${(lines["C"] ?: 0) + (lines["C++"] ?: 0) + (lines["C/C++ Header"]
                            ?: 0)} $totalLines"
                    )
                }
                "BKCHECK-OC" -> {
                    totalLines += ((lines["Objective C"] ?: 0) +
                        (lines["Objective C++"] ?: 0))
                    logger.info(
                        "Objective: ${((lines["Objective C"] ?: 0) +
                            (lines["Objective C++"] ?: 0))} $totalLines"
                    )
                }
                else -> {
                }
            }
        }

        var hasPy = false
        loop@ for (tool in tools) {
            val languageWaringConfigCount: Double
            // 单语言总行
            val totalLine: Long
            when (tool.key) {
                ComConstants.Tool.GOML.name -> {
                    languageWaringConfigCount = 0.1
                    totalLine = lines["Go"] ?: 0
                }
                ComConstants.Tool.PYLINT.name -> {
                    pyDefect += (tool.value.first * 1 + tool.value.second * 0.5)
                    hasPy = true
                    continue@loop
                }
                ComConstants.Tool.FLAKE8.name -> {
                    pyDefect += (tool.value.first * 1 + tool.value.second * 0.5)
                    hasPy = true
                    continue@loop
                }
                ComConstants.Tool.ESLINT.name -> {
                    languageWaringConfigCount = 7.toDouble()
                    totalLine = lines["JavaScript"] ?: 0
                }
                ComConstants.Tool.STYLECOP.name -> {
                    languageWaringConfigCount = 8.0
                    totalLine = lines["C#"] ?: 0
                }
                ComConstants.Tool.CHECKSTYLE.name -> {
                    languageWaringConfigCount = 6.0
                    totalLine = lines["Java"] ?: 0
                }
                ComConstants.Tool.CPPLINT.name -> {
                    languageWaringConfigCount = 7.0
                    totalLine = (lines["C"] ?: 0) + (lines["C++"] ?: 0) + (lines["C/C++ Header"] ?: 0)
                }
                "BKCHECK-OC" -> {
                    languageWaringConfigCount = 3.0
                    totalLine = ((lines["Objective C"] ?: 0) +
                        (lines["Objective C++"] ?: 0))
                }
                else -> {
                    otherDefect += (tool.value.first * 1 + tool.value.second * 0.5)
                    continue@loop
                }
            }
            if (totalLine == 0L) {
                continue@loop
            }

            // 计算百行告警数，百行告警数=（严重告警数*1+一般告警数*0.5）/代码行数*100
            val totalDefect = (tool.value.first * 1 + tool.value.second * 0.5)
            // logger.info("cal ${tool.key} code style score, totalDefect: $totalDefect | line: $totalLine | totalLine: $totalLines")
            val sc = calCodeStyleScore(totalDefect, languageWaringConfigCount, totalLine, totalLines)
            if (sc >= 0) {
                score += sc
            }
            logger.info("cal ${tool.key} code style score $sc, totalDefect: $totalDefect | line: $totalLine | totalLine: $totalLines")
        }

        // 单独计算 python 的规范分数
        if (hasPy) {
            // logger.info("cal python code style score, totalDefect: $pyDefect | line: ${lines["Python"] ?: 0} | totalLine: $totalLines")
            val scc = calCodeStyleScore(pyDefect, 4.toDouble(), lines["Python"] ?: 0, totalLines)
            if (scc >= 0) {
                score += scc
            }
            logger.info("cal python code style score $scc, totalDefect: $pyDefect | line: ${lines["Python"] ?: 0} | totalLine: $totalLines")
        }
        return if (score > 100) {
            100.toDouble()
        } else {
            BigDecimal(score).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
        }
    }

    fun newScoringStandard(
            metricsEntity: MetricsEntity,
            actualExeTools: MutableList<String>,
            scoringConfigs: MutableMap<ComConstants.CodeLang, StandardScoringConfig>
    ) {
        val totalLine = scoringConfigs.filter {
                it.key != ComConstants.CodeLang.OTHERS
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

        if (totalLine > 0) {
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
        logger.info("final scoring result: $metricsEntity")
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
        logger.info("cal ${config.clocLanguage} code style score, totalDefect: $totalDefect " +
                "| line: ${config.lineCount} | totalLine: $totalLine")
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
    private fun calCodeStyleScore(
        totalDefect: Double,
        languageWaringConfigCount: Double,
        line: Long,
        totalLine: Long
    ): Double {
        val hundredWaringCount = if (line == 0L) {
            0.toDouble()
        } else {
            (totalDefect / line) * 100.toDouble()
        }
        // 计算行占比
        val linePercentage = line.toDouble() / totalLine.toDouble()
        logger.info("cal style score, hundred: $hundredWaringCount linepercent: $linePercentage")
        // 计算代码规范评分
        return BigDecimal(
            100 * linePercentage * ((0.6.pow(1.toDouble() / languageWaringConfigCount)).pow(
                hundredWaringCount
            ))
        ).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
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
            logger.error(
                "error CodeSecurity defect num, serious: {} | normal {}",
                totalSeriousRiskCount,
                totalNormalRiskCount
            )
            -1
        }
        return BigDecimal(score.toDouble())
            .setScale(2, BigDecimal.ROUND_HALF_UP)
            .toDouble()
    }

    /**
     * 计算代码度量和检查评分，Coverity数据默认为0
     * @param totalCoveritySeriousWaringCount 总严重告警数
     * @param totalCoverityNormalWaringCount 总一般告警数
     * @param totalLine 代码总行数
     * @param totalCcnExceedNum 代码圈复杂度总超标数
     */
    private fun calCodeMeasureScore(
        totalCoveritySeriousWaringCount: Int,
        totalCoverityNormalWaringCount: Int,
        totalLine: Long,
        totalCcnExceedNum: Double
    ): Pair<Double, Double> {
        // 计算圈复杂度千行平均超标数
        val thousandCcnCount = if (totalLine == 0L)
            0.toDouble()
        else
            (1000 * (totalCcnExceedNum.toDouble() / totalLine.toDouble()))
        // 计算圈复杂度得分
        val ccnScore = if ((60 - 40 * (thousandCcnCount - 3) / (3 - 0)) < 0) {
            0.toDouble()
        } else {
            60 - 40 * (thousandCcnCount - 3) / (3 - 0)
        }
        logger.info("cal ccn of measure: $totalCcnExceedNum $totalLine $thousandCcnCount $ccnScore")
        // 计算严重告警数得分
        val coveritySeriousWaringScore = if (totalCoveritySeriousWaringCount > 0)
            0
        else
            100
        // 计算一般告警数千行均值
        val thousandCoverityNormalWaringCount = if (totalLine == 0L)
            0.toDouble()
        else
            (1000 * (totalCoverityNormalWaringCount.toDouble() / totalLine.toDouble()))
        // 计算一般告警数得分评分
        val coverityNormalWaringScore =
            if ((60 - 40 * (thousandCoverityNormalWaringCount - 0.035) / (0.035 - 0)) < 0) {
                0.toDouble()
            } else {
                (60 - 40 * (thousandCoverityNormalWaringCount - 0.035) / (0.035 - 0))
            }
        logger.info("cal defect of measure: $totalCoveritySeriousWaringCount $totalCoverityNormalWaringCount $totalLine")
        return Pair(
            BigDecimal(0.9 * ccnScore + 0.08 * coveritySeriousWaringScore + 0.02 * coverityNormalWaringScore)
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .toDouble(), thousandCcnCount
        )
    }

    /**
     * 计算 Coverity 缺陷得分
     * @param totalLine
     * @param totalCoverityNormalWaringCount
     * @param totalCoveritySeriousWaringCount
     */
    private fun calCoverityScore(
        totalCoveritySeriousWaringCount: Int,
        totalCoverityNormalWaringCount: Int,
        totalLine: Long
    ): Double {
        // 计算严重告警数得分
        val coveritySeriousWaringScore = if (totalCoveritySeriousWaringCount > 0)
            0
        else
            100
        // 计算一般告警数千行均值
        val thousandCoverityNormalWaringCount = if (totalLine == 0L)
            0.toDouble()
        else
            (1000 * (totalCoverityNormalWaringCount.toDouble() / totalLine.toDouble()))
        // 计算一般告警数得分评分
        val coverityNormalWaringScore = if ((60 - 40 * (thousandCoverityNormalWaringCount - 0.035) / (0.035 - 0)) < 0) {
            0.toDouble()
        } else {
            (60 - 40 * (thousandCoverityNormalWaringCount - 0.035) / (0.035 - 0))
        }
        return BigDecimal(0.8 * coveritySeriousWaringScore + 0.2 * coverityNormalWaringScore)
            .setScale(2, BigDecimal.ROUND_HALF_UP)
            .toDouble()
    }

    /**
     * 计算圈复杂度得分
     * @param totalLine
     * @param totalCcnExceedNum
     */
    private fun calCcnScore(
        totalLine: Long,
        totalCcnExceedNum: Double
    ): Double {
        // 计算圈复杂度千行平均超标数
        val thousandCcnCount = if (totalLine == 0L)
            0.toDouble()
        else
            (1000 * (totalCcnExceedNum / totalLine.toDouble()))
        // 计算圈复杂度得分
        val ccnScore = if ((60 - 40 * (thousandCcnCount - 3) / (3 - 0)) < 0) {
            0.toDouble()
        } else {
            60 - 40 * (thousandCcnCount - 3) / (3 - 0)
        }
        return BigDecimal(ccnScore)
            .setScale(2, BigDecimal.ROUND_HALF_UP)
            .toDouble()
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
        codeMeasureScore: Double
    ): Double {
        val rd = (codeStyleScore + codeSecurityScore + codeMeasureScore) * 0.25 + 25
        val bigDecimal = BigDecimal(rd)
        return bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
    }

    /**
     * 获取特定构建的告警信息
     */
    fun getLintStatInfo(taskId: Long, toolName: String, buildId: String): GrayTaskStatVO? {
        val lintStatisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
        return if (null == lintStatisticEntity || lintStatisticEntity.entityId.isNullOrBlank()) {
            null
        } else {
            val taskLogEntity = taskLogRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
            if (null == taskLogEntity || taskLogEntity.entityId.isNullOrBlank()) {
                null
            } else {
                GrayTaskStatVO(
                    totalSerious = lintStatisticEntity.totalSerious,
                    totalNormal = lintStatisticEntity.totalNormal,
                    totalPrompt = lintStatisticEntity.totalPrompt,
                    elapsedTime = taskLogEntity.elapseTime,
                    currStep = taskLogEntity.currStep,
                    flag = taskLogEntity.flag
                )
            }
        }
    }
}
