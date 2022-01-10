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
package com.tencent.devops.monitoring.services

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.monitoring.pojo.NocNoticeBusData
import com.tencent.devops.monitoring.pojo.NocNoticeRequest
import com.tencent.devops.monitoring.pojo.NocNoticeUserInfo
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

@Service
@RefreshScope
class NocNoticeService {
    private val logger = LoggerFactory.getLogger(NocNoticeService::class.java)

    @Value("\${gateway.appCode}")
    private lateinit var appCode: String

    @Value("\${gateway.appSecret}")
    private lateinit var appSecret: String

    @Value("\${gateway.uwork.sendNocNoticeUrl}")
    private lateinit var sendNocNoticeUrl: String

    /**
     * 发送noc语音告警
     */
    fun sendNocNotice(
        notifyReceivers: Set<String>,
        notifyTitle: String,
        notifyMessage: String,
        busiDataList: List<NocNoticeBusData>
    ): Result<Boolean> {
        logger.info("the notifyReceivers is:$notifyReceivers,notifyTitle is:$notifyTitle,notifyMessage is:$notifyMessage,busiDataList is:$busiDataList")
        val userInfoList = mutableListOf<NocNoticeUserInfo>()
        notifyReceivers.forEach {
            userInfoList.add(NocNoticeUserInfo(username = it)) // 内部用户无需填手机号，noc系统会根据rtx名称从oa系统查出来
        }
        val nocNoticeRequest = NocNoticeRequest(
            appCode = appCode,
            appSecret = appSecret,
            operator = "DevOps",
            autoReadMessage = notifyTitle,
            headDesc = notifyTitle,
            busiDataList = busiDataList,
            userInfoList = userInfoList,
            noticeInformation = notifyMessage
        )
        val requestBody = JsonUtil.toJson(nocNoticeRequest)
        logger.info("the requestBody is:$requestBody")
        val request = Request.Builder()
            .url(sendNocNoticeUrl)
            .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), requestBody))
            .build()
        OkhttpUtils.doHttp(request).use { res ->
            val data = res.body()!!.string()
            logger.info("the response>> $data")
            if (!res.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            val response: Map<String, Any> = JsonUtil.toMap(data)
            val code = response["code"]
            if (code != "00") {
                return Result(status = code.toString().toInt(), message = response["message"] as String) // 发送失败抛出错误提示
            }
        }
        return Result(true)
    }
}
