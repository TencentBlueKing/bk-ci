package com.tencent.bk.codecc.apiquery.service.impl

import com.google.common.collect.Sets
import com.tencent.bk.codecc.apiquery.defect.dao.BuildDefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CCNDefectDao
import com.tencent.bk.codecc.apiquery.defect.model.CCNDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.CCNStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.ICheckerService
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.service.MetaDataService
import com.tencent.bk.codecc.apiquery.service.TaskLogService
import com.tencent.bk.codecc.apiquery.service.TaskService
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.ToolDao
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.bk.codecc.apiquery.vo.CCNDefectVO
import com.tencent.bk.codecc.apiquery.vo.DefectQueryReqVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.util.DateTimeUtils
import com.tencent.devops.common.util.ListSortUtil
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import com.tencent.devops.common.util.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service("CCNQueryWarningBizService")
class CCNQueryWarningBizServiceImpl @Autowired constructor(
    private val toolDao: ToolDao,
    private val defectDao: DefectDao,
    private val ccnDefectDao: CCNDefectDao,
    private val taskService: TaskService,
    private val taskLogService: TaskLogService,
    private val checkerService: ICheckerService,
    private val metaDataService: MetaDataService,
    private val statisticDao: StatisticDao,
    private val buildDefectDao: BuildDefectDao
) : IDefectQueryWarningService<CCNDefectModel, CCNStatisticModel> {

    companion object {
        private val logger = LoggerFactory.getLogger(CCNQueryWarningBizServiceImpl::class.java)
    }

    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<CCNDefectModel> {
        logger.info("start to query ccn info!")
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        var defectIds = if (!defectQueryParam.buildId.isNullOrBlank()) {
            val buildDefectList = buildDefectDao.findByTaskIdToolNameAndBuildId(
                defectQueryParam.taskIdList, defectQueryParam.toolName!!,
                defectQueryParam.buildId
            )
            if (!buildDefectList.isNullOrEmpty()) {
                buildDefectList.map { ObjectId(it.defectId) }
            } else {
                null
            }
        } else {
            null
        }
        // 大于80万告警，需要截断，不然查询语句会报错
        if (!defectIds.isNullOrEmpty() && defectIds.size > 800000) {
            defectIds = defectIds.subList(0, 800000)
        }
        logger.info("finish query defect id list! size: ${defectIds?.size}")
//        val defectIds = null
        val ccnDefectList = defectDao.findCCNByTaskIdInAndToolName(
            defectQueryParam.taskIdList,
            defectQueryParam.filterFields,
            defectQueryParam.status,
            defectIds,
            pageNum,
            pageSize,
            sortField,
            sortType
        )

        return Page(pageable.pageNumber + 1, pageable.pageSize, ccnDefectList.size.toLong(), ccnDefectList)
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
    ): Page<CCNStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val ccnStatisticList = statisticDao.findCCNByTaskIdInAndToolName(
            taskIdList, startTime, endTime, filterFields,
            buildId, pageable
        )
        return Page(pageable.pageNumber + 1, pageable.pageSize, ccnStatisticList.size.toLong(), ccnStatisticList)
    }

    protected fun getNewDefectJudgeTime(taskId: Long, toolName: String, conditionDefectType: Set<String>?): Long {
        var firstSuccessTime: Long = 0
        if (CollectionUtils.isNotEmpty(conditionDefectType) && !conditionDefectType!!.containsAll(
                Sets.newHashSet(
                    ComConstants.DefectType.NEW.stringValue(), ComConstants.DefectType.HISTORY.stringValue()
                )
            )
        ) {
            // 查询新老告警判定时间，即首次分析成功时间
            val firstSuccessTimeModel = taskService.getFirstAnalyzeSuccess(taskId, toolName)
            if (firstSuccessTimeModel != null) {
                firstSuccessTime = firstSuccessTimeModel.firstAnalysisSuccessTime
            }
        }
        return firstSuccessTime
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
     * 判断是否匹配前端传入的状态条件，不匹配返回true,匹配返回false
     *
     * @param condStatusList
     * @param status
     * @return
     */
    private fun isNotMatchStatus(condStatusList: Set<Int>, status: Int): Boolean {
        var notMatchStatus = true
        for (condStatus in condStatusList) {
            // 查询条件是待修复，且告警状态是NEW
            if (ComConstants.DefectStatus.NEW.value() == condStatus && ComConstants.DefectStatus.NEW.value() == status) {
                notMatchStatus = false
                break
            } else if (ComConstants.DefectStatus.NEW.value() < condStatus &&
                condStatus and status > 0
            ) {
                notMatchStatus = false
                break
            }
        }
        return notMatchStatus
    }

    /**
     * 根据风险系数配置给告警方法赋值风险系数
     *
     * @param riskFactorConfMap
     */
    private fun fillingRiskFactor(ccnDefectModel: CCNDefectModel, riskFactorConfMap: Map<String, String>?) {
        if (riskFactorConfMap == null) {
            logger.error("Has not init risk factor config!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, arrayOf("风险系数"))
        }
        val sh = riskFactorConfMap[ComConstants.RiskFactor.SH.name]!!.toInt()
        val h = riskFactorConfMap[ComConstants.RiskFactor.H.name]!!.toInt()
        val m = riskFactorConfMap[ComConstants.RiskFactor.M.name]!!.toInt()
        val ccn: Int = ccnDefectModel.ccn
        when {
            ccn in m until h -> {
                ccnDefectModel.riskFactor = ComConstants.RiskFactor.M.value()
            }
            ccn in h until sh -> {
                ccnDefectModel.riskFactor = ComConstants.RiskFactor.H.value()
            }
            ccn >= sh -> {
                ccnDefectModel.riskFactor = ComConstants.RiskFactor.SH.value()
            }
            ccn < m -> {
                ccnDefectModel.riskFactor = ComConstants.RiskFactor.L.value()
            }
        }
    }

    fun filterDefectByCondition(
        taskId: Long,
        defectList: MutableList<CCNDefectModel>,
        allChecker: Set<String>?,
        queryWarningReq: DefectQueryReqVO,
        toolDefectRspVO: ToolDefectRspVO
    ) {
        val severity = queryWarningReq.severity
        val conditionDefectType = queryWarningReq.defectType
        val buildId = queryWarningReq.buildId
        var condStatusList = queryWarningReq.status
        if (CollectionUtils.isEmpty(condStatusList)) {
            condStatusList = HashSet(1)
            condStatusList.add(ComConstants.DefectStatus.NEW.value())
        }
        // 获取风险系数值
        val riskFactorConfMap = metaDataService.getRiskFactorConfig(ComConstants.Tool.CCN.name)
        // 查询新老告警判定时间
        val newDefectJudgeTime = getNewDefectJudgeTime(taskId, queryWarningReq.toolName, queryWarningReq.defectType)
        // 是否需要构建号过滤
        var needBuildIdFilter = false
        val currentBuildEntityIds: MutableSet<String> = Sets.newHashSet()
        if (StringUtils.isNotEmpty(buildId)) {
            if (defectCommitSuccess(taskId, ComConstants.Tool.CCN.name, buildId)) {
                val buildFiles = buildDefectDao.findByTaskIdToolNameAndBuildId(
                    listOf(taskId), ComConstants.Tool.CCN.name,
                    buildId
                )
                if (CollectionUtils.isNotEmpty(buildFiles)) {
                    for (buildDefectEntity in buildFiles) {
                        currentBuildEntityIds.add(buildDefectEntity.defectId)
                    }
                }
            }
            needBuildIdFilter = true
        }
        // 需要统计的数据
        var superHighCount = 0
        var highCount = 0
        var mediumCount = 0
        var lowCount = 0
        var totalCheckerCount = 0
        var newDefectCount = 0
        var historyDefectCount = 0
        var existCount = 0
        var fixCount = 0
        var ignoreCount = 0
        // 过滤计数
        val it = defectList.iterator()
        while (it.hasNext()) {
            val ccnDefectModel = it.next()
            // 判断是否匹配告警状态，先收集统计匹配告警状态的规则、处理人、文件路径
            val notMatchStatus: Boolean = isNotMatchStatus(condStatusList, ccnDefectModel.status)
            // 按构建号筛选
            if (needBuildIdFilter && !currentBuildEntityIds.contains(ccnDefectModel.entityId)) {
                it.remove()
                continue
            }
            // 根据状态过滤，注：已修复和其他状态（忽略，路径屏蔽，规则屏蔽）不共存，已忽略状态优先于屏蔽
            val status: Int = ccnDefectModel.status
            when {
                ComConstants.DefectStatus.NEW.value() == status -> {
                    existCount++
                }
                ComConstants.DefectStatus.FIXED.value() and status > 0 -> {
                    fixCount++
                }
                ComConstants.DefectStatus.IGNORE.value() and status > 0 -> {
                    ignoreCount++
                }
            }
            // 严重程度条件不为空且与当前数据的严重程度不匹配时，判断为true移除，否则false不移除
            if (notMatchStatus) {
                it.remove()
                continue
            }
            // 5.按照严重程度统计缺陷数量并过滤
            fillingRiskFactor(ccnDefectModel, riskFactorConfMap)
            val riskFactor: Int = ccnDefectModel.riskFactor
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
                ComConstants.RiskFactor.L.value() -> {
                    lowCount++
                }
            }

            val meetSeverity = CollectionUtils.isNotEmpty(severity) && !severity.contains(riskFactor.toString())
            if (meetSeverity) {
                it.remove()
                continue
            }
            // 统计历史告警数和新告警数并过滤
            val defectLastUpdateTime = DateTimeUtils.getThirteenTimestamp(
                if (ccnDefectModel.latestDateTime == null) 0 else ccnDefectModel.latestDateTime
            )
            if (defectLastUpdateTime < newDefectJudgeTime) {
                historyDefectCount++
            } else {
                newDefectCount++
            }
            // 按新告警和历史告警筛选
            val defectType =
                if (defectLastUpdateTime > newDefectJudgeTime) ComConstants.DefectType.NEW.stringValue() else ComConstants.DefectType.HISTORY.stringValue()
            val notMatchDefectType = CollectionUtils.isNotEmpty(
                conditionDefectType
            ) && !conditionDefectType.contains(defectType)
            if (notMatchDefectType) {
                it.remove()
                continue
            }
            totalCheckerCount++
        }
        toolDefectRspVO.superHighCount = superHighCount
        toolDefectRspVO.highCount = highCount
        toolDefectRspVO.mediumCount = mediumCount
        toolDefectRspVO.lowCount = lowCount
        toolDefectRspVO.existCount = existCount
        toolDefectRspVO.fixCount = fixCount
        toolDefectRspVO.ignoreCount = ignoreCount
        toolDefectRspVO.newDefectCount = newDefectCount
        toolDefectRspVO.historyDefectCount = historyDefectCount
        toolDefectRspVO.totalCount = totalCheckerCount
        toolDefectRspVO.newDefectJudgeTime = newDefectJudgeTime
    }

    /**
     * 根据标志修改时间与最近一次分析时间比较来判断告警是否是被标记后仍未被修改
     *
     * @param mark
     * @param markTime
     * @param statisticModel
     * @return
     */
    fun convertMarkStatus(mark: Int?, markTime: Long?, statisticModel: CCNStatisticModel?): Int? {
        var markVar = mark
        val lastAnalyzeTime = statisticModel?.time ?: 0L

        if (markVar != null && markVar == ComConstants.MarkStatus.MARKED.value() && markTime != null) {
            if (markTime < lastAnalyzeTime) {
                markVar = ComConstants.MarkStatus.NOT_FIXED.value()
            }
        }
        return markVar
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
        logger.info("query task[{}] defect list by {}", taskId, queryWarningReq)
        val toolDefectRspVO = ToolDefectRspVO()

        val pageNumReq = pageNum ?: 1
        val pageSizeReq = pageSize ?: 100
        val sortTypeVar = sortType ?: Sort.Direction.DESC.name
        val sortFieldVar = sortField ?: "ccn"

        // 获取CCN配置信息
        val toolConfigInfoModel = toolDao.findByTaskIdAndTool(taskId, ComConstants.Tool.CCN.name)

        val originalCCNDefectList = ccnDefectDao.findByTaskIdAndAuthorAndRelPaths(
            taskId, queryWarningReq.author,
            queryWarningReq.fileList
        )
        // 查询ccn圈复杂度阀值
        val ccnThreshold = checkerService.getCcnThreshold(toolConfigInfoModel)
        toolDefectRspVO.ccnThreshold = ccnThreshold

        // 根据根据前端传入的条件过滤告警，并分类统计
        filterDefectByCondition(taskId, originalCCNDefectList, null, queryWarningReq, toolDefectRspVO)

        val statisticModels = ccnDefectDao.batchFindByTaskIdInAndTool(mutableSetOf(taskId), ComConstants.Tool.CCN.name)
        val statisticModel = if (statisticModels.isNullOrEmpty()) CCNStatisticModel() else statisticModels[0]
        val ccnDefectVOS = originalCCNDefectList.stream().map { it ->
            val ccnDefectVO = CCNDefectVO()
            BeanUtils.copyProperties(it, ccnDefectVO)
            ccnDefectVO.mark = convertMarkStatus(ccnDefectVO.mark, ccnDefectVO.markTime, statisticModel)
            ccnDefectVO
        }.collect(Collectors.toList())

        toolDefectRspVO.ccnDefectList = sortAndPage(pageNumReq, pageSizeReq, sortFieldVar, sortTypeVar, ccnDefectVOS)
        toolDefectRspVO.taskId = taskId
        toolDefectRspVO.toolName = queryWarningReq.toolName

        return toolDefectRspVO
    }

    override fun statCheckerDefect(reqVO: TaskToolInfoReqVO, pageNum: Int?, pageSize: Int?): Page<CheckerDefectStatVO> {
        throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("toolName"))
    }

}
