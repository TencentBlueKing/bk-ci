/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
    10| *
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

package com.tencent.devops.worker.common.api.variable

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GET_BUILD_TASK_DETAILS_FAILURE
import com.tencent.devops.worker.common.env.AgentEnv
import java.net.URLEncoder

class BuildVariableResourceApi : AbstractBuildResourceApi(), BuildVariableSDKApi {

    override fun getBuildVariableValue(pipelineId: String, varName: String): Result<String?> {
        val encoded = URLEncoder.encode(varName, "UTF-8")
        val path = "/ms/process/api/build/variable/value?varName=$encoded"
        val request = buildGet(
            path = path,
            headers = mapOf(AUTH_HEADER_DEVOPS_PIPELINE_ID to pipelineId)
        )
        val errorMessage = MessageUtil.getMessageByLocale(
            GET_BUILD_TASK_DETAILS_FAILURE,
            AgentEnv.getLocaleLanguage()
        )
        // 大变量单值最多 4M，适当放宽读超时
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 120L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }
}
