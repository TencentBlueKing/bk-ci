package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.mongorepository.*
import com.tencent.bk.codecc.defect.model.CcnClusterStatisticEntity
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO
import com.tencent.devops.common.api.clusterresult.CcnClusterResultVO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.service.ToolMetaCacheService
import org.apache.commons.beanutils.BeanUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service("CCN")
class CcnClusterDefectServiceImpl @Autowired constructor(
        lintStatisticRepository: LintStatisticRepository,
        commonStatisticRepository: CommonStatisticRepository,
        dupcStatisticRepository: DUPCStatisticRepository,
        ccnStatisticRepository: CCNStatisticRepository,
        private val clocStatisticRepository: CLOCStatisticRepository,
        private val toolMetaCacheService: ToolMetaCacheService,
        private val ccnClusterStatisticRepository: CcnClusterStatisticRepository
): AbstractClusterDefectService(
        lintStatisticRepository,
        commonStatisticRepository,
        dupcStatisticRepository,
        ccnStatisticRepository,
        clocStatisticRepository
) {
    override fun cluster(taskId: Long, buildId: String, toolList: List<String>) {
        logger.info("ccn cluster $taskId $buildId ${toolList.size}")
        var totalCount = 0
        var ccnBeyondThresholdSum = 0
        // 获取当前分类下所有工具的告警数据
        toolList.forEach{
            val toolDetail = toolMetaCacheService.getToolBaseMetaCache(it)
            val clusterResultVO = getStatistic(
                    taskId = taskId,
                    buildId = buildId,
                    toolName = it,
                    pattern = toolDetail.pattern)
            totalCount += (clusterResultVO.totalCount ?: 0)
            ccnBeyondThresholdSum += (clusterResultVO.ccnBeyondThresholdSum ?: 0)
        }

        val lastCcnClusterStatisticEntity: CcnClusterStatisticEntity?  =
                ccnClusterStatisticRepository.findFirstByTaskIdOrderByTimeDesc(taskId)
        val clocList =
                clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, ComConstants.Tool.CLOC.name, buildId)
        val sumLines = clocList.stream().mapToInt {
            (it.sumComment + it.sumCode + it.sumBlank).toInt()
        }.sum()

        val averageThousandDefect = if (sumLines == 0) {
            0.toDouble()
        } else {
            BigDecimal((1000 * ccnBeyondThresholdSum).toDouble() / sumLines.toDouble())
                    .setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toDouble()
        }

        var totalCountChange = totalCount
        var averageThousandDefectChange = averageThousandDefect

        if (lastCcnClusterStatisticEntity != null) {
            totalCountChange = totalCount - lastCcnClusterStatisticEntity.totalCount
            averageThousandDefectChange =
                    averageThousandDefect - lastCcnClusterStatisticEntity.averageThousandDefect
        }

        val defectClusterStatisticEntity = CcnClusterStatisticEntity(
                taskId,
                buildId,
                toolList,
                System.currentTimeMillis(),
                totalCount,
                totalCountChange,
                averageThousandDefect,
                averageThousandDefectChange
        )

        ccnClusterStatisticRepository.save(defectClusterStatisticEntity)
    }

    override fun getClusterStatistic(taskId: Long, buildId: String): BaseClusterResultVO {
        val ccnClusterResultVO = CcnClusterResultVO()
        ccnClusterResultVO.type = ComConstants.ToolType.CCN.name
        val ccnClusterStatisticEntity =
                ccnClusterStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
                        ?: return ccnClusterResultVO
        BeanUtils.copyProperties(ccnClusterResultVO, ccnClusterStatisticEntity)
        ccnClusterResultVO.type = ComConstants.ToolType.CCN.name
        ccnClusterResultVO.toolList = ccnClusterStatisticEntity.toolList
        ccnClusterResultVO.toolNum = ccnClusterStatisticEntity.toolList?.size ?: 0
        return ccnClusterResultVO
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CcnClusterDefectServiceImpl::class.java)
    }
}
