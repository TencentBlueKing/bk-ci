package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.BuildDefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.model.CCNDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.CCNStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.devops.common.api.pojo.Page
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("CCNQueryWarningBizService")
class CCNQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao,
    private val buildDefectDao: BuildDefectDao
) : IDefectQueryWarningService<CCNDefectModel, CCNStatisticModel> {
    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<CCNDefectModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val ccnDefectList = defectDao.findCCNByTaskIdInAndToolName(
                defectQueryParam.taskIdList,
                defectQueryParam.filterFields,
                defectQueryParam.status,
                pageable
        )

        return Page(pageable.pageNumber + 1, pageable.pageSize, ccnDefectList.size.toLong(), ccnDefectList)
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
    ): Page<CCNStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val ccnStatisticList = statisticDao.findCCNByTaskIdInAndToolName(taskIdList, startTime, endTime, filterFields,
                buildId, pageable)
        return Page(pageable.pageNumber + 1, pageable.pageSize, ccnStatisticList.size.toLong(), ccnStatisticList)
    }
}