package com.tencent.bk.codecc.apiquery.service.impl

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.tencent.bk.codecc.apiquery.defect.dao.BuildDefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.LintDefectDao
import com.tencent.bk.codecc.apiquery.defect.model.LintDefectV2Model
import com.tencent.bk.codecc.apiquery.defect.model.LintStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.ICheckerService
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.service.TaskLogService
import com.tencent.bk.codecc.apiquery.service.TaskService
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.bk.codecc.apiquery.utils.PageUtils.convertPageSizeToPageable
import com.tencent.bk.codecc.apiquery.vo.DefectQueryReqVO
import com.tencent.bk.codecc.apiquery.vo.LintDefectVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.util.DateTimeUtils
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import com.tencent.devops.common.util.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

@Service("LINTQueryWarningBizService")
class LintQueryWarningBizServiceImpl @Autowired constructor(
    private val taskDao: TaskDao,
    private val lintDefectDao: LintDefectDao,
    private val defectDao: DefectDao,
    private val taskLogService: TaskLogService,
    private val statisticDao: StatisticDao,
    private val taskService: TaskService,
    private val checkerService: ICheckerService,
    private val buildDefectDao: BuildDefectDao
) : IDefectQueryWarningService<LintDefectV2Model, LintStatisticModel> {

    companion object {
        private val logger = LoggerFactory.getLogger(LintQueryWarningBizServiceImpl::class.java)
    }

    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<LintDefectV2Model> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        return with(defectQueryParam) {
            val defectIds = if(!buildId.isNullOrBlank()){
                val buildDefectList = buildDefectDao.findByTaskIdToolNameAndBuildIdOrderByFilePath(taskIdList, toolName!!, buildId!!, pageNum, pageSize, sortField, sortType)
                if(!buildDefectList.isNullOrEmpty()){
                    buildDefectList.map { ObjectId(it.fileDefectIds) }
                } else {
                    null
                }
            } else {
                null
            }

//            val defectIds = null
            val lintFileModelList = defectDao.findLintByTaskIdInAndToolName(taskIdList, toolName, filterFields, status,
                    checker, notChecker, defectIds, pageNum, pageSize, sortField, sortType
            )
            Page(pageable.pageNumber + 1, pageable.pageSize, lintFileModelList.size.toLong(), lintFileModelList)
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
    ): Page<LintStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val lintStatisticList = statisticDao.findLintByTaskIdInAndToolName(
            taskIdList,
            toolName,
            startTime,
            endTime,
            filterFields,
            buildId,
            pageable
        )

        // add unique id
        lintStatisticList.forEach { model ->
            model.checkerStatistic?.forEach {
                it.id = toolName + ":" + it.name
            }
        }

        return Page(pageable.pageNumber + 1, pageable.pageSize, lintStatisticList.size.toLong(), lintStatisticList)
    }

    fun defectCommitSuccess(taskId: Long, toolName: String?, buildId: String?): Boolean {
        var result = false
        val buildTaskLog = taskLogService.getBuildTaskLog(taskId, toolName, buildId)
        if (buildTaskLog != null && CollectionUtils.isNotEmpty(buildTaskLog.stepArray)) {
            for (taskUnit in buildTaskLog.stepArray) {
                if (ComConstants.Step4MutliTool.COMMIT.value() == taskUnit.stepNum
                        && ComConstants.StepFlag.SUCC.value() == taskUnit.flag) {
                    result = true
                    break
                }
            }
        }
        return result
    }

    protected fun getDefectIdsByBuildId(taskId: Long, toolName: String, buildId: String?): Set<String>? {
        var defectIdSet: MutableSet<String> = mutableSetOf()
        if (StringUtils.isNotEmpty(buildId) && defectCommitSuccess(taskId, toolName, buildId)) {
            defectIdSet = HashSet()
            val buildFiles = buildDefectDao.findByTaskIdToolNameAndBuildId(listOf(taskId), toolName, buildId!!)
            if (CollectionUtils.isNotEmpty(buildFiles)) {
                for (buildDefectEntity in buildFiles) {
                    defectIdSet.addAll(buildDefectEntity.fileDefectIds)
                }
            }
        }
        return defectIdSet
    }

    protected fun getNewDefectJudgeTime(taskId: Long, toolName: String, conditionDefectType: Set<String>?): Long {
        var firstSuccessTime: Long = 0
        if (CollectionUtils.isNotEmpty(conditionDefectType) && !conditionDefectType!!.containsAll(Sets.newHashSet(
                        ComConstants.DefectType.NEW.stringValue(), ComConstants.DefectType.HISTORY.stringValue()))) {
            // 查询新老告警判定时间，即首次分析成功时间
            val firstSuccessTimeModel = taskService.getFirstAnalyzeSuccess(taskId, toolName)
            if (firstSuccessTimeModel != null) {
                firstSuccessTime = firstSuccessTimeModel.firstAnalysisSuccessTime
            }
        }
        return firstSuccessTime
    }

    protected fun getDefectBaseFieldMap(): Map<String, Boolean> {
        val filedMap: MutableMap<String, Boolean> = HashMap()
        filedMap["_id"] = true
        filedMap["id"] = true
        filedMap["file_name"] = true
        filedMap["line_num"] = true
        filedMap["file_path"] = true
        filedMap["checker"] = true
        filedMap["message"] = true
        filedMap["author"] = true
        filedMap["severity"] = true
        filedMap["line_update_time"] = true
        filedMap["create_time"] = true
        filedMap["create_build_number"] = true
        filedMap["status"] = true
        filedMap["mark"] = true
        filedMap["mark_time"] = true
        return filedMap
    }

    /**
     * 根据标志修改时间与最近一次分析时间比较来判断告警是否是被标记后仍未被修改
     *
     * @param mark
     * @param markTime
     * @param statisticModel
     * @return
     */
    fun convertMarkStatus(mark: Int?, markTime: Long?, statisticModel: LintStatisticModel?): Int? {
        var markVar = mark
        val lastAnalyzeTime = statisticModel?.time ?: 0L

        if (markVar != null && markVar == ComConstants.MarkStatus.MARKED.value() && markTime != null) {
            if (markTime < lastAnalyzeTime) {
                markVar = ComConstants.MarkStatus.NOT_FIXED.value()
            }
        }
        return markVar
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
        // 排序分页
        val pageNumVar = if (pageNum == null || (pageNum - 1) < 0) 0 else pageNum - 1
        val pageSizeVar = if (pageSize == null) 100 else if (pageSize >= 1000) 1000 else pageSize
        val sortFieldVar = sortField ?: "fileName"
        val sortTypeVar = sortType ?: Sort.Direction.ASC.name

        val toolDefectRspVO = ToolDefectRspVO()
        // 获取任务信息
        val taskInfoModel = taskDao.findTaskById(taskId) ?: throw CodeCCException(
                CommonMessageCode.RECORD_NOT_EXITS)
        val toolName = queryWarningReq.toolName
        val buildId = queryWarningReq.buildId

        // 获取相同包id下的规则集合
        val pkgRealChecker = checkerService.queryPkgRealCheckers(queryWarningReq.pkgId, toolName, taskInfoModel)
        val pkgChecker = mutableSetOf<String>()
        // 获取规则集的规则集合
        if (queryWarningReq.checkerSet != null) {
            val queryCheckerSet = queryWarningReq.checkerSet
            val checkerSetItem = checkerService.findByCheckerSetIdAndVersion(queryCheckerSet.checkerSetId,
                    queryCheckerSet.version)
            val allChecker = checkerSetItem.checkerProps.filter { it ->
                toolName.equals(it.toolName, ignoreCase = true)
            }.map { checkerPropsModel -> checkerPropsModel.checkerKey }

            pkgChecker.addAll(allChecker)
            if (CollectionUtils.isEmpty(pkgChecker)) {
                return toolDefectRspVO
            }
            logger.info("get checker for task: {}, {}", taskId, pkgChecker.size)
        }

        // 获取某个构建id下的告警id
        val defectIdSet = getDefectIdsByBuildId(taskId, toolName, buildId)

        // 获取新旧告警判断时间
        val newDefectJudgeTime = getNewDefectJudgeTime(taskId, toolName, queryWarningReq.defectType)

        var condStatusList = queryWarningReq.status
        if (CollectionUtils.isEmpty(condStatusList)) {
            condStatusList = HashSet(1)
            condStatusList.add(DefectStatus.NEW.value())
            queryWarningReq.status = condStatusList
        }

        // 按文件聚类
        val clusterType = queryWarningReq.clusterType
        if (StringUtils.isNotEmpty(clusterType) && ComConstants.ClusterType.file.name.equals(clusterType,
                        ignoreCase = true)) {
            val pageResult =
                    lintDefectDao.findDefectFilePageByCondition(taskId, queryWarningReq, defectIdSet, pkgChecker,
                            newDefectJudgeTime, pageNumVar, pageSizeVar, sortFieldVar, sortTypeVar)
            toolDefectRspVO.lintFileList = pageResult
        } else {
            val filedMap = getDefectBaseFieldMap()
            val pageResult = lintDefectDao.findDefectPageByCondition(taskId, queryWarningReq, defectIdSet,
                    pkgChecker, newDefectJudgeTime, filedMap, pageNumVar, pageSizeVar, sortFieldVar,
                    Sort.Direction.valueOf(sortTypeVar))
            val records = pageResult.records

            var defectVOList: List<LintDefectVO> = ArrayList()
            if (CollectionUtils.isNotEmpty(records)) {
                val statisticModel = statisticDao.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName)
                defectVOList = records.map { lintDefectV2Model ->
                    val defectVO = LintDefectVO()
                    BeanUtils.copyProperties(lintDefectV2Model, defectVO)
                    defectVO.entityId = lintDefectV2Model.defectId
                    defectVO.mark = convertMarkStatus(lintDefectV2Model.mark, lintDefectV2Model.markTime,
                            statisticModel)
                    defectVO.severity = if (defectVO.severity == ComConstants.PROMPT_IN_DB) ComConstants.PROMPT else defectVO.severity
                    defectVO
                }
            }

            toolDefectRspVO.lintDefectList =
                    Page(pageResult.count, pageResult.page, pageResult.pageSize, pageResult.totalPages, defectVOList)
        }

        toolDefectRspVO.taskId = taskId
        toolDefectRspVO.firstAnalysisSuccessTime = newDefectJudgeTime
        toolDefectRspVO.newDefectJudgeTime = newDefectJudgeTime

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
        logger.info("statLintCheckerDefect req content: pageNum[{}] pageSize[{}], {}", pageNum, pageSize, reqVO)

        // 分页统计,每次最多500个任务
        val pageSizeInt = if (pageSize == null) 100 else if (pageSize >= 500) 500 else pageSize
        val pageable = convertPageSizeToPageable(pageNum, pageSizeInt, "task_id", "ASC")

        // 设置默认查询条件：默认查工蜂扫描
        if (CollectionUtils.isEmpty(reqVO.createFrom)) {
            reqVO.createFrom = Sets.newHashSet(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value())
        }
        // 默认查询有效任务
        if (reqVO.status == null) {
            reqVO.status = ComConstants.Status.ENABLE.value()
        }
        // 默认查待修复告警 | -> 按位或
        val defectStatus = if (reqVO.defectStatus == null) {
            DefectStatus.NEW.value()
        } else {
            reqVO.defectStatus or DefectStatus.NEW.value()
        }

        val startTime = DateTimeUtils.getTimeStampStart(reqVO.startTime)
        val endTime = DateTimeUtils.getTimeStampEnd(reqVO.endTime)

        var data: List<CheckerDefectStatVO> = Lists.newArrayList()

        val (count, page, pageSize1, totalPages, taskInfoModels) = taskDao.findTaskIdListByCondition(reqVO, pageable)
        if (CollectionUtils.isNotEmpty(taskInfoModels)) {
            val taskIdSet = taskInfoModels.map { obj -> obj.taskId }
            val defectByGroupChecker =
                    lintDefectDao.findDefectByGroupChecker(taskIdSet, reqVO.toolName, defectStatus, startTime, endTime)

            if (CollectionUtils.isNotEmpty(defectByGroupChecker)) {
                logger.info("findDefectByGroupChecker size: ${defectByGroupChecker.size}")
                data = defectByGroupChecker.map { model ->
                    val defectStatVO = CheckerDefectStatVO(
                            model.taskId,
                            model.toolName,
                            model.checker,
                            model.severity,
                            model.lineNum
                    )
                    defectStatVO
                }
            }
        }
        return Page(count, page, pageSize1, totalPages, data)
    }

}
