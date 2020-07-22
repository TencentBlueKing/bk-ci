package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.model.CCNDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.CCNStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.devops.common.api.pojo.Page
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("CCNQueryWarningBizService")
class CCNQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao
) : IDefectQueryWarningService<CCNDefectModel, CCNStatisticModel> {
    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<CCNDefectModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val ccnDefectList =
            defectDao.findCCNByTaskIdInAndToolName(
                defectQueryParam.taskIdList,
                defectQueryParam.filterFields,
                defectQueryParam.status,
                pageable
            )
        return Page(pageable.pageNumber + 1, pageable.pageSize, 0L, ccnDefectList)
    }

    override fun queryLintDefectStatistic(
        taskIdList: List<Long>,
        toolName: String?,
        filterFields: List<String>?,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<CCNStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val ccnStatisticList = statisticDao.findCCNByTaskIdInAndToolName(taskIdList, filterFields, pageable)
        return Page(pageable.pageNumber + 1, pageable.pageSize, 0L, ccnStatisticList)
    }
}