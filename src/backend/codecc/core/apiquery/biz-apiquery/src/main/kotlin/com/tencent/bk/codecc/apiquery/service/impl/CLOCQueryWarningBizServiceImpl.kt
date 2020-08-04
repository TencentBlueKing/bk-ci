package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.model.CLOCDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.CLOCStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.devops.common.api.pojo.Page
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("CLOCQueryWarningBizService")
class CLOCQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao
) : IDefectQueryWarningService<CLOCDefectModel, CLOCStatisticModel> {
    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<CLOCDefectModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val clocDefectList =
            defectDao.findCLOCByTaskIdInAndToolName(
                taskIds = defectQueryParam.taskIdList,
                filterFields = defectQueryParam.filterFields,
                pageable = pageable
            )
        return Page(pageable.pageNumber + 1, pageable.pageSize, 0L, clocDefectList)
    }

    override fun queryLintDefectStatistic(
        taskIdList: List<Long>,
        toolName: String?,
        filterFields: List<String>?,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<CLOCStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val clocStatisticList = statisticDao.findCLOCByTaskIdInAndToolName(
            taskIds = taskIdList,
            filterFields = filterFields,
            pageable = pageable
        )
        return Page(pageable.pageNumber + 1, pageable.pageSize, 0L, clocStatisticList)
    }
}