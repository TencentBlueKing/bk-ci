package com.tencent.bk.codecc.apiquery.service.impl

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.tencent.bk.codecc.apiquery.defect.dao.BuildDefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.model.CommonStatisticModel
import com.tencent.bk.codecc.apiquery.defect.model.DefectModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.service.MetaDataService
import com.tencent.bk.codecc.apiquery.service.TaskService
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.ToolDao
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.bk.codecc.apiquery.vo.DefectDetailVO
import com.tencent.bk.codecc.apiquery.vo.DefectQueryReqVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import com.tencent.devops.common.util.DateTimeUtils
import com.tencent.devops.common.util.ListSortUtil
import com.tencent.devops.common.util.PathUtils
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import com.tencent.devops.common.util.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service("CommonQueryWarningBizService")
class CommonQueryWarningBizServiceImpl @Autowired constructor(
    private val taskDao: TaskDao,
    private val toolDao: ToolDao,
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao,
    private val taskService: TaskService,
    private val metaDataService: MetaDataService,
    private val buildDefectDao: BuildDefectDao
) : IDefectQueryWarningService<DefectModel, CommonStatisticModel> {

    companion object {
        private val logger = LoggerFactory.getLogger(CommonQueryWarningBizServiceImpl::class.java)
    }

    @Value("\${bkci.public.url:#{null}}")
    lateinit var codeccHost: String

    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<DefectModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        return with(defectQueryParam) {
            val defectIds = if (!defectQueryParam.buildId.isNullOrBlank()) {
                buildDefectDao.findByTaskIdToolNameAndBuildId(taskIdList, toolName!!, buildId!!).map { it.defectId }
            } else {
                null
            }
            val commonDefectList = defectDao.findCommonByTaskIdInAndToolName(
                taskIdList,
                toolName,
                filterFields,
                status,
                checker,
                defectIds,
                pageNum,
                pageSize,
                sortField,
                sortType
            )

            Page(pageable.pageNumber + 1, pageable.pageSize, commonDefectList.size.toLong(), commonDefectList)
        }
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
    ): Page<CommonStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val commonStatisticList = statisticDao.findCommonByTaskIdInAndToolName(
            taskIdList, toolName, startTime, endTime, filterFields, pageable
        )

        // add unique id
        commonStatisticList.forEach { model ->
            model.checkerStatistic?.forEach {
                it.id = toolName + ":" + it.name
            }
        }

        return Page(pageable.pageNumber + 1, pageable.pageSize, commonStatisticList.size.toLong(), commonStatisticList)
    }

    private fun getConditionFilterFiles(
        defectQueryReqVO: DefectQueryReqVO
    ): Set<String>? {
        return defectQueryReqVO.fileList
    }

    private fun checkIfMaskByPath(filePathname: String?, condFileList: Set<String>?): Boolean {
        return PathUtils.checkIfMaskByPath(filePathname, condFileList)
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

    private fun filterDefectByCondition(
        taskId: Long,
        defectList: List<DefectModel>,
        defectQueryReqVO: DefectQueryReqVO,
        resultDefectList: MutableList<DefectModel>
    ) {
        if (CollectionUtils.isEmpty(defectList)) {
            logger.info("task[{}] defect entity list is empty", taskId)
        }
        val firstSuccessTimeModel = taskService.getFirstAnalyzeSuccess(taskId, defectQueryReqVO.toolName)
        var firstSuccessTime = 0L
        if (firstSuccessTimeModel != null) {
            firstSuccessTime = firstSuccessTimeModel.firstAnalysisSuccessTime
        }
        // 根据查询条件进行过滤，并统计数量
        val condAuthor = defectQueryReqVO.author
        val condChecker = defectQueryReqVO.checker
        val condStartCreateTime = defectQueryReqVO.startCreateTime
        val condEndCreateTime = defectQueryReqVO.endCreateTime
        val condFileList = this.getConditionFilterFiles(defectQueryReqVO)
        val condSeverityList = defectQueryReqVO.severity
        val condDefectTypeList = defectQueryReqVO.defectType
        var condStatusList = defectQueryReqVO.status
        if (CollectionUtils.isEmpty(condStatusList)) {
            condStatusList = HashSet(3)
            condStatusList.add(DefectStatus.NEW.value())
            condStatusList.add(DefectStatus.FIXED.value())
            condStatusList.add(DefectStatus.IGNORE.value())
        }
        val it = defectList.iterator()
        while (it.hasNext()) {
            val defectModel = it.next()

            // 判断是否匹配告警状态，先收集统计匹配告警状态的规则、处理人、文件路径
            val notMatchStatus: Boolean = isNotMatchStatus(condStatusList, defectModel.status)
            val checkerName: String = defectModel.checkerName
            val authorList: Set<String>? = defectModel.authorList

            // 规则类型条件不为空且与当前数据的规则类型不匹配时，判断为true移除，否则false不移除
            if (StringUtils.isNotEmpty(condChecker) && condChecker != checkerName) {
                continue
            }

            // 告警作者条件不为空且与当前数据的作者不匹配时，判断为true移除，否则false不移除
            val notMatchAuthor = StringUtils.isNotEmpty(condAuthor) && (CollectionUtils.isEmpty(authorList) ||
                !authorList!!.contains(condAuthor))
            if (notMatchAuthor) {
                continue
            }

            // 根据创建时间过滤，判断为true移除，否则false不移除
            val notMatchDateTime = DateTimeUtils.filterDate(
                condStartCreateTime, condEndCreateTime,
                defectModel.createTime
            )
            if (notMatchDateTime) {
                continue
            }

            // 根据文件过滤
            var notMatchFilePath = false
            if (CollectionUtils.isNotEmpty(condFileList)) { // 判断文件名是否匹配文件路径列表，不匹配就移除
                notMatchFilePath = !checkIfMaskByPath(defectModel.filePathname, condFileList)
            }
            if (notMatchFilePath) {
                continue
            }

            // 严重程度条件不为空且与当前数据的严重程度不匹配时，判断为true移除，否则false不移除
            if (notMatchStatus) {
                continue
            }

            // 根据严重等级过滤
            val severity: Int = defectModel.severity
            // 严重程度条件不为空且与当前数据的严重程度不匹配时，判断为true移除，否则false不移除
            val notMatchSeverity = CollectionUtils.isNotEmpty(
                condSeverityList
            ) &&
                !condSeverityList.contains(severity.toString())
            if (notMatchSeverity) {
                continue
            }

            // 根据新旧告警类型过滤
            val defectType = if (defectModel.createTime > firstSuccessTime) {
                ComConstants.DefectType.NEW.stringValue()
            } else {
                ComConstants.DefectType.HISTORY.stringValue()
            }
            val notMatchDefectType = CollectionUtils.isNotEmpty(
                condDefectTypeList
            ) && !condDefectTypeList.contains(defectType)
            if (notMatchDefectType) {
                continue
            }
            resultDefectList.add(defectModel)
        }
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
            sortFieldVar = "createTime"
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

    override fun queryToolDefectList(
        taskId: Long,
        queryWarningReq: DefectQueryReqVO,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): ToolDefectRspVO {
        logger.info("query task[{}] defect list by {}", taskId, queryWarningReq)

        val toolName = queryWarningReq.toolName
        val toolDefectRspVO = ToolDefectRspVO()

        // 根据任务ID和工具名查询所有的告警
        val defectList: List<DefectModel> = defectDao.findCommonByTaskIdInAndToolName(
            mutableListOf(taskId),
            toolName, null, null, null, null, null, null, null, null
        )

        // 根据根据前端传入的条件过滤告警，并分类统计
        val resultDefectList = mutableListOf<DefectModel>()
        filterDefectByCondition(taskId, defectList, queryWarningReq, resultDefectList)

        val defectDetailExtVOs: MutableList<DefectDetailVO> = ArrayList()
        if (CollectionUtils.isNotEmpty(resultDefectList)) {
            // 获取任务信息
            val taskInfoModel = taskDao.findTaskById(taskId)

            val projectId = taskInfoModel!!.projectId
            resultDefectList.forEach {
                val defectVO = DefectDetailVO()
                BeanUtils.copyProperties(it, defectVO)
                if (ComConstants.Tool.COVERITY.name == toolName) {
                    defectVO.cid = it.id.toLong()
                }
                defectVO.filePathName = it.filePathname
                val url = String.format(
                    "%s/codecc/%s/task/%d/defect/compile/%s/list?entityId=%s", codeccHost,
                    projectId, taskId, toolName, it.entityId
                )
                defectVO.defectDetailUrl = url
                defectDetailExtVOs.add(defectVO)
            }
        }

        // 默认以创建时间排序和分页
        val detailExtPage = sortAndPage<DefectDetailVO>(pageNum, pageSize, sortField, sortType, defectDetailExtVOs)

        toolDefectRspVO.taskId = taskId
        toolDefectRspVO.toolName = toolName
        toolDefectRspVO.defectList = detailExtPage

        return toolDefectRspVO
    }

    /**
     * 按规则分页统计告警数
     */
    override fun statCheckerDefect(
            reqVO: TaskToolInfoReqVO,
            pageNum: Int?,
            pageSize: Int?
    ): Page<CheckerDefectStatVO> {
        logger.info("statCommonCheckerDefect req content: pageNum[{}] pageSize[{}], {}", pageNum, pageSize, reqVO)
        val startTime = DateTimeUtils.getTimeStampStart(reqVO.startTime)
        val endTime = DateTimeUtils.getTimeStampEnd(reqVO.endTime)

        // 默认查询有效任务
        if (reqVO.status == null) {
            reqVO.status = ComConstants.Status.ENABLE.value()
        }
        // 设置默认查询条件：默认查工蜂扫描
        if (CollectionUtils.isEmpty(reqVO.createFrom)) {
            reqVO.createFrom = Sets.newHashSet(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value())
        }
        // 默认查待修复告警 | -> 按位或
        val defectStatus = if (reqVO.defectStatus == null) {
            DefectStatus.NEW.value()
        } else {
            reqVO.defectStatus or DefectStatus.NEW.value()
        }
        if (reqVO.hasAdminTask == null) {
            reqVO.hasAdminTask = 1
        } else {
            reqVO.excludeUserList = metaDataService.queryExcludeUserList()
        }
        reqVO.taskIds = taskDao.findByBgIdAndDeptId(reqVO)
        reqVO.startTime = null
        reqVO.endTime = null
        reqVO.followStatus = -1
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, "task_id", Sort.Direction.ASC.name)
        val (count, page, pageSizeRes, totalPages, toolInfoList) = toolDao.findToolInfoPage(reqVO, pageable)
        var data: List<CheckerDefectStatVO> = Lists.newArrayList()
        if (!toolInfoList.isNullOrEmpty()) {
            val taskIdList = toolInfoList.map { it.taskId }
            val defectByGroupChecker =
                    defectDao.findDefectByGroupChecker(taskIdList, reqVO.toolName, defectStatus, startTime, endTime)

            if (!defectByGroupChecker.isNullOrEmpty()) {
                logger.info("findDefectByGroupChecker size: ${defectByGroupChecker.size}")
                data = defectByGroupChecker.map { defectModel ->
                    val checkerDefectStatVO = CheckerDefectStatVO(
                            defectModel.taskId,
                            defectModel.toolName,
                            defectModel.checkerName,
                            defectModel.severity,
                            defectModel.lineNumber
                    )
                    checkerDefectStatVO
                }
            }
        }
        return Page(count, page, pageSizeRes, totalPages, data)
    }

}
