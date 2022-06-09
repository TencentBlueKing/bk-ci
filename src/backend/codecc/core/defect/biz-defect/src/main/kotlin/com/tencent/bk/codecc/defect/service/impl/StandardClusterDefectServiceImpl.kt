package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.mongorepository.*
import com.tencent.bk.codecc.defect.model.StandardClusterStatisticEntity
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO
import com.tencent.devops.common.api.clusterresult.StandardClusterResultVO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.service.ToolMetaCacheService
import org.apache.commons.beanutils.BeanUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service("STANDARD")
class StandardClusterDefectServiceImpl(
        lintStatisticRepository: LintStatisticRepository,
        commonStatisticRepository: CommonStatisticRepository,
        dupcStatisticRepository: DUPCStatisticRepository,
        ccnStatisticRepository: CCNStatisticRepository,
        private val clocStatisticRepository: CLOCStatisticRepository,
        private val toolMetaCacheService: ToolMetaCacheService,
        private val standardClusterStatisticRepository: StandardClusterStatisticRepository
) : AbstractClusterDefectService(
        lintStatisticRepository,
        commonStatisticRepository,
        dupcStatisticRepository,
        ccnStatisticRepository,
        clocStatisticRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StandardClusterDefectServiceImpl::class.java)
    }

    override fun cluster(taskId: Long, buildId: String, toolList: List<String>) {
        logger.info("Standard cluster $taskId $buildId ${toolList.size}")
        var totalCount = 0
        // 获取当前分类下所有工具的告警数据
        toolList.forEach{
            val toolDetail = toolMetaCacheService.getToolBaseMetaCache(it)
            val clusterResultVO = getStatistic(
                taskId = taskId,
                buildId = buildId,
                toolName = it,
                pattern = toolDetail.pattern)
            totalCount += (clusterResultVO.totalCount ?: 0)
        }

        // 取上一次的聚类信息与当前对比
        val lastStandardClusterStatisticEntity: StandardClusterStatisticEntity?  =
                standardClusterStatisticRepository.findFirstByTaskIdOrderByTimeDesc(taskId)
        // 获取总行数计算千行平均告警数
        val clocList =
                clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, ComConstants.Tool.CLOC.name, buildId)
        val sumLines = clocList.stream().mapToInt {
            it.sumCode.toInt()
        }.sum()

        val average = if (sumLines == 0) {
            0.toDouble()
        } else {
            BigDecimal(1000 * (totalCount.toDouble() / sumLines.toDouble()))
                    .setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toDouble()
        }

        val currStandardClusterStatisticEntity = StandardClusterStatisticEntity(
                taskId,
                buildId,
                toolList,
                System.currentTimeMillis(),
                totalCount,
                totalCount - (lastStandardClusterStatisticEntity?.totalCount?:0),
                average,
                average.minus((lastStandardClusterStatisticEntity?.averageThousandDefect?:0.0))
        )

        standardClusterStatisticRepository.save(currStandardClusterStatisticEntity)
    }

    override fun getClusterStatistic(taskId: Long, buildId: String): BaseClusterResultVO {
        val standardClusterResultVO = StandardClusterResultVO()
        standardClusterResultVO.type = ComConstants.ToolType.STANDARD.name
        val standardClusterStatisticEntity =
                standardClusterStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
                        ?: return standardClusterResultVO
        BeanUtils.copyProperties(standardClusterResultVO, standardClusterStatisticEntity)
        standardClusterResultVO.type = ComConstants.ToolType.STANDARD.name
        standardClusterResultVO.toolList = standardClusterStatisticEntity.toolList
        standardClusterResultVO.toolNum = standardClusterStatisticEntity.toolList?.size ?: 0
        return standardClusterResultVO
    }
}
