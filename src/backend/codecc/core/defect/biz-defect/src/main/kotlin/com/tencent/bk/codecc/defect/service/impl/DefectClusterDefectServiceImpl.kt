package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.mongorepository.*
import com.tencent.bk.codecc.defect.model.DefectClusterStatisticEntity
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO
import com.tencent.devops.common.api.clusterresult.DefectClusterResultVO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.service.ToolMetaCacheService
import org.apache.commons.beanutils.BeanUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("DEFECT")
class DefectClusterDefectServiceImpl @Autowired constructor(
        lintStatisticRepository: LintStatisticRepository,
        commonStatisticRepository: CommonStatisticRepository,
        dupcStatisticRepository: DUPCStatisticRepository,
        ccnStatisticRepository: CCNStatisticRepository,
        clocStatisticRepository: CLOCStatisticRepository,
        private val toolMetaCacheService: ToolMetaCacheService,
        private val defectClusterStatisticRepository: DefectClusterStatisticRepository
): AbstractClusterDefectService(
        lintStatisticRepository,
        commonStatisticRepository,
        dupcStatisticRepository,
        ccnStatisticRepository,
        clocStatisticRepository
) {
    override fun cluster(taskId: Long, buildId: String, toolList: List<String>) {
        logger.info("Standard cluster $taskId $buildId ${toolList.size}")
        var totalCount = 0
        var newCount = 0
        var fixCount = 0
        var maskCount = 0
        // 获取当前分类下所有工具的告警数据
        toolList.forEach{
            val toolDetail = toolMetaCacheService.getToolBaseMetaCache(it)
            val clusterResultVO = getStatistic(
                    taskId = taskId,
                    buildId = buildId,
                    toolName = it,
                    pattern = toolDetail.pattern)
            totalCount += (clusterResultVO.totalCount ?: 0)
            newCount += (clusterResultVO.newCount ?: 0)
            fixCount += (clusterResultVO.fixCount ?: 0)
            maskCount += (clusterResultVO.maskCount ?: 0)
        }

        val defectClusterStatisticEntity = DefectClusterStatisticEntity(
                taskId,
                buildId,
                toolList,
                System.currentTimeMillis(),
                totalCount,
                newCount,
                fixCount,
                maskCount
        )

        defectClusterStatisticRepository.save(defectClusterStatisticEntity)
    }

    override fun getClusterStatistic(taskId: Long, buildId: String): BaseClusterResultVO {
        val defectClusterResultVO = DefectClusterResultVO()
        defectClusterResultVO.type = ComConstants.ToolType.DEFECT.name
        val defectClusterStatisticEntity =
                defectClusterStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
                        ?: return defectClusterResultVO
        BeanUtils.copyProperties(defectClusterResultVO, defectClusterStatisticEntity)
        defectClusterResultVO.type = ComConstants.ToolType.DEFECT.name
        defectClusterResultVO.toolList = defectClusterStatisticEntity.toolList
        defectClusterResultVO.toolNum = defectClusterStatisticEntity.toolList?.size ?: 0
        return defectClusterResultVO
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefectClusterDefectServiceImpl::class.java)
    }
}
