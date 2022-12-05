package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.mongorepository.*
import com.tencent.bk.codecc.defect.service.ClusterDefectService
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO
import com.tencent.devops.common.service.annotation.CCN
import com.tencent.devops.common.service.annotation.CLOC
import com.tencent.devops.common.service.annotation.DUPC
import com.tencent.devops.common.service.annotation.tool_pattern.LINT
import org.slf4j.LoggerFactory

abstract class AbstractClusterDefectService constructor(
        private val lintStatisticRepository: LintStatisticRepository,
        private val commonStatisticRepository: CommonStatisticRepository,
        private val dupcStatisticRepository: DUPCStatisticRepository,
        private val ccnStatisticRepository: CCNStatisticRepository,
        private val clocStatisticRepository: CLOCStatisticRepository
): ClusterDefectService {
    /**
     * 获取指定工具告警信息
     * 根据 pattern 区分
     * 利用注解实现映射各个 pattern 的数据查询逻辑
     *
     * @param taskId
     * @param buildId
     * @param toolName
     * @param pattern
     */
    fun getStatistic(
            taskId: Long,
            buildId: String,
            toolName: String,
            pattern: String
    ): BaseClusterResultVO {
        logger.info("get statistic info: $taskId $buildId $toolName $pattern")
        val clusterResultVO = BaseClusterResultVO()
        // 遍历方法匹配查询逻辑
        this::class.java.methods.find { method ->
            method.declaredAnnotations.find {
                it.annotationClass.simpleName == pattern} != null
        }.let { method ->
            if (method == null) {
                getDefectStatistic(clusterResultVO, taskId, buildId, toolName)
            } else {
                method.invoke(this, clusterResultVO, taskId, buildId, toolName)
            }
        }
        return clusterResultVO
    }

    /**
     * Defect 表数据查询逻辑，因为这部分工具的pattern不固定，则不需要注解匹配
     *
     * @param clusterResultVO
     * @param taskId
     * @param buildId
     * @param toolName
     */
    fun getDefectStatistic(clusterResultVO: BaseClusterResultVO, taskId: Long, buildId: String, toolName: String) {
        logger.info("defect statistic")
        val commonStatistic = commonStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(
                taskId,
                toolName,
                buildId
        ) ?: return
        clusterResultVO.newCount = commonStatistic.newCount
        clusterResultVO.fixCount =
                (commonStatistic.normalFixedCount + commonStatistic.promptFixedCount + commonStatistic.seriousFixedCount)
                        .toInt()
        clusterResultVO.maskCount =
                (commonStatistic.normalMaskCount + commonStatistic.promptMaskCount + commonStatistic.seriousMaskCount)
                        .toInt()
        clusterResultVO.totalCount = commonStatistic.existCount
    }

    /**
     * Lint 表数据查询逻辑
     *
     */
    @LINT
    fun getLintStatistic(clusterResultVO: BaseClusterResultVO, taskId: Long, buildId: String, toolName: String) {
        logger.info("lint statistic")
        val lintStatistic = lintStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(
                taskId,
                toolName,
                buildId
        ) ?: return
        clusterResultVO.newCount = lintStatistic.newDefectCount
        clusterResultVO.fixCount =
                (lintStatistic.normalFixedCount + lintStatistic.promptFixedCount + lintStatistic.seriousFixedCount)
                        .toInt()
        clusterResultVO.maskCount =
                (lintStatistic.normalMaskCount + lintStatistic.promptMaskCount + lintStatistic.seriousMaskCount)
                        .toInt()
        clusterResultVO.totalCount =
                lintStatistic.totalPrompt + lintStatistic.totalNormal + lintStatistic.totalSerious
    }

    /**
     * DUPC 表数据查询逻辑
     *
     */
    @DUPC
    fun getDUPCStatistic(clusterResultVO: BaseClusterResultVO, taskId: Long, buildId: String, toolName: String) {
        logger.info("dupc statistic")
    }

    /**
     * CCN 表数据查询逻辑
     *
     */
    @CCN
    fun getCCNStatistic(clusterResultVO: BaseClusterResultVO, taskId: Long, buildId: String, toolName: String) {
        logger.info("ccn statistic")
        val ccnStatistic = ccnStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId) ?: return
        clusterResultVO.ccnBeyondThresholdSum = ccnStatistic.ccnBeyondThresholdSum
        clusterResultVO.totalCount = ccnStatistic.superHighCount + ccnStatistic.highCount+
                ccnStatistic.mediumCount + ccnStatistic.lowCount
    }

    /**
     * CLOC 表数据查询逻辑
     *
     */
    @CLOC
    fun getCLOCStatistic(clusterResultVO: BaseClusterResultVO, taskId: Long, buildId: String, toolName: String) {
        logger.info("cloc statistic")
        val clocStatisticList = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId)
        val sumLines = clocStatisticList.stream()
                .mapToLong { (it.sumBlank + it.sumCode + it.sumComment) }
                .sum()
        clusterResultVO.totalLines = sumLines
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractClusterDefectService::class.java)
    }
}
