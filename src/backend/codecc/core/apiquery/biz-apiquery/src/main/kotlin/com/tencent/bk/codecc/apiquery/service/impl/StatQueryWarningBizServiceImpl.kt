package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao
import com.tencent.bk.codecc.apiquery.defect.model.StatDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.StatStatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.bk.codecc.apiquery.vo.DefectQueryReqVO
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.util.JsonUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Collections

@Service("STATQueryWarningBizService")
class StatQueryWarningBizServiceImpl @Autowired constructor(
    private val defectDao: DefectDao,
    private val statisticDao: StatisticDao
) : IDefectQueryWarningService<StatDefectModel, StatStatisticModel> {
    override fun queryLintDefectDetail(
        defectQueryParam: DefectQueryParam,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<StatDefectModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        var startTime = defectQueryParam.startTime
        var endTime = defectQueryParam.endTime
        if (startTime == null) {
            val begin = LocalDateTime.of(LocalDate.now(), LocalTime.of(
                    0,
                    0,
                    0,
                    0
            ))
            startTime = begin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        if (endTime == null) {
            val end = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(
                    0,
                    0,
                    0,
                    0
            ))
            endTime = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val docList = defectDao.findStatByTaskIdAndToolNameAndTime(
                defectQueryParam.taskIdList,
                defectQueryParam.toolName!!,
                startTime,
                endTime,
                pageable
        )
                ?: Collections.emptyList()
        logger.info("stat find ${docList.size} defects at $pageNum of $pageSize")
        val defectList = mutableListOf<StatDefectModel>()
        docList.forEach { document ->
            defectList.add(StatDefectModel(JsonUtil.toMap(document.toJson())))
        }

        return Page(pageable.pageNumber + 1, pageable.pageSize, 0L, defectList)
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
    ): Page<StatStatisticModel> {
        return Page(0, 0, 0L, emptyList())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StatQueryWarningBizServiceImpl::class.java)
    }

    /**
     * 查询工具告警清单(从defect迁移过来的通用接口)
     */
    override fun queryToolDefectList(
        taskId: Long,
        queryWarningReq: DefectQueryReqVO,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): ToolDefectRspVO {
        return ToolDefectRspVO()
    }

    override fun statCheckerDefect(reqVO: TaskToolInfoReqVO, pageNum: Int?, pageSize: Int?): Page<CheckerDefectStatVO> {
        throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("toolName"))
    }

}
