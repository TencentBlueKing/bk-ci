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
package com.tencent.devops.openapi.utils

import com.tencent.devops.openapi.constant.OpenAPIMessageCode.ERROR_OPENAPI_APIGW_PUBFILE_CONTENT_EMPTY
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.ERROR_OPENAPI_APIGW_PUBFILE_NOT_EXIST
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.ERROR_OPENAPI_APIGW_PUBFILE_NOT_SETTLE
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.ERROR_OPENAPI_APIGW_PUBFILE_READ_ERROR
import com.tencent.devops.openapi.exception.InvalidConfigException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Component
import java.io.File

@Component
@RefreshScope
class ApiGatewayPubFile {

    companion object {
        private val logger = LoggerFactory.getLogger(ApiGatewayPubFile::class.java)
    }

    @Value("\${api.gateway.pub.file.outer:#{null}}")
    private val pubFileOuter: String? = null

    private var pubOuter: String? = null

    fun getPubOuter(): String {
        if (pubOuter == null) {
            synchronized(this) {
                if (pubOuter != null) {
                    return pubOuter!!
                }
                if (pubFileOuter == null) {
                    throw InvalidConfigException(
                        message = "Api gateway pub file is not settle",
                        errorCode = ERROR_OPENAPI_APIGW_PUBFILE_NOT_SETTLE
                    )
                }

                val file = File(pubFileOuter)
                if (!file.exists()) {
                    throw InvalidConfigException(
                        message = "The pub file (${file.absolutePath}) is not exist",
                        errorCode = ERROR_OPENAPI_APIGW_PUBFILE_NOT_EXIST,
                        params = arrayOf(file.absolutePath)
                    )
                }
                pubOuter = file.readText()
                if (pubOuter == null) {
                    throw InvalidConfigException(
                        message = "Can't read the pub content from ${file.absolutePath}",
                        errorCode = ERROR_OPENAPI_APIGW_PUBFILE_READ_ERROR,
                        params = arrayOf(file.absolutePath)
                    )
                }

                if (pubOuter!!.trim().isEmpty()) {
                    throw InvalidConfigException(
                        message = "The pub file is empty from ${file.absolutePath}",
                        errorCode = ERROR_OPENAPI_APIGW_PUBFILE_CONTENT_EMPTY,
                        params = arrayOf(file.absolutePath)
                    )
                }
                logger.info("Get the pub($pubOuter) from ${file.absolutePath}")
            }
        }

        return pubOuter!!
    }
}
