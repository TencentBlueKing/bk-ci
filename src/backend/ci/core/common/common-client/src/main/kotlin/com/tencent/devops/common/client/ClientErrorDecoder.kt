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

package com.tencent.devops.common.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode.RETURNED_RESULT_COULD_NOT_BE_PARSED
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import feign.Response
import feign.codec.ErrorDecoder
import java.io.BufferedReader
import java.io.IOException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 *
 * Powered By Tencent
 */
@Service
class ClientErrorDecoder @Autowired constructor(val objectMapper: ObjectMapper) : ErrorDecoder {

    override fun decode(methodKey: String, response: Response): Exception {
        // 首先判断返回结果是否能被序列化
        val content = response.body().asReader(Charsets.UTF_8).buffered().use(BufferedReader::readText)
        val result: Result<*>
        try {
            result = objectMapper.readValue(content)
        } catch (ignore: IOException) {
            return ErrorCodeException(
                errorCode = RETURNED_RESULT_COULD_NOT_BE_PARSED,
                params = arrayOf(response.status().toString(), content)
            )
        }
        return RemoteServiceException(
            httpStatus = response.status(),
            errorCode = result.status,
            errorMessage = result.message ?: ""
        )
    }
}
