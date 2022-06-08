/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.apiquery.service.impl.op

import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.service.AbstractOpDefectDataService
import com.tencent.bk.codecc.apiquery.service.MetaDataService
import com.tencent.bk.codecc.apiquery.service.TaskLogService
import com.tencent.bk.codecc.apiquery.service.TaskService
import com.tencent.bk.codecc.apiquery.service.ToolService
import com.tencent.bk.codecc.apiquery.utils.ConvertUtil
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.bk.codecc.apiquery.vo.DefectDetailVO
import com.tencent.bk.codecc.apiquery.vo.TaskInfoExtVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.apiquery.vo.op.TaskDefectVO
import com.tencent.bk.codecc.apiquery.vo.report.CommonChartAuthorVO
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import com.tencent.devops.common.util.DateTimeUtils
import org.apache.commons.lang.ObjectUtils
import org.slf4j.LoggerFactory
import com.tencent.devops.common.util.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service("CommonDefectDataBizService")
class CommonDefectDataBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    taskService: TaskService,
    toolService: ToolService,
    taskLogService: TaskLogService,
    metaDataService: MetaDataService,
    redisTemplate: RedisTemplate<String, String>
) : AbstractOpDefectDataService(taskService, toolService, taskLogService, metaDataService, redisTemplate) {

    companion object {
        private val logger = LoggerFactory.getLogger(CommonDefectDataBizServiceImpl::class.java)
    }

    /**
     * 按部门查询对应工具的原始告警数据统计
     */
    override fun queryDeptTaskDefect(
        reqVO: TaskToolInfoReqVO,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<TaskDefectVO> {
        logger.info("queryDeptTaskDefect ReqVO content: pageNum[$pageNum] pageSize[$pageSize] $reqVO")
        val toolName = reqVO.toolName
        val timeoutDays = reqVO.timeoutDays
        // 日期范围时间戳
        val startTime = DateTimeUtils.getTimeStampStart(reqVO.startTime)
        val endTime = DateTimeUtils.getTimeStampEnd(reqVO.endTime)

        val taskDefectList = mutableListOf<TaskDefectVO>()

        val pageable = PageUtils.generaPageableUnlimitedPageSize(pageNum, pageSize, sortField, sortType)
        val (count, page, pageSize1, totalPages, records) = queryTaskInfoPage(reqVO, pageable)
        if (!records.isNullOrEmpty()) {
            // 代码语言元数据
            val langMetadataList = queryLangMetadataList()
            // 组织架构信息
            val deptInfoMap = queryDeptInfoMap()
            // 筛选告警状态：1新增 3修复 5忽略 9/17屏蔽
            val new = DefectStatus.NEW.value()
            val exist = mutableListOf(new)
            val fixed = mutableListOf(new or DefectStatus.FIXED.value())
            val ignore = mutableListOf(new or DefectStatus.IGNORE.value())
            // 11-11调整：新增告警仅包括：待修复、已修复
            val newAdd = mutableListOf(new, new or DefectStatus.FIXED.value())
            // 超时告警：截止时间 - 超时天数时间戳
            val timeoutEndTime = endTime - DateTimeUtils.DAY_TIMESTAMP * timeoutDays

            val pageTaskLists = PageUtils.averageAssignFixLength(records, 1000)
            pageTaskLists.forEach { taskList ->
                val taskIdList = taskList.map { it.taskId }
                // 最近分析状态
                val lastTaskLogMap = taskLogService.batchFindLastTaskLogByTool(taskIdList, toolName)

                // 批量查询各状态告警列表
                val newAddDefect = defectDao.batchStatDefect(taskIdList, toolName, newAdd, "create_time", startTime,
                        endTime)
                val fixedDefect = defectDao.batchStatDefect(taskIdList, toolName, fixed, "fixed_time", startTime,
                        endTime)
                val existDefect = defectDao.batchStatDefect(taskIdList, toolName, exist, "create_time",
                        0, endTime)
                val ignoreDefect = defectDao.batchStatDefect(taskIdList, toolName, ignore, "ignore_time", startTime,
                        endTime)
                val existTimeoutDefect = defectDao.batchStatDefect(taskIdList, toolName, exist,
                        "create_time", 0, timeoutEndTime)

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
                    newAddDefectMap[taskId]?.forEach { taskDefectVO.newAddCount.setProp(it.severity, it.lineNumber) }
                    fixedDefectMap[taskId]?.forEach { taskDefectVO.fixedCount.setProp(it.severity, it.lineNumber) }
                    existDefectMap[taskId]?.forEach { taskDefectVO.existCount.setProp(it.severity, it.lineNumber) }
                    // 统计忽略告警数
                    val ignoreCount = CommonChartAuthorVO()
                    ignoreDefectMap[taskId]?.forEach { ignoreCount.setProp(it.severity, it.lineNumber) }
                    taskDefectVO.ignoreCount = ignoreCount.total
                    // 统计超时告警数
                    val existTimeoutCount = CommonChartAuthorVO()
                    timeoutDefectMap[taskId]?.forEach { existTimeoutCount.setProp(it.severity, it.lineNumber) }
                    taskDefectVO.timeoutDefectNum = existTimeoutCount.total

                    taskDefectList.add(taskDefectVO)
                }
            }
        }

        return Page(count, page, pageSize1, totalPages, taskDefectList)
    }

    /**
     * 指定查询告警字段
     */
    private fun getFieldSet(): Set<String> {
        return mutableSetOf("_id", "id", "task_id", "checker_name", "status", "file_path_name", "display_type",
                "author_list", "severity", "create_time", "fixed_time")
    }

    /**
     * 按部门分页批量导出告警列表
     */
    override fun batchQueryDeptDefectList(reqVO: TaskToolInfoReqVO, pageNum: Int?, pageSize: Int?): ToolDefectRspVO {
        logger.info("batchQueryDeptDefectList ReqVO content: pageNum[$pageNum] pageSize[$pageSize] $reqVO")
        val toolName = reqVO.toolName
        val defectStatus = reqVO.defectStatus
        // 日期范围时间戳
        var startTime = DateTimeUtils.getTimeStamp(reqVO.startTime)
        val endTime = DateTimeUtils.getTimeStamp(reqVO.endTime)

        val taskPageable = PageUtils.generaPageableUnlimitedPageSize(1, queryTaskPageSize, "task_id", "ASC")
        val defectPageable = PageUtils.generaPageableUnlimitedPageSize(pageNum, pageSize, "task_id", "ASC")
        val (_, _, _, _, taskRecords) = queryTaskInfoPage(reqVO, taskPageable)
        if (taskRecords.isEmpty()) {
            return ToolDefectRspVO()
        }

        // 按告警状态查询不同告警
        var timeField = "create_time"
        val new = DefectStatus.NEW.value()
        val status: List<Int>
        when (defectStatus) {
            new -> {
                status = mutableListOf(new, new or DefectStatus.FIXED.value())
            }
            DefectStatus.FIXED.value() -> {
                timeField = "fixed_time"
                status = mutableListOf(new or DefectStatus.FIXED.value())
            }
            // 已忽略
            DefectStatus.IGNORE.value() -> {
                timeField = "ignore_time"
                status = mutableListOf(new or DefectStatus.IGNORE.value())
            }
            // 已屏蔽
            DefectStatus.PATH_MASK.value() -> {
                timeField = "exclude_time"
                status = mutableListOf(new or DefectStatus.PATH_MASK.value(), new or DefectStatus.CHECKER_MASK.value())
            }
            // 遗留待修复
            else -> {
                startTime = 1
                status = mutableListOf(new)
            }
        }

        val taskIdList = taskRecords.map { it.taskId }
        val batchQueryDefectPage =
                defectDao.batchQueryDefectPage(taskIdList, toolName, status, timeField, startTime, endTime,
                        getFieldSet(), defectPageable)

        val dataList = batchQueryDefectPage.map {
            val defectDetailVO = DefectDetailVO()
            BeanUtils.copyProperties(it, defectDetailVO, "filePathname")
            defectDetailVO.filePathName = it.filePathname
            defectDetailVO
        }

        val resultTaskIdList = batchQueryDefectPage.map { it.taskId }
        val taskInfoVoMap = taskRecords.filter { resultTaskIdList.contains(it.taskId) }.map {
            val taskInfoExtVO = TaskInfoExtVO()
            BeanUtils.copyProperties(it, taskInfoExtVO)
            taskInfoExtVO
        }.associateBy { it.taskId }

        val toolDefectRspVO = ToolDefectRspVO()
        toolDefectRspVO.taskDetailVoMap = taskInfoVoMap
        toolDefectRspVO.defectList = Page(0, defectPageable.pageNumber + 1, defectPageable.pageSize, 0, dataList)
        return toolDefectRspVO
    }
}