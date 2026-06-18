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

package com.tencent.devops.environment.service.cc

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.environment.constant.Constants
import com.tencent.devops.environment.exception.CCApiException
import com.tencent.devops.environment.pojo.job.ccreq.CCAddHostReq
import com.tencent.devops.environment.pojo.job.ccreq.CCDeleteHostReq
import com.tencent.devops.environment.pojo.job.ccreq.CCFindHostBizRelationsReq
import com.tencent.devops.environment.pojo.job.ccreq.CCHostPropertyFilter
import com.tencent.devops.environment.pojo.job.ccreq.CCListHostWithoutBizReq
import com.tencent.devops.environment.pojo.job.ccreq.CCPage
import com.tencent.devops.environment.pojo.job.ccreq.CCRules
import com.tencent.devops.environment.pojo.job.ccres.CCBkHost
import com.tencent.devops.environment.pojo.job.ccres.CCHost
import com.tencent.devops.environment.pojo.job.ccres.CCPageData
import com.tencent.devops.environment.pojo.job.ccres.CCResp
import com.tencent.devops.environment.pojo.job.ccres.HostBizRelation
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TencentCCService {
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
        private val logger = LoggerFactory.getLogger(TencentCCService::class.java)
        private val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        const val LOG_OUTPUT_MAX_LENGTH = 4000
        const val DEFAULT_PAGE_LIMIT = 500
        const val DEFAULT_PAGE_START = 0
        const val AND_CONDITATION = "AND"
        const val IN_OPERATION = "in"
    }

    fun listHostByServerId(
        serverIdSet: Set<Long>
    ): List<CCHost> {
        val hostList = mutableListOf<CCHost>()
        var pageData: CCPageData<CCHost>
        var start = 0
        do {
            pageData = listHostsWithoutBiz(
                listOf(
                    Constants.FIELD_BK_HOST_ID,
                    Constants.FIELD_BK_CLOUD_ID,
                    Constants.FIELD_BK_HOST_INNERIP,
                    Constants.FIELD_BK_SVR_ID,
                    Constants.FIELD_BK_OS_TYPE
                ),
                serverIdSet,
                Constants.FIELD_BK_SVR_ID,
                start,
                DEFAULT_PAGE_LIMIT
            ).data!!
            hostList.addAll(pageData.info)
            start += DEFAULT_PAGE_LIMIT
        } while (pageData.info.isNotEmpty())
        return hostList
    }

    fun <T> listHostsWithoutBiz(
        fields: List<String>,
        inValueList: T,
        field: String,
        start: Int = DEFAULT_PAGE_START,
        limit: Int = DEFAULT_PAGE_LIMIT
    ): CCResp<CCPageData<CCHost>> {
        val ccListHostWithoutBizReq = CCListHostWithoutBizReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
            bkSupplierAccount = bkSupplierAccount,
            page = CCPage(start, limit),
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
            getAuthHeaders(), url, ccListHostWithoutBizReq
        )
        return parseCCResp(resBody, url)
    }

    fun addHostToCiBiz(svrIdList: List<Long>): CCResp<CCBkHost> {
        val ccAddHostReq = CCAddHostReq(svrIdList)
        val url = bkccExecuteBaseUrl + bkccAddHostToCiBizPath
        val resBody = executePostRequest(getAuthHeaders(), url, ccAddHostReq)
        return parseCCResp(resBody, url)
    }

    fun deleteHostFromCiBiz(hostIdList: Set<Long>): CCResp<Nothing> {
        val ccDeleteHostReq = CCDeleteHostReq(hostIdList)
        val url = bkccExecuteBaseUrl + bkccDeleteHostFromCiBizPath
        val resBody = executeDeleteRequest(getAuthHeaders(), url, ccDeleteHostReq)
        return parseCCResp(resBody, url)
    }

    fun queryCCFindHostBizRelations(hostIdList: List<Int>): CCResp<List<HostBizRelation>> {
        val ccFindHostBizRelationsReq = CCFindHostBizRelationsReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
            bkHostId = hostIdList
        )
        val url = bkccQueryBaseUrl + bkccFindHostBizRelationsPath
        val resBody = executePostRequest(getAuthHeaders(), url, ccFindHostBizRelationsReq)
        return parseCCResp(resBody, url)
    }

    /**
     * 反序列化 CC 响应并校验业务级 `result`，失败时统一抛 [CCApiException]。
     *
     * 蓝鲸 Open API 业务失败时 HTTP 仍可能为 200，但响应体中 `result=false`、`data=null`，
     * 失败原因由 `code`/`code_name`/`message` 体现（例：限频返回
     * `code=1642902`、`code_name=RATE_LIMIT_RESTRICTION`）。不主动校验会让上层把失败
     * 误当成"成功但数据为空"，引发删错节点之类的下游事故。
     *
     * 类型参数取外层 `R : CCResp<*>` 而非内层 `T`：reified 不允许被替换为 `Nothing`，
     * 而 [deleteHostFromCiBiz] 的 `CCResp<Nothing>` 会反推出 `T = Nothing` 触发该限制。
     */
    private inline fun <reified R : CCResp<*>> parseCCResp(resBody: String?, url: String): R {
        if (resBody.isNullOrBlank()) {
            logger.warn("[bkccAPI]CC API failed|url={}|reason=blank response body", url)
            throw CCApiException(url = url, message = "Response body is blank")
        }
        val resp: R = try {
            mapper.readValue(resBody)
        } catch (e: Exception) {
            logger.warn(
                "[bkccAPI]CC API failed|url={}|reason=deserialize error|body={}",
                url, logWithLengthLimit(resBody)
            )
            throw CCApiException(
                url = url,
                message = "Deserialize CC response failed: ${e.message}",
                cause = e
            )
        }
        if (resp.result != true) {
            logger.warn(
                "[bkccAPI]CC API failed|url={}|code={}|codeName={}|requestId={}|message={}",
                url, resp.code, resp.codeName, resp.requestId, resp.message
            )
            throw CCApiException(
                url = url,
                code = resp.code,
                codeName = resp.codeName,
                requestId = resp.requestId,
                message = resp.message
            )
        }
        return resp
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

    private fun <T : Any> executePostRequest(headers: Map<String, String>, url: String, req: T): String? {
        val requestContent = mapper.writeValueAsString(req)
        logger.info("[bkccAPI]Post url: $url, req: ${logWithLengthLimit(JsonUtil.skipLogFields(req) ?: "")}")

        val ccPostResBody = OkhttpUtils.doPost(url, requestContent, headers).body?.string()
        logger.info("[bkccAPI]Post url: $url, resp: ${logWithLengthLimit(ccPostResBody ?: "")}")
        return ccPostResBody
    }

    private fun <T> executeDeleteRequest(headers: Map<String, String>, url: String, req: T): String? {
        val requestContent = mapper.writeValueAsString(req)
        logger.info("[bkccAPI]Delete url: $url, req: ${logWithLengthLimit(requestContent)}")
        val requestBody = requestContent.toRequestBody("text/plain".toMediaTypeOrNull())
        val deleteReq: Request = Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .delete(requestBody)
            .build()

        val deleteResBody = OkhttpUtils.doHttp(deleteReq).body?.string()
        logger.info("[bkccAPI]Delete url: $url, resp: ${logWithLengthLimit(deleteResBody ?: "")}")
        return deleteResBody
    }

    private fun logWithLengthLimit(logOrigin: String): String {
        return if (logOrigin.length > LOG_OUTPUT_MAX_LENGTH)
            logOrigin.substring(0, LOG_OUTPUT_MAX_LENGTH)
        else
            logOrigin
    }
}
