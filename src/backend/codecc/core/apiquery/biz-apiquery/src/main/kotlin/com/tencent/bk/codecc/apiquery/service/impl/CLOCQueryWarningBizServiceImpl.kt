package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.TaskLogDao
import com.tencent.bk.codecc.apiquery.defect.model.CLOCDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.CLOCStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.bk.codecc.apiquery.vo.DefectQueryReqVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
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
                toolName = (defectQueryParam.toolName?:ComConstants.Tool.CLOC.name),
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
            toolName = (toolName?:ComConstants.Tool.CLOC.name),
            startTime = startTime,
            endTime = endTime,
            filterFields = filterFields,
            buildId = buildId,
            pageable = pageable
        )
        return Page(pageable.pageNumber + 1, pageable.pageSize, clocStatisticList.size.toLong(), clocStatisticList)
    }

    /**
     * 查询工具告警清单(从defect迁移过来的通用接口)
     */
    override fun queryToolDefectList(taskId: Long, queryWarningReq: DefectQueryReqVO, pageNum: Int?, pageSize: Int?,
                                     sortField: String?, sortType: String?): ToolDefectRspVO {
        throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("toolName"))
    }

    override fun statCheckerDefect(reqVO: TaskToolInfoReqVO, pageNum: Int?, pageSize: Int?): Page<CheckerDefectStatVO> {
        throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("toolName"))
    }

}
