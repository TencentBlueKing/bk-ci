package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.BuildDefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.TaskLogDao
import com.tencent.bk.codecc.apiquery.defect.model.CommonStatisticModel
import com.tencent.bk.codecc.apiquery.defect.model.DefectModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.devops.common.api.pojo.Page
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("CommonQueryWarningBizService")
class CommonQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao,
    private val taskLogDao: TaskLogDao,
    private val buildDefectDao: BuildDefectDao
) : IDefectQueryWarningService<DefectModel, CommonStatisticModel> {

    companion object {
        private val logger = LoggerFactory.getLogger(CommonQueryWarningBizServiceImpl::class.java)
    }

    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<DefectModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        return with(defectQueryParam) {
            val commonDefectList = defectDao.findCommonByTaskIdInAndToolName(taskIdList, toolName, filterFields, status, checker, pageable)

            Page(pageable.pageNumber + 1, pageable.pageSize, commonDefectList.size.toLong(), commonDefectList)
        }
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
    ): Page<CommonStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val commonStatisticList = statisticDao.findCommonByTaskIdInAndToolName(taskIdList, toolName, startTime,
                endTime, filterFields, pageable)
        return Page(pageable.pageNumber + 1, pageable.pageSize, commonStatisticList.size.toLong(), commonStatisticList)
    }
}