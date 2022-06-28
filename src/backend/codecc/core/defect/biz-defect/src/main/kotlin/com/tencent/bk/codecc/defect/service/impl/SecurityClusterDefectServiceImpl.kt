package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.mongorepository.*
import com.tencent.bk.codecc.defect.model.SecurityClusterStatisticEntity
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO
import com.tencent.devops.common.api.clusterresult.SecurityClusterResultVO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.service.ToolMetaCacheService
import org.apache.commons.beanutils.BeanUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("SECURITY")
class SecurityClusterDefectServiceImpl @Autowired constructor(
        lintStatisticRepository: LintStatisticRepository,
        commonStatisticRepository: CommonStatisticRepository,
        dupcStatisticRepository: DUPCStatisticRepository,
        ccnStatisticRepository: CCNStatisticRepository,
        clocStatisticRepository: CLOCStatisticRepository,
        private val toolMetaCacheService: ToolMetaCacheService,
        private val securityClusterStatisticRepository: SecurityClusterStatisticRepository
): AbstractClusterDefectService(
        lintStatisticRepository,
        commonStatisticRepository,
        dupcStatisticRepository,
        ccnStatisticRepository,
        clocStatisticRepository
) {
    override fun cluster(taskId: Long, buildId: String, toolList: List<String>) {
        logger.info("security cluster $taskId $buildId ${toolList.size}")
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

        val defectClusterStatisticEntity = SecurityClusterStatisticEntity(
                taskId,
                buildId,
                toolList,
                System.currentTimeMillis(),
                totalCount,
                newCount,
                fixCount,
                maskCount
        )

        securityClusterStatisticRepository.save(defectClusterStatisticEntity)
    }

    override fun getClusterStatistic(taskId: Long, buildId: String): BaseClusterResultVO {
        val securityClusterResultVO = SecurityClusterResultVO()
        securityClusterResultVO.type = ComConstants.ToolType.SECURITY.name
        val securityClusterStatisticEntity =
                securityClusterStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
                        ?: return securityClusterResultVO
        BeanUtils.copyProperties(securityClusterResultVO, securityClusterStatisticEntity)
        securityClusterResultVO.type = ComConstants.ToolType.SECURITY.name
        securityClusterResultVO.toolList = securityClusterStatisticEntity.toolList
        securityClusterResultVO.toolNum = securityClusterStatisticEntity.toolList?.size ?: 0
        return securityClusterResultVO
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SecurityClusterDefectServiceImpl::class.java)
    }
}
