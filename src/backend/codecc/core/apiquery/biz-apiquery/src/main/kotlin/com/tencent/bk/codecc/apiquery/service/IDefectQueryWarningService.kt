package com.tencent.bk.codecc.apiquery.service

import com.tencent.bk.codecc.apiquery.defect.model.CommonModel
import com.tencent.bk.codecc.apiquery.defect.model.StatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.vo.DefectQueryReqVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO
import com.tencent.devops.common.api.pojo.Page

interface IDefectQueryWarningService<T : CommonModel, E : StatisticModel> {

    /**
     * 根据任务清单查询告警详细信息
     */
    fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        // todo pageSize加校验
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<T>

    /**
     * 根据任务清单查询告警统计信息
     */
    fun queryLintDefectStatistic(
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
    ): Page<E>

    /**
     * 查询工具告警清单(从defect迁移过来的通用接口)
     */
    fun queryToolDefectList(
        taskId: Long,
        queryWarningReq: DefectQueryReqVO,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): ToolDefectRspVO


    /**
     * 按规则分页统计告警数
     */
    fun statCheckerDefect(
            reqVO: TaskToolInfoReqVO,
            pageNum: Int?,
            pageSize: Int?
    ): Page<CheckerDefectStatVO>

}