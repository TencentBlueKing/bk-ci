package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.model.CommonStatisticModel
import com.tencent.bk.codecc.apiquery.defect.model.DefectModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.service.MetaDataService
import com.tencent.bk.codecc.apiquery.service.TaskLogService
import com.tencent.bk.codecc.apiquery.service.ToolService
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao
import com.tencent.bk.codecc.apiquery.utils.ConvertUtil
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
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service("CommonQueryWarningBizService")
class CommonQueryWarningBizServiceImpl @Autowired constructor(
    private val toolService: ToolService,
    private val taskDao: TaskDao,
    private val metaDataService: MetaDataService,
    private val redisTemplate: RedisTemplate<String, String>,
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao,
    private val taskLogService: TaskLogService
) : IDefectQueryWarningService<DefectModel, CommonStatisticModel> {

    companion object {
        private val logger = LoggerFactory.getLogger(CommonQueryWarningBizServiceImpl::class.java)
    }

    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<DefectModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        return with(defectQueryParam) {
            val commonDefectList = defectDao.findCommonByTaskIdInAndToolName(taskIdList, toolName, filterFields, status, checker, pageable)

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
            taskIdList, toolName, startTime, endTime, filterFields, pageable)

        // add unique id
        commonStatisticList.forEach { model ->
            model.checkerStatistic?.forEach {
                it.id = toolName + ":" + it.name
            }
        }

        return Page(pageable.pageNumber + 1, pageable.pageSize, commonStatisticList.size.toLong(), commonStatisticList)
    }


    /**
     * 按部门查询对应工具的原始告警数据统计
     */
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
        if (reqVO.hasAdminTask == null) {
            reqVO.hasAdminTask = 1
        } else {
            reqVO.excludeUserList = metaDataService.queryExcludeUserList()
        }

        if (reqVO.status == null) {
            reqVO.status = ComConstants.Status.ENABLE.value()
        }

        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val (count, page, pageSize1, totalPages, records) = taskDao.findTaskInfoPage(reqVO, pageable)
        if (!records.isNullOrEmpty()) {
            // 代码语言元数据
            val langMetadataList = metaDataService.codeLangMetadataList
            // 组织架构信息
            val deptInfoMap = redisTemplate.opsForHash<String, String>().entries(RedisKeyConstants.KEY_DEPT_INFOS)
            // 筛选告警状态：1新增 3修复 5忽略
            val new = DefectStatus.NEW.value()
            val defectStatusList = mutableListOf(new, (new or DefectStatus.FIXED.value()), (new or DefectStatus
                    .IGNORE.value()))

            val pageTaskLists = PageUtils.averageAssignFixLength(records, 100)
            pageTaskLists.forEach { taskList ->
                val taskIdList = taskList.map { it.taskId }
                // 最近分析状态
                val lastTaskLogMap = taskLogService.batchFindLastTaskLogByTool(taskIdList, toolName)

                // 批量查询告警列表
                val batchQueryDefect = defectDao.batchQueryDefect(taskIdList, toolName, defectStatusList)
                val defectMapGroupByTaskId = batchQueryDefect.groupBy { it.taskId }

                for (it in taskList) {
                    val taskDefectVO = TaskDefectVO()
                    BeanUtils.copyProperties(it, taskDefectVO)
                    taskDefectVO.bgName = ObjectUtils.toString(deptInfoMap[it.bgId.toString()], "")
                    taskDefectVO.deptName = ObjectUtils.toString(deptInfoMap[it.deptId.toString()], "")
                    taskDefectVO.centerName = ObjectUtils.toString(deptInfoMap[it.centerId.toString()], "")
                    taskDefectVO.codeLang = ConvertUtil.convertCodeLang(it.codeLang, langMetadataList)

                    val taskId = it.taskId

                    // 组装最近分析状态
                    var analyzeDateStr = ""
                    val taskLogModel = lastTaskLogMap[taskId]
                    if (taskLogModel != null) {
                        val analyzeStatus = ConvertUtil.generaAnalyzeStatus4Cov(taskLogModel.currStep,
                                taskLogModel.flag)
                        analyzeDateStr = DateTimeUtils.second2DateString(taskLogModel.startTime) + " " + analyzeStatus
                    }
                    taskDefectVO.analyzeDate = analyzeDateStr

                    val newAddCount = CommonChartAuthorVO()
                    taskDefectVO.newAddCount = newAddCount

                    val defectList = defectMapGroupByTaskId[taskId]
                    if (defectList.isNullOrEmpty()) {
                        taskDefectVO.ignoreCount = 0
                        taskDefectVO.timeoutDefectNum = 0
                        taskDefectList.add(taskDefectVO)
                        continue
                    }

                    var defectTimeoutNum = 0
                    var ignoreDefectCount = 0
                    for (defect in defectList) {
                        val createTime = defect.createTime
                        val status = defect.status
                        val severity = defect.severity

                        // 跳过屏蔽的告警
                        if ((new or DefectStatus.PATH_MASK.value()) == status && (new or DefectStatus.CHECKER_MASK
                                        .value()) == status) {
                            continue
                        }
                        if (createTime in startTime..endTime) {
                            taskDefectVO.newAddCount.count(severity)
                        }
                        // 已修复告警
                        val fixedTime = defect.fixedTime
                        if (fixedTime in startTime..endTime) {
                            if (DefectStatus.FIXED.value() and status > 0) {
                                taskDefectVO.fixedCount.count(severity)
                            }
                        }
                        // 已忽略告警数
                        if (status == (new or DefectStatus.IGNORE.value())) {
                            if (defect.ignoreTime in startTime..endTime) {
                                ignoreDefectCount++
                            }
                        }
                        // 遗留未修复的告警
                        if (createTime <= endTime && DefectStatus.NEW.value() == status) {
                            taskDefectVO.existCount.count(severity)
                            // 统计超时告警数
                            if ((endTime - createTime) / DateTimeUtils.DAY_TIMESTAMP > timeoutDays) {
                                defectTimeoutNum++
                            }
                        }
                    }
                    taskDefectVO.ignoreCount = ignoreDefectCount
                    taskDefectVO.timeoutDefectNum = defectTimeoutNum
                    taskDefectList.add(taskDefectVO)
                }
            }
        }

        return Page(count, page, pageSize1, totalPages, taskDefectList)
    }


}