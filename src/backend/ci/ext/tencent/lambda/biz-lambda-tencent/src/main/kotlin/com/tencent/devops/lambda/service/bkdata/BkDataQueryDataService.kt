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
package com.tencent.devops.lambda.service.bkdata

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.lambda.pojo.bkdata.BkDataQueryData
import com.tencent.devops.lambda.pojo.bkdata.BkDataQueryParam
import com.tencent.devops.lambda.pojo.bkdata.BkDataQueryRequest
import com.tencent.devops.lambda.pojo.bkdata.BkDataResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BkDataQueryDataService @Autowired constructor() {

    companion object {
        private val logger = LoggerFactory.getLogger(BkDataQueryDataService::class.java)
    }

    @Value("\${esb.appCode:#{null}}")
    val appCode: String = ""

    @Value("\${esb.appSecret:#{null}}")
    val appSecret: String = ""

    @Value("\${bkdata.queryDataUrl:#{null}}")
    val queryDataUrl: String = ""

    @Value("\${bkdata.token:#{null}}")
    val token: String = ""

    fun queryData(bkDataQueryParam: BkDataQueryParam): BkDataResult<BkDataQueryData> {
        // 组装数据平台查询接口参数
        val bkDataQueryRequest = BkDataQueryRequest(
            authenticationMethod = "token",
            dataToken = token,
            bkAppCode = appCode,
            bkAppSecret = appSecret,
            sql = bkDataQueryParam.sql,
            preferStorage = bkDataQueryParam.preferStorage
        )
        // 调用数据平台查询接口
        val resp = OkhttpUtils.doPost(
            url = queryDataUrl,
            jsonParam = JsonUtil.toJson(bkDataQueryRequest)
        )
        if (!resp.isSuccessful) {
            logger.warn("bkDataQueryParam:$bkDataQueryParam queryData fail: $resp")
            throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
        } else if (resp.body == null) {
            logger.warn("bkDataQueryParam:$bkDataQueryParam resp is empty")
        }
        return JsonUtil.to(resp.body!!.string(), object : TypeReference<BkDataResult<BkDataQueryData>>() {})
    }
}
