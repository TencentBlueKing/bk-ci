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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.SocketTimeoutException
import javax.ws.rs.core.Response

@Service
@Suppress("LongMethod")
class BkTicketService @Autowired constructor(
    private val commonService: CommonService
) {
    @Value("\${remoteDev.bkTicketCheckUrl:}")
    private val bkTicketCheckUrl: String = ""

    @Value("\${remoteDev.bkTokenCheckUrl:}")
    private val bkTokenCheckUrl: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(BkTicketService::class.java)
    }

    // 更新容器内的bkticket
    fun updateBkTicket(userId: String, bkTicket: String?, hostName: String?, retryTime: Int = 3): Boolean {
        logger.info("updateBkTicket|userId|$userId|bkTicket|$bkTicket|hostName|$hostName")
        if (bkTicket.isNullOrBlank() || hostName.isNullOrBlank()) {
            return false
        }
        val url = "http://$hostName/_remoting/api/token/updateBkTicket"
        val params = mutableMapOf<String, Any?>()
        params["ticket"] = bkTicket
        params["user"] = userId
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .header("Cookie", "X-DEVOPS-BK-TICKET=$bkTicket")
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), JsonUtil.toJson(params)))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                logger.info("updateBkTicket|response code|${response.code}|content|$data")
                if (!response.isSuccessful && retryTime > 0) {
                    val retryTimeLocal = retryTime - 1
                    return updateBkTicket(userId, bkTicket, hostName, retryTimeLocal)
                }
                if (!response.isSuccessful && retryTime <= 0) {
                    throw ErrorCodeException(
                        statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                        errorCode = ErrorCodeEnum.UPDATE_BK_TICKET_FAIL.errorCode,
                        defaultMessage = ErrorCodeEnum.UPDATE_BK_TICKET_FAIL.formatErrorMessage
                    )
                }

                val dataMap = JsonUtil.toMap(data)
                val status = dataMap["status"]
                return (status == 0)
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryTime > 0) {
                logger.info("User $userId updateBkTicket. retry: $retryTime")
                return updateBkTicket(userId, bkTicket, hostName, retryTime - 1)
            } else {
                logger.error("User $userId updateBkTicket failed.", e)
                throw ErrorCodeException(
                    statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                    errorCode = ErrorCodeEnum.UPDATE_BK_TICKET_FAIL.errorCode,
                    defaultMessage = ErrorCodeEnum.UPDATE_BK_TICKET_FAIL.formatErrorMessage
                )
            }
        }
    }

    // 调用蓝盾统一登录接口校验用户的登录信息是否合法
    fun validateUserTicket(userId: String, isOffshore: Boolean, ticket: String, retryTime: Int = 3): Boolean {
        logger.info("updateBkTicket|userId|$userId|isOffshore|$isOffshore|ticket|$ticket")
        if (ticket.isBlank()) {
            return false
        }
        val url = if(isOffshore) bkTokenCheckUrl.plus("?bk_ticket=$ticket")
                    else bkTicketCheckUrl.plus("?bk_ticket=$ticket")
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .header("Content-Type", "application/json")
            .get()
            .build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                logger.info("updateBkTicket|response code|${response.code}|content|$data")
                if (!response.isSuccessful && retryTime > 0) {
                    val retryTimeLocal = retryTime - 1
                    return validateUserTicket(userId, isOffshore, ticket, retryTimeLocal)
                }
                // 重试结束抛出异常
                if (!response.isSuccessful && retryTime <= 0) {
                    throw ErrorCodeException(
                        statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                        errorCode = ErrorCodeEnum.CHECK_USER_TICKET_FAIL.errorCode,
                        defaultMessage = ErrorCodeEnum.CHECK_USER_TICKET_FAIL.formatErrorMessage
                    )
                }

                val dataMap = JsonUtil.toMap(data)
                val status = dataMap["ret"]
                return (status == 0)
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryTime > 0) {
                logger.info("check user $userId ticket. retry: $retryTime")
                return validateUserTicket(userId, isOffshore, ticket, retryTime - 1)
            } else {
                logger.error("check user $userId ticket failed.", e)
                throw ErrorCodeException(
                    statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                    errorCode = ErrorCodeEnum.CHECK_USER_TICKET_FAIL.errorCode,
                    defaultMessage = ErrorCodeEnum.CHECK_USER_TICKET_FAIL.formatErrorMessage
                )
            }
        }








    }

}
