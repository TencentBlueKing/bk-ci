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

package com.tencent.devops.environment.service.cmdb

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_CMDB_INTERFACE_TIME_OUT
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_CMDB_RESPONSE
import com.tencent.devops.environment.pojo.cmdb.req.NewCmdbCondition
import com.tencent.devops.environment.pojo.cmdb.req.NewCmdbQueryReq
import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbResp
import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbScrollPageData
import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbServer
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import java.net.SocketTimeoutException
import jakarta.ws.rs.core.Response

class NewCmdbClient(
    private val newCmdbBaseUrl: String,
    private val appId: String,
    private val appKey: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(NewCmdbClient::class.java)
        private val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        // HTTP请求超时时间（秒）
        private const val CONNECT_TIMEOUT_SECONDS = 5L
        private const val READ_TIMEOUT_SECONDS = 15L
        private const val WRITE_TIMEOUT_SECONDS = 15L

        // 需要获取的字段
        private val resultColumn = listOf(
            // 主负责人
            "maintainer",
            // 备份负责人(多个人用;分隔)
            "maintainerBak",
            // 服务器ID
            "serverId",
            // 操作系统名称
            "osName",
            // 主机名称
            "hostName",
            // 运维部门ID
            "maintenanceDepartmentId",
            // 服务器的内网Ipv4地址列表
            "innerServerIpv4"
        )
    }

    /**
     * 根据基础条件（serverId、IP）查询服务器列表
     * @param newCmdbCondition 基础条件（serverId、IP）
     * @return 服务器列表
     */
    fun queryAllServerByBaseCondition(
        newCmdbCondition: NewCmdbCondition
    ): List<NewCmdbServer> {
        val path = "/cmdb-service-federal-query/queryAllServerByBaseCondition"
        val serverScrollPageData = queryServerScrollPageData(path, newCmdbCondition)
        return serverScrollPageData.list
    }

    /**
     * 根据业务条件（主/备负责人）查询服务器列表
     * @param newCmdbCondition 业务条件（主/备负责人）
     * @param size 分页大小
     * @param scrollId 分页游标
     * @return 服务器分页数据
     */
    fun queryAllServerByBusiness(
        newCmdbCondition: NewCmdbCondition,
        size: Int,
        scrollId: String
    ): NewCmdbScrollPageData<NewCmdbServer> {
        val path = "/cmdb-service-federal-query/queryAllServerByBusiness"
        return queryServerScrollPageData(path, newCmdbCondition, size, scrollId)
    }

    private fun queryServerScrollPageData(
        path: String,
        condition: NewCmdbCondition,
        size: Int? = null,
        scrollId: String? = null
    ): NewCmdbScrollPageData<NewCmdbServer> {
        val req = NewCmdbQueryReq(
            resultColumn = resultColumn,
            condition = condition,
            size = size,
            scrollId = scrollId
        )
        val respStr = postAndGetRespStr(path, req)
        return parseServerScrollPageData(respStr)
    }

    private fun <T : Any> postAndGetRespStr(path: String, req: T): String {
        val reqBodyStr = mapper.writeValueAsString(req)
        val reqStrToLog = LogUtils.getLogWithLengthLimit(JsonUtil.skipLogFields(req))
        logger.info("PostCMDB|path=$path|req=$reqStrToLog")

        val respStr: String?
        try {
            respStr = OkhttpUtils.doCustomTimeoutPost(
                connectTimeout = CONNECT_TIMEOUT_SECONDS,
                readTimeout = READ_TIMEOUT_SECONDS,
                writeTimeout = WRITE_TIMEOUT_SECONDS,
                url = buildCompleteUrl(path),
                jsonParam = reqBodyStr,
                headers = buildBasicHeaders()
            ).body?.string()
            val respStrToLog = LogUtils.getLogWithLengthLimit(respStr)
            logger.info("PostCMDB|respStr=$respStrToLog")
            assertRespNotNull(respStr)
        } catch (timeoutError: SocketTimeoutException) {
            logger.warn("CMDBInterfaceTimeout", timeoutError)
            throw ErrorCodeException(
                errorCode = ERROR_CMDB_INTERFACE_TIME_OUT,
                defaultMessage = I18nUtil.getCodeLanMessage(ERROR_CMDB_INTERFACE_TIME_OUT)
            )
        } catch (error: Exception) {
            logger.warn("CMDBInterfaceException", error)
            throw ErrorCodeException(
                errorCode = ERROR_CMDB_RESPONSE,
                defaultMessage = I18nUtil.getCodeLanMessage(ERROR_CMDB_RESPONSE)
            )
        }
        return respStr!!
    }

    private fun buildCompleteUrl(path: String): String {
        return newCmdbBaseUrl + path
    }

    private fun buildBasicHeaders(): MutableMap<String, String> {
        val headers = mutableMapOf("accept" to "*/*", "Content-Type" to "application/json")
        // 接入系统英文名称
        val appId = appId
        // 接入完毕后自动生成的appKey
        val appKey = appKey
        // 获取当前时间戳
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        // 使用标准算法库生成签名
        val signature: String = DigestUtils.sha256Hex(timestamp + appKey)
        // 添加请求头
        headers["x-timestamp"] = timestamp
        headers["x-signature"] = signature
        headers["x-app-id"] = appId
        return headers
    }

    private fun assertRespNotNull(respStr: String?) {
        if (null == respStr) {
            logger.warn("CMDBInterfaceRespStrIsNull")
            throw ErrorCodeException(
                errorCode = ERROR_CMDB_RESPONSE,
                defaultMessage = I18nUtil.getCodeLanMessage(ERROR_CMDB_RESPONSE)
            )
        }
    }

    /**
     * 从响应体中解析主机滚动分页数据
     * @param respStr 响应体
     * @return 主机滚动分页数据
     */
    private fun parseServerScrollPageData(respStr: String): NewCmdbScrollPageData<NewCmdbServer> {
        val serverScrollPageData: NewCmdbScrollPageData<NewCmdbServer>?
        try {
            val resp = mapper.readValue<NewCmdbResp<NewCmdbServer>>(respStr)
            logger.info("parseServerScrollPageData|cmdbTraceId=${resp.traceId}")
            serverScrollPageData = resp.data
        } catch (e: Exception) {
            logger.warn("parseServerScrollPageDataException", e)
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "parseServerScrollPageDataException")
        }
        return serverScrollPageData
    }
}
