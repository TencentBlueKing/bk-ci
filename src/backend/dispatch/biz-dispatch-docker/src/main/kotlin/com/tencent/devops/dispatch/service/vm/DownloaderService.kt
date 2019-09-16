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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.service.vm

import com.tencent.devops.common.api.util.FileUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Service
class DownloaderService {

    @Value("\${dispatch.workerFile:#{null}}")
    private val workerFile: String? = null

    @Value("\${dispatch.dockerFile:#{null}}")
    private val dockerFile: String? = null

    fun downloadWorker(eTag: String?): Response {
        if (workerFile.isNullOrBlank()) {
            throw RuntimeException("worker.jar文件路径没有配置")
        }
        return download(workerFile!!, eTag)
    }

    fun downloadDocker(eTag: String?): Response {
        if (dockerFile.isNullOrBlank()) {
            throw RuntimeException("docker.jar文件路径没有配置")
        }
        return download(dockerFile!!, eTag)
    }

    private fun download(file: String, eTag: String?): Response {
        val worker = File(file)
        if (!worker.exists()) {
            throw NotFoundException("${worker.absolutePath} 不存在")
        }

        if (!worker.isFile) {
            throw RuntimeException("${worker.absolutePath} 不是一个文件")
        }

        if (eTag != null && eTag.isNotBlank()) {
            // 检查文件的MD5值是否和客户端一致
            val workerMD5 = FileUtil.getMD5(worker)
            if (workerMD5 != null && workerMD5 == eTag) {
                return Response.status(Response.Status.NOT_MODIFIED).build()
            }
        }
        val fileStream = StreamingOutput { output ->
            output.write(worker.readBytes())
            output.flush()
        }
        return Response
                .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("content-disposition", "attachment; filename = ${worker.name}")
                .build()
    }
}