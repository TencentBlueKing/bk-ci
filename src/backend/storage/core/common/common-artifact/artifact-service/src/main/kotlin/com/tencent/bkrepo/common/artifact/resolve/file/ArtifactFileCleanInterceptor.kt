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

package com.tencent.bkrepo.common.artifact.resolve.file

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerInterceptor
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.system.measureTimeMillis

/**
 * 此拦截器用于请求结束后清理创建的临时文件
 */
class ArtifactFileCleanInterceptor : HandlerInterceptor {

    @Suppress("UNCHECKED_CAST")
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        try {
            val artifactFileList = request.getAttribute(ArtifactFileFactory.ARTIFACT_FILES) as? List<ArtifactFile>
            artifactFileList?.filter { it.hasInitialized() && !it.isInMemory() }?.forEach {
                val absolutePath = it.getFile()!!.absolutePath
                measureTimeMillis { it.delete() }.apply {
                    logger.info("Delete temp artifact file [$absolutePath] success, elapse $this ms")
                }
            }
        } catch (exception: IOException) {
            logger.warn("Failed to clean temp artifact file.", exception)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactFileCleanInterceptor::class.java)
    }
}
