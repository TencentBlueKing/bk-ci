/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.service.exception

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.LocaleMessageUtils
import com.tencent.bkrepo.common.service.util.ResponseBuilder

open class AbstractExceptionHandler {

    /**
     * 处理ErrorCodeException
     * 打印日志并输出给给用户
     */
    fun response(exception: ErrorCodeException): Response<Void> {
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(exception.messageCode, exception.params)
        LoggerHolder.logErrorCodeException(exception, "[${exception.messageCode.getCode()}]$errorMessage")
        HttpContextHolder.getResponse().status = exception.status.value
        return ResponseBuilder.fail(exception.messageCode.getCode(), errorMessage)
    }

    /**
     * 处理系统未捕获的Exception响应
     * 日志按照ErrorCodeException格式打印
     * 用户输出为CommonMessageCode.SYSTEM_ERROR，防止将敏感信息或者无用信息响应给用户
     */
    fun response(exception: Exception): Response<Void> {
        val errorMessage = LocaleMessageUtils.getLocalizedMessage(CommonMessageCode.SYSTEM_ERROR)
        val code = CommonMessageCode.SYSTEM_ERROR.getCode()
        LoggerHolder.logException(exception, "[$code]${exception.message}", true)
        HttpContextHolder.getResponse().status = HttpStatus.INTERNAL_SERVER_ERROR.value
        return ResponseBuilder.fail(code, errorMessage)
    }
}
