package com.tencent.bk.codecc.apiquery.service

import com.tencent.bk.codecc.apiquery.defect.model.CommonModel
import com.tencent.bk.codecc.apiquery.defect.model.StatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.devops.common.api.pojo.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

interface IDefectQueryWarningService<T : CommonModel, E : StatisticModel> {

    /**
     * 根据任务清单查询告警详细信息
     */
    fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        //todo pageSize加校验
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
        filterFields : List<String>?,
        buildId: String?,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<E>

}