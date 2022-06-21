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

import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.LintDefectDao
import com.tencent.bk.codecc.apiquery.service.AbstractOpDefectDataService
import com.tencent.bk.codecc.apiquery.service.MetaDataService
import com.tencent.bk.codecc.apiquery.service.TaskLogService
import com.tencent.bk.codecc.apiquery.service.TaskService
import com.tencent.bk.codecc.apiquery.service.ToolService
import com.tencent.bk.codecc.apiquery.utils.ConvertUtil
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.bk.codecc.apiquery.vo.LintDefectVO
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

@Service("LINTDefectDataBizService")
class LintDefectDataBizServiceImpl @Autowired constructor(
    private val lintDefectDao: LintDefectDao,
    taskService: TaskService,
    toolService: ToolService,
    taskLogService: TaskLogService,
    metaDataService: MetaDataService,
    redisTemplate: RedisTemplate<String, String>
) : AbstractOpDefectDataService(taskService, toolService, taskLogService, metaDataService, redisTemplate) {

    companion object {
        private val logger = LoggerFactory.getLogger(LintDefectDataBizServiceImpl::class.java)
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
        logger.info("TaskToolInfoReqVO content: pageNum[$pageNum] pageSize[$pageSize] $reqVO")
        val toolName = reqVO.toolName
        val timeoutDays = reqVO.timeoutDays

        // 日期范围时间戳
        val startTime = DateTimeUtils.getTimeStampStart(reqVO.startTime)
        val endTime = DateTimeUtils.getTimeStampEnd(reqVO.endTime)

        val taskDefectList = mutableListOf<TaskDefectVO>()

        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val (count, page, pageSize1, totalPages, records) = queryTaskInfoPage(reqVO, pageable)
        if (!records.isNullOrEmpty()) {
            // 代码语言元数据
            val langMetadataList = queryLangMetadataList()
            // 组织架构信息
            val deptInfoMap = queryDeptInfoMap()
            // 筛选告警状态：1新增 3修复 5忽略 9屏蔽
            val newStatus = DefectStatus.NEW.value()
            val exist = mutableListOf(newStatus)
            val fixed = mutableListOf(newStatus or DefectStatus.FIXED.value())
            val newAdd = mutableListOf(newStatus, newStatus or DefectStatus.FIXED.value())
            val ignore = mutableListOf(newStatus or DefectStatus.IGNORE.value())
            // 超时告警：截止时间 - 超时天数时间戳
            val timeoutEndTime = endTime - DateTimeUtils.DAY_TIMESTAMP * timeoutDays

            // 分割等长列表后遍历
            val pageTaskLists = PageUtils.averageAssignFixLength(records, 500)
            pageTaskLists.forEach { taskList ->
                val taskIdList = taskList.map { it.taskId }

                // 最近分析状态
                val lastTaskLogMap = taskLogService.batchFindLastTaskLogByTool(taskIdList, toolName)
                // 批量查询各状态告警列表
                val newAddDefect = lintDefectDao.batchQueryDefect(taskIdList, toolName, newAdd, "create_time",
                        startTime, endTime)
                val fixedDefect = lintDefectDao.batchQueryDefect(taskIdList, toolName, fixed, "fixed_time", startTime,
                        endTime)
                val existDefect =
                        lintDefectDao.batchQueryDefect(taskIdList, toolName, exist, "create_time", 0,
                                endTime)
                val ignoreDefect = lintDefectDao.batchQueryDefect(taskIdList, toolName, ignore, "ignore_time",
                        startTime, endTime)
                val existTimeoutDefect =
                        lintDefectDao.batchQueryDefect(taskIdList, toolName, exist, "create_time", 0,
                                timeoutEndTime)

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
                    newAddDefectMap[taskId]?.forEach { taskDefectVO.newAddCount.setProp(it.severity, it.count) }
                    fixedDefectMap[taskId]?.forEach { taskDefectVO.fixedCount.setProp(it.severity, it.count) }
                    existDefectMap[taskId]?.forEach { taskDefectVO.existCount.setProp(it.severity, it.count) }
                    // 统计忽略告警数
                    val ignoreCount = CommonChartAuthorVO()
                    ignoreDefectMap[taskId]?.forEach { ignoreCount.setProp(it.severity, it.count) }
                    taskDefectVO.ignoreCount = ignoreCount.total
                    // 统计超时告警数
                    val existTimeoutCount = CommonChartAuthorVO()
                    timeoutDefectMap[taskId]?.forEach { existTimeoutCount.setProp(it.severity, it.count) }
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
        return mutableSetOf("_id", "id", "task_id", "checker", "status", "rel_path", "message", "defect_type",
                "author", "severity", "create_time", "line_num", "fixed_time")
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
            logger.info("taskRecords is empty: {}", taskPageable)
            return ToolDefectRspVO()
        }

        // 按告警状态查询不同告警
        var timeField = "create_time"
        val new = DefectStatus.NEW.value()
        val status: MutableList<Int>
        when (defectStatus) {
            // 新增告警(不包括已忽略 已屏蔽)
            new -> {
                status = mutableListOf(new, new or DefectStatus.FIXED.value())
            }
            // 已修复
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
                status = mutableListOf(new or DefectStatus.PATH_MASK.value())
            }
            // 遗留待修复
            else -> {
                startTime = 0
                status = mutableListOf(new)
            }
        }

        val taskIdList = taskRecords.map { it.taskId }
        val batchQueryDefectPage =
                lintDefectDao.batchQueryDefectPage(taskIdList, toolName, status, timeField, startTime, endTime,
                        getFieldSet(), defectPageable)

        val resultTaskIdList = mutableSetOf<Long>()
        val dataList = batchQueryDefectPage.map {
            resultTaskIdList.add(it.taskId)
            val lintDefectVO = LintDefectVO()
            BeanUtils.copyProperties(it, lintDefectVO)
            lintDefectVO.entityId = it.defectId
            lintDefectVO
        }

        val taskInfoVoMap = taskRecords.filter { resultTaskIdList.contains(it.taskId) }.map {
            val taskInfoExtVO = TaskInfoExtVO()
            BeanUtils.copyProperties(it, taskInfoExtVO)
            taskInfoExtVO
        }.associateBy { it.taskId }

        val toolDefectRspVO = ToolDefectRspVO()
        toolDefectRspVO.taskDetailVoMap = taskInfoVoMap
        toolDefectRspVO.lintDefectList = Page(0, defectPageable.pageNumber + 1, defectPageable.pageSize, 0, dataList)
        return toolDefectRspVO
    }
}