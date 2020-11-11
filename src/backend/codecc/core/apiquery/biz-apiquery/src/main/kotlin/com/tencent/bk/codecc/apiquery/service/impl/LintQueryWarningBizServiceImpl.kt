package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.BuildDefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.LintDefectDao
import com.tencent.bk.codecc.apiquery.defect.model.LintDefectV2Model
import com.tencent.bk.codecc.apiquery.defect.model.LintStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.op.TaskDefectVO
import com.tencent.bk.codecc.apiquery.vo.report.CommonChartAuthorVO
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.util.DateTimeUtils
import org.apache.commons.lang.ObjectUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("LINTQueryWarningBizService")
class LintQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val taskLogService: TaskLogService,
    private val statisticDao: StatisticDao,
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
            val lintFileModelList = defectDao.findLintByTaskIdInAndToolName(taskIdList, toolName, filterFields, status,
                    checker, notChecker, pageable
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


    override fun queryDeptTaskDefect(reqVO: TaskToolInfoReqVO, pageNum: Int?, pageSize: Int?, sortField: String?,
                                     sortType: String?): Page<TaskDefectVO> {
        logger.info("TaskToolInfoReqVO content: pageNum[$pageNum] pageSize[$pageSize] $reqVO")
        val toolName = reqVO.toolName
        val timeoutDays = reqVO.timeoutDays

        // 日期范围时间戳
        val startTime = DateTimeUtils.getTimeStampStart(reqVO.startTime)
        val endTime = DateTimeUtils.getTimeStampEnd(reqVO.endTime)
        reqVO.startTime = null
        reqVO.endTime = null

        val taskDefectList = mutableListOf<TaskDefectVO>()
        reqVO.taskIds = toolService.findTaskIdByToolNames(toolName, ComConstants.FOLLOW_STATUS.ACCESSED.value(), false)

        if (reqVO.status == null) {
            reqVO.status = ComConstants.Status.ENABLE.value()
        }
        if (reqVO.hasAdminTask == null) {
            reqVO.hasAdminTask = 1
        } else {
            reqVO.excludeUserList = metaDataService.queryExcludeUserList()
        }

        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val (count, page, pageSize1, totalPages, records) = taskDao.findTaskInfoPage(reqVO, pageable)
        if (!records.isNullOrEmpty()) {
            // 代码语言元数据
            val langMetadataList = metaDataService.codeLangMetadataList
            // 组织架构信息
            val deptInfoMap = redisTemplate.opsForHash<String, String>().entries(RedisKeyConstants.KEY_DEPT_INFOS)
            // 筛选告警状态：1新增 3修复
            val fixed = DefectStatus.NEW.value() or DefectStatus.FIXED.value()
            val pathMask = DefectStatus.NEW.value() or DefectStatus.PATH_MASK.value()
            val ignore = DefectStatus.NEW.value() or DefectStatus.IGNORE.value()
            // 超时告警：截止时间 - 超时天数时间戳
            val timeoutEndTime = endTime - DateTimeUtils.DAY_TIMESTAMP * timeoutDays

            // 分割等长列表后遍历
            val pageTaskLists = PageUtils.averageAssignFixLength(records, 100)
            pageTaskLists.forEach { taskList ->
                val taskIdList = taskList.map { it.taskId }

                // 最近分析状态
                val lastTaskLogMap = taskLogService.batchFindLastTaskLogByTool(taskIdList, toolName)
                // 批量查询各状态告警列表
                val newAddDefect = lintDefectDao.batchQueryDefect(taskIdList, toolName, pathMask, "create_time",
                        startTime, endTime, true)
                val fixedDefect = lintDefectDao.batchQueryDefect(taskIdList, toolName, fixed, "fixed_time", startTime,
                        endTime, false)
                val existDefect = lintDefectDao.batchQueryDefect(taskIdList, toolName, DefectStatus.NEW.value(),
                        "create_time", 0, endTime, false)
                val ignoreDefect = lintDefectDao.batchQueryDefect(taskIdList, toolName, ignore, "ignore_time",
                        startTime,
                        endTime, false)
                val existTimeoutDefect = lintDefectDao.batchQueryDefect(taskIdList, toolName, DefectStatus.NEW.value(),
                        "create_time", 0, timeoutEndTime, false)

                val newAddDefectMap = newAddDefect.groupBy { it.taskId }
                val fixedDefectMap = fixedDefect.groupBy { it.taskId }
                val existDefectMap = existDefect.groupBy { it.taskId }
                val ignoreDefectMap = ignoreDefect.groupBy { it.taskId }
                val timeoutDefectMap = existTimeoutDefect.groupBy { it.taskId }

                for (it in taskList) {
                    val taskDefectVO = TaskDefectVO()
                    BeanUtils.copyProperties(it, taskDefectVO)
                    taskDefectVO.bgName = ObjectUtils.toString(deptInfoMap[it.bgId.toString()], "")
                    taskDefectVO.deptName = ObjectUtils.toString(deptInfoMap[it.deptId.toString()], "")
                    taskDefectVO.centerName = ObjectUtils.toString(deptInfoMap[it.centerId.toString()], "")
                    taskDefectVO.codeLang = ConvertUtil.convertCodeLang(it.codeLang, langMetadataList)

                    val taskId = it.taskId

                    var analyzeDateStr = ""
                    val taskLogModel = lastTaskLogMap[taskId]
                    if (taskLogModel != null) {
                        val analyzeStatus = ConvertUtil.generaAnalyzeStatus4MultiTool(taskLogModel.currStep,
                                taskLogModel.flag)
                        analyzeDateStr = DateTimeUtils.second2DateString(taskLogModel.startTime) + " " + analyzeStatus
                    }
                    taskDefectVO.analyzeDate = analyzeDateStr

                    val newAddCount = CommonChartAuthorVO()
                    taskDefectVO.newAddCount = newAddCount
                    newAddDefectMap[taskId]?.forEach { taskDefectVO.newAddCount.setProp(it.severity, it.lineNum) }
                    fixedDefectMap[taskId]?.forEach { taskDefectVO.fixedCount.setProp(it.severity, it.lineNum) }
                    existDefectMap[taskId]?.forEach { taskDefectVO.existCount.setProp(it.severity, it.lineNum) }
                    // 统计忽略告警数
                    val ignoreCount = CommonChartAuthorVO()
                    ignoreDefectMap[taskId]?.forEach { ignoreCount.setProp(it.severity, it.lineNum) }
                    taskDefectVO.ignoreCount = ignoreCount.total
                    // 统计超时告警数
                    val existTimeoutCount = CommonChartAuthorVO()
                    timeoutDefectMap[taskId]?.forEach { existTimeoutCount.setProp(it.severity, it.lineNum) }
                    taskDefectVO.timeoutDefectNum = existTimeoutCount.total

                    taskDefectList.add(taskDefectVO)
                }
            }
        }

        return Page(count, page, pageSize1, totalPages, taskDefectList)
    }
}