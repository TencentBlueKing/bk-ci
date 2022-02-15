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
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.environment.agent.pojo.agent.CmdbServerPage
import com.tencent.devops.common.environment.agent.pojo.agent.RawCmdbNode
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.ws.rs.core.Response

@Component
class EsbAgentClient {

    @Value("\${esb.appCode}")
    private val appCode = "bkci"

    @Value("\${esb.appSecret}")
    private val appSecret = ""

    companion object {
        private val logger = LoggerFactory.getLogger(EsbAgentClient::class.java)
        private val JSON = MediaType.parse("application/json;charset=utf-8")
        private const val DEFAULT_SYTEM_USER = "devops"
    }

    fun getAgentStatus(
        userId: String,
        ips: Collection<String>
    ): Map<String, Boolean> {
        if (ips.isEmpty()) return mapOf()

        val url = "http://open.oa.com/component/compapi/gse/get_agent_status/"

        val requestData = mapOf("app_code" to appCode,
            "app_secret" to appSecret,
            "operator" to userId,
            "company_id" to 0,
            "ip_infos" to ips.map { mapOf("ip" to it, "plat_id" to 0) }
        )

        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url).post(RequestBody.create(JSON, requestBody)).build()
        OkhttpUtils.doHttp(request).use { response ->
            try {
                val responseBody = response.body()?.string()
                logger.info("responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                if (responseData["result"] == false) {
                    val msg = responseData["msg"]
                    logger.error("get user cmdb nodes failed: $msg")
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "查询 Gse Agent 状态失败")
                }

                val ipInfoMap = (responseData["data"] as Map<String, *>)["data"] as Map<String, *>
                val resultMap = mutableMapOf<String, Boolean>()
                ips.forEach {
                    val ipInfo = ipInfoMap["0:$it"]
                    if (ipInfo != null) {
                        resultMap[it] = (ipInfo as Map<String, *>)["exist"] == 1
                    } else {
                        resultMap[it] = false
                    }
                }
                for (ip in ips) {
                    val ipInfo = ipInfoMap["0:$ip"]
                    if (ipInfo != null) {
                        resultMap[ip] = (ipInfo as Map<String, *>)["exist"] == 1
                    } else {
                        resultMap[ip] = false
                    }
                }
                return resultMap
            } catch (e: Exception) {
                logger.error("get agent status failed", e)
                throw OperationException("获取agent状态失败")
            }
        }
    }

    fun getCmdbNodeByIps(userId: String, ips: List<String>): CmdbServerPage {
        return queryCmdbNode(
            mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "operator" to userId,
                "req_column" to listOf("SvrBakOperator", "SvrOperator", "SvrIp", "SvrName", "SfwName", "serverLanIP"),
                "key_values" to mapOf("SvrIp" to ips.joinToString(";")),
                "paging_info" to mapOf("page_size" to 1000, "start_index" to 0, "return_total_rows" to 1)
            )
        )
    }

    fun queryCmdbNode(requestData: Map<String, Any>): CmdbServerPage {
        val url = "http://open.oa.com/component/compapi/cmdb/get_query_info/"

        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

        val request = Request.Builder().url(url).post(RequestBody.create(JSON, requestBody)).build()
        OkhttpUtils.doHttp(request).use { response ->
            try {
                val responseBody = response.body()?.string()
                logger.info("responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                if (responseData["result"] == false) {
                    val msg = responseData["msg"]
                    logger.error("get cmdb nodes failed: $msg")
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "获取 CMDB 节点失败")
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
                            ip = lanIPs[0],
                            displayIp = lanIPs.joinToString(";"),
                            osName = osName,
                            agentStatus = false
                        )
                    }
                }
                return CmdbServerPage(
                    nodes = rawNodes,
                    returnRows = returnRows,
                    totalRows = totalRows
                )
            } catch (e: Exception) {
                logger.error("get cmdb nodes error", e)
                throw OperationException("获取CMDB列表失败")
            }
        }
    }

    private fun getAndSetDisplayIp(displayIp: String): Pair<String, List<String>> {
        val allIpList = displayIp.split(",", ";").filterNot { it.isNullOrBlank() }.map { it.trim() }.toList()
        return Pair(allIpList.joinToString(";"), allIpList)
    }

    private fun checkAndGetOperator(bakOperator: String): String {
        val allOperators = bakOperator.split(",", ";").filterNot { it.isNullOrBlank() }.map { it.trim() }.toList()
        return when {
            allOperators.size == 1 -> allOperators[0]
            bakOperator.length > 255 -> allOperators.subList(0, 9).joinToString(";")
            else -> allOperators.joinToString(";")
        }
    }

    fun getUserCmdbNode(userId: String, start: Int, limit: Int): List<RawCmdbNode> {
        val nodeList = mutableListOf<RawCmdbNode>()
        nodeList.addAll(getUserCmdbNodeByOperator(userId, false, listOf(), start, limit).nodes)
        nodeList.addAll(getUserCmdbNodeByOperator(userId, true, listOf(), start, limit).nodes)
        val noDuplicateNodeList = nodeList.associateBy { it.displayIp }.values.toList()

        // 根据 gseAgent 状态重新设置IP
        val displayIp2IpsMap =
            noDuplicateNodeList.map { it.displayIp }.associate { Pair(it, it.split(";")) }
        val allInnerIp = mutableSetOf<String>()

        displayIp2IpsMap.forEach {
            allInnerIp.addAll(it.value)
        }

        val ipStatusMap = getAgentStatus(DEFAULT_SYTEM_USER, allInnerIp)
        noDuplicateNodeList.forEach { node ->
            val ips = displayIp2IpsMap[node.displayIp]
            ips!!.forEach lit@{ ip ->
                if (ipStatusMap[ip] == true) {
                    node.ip = ip
                    node.agentStatus = true
                    return@lit
                }
            }
        }
        return noDuplicateNodeList
    }

    fun getUserCmdbNodeNew(
        userId: String,
        bakOperator: Boolean,
        ips: List<String>,
        offset: Int,
        limit: Int
    ): CmdbServerPage {
        val cmdbServerPage = getUserCmdbNodeByOperator(userId, bakOperator, ips, offset, limit)
        val cmdbNodes = cmdbServerPage.nodes

        // 根据 gseAgent 状态重新设置IP
        val displayIp2IpsMap =
            cmdbNodes.map { it.displayIp }.associate { Pair(it, it.split(";")) }
        val allInnerIp = mutableSetOf<String>()

        displayIp2IpsMap.forEach {
            allInnerIp.addAll(it.value)
        }

        val ipStatusMap = getAgentStatus(DEFAULT_SYTEM_USER, allInnerIp)
        cmdbNodes.forEach { node ->
            val ips = displayIp2IpsMap[node.displayIp]
            ips!!.forEach lit@{ ip ->
                if (ipStatusMap[ip] == true) {
                    node.ip = ip
                    node.agentStatus = true
                    return@lit
                }
            }
        }

        return cmdbServerPage
    }

    fun getUserCmdbNodeByOperator(
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
                "req_column" to listOf("SvrBakOperator", "SvrOperator", "SvrIp", "SvrName", "SfwName", "serverLanIP"),
                "key_values" to operatorCondition,
                "paging_info" to mapOf("page_size" to limit, "start_index" to start, "return_total_rows" to 1)
            )
        )
    }
}
