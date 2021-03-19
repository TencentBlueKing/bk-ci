package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.model.DUPCDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.DUPCStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.devops.common.api.pojo.Page
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("DUPCQueryWarningBizService")
class DUPCQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao
) : IDefectQueryWarningService<DUPCDefectModel, DUPCStatisticModel> {
    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<DUPCDefectModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val dupcDefectList =
            defectDao.findDUPCByTaskIdInAndToolName(
                defectQueryParam.taskIdList,
                defectQueryParam.filterFields,
                defectQueryParam.status,
                pageable
            )
        return Page(pageable.pageNumber + 1, pageable.pageSize, dupcDefectList.size.toLong(), dupcDefectList)
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
    ): Page<DUPCStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val dupcStatisticList = statisticDao.findDUPCByTaskIdInAndToolName(taskIdList, startTime, endTime,
                filterFields, pageable)
        return Page(pageable.pageNumber + 1, pageable.pageSize, dupcStatisticList.size.toLong(), dupcStatisticList)
    }
}