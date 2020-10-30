package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.TaskLogDao
import com.tencent.bk.codecc.apiquery.defect.model.CLOCDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.CLOCStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.service.TaskLogService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.ComConstants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("CLOCQueryWarningBizService")
class CLOCQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao,
    private val taskLogDao: TaskLogDao
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
        return Page(pageable.pageNumber + 1, pageable.pageSize, clocDefectList.size.toLong(), clocDefectList)
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
    ): Page<CLOCStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val clocStatisticList = statisticDao.findCLOCByTaskIdInAndToolName(
            taskIds = taskIdList,
            startTime = startTime,
            endTime = endTime,
            filterFields = filterFields,
            buildId = buildId,
            pageable = pageable
        )
        return Page(pageable.pageNumber + 1, pageable.pageSize, clocStatisticList.size.toLong(), clocStatisticList)
    }
}