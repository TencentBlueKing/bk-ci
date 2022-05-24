package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.BuildDefectDao
import com.google.common.collect.Sets
import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.DUPCDefectDao
import com.tencent.bk.codecc.apiquery.defect.model.DUPCDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.DUPCStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.service.MetaDataService
import com.tencent.bk.codecc.apiquery.service.TaskLogService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.bk.codecc.apiquery.vo.DUPCDefectVO
import com.tencent.bk.codecc.apiquery.vo.DefectQueryReqVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.util.ListSortUtil
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import com.tencent.devops.common.util.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.util.ArrayList

@Service("DUPCQueryWarningBizService")
class DUPCQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val dupcDefectDao: DUPCDefectDao,
    private val buildDefectDao: BuildDefectDao,
    private val taskLogService: TaskLogService,
    private val metaDataService: MetaDataService,
    private val statisticDao: StatisticDao
) : IDefectQueryWarningService<DUPCDefectModel, DUPCStatisticModel> {

    companion object {
        private val logger = LoggerFactory.getLogger(DUPCQueryWarningBizServiceImpl::class.java)
    }

    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<DUPCDefectModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val relPathList = if (!defectQueryParam.buildId.isNullOrBlank()) {
            val buildDefectList = buildDefectDao.findByTaskIdToolNameAndBuildId(
                defectQueryParam.taskIdList,
                defectQueryParam.toolName!!,
                defectQueryParam.buildId
            )
            if (!buildDefectList.isNullOrEmpty()) {
                buildDefectList.map { it.fileRelPath }
            } else {
                null
            }
        } else {
            null
        }
//        val relPathList = null
        val dupcDefectList =
            defectDao.findDUPCByTaskIdInAndToolName(
                defectQueryParam.taskIdList,
                defectQueryParam.filterFields,
                defectQueryParam.status,
                relPathList,
                pageable
            )
        return Page(pageable.pageNumber + 1, pageable.pageSize, dupcDefectList.size.toLong(), dupcDefectList)
    }

    override fun queryLintDefectStatistic(
        taskIdList: List<Long>,
        toolName: String?,
        startTime: Long?,
        endTime: Long?,
        filterFields: List<String>?,
        buildId: String?,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<DUPCStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val dupcStatisticList = statisticDao.findDUPCByTaskIdInAndToolName(
            taskIdList, startTime, endTime,
            filterFields, pageable
        )
        return Page(pageable.pageNumber + 1, pageable.pageSize, dupcStatisticList.size.toLong(), dupcStatisticList)
    }

    fun defectCommitSuccess(taskId: Long, toolName: String?, buildId: String?): Boolean {
        var result = false
        val buildTaskLog = taskLogService.getBuildTaskLog(taskId, toolName, buildId)
        if (buildTaskLog != null && CollectionUtils.isNotEmpty(buildTaskLog.stepArray)) {
            for (taskUnit in buildTaskLog.stepArray) {
                if (ComConstants.Step4MutliTool.COMMIT.value() == taskUnit.stepNum &&
                    ComConstants.StepFlag.SUCC.value() == taskUnit.flag
                ) {
                    result = true
                    break
                }
            }
        }
        return result
    }

    /**
     * 根据风险系数配置给告警方法赋值风险系数
     *
     * @param riskFactorConfMap
     */
    private fun fillingRiskFactor(dupcDefectModel: DUPCDefectModel, riskFactorConfMap: Map<String, String>?) {
        if (riskFactorConfMap == null) {
            logger.error("Has not init risk factor config!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, arrayOf("风险系数"))
        }
        val sh = riskFactorConfMap[ComConstants.RiskFactor.SH.name]!!.toFloat()
        val h = riskFactorConfMap[ComConstants.RiskFactor.H.name]!!.toFloat()
        val m = riskFactorConfMap[ComConstants.RiskFactor.M.name]!!.toFloat()

        val dupRateStr: String = dupcDefectModel.dupRate
        val dupRate: Float =
            (if (StringUtils.isEmpty(dupRateStr)) "0" else dupRateStr.substring(0, dupRateStr.length - 1)).toFloat()
        if (dupRate >= m && dupRate < h) {
            dupcDefectModel.riskFactor = ComConstants.RiskFactor.M.value()
        } else if (dupRate >= h && dupRate < sh) {
            dupcDefectModel.riskFactor = ComConstants.RiskFactor.H.value()
        } else if (dupRate >= sh) {
            dupcDefectModel.riskFactor = ComConstants.RiskFactor.SH.value()
        }
    }

    /**
     * 设置告警文件名
     *
     * @param dupcDefectModel
     */
    private fun setFileName(dupcDefectModel: DUPCDefectModel) {
        val filePath: String = dupcDefectModel.filePath ?: return
        var fileNameIndex = filePath.lastIndexOf("/")
        if (fileNameIndex == -1) {
            fileNameIndex = filePath.lastIndexOf("\\")
        }
        dupcDefectModel.fileName = filePath.substring(fileNameIndex + 1)
    }

    private fun <T> sortAndPage(
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?,
        defectBaseVoList: List<T>
    ): Page<T> {
        var sortFieldVar = sortField
        var sortTypeVar = sortType
        var defectBaseVoListVar = defectBaseVoList

        if (StringUtils.isEmpty(sortFieldVar)) {
            sortFieldVar = "severity"
        }
        if (null == sortType) {
            sortTypeVar = Sort.Direction.ASC.name
        }
        ListSortUtil.sort(defectBaseVoList, sortFieldVar, sortTypeVar)
        val total = defectBaseVoList.size
        val pageNumVar: Int = if (pageNum == null || (pageNum - 1) < 0) 0 else pageNum - 1
        var pageSizeNum = 10
        if (pageSize != null && pageSize >= 0) {
            pageSizeNum = pageSize
        }
        var totalPageNum = 0
        if (total > 0) {
            totalPageNum = (total + pageSizeNum - 1) / pageSizeNum
        }
        var subListBeginIdx = pageNumVar * pageSizeNum
        val subListEndIdx = subListBeginIdx + pageSizeNum
        if (subListBeginIdx > total) {
            subListBeginIdx = 0
        }
        defectBaseVoListVar = defectBaseVoListVar.subList(
            subListBeginIdx,
            if (subListEndIdx > total) total else subListEndIdx
        )
        return Page(total.toLong(), pageNumVar + 1, pageSizeNum, totalPageNum, defectBaseVoListVar)
    }

    /**
     * 查询工具告警清单(从defect迁移过来的通用接口)
     */
    override fun queryToolDefectList(
        taskId: Long,
        queryWarningReq: DefectQueryReqVO,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): ToolDefectRspVO {
        // 排序分页
        val pageNumVar = if (pageNum == null || (pageNum - 1) < 0) 0 else pageNum - 1
        val pageSizeVar = pageSize ?: 100
        val sortFieldVar = sortField ?: "dupRate"
        val sortTypeVar = sortType ?: Sort.Direction.ASC.name

        logger.info("query task[{}] defect list by {}", taskId, queryWarningReq)

        val toolName = queryWarningReq.toolName
        val buildId = queryWarningReq.buildId

        // 获取风险系数值
        val riskConfigMap = metaDataService.getRiskFactorConfig(toolName)

        // 是否需要构建号过滤
        var needBuildIdFilter = false
        val currentDupeFileRelPaths: MutableSet<String> = Sets.newHashSet()
        if (StringUtils.isNotEmpty(buildId)) {
            if (defectCommitSuccess(taskId, toolName, buildId)) {
                val buildFiles = buildDefectDao.findByTaskIdToolNameAndBuildId(listOf(taskId), toolName, buildId)
                if (CollectionUtils.isNotEmpty(buildFiles)) {
                    for (buildDefectEntity in buildFiles) {
                        currentDupeFileRelPaths.add(buildDefectEntity.fileRelPath)
                    }
                }
            }
            needBuildIdFilter = true
        }

        // 是否需要风险级别过滤
        var needSeverityFilter = false
        val severity = queryWarningReq.severity
        if (!severity.isNullOrEmpty()) {
            needSeverityFilter = true
        }

        // 需要统计的数据
        var superHighCount = 0
        var highCount = 0
        var mediumCount = 0
        var totalCount = 0

        // 查询总的数量，并且过滤计数
        val dupcDefectVOS: MutableList<DUPCDefectVO> = ArrayList()
        val defectList = dupcDefectDao.findByTaskIdAndAuthorAndRelPaths(
            taskId, queryWarningReq.author,
            queryWarningReq.fileList
        )
        val it = defectList.iterator()
        while (it.hasNext()) {
            val defectModel = it.next()
            // 按照构建号过滤
            if (needBuildIdFilter && !currentDupeFileRelPaths.contains(defectModel.relPath)) {
                it.remove()
                continue
            }
            // 根据当前处理人，文件过滤之后，需要按照严重程度统计缺陷数量
            fillingRiskFactor(defectModel, riskConfigMap)
            val riskFactor: Int = defectModel.riskFactor
            when (riskFactor) {
                ComConstants.RiskFactor.SH.value() -> {
                    superHighCount++
                }
                ComConstants.RiskFactor.H.value() -> {
                    highCount++
                }
                ComConstants.RiskFactor.M.value() -> {
                    mediumCount++
                }
            }

            val meetSeverity = needSeverityFilter && !severity.contains(riskFactor.toString())
            if (meetSeverity) {
                it.remove()
                continue
            }
            // 设置告警文件名
            setFileName(defectModel)
            // 统计告警数
            totalCount++
            // 构造视图
            val dupcDefectVO = DUPCDefectVO()
            BeanUtils.copyProperties(defectModel, dupcDefectVO)
            dupcDefectVOS.add(dupcDefectVO)
        }

        val toolDefectRspVO = ToolDefectRspVO()
        toolDefectRspVO.taskId = taskId
        toolDefectRspVO.dupcDefectList = sortAndPage(pageNumVar, pageSizeVar, sortFieldVar, sortTypeVar, dupcDefectVOS)
        toolDefectRspVO.totalCount = totalCount
        toolDefectRspVO.superHighCount = superHighCount
        toolDefectRspVO.highCount = highCount
        toolDefectRspVO.mediumCount = mediumCount
        toolDefectRspVO.toolName = toolName

        return toolDefectRspVO
    }

    override fun statCheckerDefect(reqVO: TaskToolInfoReqVO, pageNum: Int?, pageSize: Int?): Page<CheckerDefectStatVO> {
        throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("toolName"))
    }

}
