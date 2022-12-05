package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.mongorepository.*
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO
import com.tencent.devops.common.api.clusterresult.DupcClusterResultVO
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("DUPC")
class DupcClusterDefectServiceImpl @Autowired constructor(
        lintStatisticRepository: LintStatisticRepository,
        commonStatisticRepository: CommonStatisticRepository,
        ccnStatisticRepository: CCNStatisticRepository,
        clocStatisticRepository: CLOCStatisticRepository,
        private val dupcStatisticRepository: DUPCStatisticRepository
): AbstractClusterDefectService(
        lintStatisticRepository,
        commonStatisticRepository,
        dupcStatisticRepository,
        ccnStatisticRepository,
        clocStatisticRepository
) {
    /**
     * 重复率工具不参与开源治理度量，没有聚类逻辑
     */
    override fun cluster(taskId: Long, buildId: String, toolList: List<String>) {
        logger.info("dupc cluster $taskId $buildId ${toolList.size}")
    }

    override fun getClusterStatistic(taskId: Long, buildId: String): BaseClusterResultVO {
        val dupcClusterResultVO = DupcClusterResultVO()
        dupcClusterResultVO.type = ComConstants.ToolType.DUPC.name
        val dupcClusterStatisticEntity =
                dupcStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
                        ?: return dupcClusterResultVO
        dupcClusterResultVO.totalCount = dupcClusterStatisticEntity.defectCount
        dupcClusterResultVO.defectChange = dupcClusterStatisticEntity.defectChange
        dupcClusterResultVO.dupRate = dupcClusterStatisticEntity.dupRate.toDouble()
        dupcClusterResultVO.dupRateChange = dupcClusterStatisticEntity.dupRateChange.toDouble()
        dupcClusterResultVO.type = ComConstants.ToolType.DUPC.name
        dupcClusterResultVO.toolNum = 1
        dupcClusterResultVO.toolList = listOf(ComConstants.Tool.DUPC.name)
        return dupcClusterResultVO
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CcnClusterDefectServiceImpl::class.java)
    }
}
