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

package com.tencent.devops.common.environment.agent.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode.FAILED_TO_GET_CMDB_LIST
import com.tencent.devops.common.api.constant.CommonMessageCode.FAILED_TO_GET_CMDB_NODE
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.environment.agent.pojo.agent.CmdbServerPage
import com.tencent.devops.common.environment.agent.pojo.agent.RawCmdbNode
import com.tencent.devops.common.web.utils.I18nUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException
import javax.ws.rs.core.Response

@Component
class EsbAgentClient {

    @Value("\${esb.appCode}")
    private val appCode = "bkci"

    @Value("\${esb.appSecret}")
    private val appSecret = ""

    companion object {
        private val logger = LoggerFactory.getLogger(EsbAgentClient::class.java)
        private val JSON = "application/json;charset=utf-8".toMediaTypeOrNull()
    }

    fun queryCmdbServerByIps(userId: String, ips: Set<String>, start: Int, limit: Int): CmdbServerPage {
        val requestData = buildQueryByIpReqData(userId, ips, start, limit)
        return queryCmdbNode(requestData)
    }

    fun queryCmdbServerByServerIds(userId: String, serverIds: Set<Long>, start: Int, limit: Int): CmdbServerPage {
        val requestData = buildQueryByServerIdReqData(userId, serverIds, start, limit)
        return queryCmdbNode(requestData)
    }

    private fun buildQueryByIpReqData(userId: String, ips: Set<String>, start: Int, limit: Int): Map<String, Any> {
        val basicRequestData = buildBasicRequestData(userId, start, limit)
        return basicRequestData.plus("key_values" to mapOf("SvrIp" to ips.joinToString(";")))
    }

    private fun buildQueryByServerIdReqData(
        userId: String,
        serverIds: Set<Long>,
        start: Int,
        limit: Int
    ): Map<String, Any> {
        val basicRequestData = buildBasicRequestData(userId, start, limit)
        return basicRequestData.plus("key_values" to mapOf("serverId" to serverIds.joinToString(";")))
    }

    private fun buildBasicRequestData(userId: String, start: Int, limit: Int): Map<String, Any> {
        return mapOf(
            "app_code" to appCode,
            "app_secret" to appSecret,
            "req_column" to listOf(
                "SvrBakOperator", "SvrOperator", "SvrIp", "SvrName", "SfwName", "serverLanIP", "DeptId"
            ),
            "operator" to userId,
            "paging_info" to mapOf("start_index" to start, "page_size" to limit, "return_total_rows" to 1)
        )
    }

    fun getUserCmdbNodeNew(
        userId: String,
        bakOperator: Boolean,
        ips: List<String>,
        offset: Int,
        limit: Int
    ): CmdbServerPage {
        return getUserCmdbNodeByOperator(userId, bakOperator, ips, offset, limit)
    }

    private fun queryCmdbNode(requestData: Map<String, Any>): CmdbServerPage {
        val url = "http://open.oa.com/component/compapi/cmdb/get_query_info/"

        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("[queryCmdbNode]POST url: $url")
        logger.info("[queryCmdbNode]requestBody: $requestBody")

        val request = Request.Builder().url(url).post(requestBody.toRequestBody(JSON)).build()
        OkhttpUtils.doHttp(request).use { response ->
            try {
                val responseBody = response.body?.string()
                logger.info("[queryCmdbNode]responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                if (responseData["result"] == false) {
                    val msg = responseData["msg"]
                    logger.error("get cmdb nodes failed: $msg")
                    throw CustomException(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        I18nUtil.getCodeLanMessage(messageCode = FAILED_TO_GET_CMDB_NODE)
                    )
                }

                val data = responseData["data"] as Map<String, *>

                val header = data["header"] as Map<String, *>
                val returnRows = header["returnRows"] as Int
                val totalRows = header["totalRows"] as Int

                val dataList = data["data"] as List<Map<String, *>>
                val rawNodes = dataList.filterNot { it["serverLanIP"] == null }.mapNotNull {
                    if (it["serverLanIP"] == null || (it["serverLanIP"] as List<String>).isEmpty()) {
                        null
                    } else {
                        val lanIPs = it["serverLanIP"] as List<String>
                        val bakOperator = checkAndGetOperator(it["SvrBakOperator"] as String)
                        val osName = if (it["SfwName"] == null) {
                            ""
                        } else {
                            it["SfwName"] as String
                        }

                        RawCmdbNode(
                            name = it["SvrName"] as String,
                            operator = it["SvrOperator"] as String,
                            bakOperator = bakOperator,
                            ip = it["SvrIp"] as String,
                            displayIp = lanIPs.joinToString(";"),
                            osName = osName,
                            agentStatus = false,
                            serverId = (it["serverId"] as Int).toLong(),
                            deptId = it["DeptId"] as Int
                        )
                    }
                }
                return CmdbServerPage(
                    nodes = rawNodes,
                    returnRows = returnRows,
                    totalRows = totalRows
                )
            } catch (timeoutError: SocketTimeoutException) {
                logger.error("Query CMDB interface time out. Error:", timeoutError)
                throw OperationException(
                    I18nUtil.getCodeLanMessage(messageCode = FAILED_TO_GET_CMDB_LIST)
                )
            } catch (e: Exception) {
                logger.error("get cmdb nodes error", e)
                throw OperationException(
                    I18nUtil.getCodeLanMessage(messageCode = FAILED_TO_GET_CMDB_LIST)
                )
            }
        }
    }

    private fun checkAndGetOperator(bakOperator: String): String {
        val allOperators = bakOperator.split(",", ";").filterNot { it.isBlank() }.map { it.trim() }.toList()
        return when {
            allOperators.size == 1 -> allOperators[0]
            bakOperator.length > 255 -> allOperators.subList(0, 9).joinToString(";")
            else -> allOperators.joinToString(";")
        }
    }

    private fun getUserCmdbNodeByOperator(
        userId: String,
        isBakOperator: Boolean,
        ips: List<String>,
        start: Int,
        limit: Int
    ): CmdbServerPage {
        val operatorCondition = if (isBakOperator) {
            mutableMapOf("serverBakOperator" to userId)
        } else {
            mutableMapOf("SvrOperator" to userId)
        }

        if (ips.isNotEmpty()) {
            operatorCondition["SvrIp"] = ips.joinToString(";")
        }

        return queryCmdbNode(
            mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "operator" to userId,
                "req_column" to listOf(
                    "SvrBakOperator", "SvrOperator", "SvrIp", "SvrName", "SfwName", "serverLanIP", "DeptId"
                ),
                "key_values" to operatorCondition,
                "paging_info" to mapOf("page_size" to limit, "start_index" to start, "return_total_rows" to 1)
            )
        )
    }
}
