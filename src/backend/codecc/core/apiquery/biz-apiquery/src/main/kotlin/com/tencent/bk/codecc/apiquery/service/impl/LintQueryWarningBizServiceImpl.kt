package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.BuildDefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.model.BuildDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.LintDefectV2Model
import com.tencent.bk.codecc.apiquery.defect.model.LintStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.CommonMessageCode
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("LINTQueryWarningBizService")
class LintQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao,
    private val buildDefectDao: BuildDefectDao
) : IDefectQueryWarningService<LintDefectV2Model, LintStatisticModel> {

    companion object {
        private val logger = LoggerFactory.getLogger(LintQueryWarningBizServiceImpl::class.java)
    }

    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<LintDefectV2Model> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        return with(defectQueryParam) {
            val lintFileModelList = defectDao.findLintByTaskIdInAndToolName(taskIdList, toolName, filterFields, status,
                    checker, notChecker, pageable
            )
            Page(pageable.pageNumber + 1, pageable.pageSize, lintFileModelList.size.toLong(), lintFileModelList)
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
    ): Page<LintStatisticModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val lintStatisticList = statisticDao.findLintByTaskIdInAndToolName(
            taskIdList,
            toolName,
            startTime,
            endTime,
            filterFields,
            buildId,
            pageable
        )
        return Page(pageable.pageNumber + 1, pageable.pageSize, lintStatisticList.size.toLong(), lintStatisticList)
    }
}