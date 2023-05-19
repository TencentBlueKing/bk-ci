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

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_NULL
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.FileUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service@Suppress("ALL")
class DownloadScriptService {

    @Value("\${dispatch.scripts:#{null}}")
    private val scriptPath: String? = null

    fun downloadScript(scriptName: String, eTag: String?): Response {
        logger.info("downloadScript, scriptName: $scriptName, eTag: $eTag")
        if (scriptPath.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = PARAMETER_IS_NULL,
                params = arrayOf("scriptPath")
            )
        }

        val scriptFile = File("$scriptPath/$scriptName")
        if (!scriptFile.exists() || !scriptFile.isFile) {
            throw NotFoundException("script($scriptName)not exist")
        }

        if (!eTag.isNullOrBlank()) {
            // Check if md5 is the same
            val md5 = FileUtil.getMD5(scriptFile)
            if (md5 == eTag) {
                return Response.status(Response.Status.NOT_MODIFIED).build()
            }
        }

        return Response.ok(scriptFile.inputStream(), MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $scriptName")
            .build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DownloadScriptService::class.java)
    }
}
