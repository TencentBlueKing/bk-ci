/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.service.feign

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.service.exception.RemoteErrorCodeException
import feign.Response
import feign.codec.ErrorDecoder
import java.io.IOException

/**
 * Feign ErrorDecoder
 */
class ErrorCodeDecoder : ErrorDecoder {

    private val delegate: ErrorDecoder = ErrorDecoder.Default()

    override fun decode(methodKey: String, feignResponse: Response): Exception {
        if (feignResponse.status() == HttpStatus.BAD_REQUEST.value) {
            return try {
                feignResponse.body().asInputStream().use {
                    val response = it.readJsonString<com.tencent.bkrepo.common.api.pojo.Response<Any>>()
                    RemoteErrorCodeException(methodKey, response.code, response.message)
                }
            } catch (ignored: IOException) {
                delegate.decode(methodKey, feignResponse)
            }
        }
        return delegate.decode(methodKey, feignResponse)
    }
}
