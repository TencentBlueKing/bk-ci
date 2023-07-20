/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.metrics.service.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.metrics.config.MetricsConfig
import com.tencent.devops.metrics.constant.Constants.BK_ATOM_CODE
import com.tencent.devops.metrics.constant.Constants.BK_ATOM_CODE_FIELD_NAME_ENGLISH
import com.tencent.devops.metrics.constant.Constants.BK_ATOM_NAME
import com.tencent.devops.metrics.constant.Constants.BK_AVG_COST_TIME
import com.tencent.devops.metrics.constant.Constants.BK_AVG_COST_TIME_FIELD_NAME_ENGLISH
import com.tencent.devops.metrics.constant.Constants.BK_CLASSIFY_CODE
import com.tencent.devops.metrics.constant.Constants.BK_CLASSIFY_CODE_FIELD_NAME_ENGLISH
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_COUNT_SUM
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_TYPE
import com.tencent.devops.metrics.constant.Constants.BK_FAIL_COMPLIANCE_COUNT
import com.tencent.devops.metrics.constant.Constants.BK_FAIL_EXECUTE_COUNT
import com.tencent.devops.metrics.constant.Constants.BK_STATISTICS_TIME
import com.tencent.devops.metrics.constant.Constants.BK_SUCCESS_EXECUTE_COUNT
import com.tencent.devops.metrics.constant.Constants.BK_SUCCESS_EXECUTE_COUNT_FIELD_NAME_ENGLISH
import com.tencent.devops.metrics.constant.Constants.BK_SUCCESS_EXECUTE_COUNT_SUM
import com.tencent.devops.metrics.constant.Constants.BK_SUCCESS_RATE
import com.tencent.devops.metrics.constant.Constants.BK_SUCCESS_RATE_FIELD_NAME_ENGLISH
import com.tencent.devops.metrics.constant.Constants.BK_TOTAL_COST_TIME_SUM
import com.tencent.devops.metrics.constant.Constants.BK_TOTAL_EXECUTE_COUNT
import com.tencent.devops.metrics.constant.Constants.BK_TOTAL_EXECUTE_COUNT_FIELD_NAME_ENGLISH
import com.tencent.devops.metrics.constant.Constants.BK_TOTAL_EXECUTE_COUNT_SUM
import com.tencent.devops.metrics.constant.MetricsMessageCode
import com.tencent.devops.metrics.dao.AtomDisplayConfigDao
import com.tencent.devops.metrics.dao.AtomStatisticsDao
import com.tencent.devops.metrics.pojo.`do`.AtomBaseInfoDO
import com.tencent.devops.metrics.pojo.`do`.AtomBaseTrendInfoDO
import com.tencent.devops.metrics.pojo.`do`.AtomExecutionStatisticsInfoDO
import com.tencent.devops.metrics.pojo.`do`.AtomTrendInfoDO
import com.tencent.devops.metrics.pojo.`do`.ComplianceInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryAtomStatisticsInfoDTO
import com.tencent.devops.metrics.pojo.qo.QueryAtomStatisticsQO
import com.tencent.devops.metrics.pojo.vo.AtomTrendInfoVO
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.ListPageVO
import com.tencent.devops.metrics.pojo.vo.QueryIntervalVO
import com.tencent.devops.metrics.service.AtomStatisticsManageService
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.getBetweenDate
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.getErrorTypeName
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.toMinutes
import java.math.BigDecimal
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AtomStatisticsServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomStatisticsDao: AtomStatisticsDao,
    private val atomDisplayConfigDao: AtomDisplayConfigDao,
    private val metricsConfig: MetricsConfig
) : AtomStatisticsManageService {
    override fun queryAtomTrendInfo(queryAtomTrendInfoDTO: QueryAtomStatisticsInfoDTO): AtomTrendInfoVO {
        val atomCodes = getDefaultAtomCodes(queryAtomTrendInfoDTO)
        // 查询符合查询条件的记录数
        val queryAtomExecuteStatisticsCount =
            atomStatisticsDao.queryAtomExecuteStatisticsInfoCount(
                dslContext = dslContext,
                queryCondition = QueryAtomStatisticsQO(
                    projectId = queryAtomTrendInfoDTO.projectId,
                    baseQueryReq = BaseQueryReqVO(
                        pipelineIds = queryAtomTrendInfoDTO.pipelineIds,
                        pipelineLabelIds = queryAtomTrendInfoDTO.pipelineLabelIds,
                        startTime = queryAtomTrendInfoDTO.startTime,
                        endTime = queryAtomTrendInfoDTO.endTime
                    ),
                    errorTypes = queryAtomTrendInfoDTO.errorTypes,
                    atomCodes = atomCodes ?: emptyList()
                )
            )
        // 查询记录过多，提醒用户缩小查询范围
        if (queryAtomExecuteStatisticsCount > metricsConfig.queryCountMax) {
            throw ErrorCodeException(
                errorCode = MetricsMessageCode.QUERY_DETAILS_COUNT_BEYOND,
                params = arrayOf("${metricsConfig.queryCountMax}")
            )
        }
        //  查询插件趋势信息
        val result = atomStatisticsDao.queryAtomTrendInfo(
            dslContext,
            QueryAtomStatisticsQO(
                projectId = queryAtomTrendInfoDTO.projectId,
                baseQueryReq = BaseQueryReqVO(
                    pipelineIds = queryAtomTrendInfoDTO.pipelineIds,
                    pipelineLabelIds = queryAtomTrendInfoDTO.pipelineLabelIds,
                    startTime = queryAtomTrendInfoDTO.startTime,
                    endTime = queryAtomTrendInfoDTO.endTime
                ),
                errorTypes = queryAtomTrendInfoDTO.errorTypes,
                atomCodes = atomCodes ?: emptyList()
            )
        )
        val atomBaseTrendInfoMap = mutableMapOf<String, MutableMap<String, AtomBaseTrendInfoDO>>()
        val atomTrendInfoMap = mutableMapOf<String, AtomTrendInfoDO>()
        //  查询的时间区间
        val betweenDate = getBetweenDate(queryAtomTrendInfoDTO.startTime, queryAtomTrendInfoDTO.endTime)
        result?.forEach { record ->
            //  按插件code和统计时间分组数据
            val atomCode = record[BK_ATOM_CODE] as String
            val statisticsTime = (record[BK_STATISTICS_TIME] as LocalDateTime).toLocalDate()

            if (!atomTrendInfoMap.containsKey(atomCode)) {
                val atomBaseTrendInfo = AtomBaseTrendInfoDO(
                        successRate = (record[BK_SUCCESS_RATE] as BigDecimal).toDouble(),
                        statisticsTime = statisticsTime,
                        avgCostTime = toMinutes(record[BK_AVG_COST_TIME] as Long)
                    )
                val atomTrendInfoDO = AtomTrendInfoDO(
                    atomCode = atomCode,
                    atomName = record[BK_ATOM_NAME] as String,
                    atomTrendInfos = null
                )
                atomBaseTrendInfoMap[atomCode] = mutableMapOf("$statisticsTime" to atomBaseTrendInfo)
                atomTrendInfoMap[atomCode] = atomTrendInfoDO
            } else {
                val atomBaseInfo = AtomBaseTrendInfoDO(
                    successRate = (record[BK_SUCCESS_RATE] as BigDecimal).toDouble(),
                    avgCostTime = toMinutes(record[BK_AVG_COST_TIME] as Long),
                    statisticsTime = (record[BK_STATISTICS_TIME] as LocalDateTime).toLocalDate()
                )
                atomBaseTrendInfoMap[atomCode]?.put("$statisticsTime", atomBaseInfo)
            }
        }
        //  对查询区间中没有数据的时间添加占位数据
        atomBaseTrendInfoMap.keys.forEach { atomCode ->
            val atomBaseTrendInfos = atomBaseTrendInfoMap[atomCode]
            val atomTrendInfos = mutableListOf<AtomBaseTrendInfoDO>()
            (betweenDate).forEach { date ->
                if (atomBaseTrendInfos!!.containsKey(date)) {
                    atomTrendInfos.add(atomBaseTrendInfos[date]!!)
                } else {
                    atomTrendInfos.add(
                        AtomBaseTrendInfoDO(statisticsTime = DateTimeUtil.stringToLocalDate(date)!!)
                    )
                }
                atomTrendInfoMap[atomCode]?.atomTrendInfos = atomTrendInfos
            }
        }
        return AtomTrendInfoVO(
            atomTrendInfoMap.values.toList()
        )
    }

    override fun queryAtomComplianceInfo(
        userId: String,
        atomCode: String,
        queryIntervalVO: QueryIntervalVO
    ): ComplianceInfoDO? {
        val queryAtomComplianceInfo = atomStatisticsDao.queryAtomComplianceInfo(
            dslContext = dslContext,
            atomCode = atomCode,
            startDateTime = queryIntervalVO.startDateTime,
            endDateTime = queryIntervalVO.endDateTime
        )
        if (queryAtomComplianceInfo != null) {
            val failExecuteCount = queryAtomComplianceInfo.get(BK_FAIL_EXECUTE_COUNT) as? BigDecimal?
            val failComplianceCount = queryAtomComplianceInfo.get(BK_FAIL_COMPLIANCE_COUNT) as? BigDecimal?
            if (failExecuteCount != null && failComplianceCount != null) {
                return ComplianceInfoDO(
                    failExecuteCount.toInt(),
                    failComplianceCount.toInt()
                )
            }
        }
        return null
    }

    override fun queryAtomExecuteStatisticsInfo(
        queryAtomTrendInfoDTO: QueryAtomStatisticsInfoDTO
    ): ListPageVO<AtomExecutionStatisticsInfoDO> {
        val atomCodes = getDefaultAtomCodes(queryAtomTrendInfoDTO)
        // 查询符合查询条件的记录数
        val queryAtomExecuteStatisticsCount =
            atomStatisticsDao.queryAtomExecuteStatisticsInfoCount(
                dslContext,
                QueryAtomStatisticsQO(
                    projectId = queryAtomTrendInfoDTO.projectId,
                    baseQueryReq = BaseQueryReqVO(
                        pipelineIds = queryAtomTrendInfoDTO.pipelineIds,
                        pipelineLabelIds = queryAtomTrendInfoDTO.pipelineLabelIds,
                        startTime = queryAtomTrendInfoDTO.startTime,
                        endTime = queryAtomTrendInfoDTO.endTime
                    ),
                    errorTypes = queryAtomTrendInfoDTO.errorTypes,
                    atomCodes = atomCodes ?: emptyList()
                )
            )
        // 查询记录过多，提醒用户缩小查询范围
        if (queryAtomExecuteStatisticsCount > metricsConfig.queryCountMax) {
            throw ErrorCodeException(
                errorCode = MetricsMessageCode.QUERY_DETAILS_COUNT_BEYOND,
                params = arrayOf("${metricsConfig.queryCountMax}")
            )
        }
        logger.info("query atom executeStatisticsInfo Count: $queryAtomExecuteStatisticsCount")
        val atomStatisticResult = atomStatisticsDao.queryAtomExecuteStatisticsInfo(
            dslContext,
            QueryAtomStatisticsQO(
                projectId = queryAtomTrendInfoDTO.projectId,
                baseQueryReq = BaseQueryReqVO(
                    pipelineIds = queryAtomTrendInfoDTO.pipelineIds,
                    pipelineLabelIds = queryAtomTrendInfoDTO.pipelineLabelIds,
                    startTime = queryAtomTrendInfoDTO.startTime,
                    endTime = queryAtomTrendInfoDTO.endTime
                ),
                errorTypes = queryAtomTrendInfoDTO.errorTypes,
                atomCodes = atomCodes ?: emptyList(),
                page = queryAtomTrendInfoDTO.page,
                pageSize = queryAtomTrendInfoDTO.pageSize
            )
        )
        val queryAtomFailStatisticsInfo = atomStatisticsDao.queryAtomFailStatisticsInfo(
            dslContext,
            QueryAtomStatisticsQO(
                projectId = queryAtomTrendInfoDTO.projectId,
                baseQueryReq = BaseQueryReqVO(
                    pipelineIds = queryAtomTrendInfoDTO.pipelineIds,
                    pipelineLabelIds = queryAtomTrendInfoDTO.pipelineLabelIds,
                    startTime = queryAtomTrendInfoDTO.startTime,
                    endTime = queryAtomTrendInfoDTO.endTime
                ),
                errorTypes = queryAtomTrendInfoDTO.errorTypes,
                atomCodes = atomCodes ?: emptyList()
            )
        )
        //  获取表头固定字段
        val headerInfo = getHeaderInfo()
        val atomFailInfos = mutableMapOf<String, MutableMap<String, String>>()
        queryAtomFailStatisticsInfo.map {
            val atomCode = it[BK_ATOM_CODE].toString()
            val errorType = it[BK_ERROR_TYPE] as Int

            //  动态扩展表头
            if (!headerInfo.containsKey(getHeaderFieldName(it[BK_ERROR_TYPE].toString()))) {
                headerInfo[getHeaderFieldName("$errorType")] = getErrorTypeName(errorType)
            }
            if (!atomFailInfos.containsKey(atomCode)) {
                atomFailInfos.put(
                    atomCode,
                    mutableMapOf(
                        getHeaderFieldName(it[BK_ERROR_TYPE].toString())
                                to (it[BK_ERROR_COUNT_SUM] as BigDecimal).toString()
                    )
                )
            } else {
                atomFailInfos[atomCode]?.put(
                    getHeaderFieldName(it[BK_ERROR_TYPE].toString()), (it[BK_ERROR_COUNT_SUM] as BigDecimal).toString()
                )
            }
        }
        val atomExecutionStatisticsInfos = mutableListOf<AtomExecutionStatisticsInfoDO>()
        atomStatisticResult?.forEach {
            val totalExecuteCount = (it[BK_TOTAL_EXECUTE_COUNT_SUM] as BigDecimal).toLong()
            val successExecuteCount = (it[BK_SUCCESS_EXECUTE_COUNT_SUM] as BigDecimal).toLong()
            val totalCostTimeSum = (it[BK_TOTAL_COST_TIME_SUM] as BigDecimal).toLong()
            atomExecutionStatisticsInfos.add(
                AtomExecutionStatisticsInfoDO(
                    projectId = queryAtomTrendInfoDTO.projectId,
                    atomBaseInfo = AtomBaseInfoDO(
                        atomCode = it[BK_ATOM_CODE] as String,
                        atomName = it[BK_ATOM_NAME] as String
                    ),
                    classifyCode = it[BK_CLASSIFY_CODE] as String,
                    avgCostTime =
                    String.format("%.2f", (totalCostTimeSum.toDouble() / totalExecuteCount.toDouble())).toDouble(),
                    totalExecuteCount = totalExecuteCount,
                    successExecuteCount = successExecuteCount,
                    successRate = if (successExecuteCount <= 0L || totalExecuteCount <= 0L) 0.0
                    else String.format("%.2f", (successExecuteCount.toDouble() * 100 / totalExecuteCount.toDouble()))
                        .toDouble(),
                    atomFailInfos = atomFailInfos[it[BK_ATOM_CODE]]?.toMap() ?: emptyMap()
                )
            )
        }
        logger.info("query atom executeStatisticsInfo headerInfo: $headerInfo")

        return ListPageVO(
            count = queryAtomExecuteStatisticsCount,
            page = queryAtomTrendInfoDTO.page,
            pageSize = queryAtomTrendInfoDTO.pageSize,
            headerInfo = headerInfo,
            records = atomExecutionStatisticsInfos
        )
    }

    private fun getDefaultAtomCodes(queryAtomStatisticsInfoDTO: QueryAtomStatisticsInfoDTO): List<String>? {
        val pipelineIds = queryAtomStatisticsInfoDTO.pipelineIds
        val pipelineLabelIds = queryAtomStatisticsInfoDTO.pipelineLabelIds
        val errorTypes = queryAtomStatisticsInfoDTO.errorTypes
        // 未选择查询的插件时读取插件显示配置
        return if (!queryAtomStatisticsInfoDTO.atomCodes.isNullOrEmpty()) {
            queryAtomStatisticsInfoDTO.atomCodes
        } else {
            if (pipelineIds.isNullOrEmpty() && pipelineLabelIds.isNullOrEmpty() && errorTypes.isNullOrEmpty()) {
                // 插件配置为空则读取项目下插件
                atomDisplayConfigDao.getOptionalAtomDisplayConfig(
                    dslContext = dslContext,
                    projectId = queryAtomStatisticsInfoDTO.projectId,
                    atomCodes = emptyList(),
                    keyword = null,
                    page = 1,
                    pageSize = metricsConfig.defaultLimitNum
                ).map { it.atomCode }
            } else {
                queryAtomStatisticsInfoDTO.atomCodes
            }
        }
    }

    private fun getHeaderFieldName(type: String) = "errorCount-$type"

    private fun getHeaderInfo(): MutableMap<String, String> {
        val headerInfo = mutableMapOf<String, String>()
        headerInfo[BK_ATOM_CODE] = I18nUtil.getCodeLanMessage(BK_ATOM_CODE_FIELD_NAME_ENGLISH)
        headerInfo[BK_CLASSIFY_CODE] =
            I18nUtil.getCodeLanMessage(BK_CLASSIFY_CODE_FIELD_NAME_ENGLISH)
        headerInfo[BK_SUCCESS_RATE] =
            I18nUtil.getCodeLanMessage(BK_SUCCESS_RATE_FIELD_NAME_ENGLISH)
        headerInfo[BK_AVG_COST_TIME] =
            I18nUtil.getCodeLanMessage(BK_AVG_COST_TIME_FIELD_NAME_ENGLISH)
        headerInfo[BK_TOTAL_EXECUTE_COUNT] = I18nUtil
            .getCodeLanMessage(BK_TOTAL_EXECUTE_COUNT_FIELD_NAME_ENGLISH)
        headerInfo[BK_SUCCESS_EXECUTE_COUNT] = I18nUtil
            .getCodeLanMessage(BK_SUCCESS_EXECUTE_COUNT_FIELD_NAME_ENGLISH)
        return headerInfo
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AtomStatisticsServiceImpl::class.java)
    }
}
