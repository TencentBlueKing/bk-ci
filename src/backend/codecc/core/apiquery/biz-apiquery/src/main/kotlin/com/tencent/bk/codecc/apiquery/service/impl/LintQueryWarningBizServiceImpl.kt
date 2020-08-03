package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.model.LintDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.LintStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.devops.common.api.pojo.Page
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("LINTQueryWarningBizService")
class LintQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao
) : IDefectQueryWarningService<LintDefectModel, LintStatisticModel> {

    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<LintDefectModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        return with(defectQueryParam) {
            val lintFileModelList = defectDao.findLintByTaskIdInAndToolName(
                taskIdList,
                toolName,
                filterFields,
                status,
                checker,
                notChecker,
                pageable
            )
            Page(pageable.pageNumber + 1, pageable.pageSize, 0L, lintFileModelList)
        }
    }

    override fun queryLintDefectStatistic(
        taskIdList: List<Long>,
        toolName: String?,
        filterFields: List<String>?,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<LintStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val lintStatisticList = statisticDao.findLintByTaskIdInAndToolName(
            taskIdList,
            toolName,
            filterFields,
            pageable
        )
        return Page(pageable.pageNumber + 1, pageable.pageSize, 0L, lintStatisticList)
    }
}