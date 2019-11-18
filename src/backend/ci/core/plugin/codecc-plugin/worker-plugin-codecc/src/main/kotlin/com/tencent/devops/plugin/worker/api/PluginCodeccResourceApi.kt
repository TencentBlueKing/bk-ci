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

package com.tencent.devops.plugin.worker.api

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import okhttp3.Protocol
import okhttp3.Response
import org.springframework.http.HttpStatus

class PluginCodeccResourceApi : AbstractBuildResourceApi(), CodeccSDKApi {

    override fun saveTask(projectId: String, pipelineId: String, buildId: String): Result<String> {
        val path = "/ms/plugin/api/build/codecc/save/task/$projectId/$pipelineId/$buildId"
        val request = buildPost(path)
        val responseContent = request(request, "保存CodeCC原子信息失败")
        return Result(responseContent)
    }

    override fun downloadTool(tool: String, osType: OSType, fileMd5: String, is32Bit: Boolean): Response {
        val path = "/ms/plugin/api/build/codecc/$tool?osType=${osType.name}&fileMd5=$fileMd5&is32Bit=$is32Bit"
        val request = buildGet(path)

        val response = requestForResponse(request)
        if (response.code() == HttpStatus.NOT_MODIFIED.value()) {
            return Response.Builder().request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("")
                .code(HttpStatus.NOT_MODIFIED.value()).build()
        }
        if (!response.isSuccessful) {
            throw RemoteServiceException("下载Codecc的 $tool 工具失败")
        }
        return response
    }

    override fun downloadToolScript(osType: OSType, fileMd5: String): Response {
        val path = "/ms/plugin/api/build/codecc/tools/script?osType=${osType.name}&fileMd5=$fileMd5"
        val request = buildGet(path)
        val response = requestForResponse(request)
        if (response.code() == HttpStatus.NOT_MODIFIED.value()) {
            return Response.Builder().request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("")
                .code(HttpStatus.NOT_MODIFIED.value()).build()
        }

        if (!response.isSuccessful) {
            throw RemoteServiceException("下载codecc的多工具执行脚本失败")
        }
        return response
    }
}
