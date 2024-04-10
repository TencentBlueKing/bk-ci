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

package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.T_NODE_CREATED_USER
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.pojo.job.ccreq.CCAddHostReq
import com.tencent.devops.environment.pojo.job.ccreq.CCDeleteHostReq
import com.tencent.devops.environment.pojo.job.ccreq.CCFindHostBizRelationsReq
import com.tencent.devops.environment.pojo.job.ccreq.CCHostPropertyFilter
import com.tencent.devops.environment.pojo.job.ccreq.CCListHostWithoutBizReq
import com.tencent.devops.environment.pojo.job.ccreq.CCPage
import com.tencent.devops.environment.pojo.job.ccreq.CCRules
import com.tencent.devops.environment.pojo.job.ccres.CCBkHost
import com.tencent.devops.environment.pojo.job.ccres.QueryCCListHostWithoutBizData
import com.tencent.devops.environment.pojo.job.ccres.CCResp
import com.tencent.devops.environment.pojo.job.ccres.HostBizRelation
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.Record5
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class QueryFromCCService {
    @Value("\${environment.apigw.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${environment.apigw.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${environment.cc.bkSupplierAccount:}")
    private val bkSupplierAccount = ""

    @Value("\${environment.cc.bkccQueryBaseUrl:}")
    private val bkccQueryBaseUrl = ""

    @Value("\${environment.cc.bkccListHostWithoutBizPath:#{\"/list_hosts_without_biz\"}}")
    private val bkccListHostWithoutBizPath = ""

    @Value("\${environment.cc.bkccFindHostBizRelationsPath:#{\"/find_host_biz_relations\"}}")
    private val bkccFindHostBizRelationsPath = ""

    @Value("\${environment.cc.bkccExecuteBaseUrl:}")
    private val bkccExecuteBaseUrl = ""

    @Value("\${environment.cc.bkccAddHostToCiBizPath:#{\"/sync/cmdb/add_host_to_ci_biz\"}}")
    private val bkccAddHostToCiBizPath = ""

    @Value("\${environment.cc.bkccDeleteHostFromCiBizPath:#{\"/delete/cmdb/delete_host_from_ci_biz\"}}")
    private val bkccDeleteHostFromCiBizPath = ""

    companion object {
        private val logger = LoggerFactory.getLogger(QueryFromCCService::class.java)
        private val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        const val LOG_OUTPUT_MAX_LENGTH = 4000
        const val DEFAULT_PAGE_LIMIT = 500
        const val DEFAULT_PAGE_START = 0
        const val FIELD_BK_HOST_ID = "bk_host_id"
        const val FIELD_BK_CLOUD_ID = "bk_cloud_id"
        const val FIELD_BK_HOST_INNERIP = "bk_host_innerip"
        const val FIELD_OPERATOR = "operator"
        const val FIELD_BAK_OPERATOR = "bk_bak_operator"
        const val AND_CONDITATION = "AND"
        const val IN_OPERATION = "in"
    }

    /**
     *  判断：用户or节点导入人 是机器的主备负责人（用户：函数中形参userId；节点导入人：T_NODE表中的createdUser）
     *  core中实现：从CC中 用对应T_NODE表中记录的host_id查询机器的主备负责人
     */
    fun isOperatorOrBakOperator(userId: String, nodeRecords: Set<Record5<Long, String, Long, Long, String>>) {
        val nodeIpList: List<String> = nodeRecords.mapNotNull { it[T_NODE_NODE_IP] as? String } // 所有host对应的ip
        val nodeIpToNodeMap = nodeRecords.associateBy { it[T_NODE_NODE_IP] as? String } // 所有host的：ip - 记录 映射
        val nodeHostIdList: List<Long> = nodeRecords.mapNotNull { it[T_NODE_HOST_ID] as? Long } // 所有host对应的id

        val ccResp = queryCCListHostWithoutBizByInRules(
            listOf(FIELD_BK_HOST_ID, FIELD_BK_CLOUD_ID, FIELD_BK_HOST_INNERIP, FIELD_OPERATOR, FIELD_BAK_OPERATOR),
            nodeHostIdList, FIELD_BK_HOST_ID
        )
        if (null != ccResp.data) {
            val ccData = ccResp.data.info
            val ccIpToNodeMap = ccData.associateBy { it.bkHostInnerip }
            val invalidIpList = nodeIpList.filter {
                val isOperator = userId == ccIpToNodeMap[it]?.operator ||
                    nodeIpToNodeMap[it]?.get(T_NODE_CREATED_USER) as? String == ccIpToNodeMap[it]?.operator
                val isBakOpertor = ccIpToNodeMap[it]?.bkBakOperator?.split(",")?.contains(userId)!! ||
                    ccIpToNodeMap[it]?.bkBakOperator?.split(",")
                        ?.contains(nodeIpToNodeMap[it]?.get(T_NODE_CREATED_USER) as? String)!!
                !isOperator && !isBakOpertor
            }

            if (invalidIpList.isNotEmpty()) {
                logger.warn("[isOperatorOrBakOperator] invalidIpList: ${invalidIpList.joinToString()}")
                throw ErrorCodeException(
                    errorCode = EnvironmentMessageCode.ERROR_NODE_IP_ILLEGAL_USER,
                    params = arrayOf(invalidIpList.joinToString(","), userId)
                )
            }
        }
    }

    fun <T> queryCCListHostWithoutBizByInRules(
        fields: List<String>,
        inValueList: T,
        field: String
    ): CCResp<QueryCCListHostWithoutBizData> {
        val ccListHostWithoutBizReq = CCListHostWithoutBizReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
            bkSupplierAccount = bkSupplierAccount,
            page = CCPage(DEFAULT_PAGE_START, DEFAULT_PAGE_LIMIT),
            fields = fields,
            hostPropertyFilter = CCHostPropertyFilter(
                condition = AND_CONDITATION,
                rules = listOf(
                    CCRules(
                        field = field,
                        operator = IN_OPERATION,
                        value = inValueList
                    )
                )
            )
        )
        val url = bkccQueryBaseUrl + bkccListHostWithoutBizPath
        val resBody = executePostRequest(
            getcommonHeaders(), url, ccListHostWithoutBizReq
        )
        return mapper.readValue(resBody!!)
    }

    fun addHostToCiBiz(svrIdList: List<Long>): CCResp<CCBkHost> {
        val ccAddHostReq = CCAddHostReq(svrIdList)
        val resBody = executePostRequest(
            getAuthHeaders(), bkccExecuteBaseUrl + bkccAddHostToCiBizPath, ccAddHostReq
        )
        return mapper.readValue(resBody!!)
    }

    fun deleteHostFromCiBiz(hostIdList: Set<Long>): CCResp<Nothing> {
        val ccDeleteHostReq = CCDeleteHostReq(hostIdList)
        val resBody = executeDeleteRequest(
            getAuthHeaders(), bkccExecuteBaseUrl + bkccDeleteHostFromCiBizPath, ccDeleteHostReq
        )
        return mapper.readValue(resBody!!)
    }

    fun queryCCFindHostBizRelations(hostIdList: List<Long>): CCResp<List<HostBizRelation>> {
        val ccFindHostBizRelationsReq = CCFindHostBizRelationsReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
            bkHostId = hostIdList
        )
        val resBody = executePostRequest(
            getcommonHeaders(), bkccQueryBaseUrl + bkccFindHostBizRelationsPath, ccFindHostBizRelationsReq
        )
        return mapper.readValue(resBody!!)
    }

    private fun getcommonHeaders(): Map<String, String> {
        return mapOf("accept" to "*/*", "Content-Type" to "application/json")
    }

    private fun getAuthHeaders(): Map<String, String> {
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"bk_username\": \"$AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE\"}"
        return mapOf(
            "accept" to "*/*",
            "Content-Type" to "application/json",
            "X-Bkapi-Authorization" to bkAuthorization
        )
    }

    private fun <T> executePostRequest(headers: Map<String, String>, url: String, req: T): String? {
        val requestContent = mapper.writeValueAsString(req)
        logger.info("POST url: $url, req: ${logWithLengthLimit(requestContent)}")

        val ccPostResBody = OkhttpUtils.doPost(url, requestContent, headers).body?.string()
        logger.info("POST res: ${logWithLengthLimit(ccPostResBody ?: "")}")
        return ccPostResBody
    }

    private fun <T> executeDeleteRequest(headers: Map<String, String>, url: String, req: T): String? {
        val requestContent = mapper.writeValueAsString(req)
        logger.info("DELETE url: $url, req: ${logWithLengthLimit(requestContent)}")
        val requestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), requestContent)
        val deleteReq: Request = Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .delete(requestBody)
            .build()

        val deleteResBody = OkhttpUtils.doHttp(deleteReq).body?.string()
        logger.info("DELETE res: ${logWithLengthLimit(deleteResBody ?: "")}")
        return deleteResBody
    }

    private fun logWithLengthLimit(logOrigin: String): String {
        return if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }
}