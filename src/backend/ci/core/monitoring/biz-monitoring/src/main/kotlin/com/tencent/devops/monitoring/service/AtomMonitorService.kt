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

package com.tencent.devops.monitoring.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.pojo.AtomMonitorFailDetailData
import com.tencent.devops.monitoring.pojo.AtomMonitorStatisticData
import org.influxdb.dto.QueryResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class AtomMonitorService @Autowired constructor(
    private val influxdbClient: InfluxdbClient
) {

    /**
     * 查询监控统计数据
     * @param atomCode 插件标识
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    fun queryAtomMonitorStatisticData(
        atomCode: String,
        startTime: Long,
        endTime: Long
    ): AtomMonitorStatisticData {
        logger.info("queryAtomMonitorStatisticData  atomCode:$atomCode, startTime:$startTime, endTime:$endTime")
        val baseSqlSb =
            StringBuilder(
                "select count(atomCode)  from AtomMonitorData where atomCode='$atomCode' and" +
                    " time>=${startTime}000000 and time<${endTime}000000"
            )
        val totalSuccessSb = StringBuilder(baseSqlSb.toString()).append(" and errorCode=0")
        val failBaseSqlSb = StringBuilder(baseSqlSb.toString()).append(" and errorCode!=0")
        val totalSystemFailSql = getErrorTypeQuerySql(failBaseSqlSb, ErrorType.SYSTEM.name)
        val totalUserFailSql = getErrorTypeQuerySql(failBaseSqlSb, ErrorType.USER.name)
        val totalThirdFailSql = getErrorTypeQuerySql(failBaseSqlSb, ErrorType.THIRD_PARTY.name)
        val totalComponentFailSql = getErrorTypeQuerySql(failBaseSqlSb, ErrorType.PLUGIN.name)
        val totalSuccessNum = getNum(totalSuccessSb.toString())
        val totalFailNum = getNum(failBaseSqlSb.toString())
        return AtomMonitorStatisticData(
            atomCode = atomCode,
            totalSuccessNum = totalSuccessNum,
            totalFailNum = totalFailNum,
            totalFailDetail = AtomMonitorFailDetailData(
                totalSystemFailNum = getNum(totalSystemFailSql),
                totalUserFailNum = getNum(totalUserFailSql),
                totalThirdFailNum = getNum(totalThirdFailSql),
                totalComponentFailNum = getNum(totalComponentFailSql)
            )
        )
    }

    private fun getNum(sql: String): Int {
        val num: Int
        val queryResult = influxdbClient.select(sql)
        if (null != queryResult && !queryResult.hasError()) {
            num = getNumFromResult(queryResult)
        } else {
            logger.error("BKSystemErrorMonitor|queryAtomExecuteNum|sql=$sql|error=${queryResult?.error}")
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = CommonMessageCode.ERROR_REST_EXCEPTION_COMMON_TIP
            )
        }
        return num
    }

    private fun getNumFromResult(queryResult: QueryResult): Int {
        var num = 0
        queryResult.results.forEach { result ->
            result.run {
                series?.forEach { serie ->
                    num = covertToNum(serie.values[0][1])
                }
            }
        }
        return num
    }

    private fun covertToNum(it: Any?) = if (it is Number) it.toInt() else 0

    private fun getErrorTypeQuerySql(
        failBaseSqlSb: StringBuilder,
        errorType: String
    ): String {
        return StringBuilder(failBaseSqlSb.toString()).append(" and errorType='$errorType'").toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AtomMonitorService::class.java)
    }
}
