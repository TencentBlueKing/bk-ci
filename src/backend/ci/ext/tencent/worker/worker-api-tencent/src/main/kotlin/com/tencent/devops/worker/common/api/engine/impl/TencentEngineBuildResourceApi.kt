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

package com.tencent.devops.worker.common.api.engine.impl

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.api.engine.EngineBuildSDKApi
import com.tencent.devops.worker.common.api.utils.ThirdPartyAgentBuildInfoUtils
import com.tencent.devops.worker.common.env.AgentEnv

@Suppress("UNUSED")
@ApiPriority(priority = 9)
class TencentEngineBuildResourceApi : EngineBuildResourceApi(), EngineBuildSDKApi {

    private fun identifyUrl(url: String, paramConcat: String = "&"): String {
        val buildInfo = ThirdPartyAgentBuildInfoUtils.getBuildInfo()
        return url + paramConcat + "buildId=${buildInfo?.buildId}"
    }

    override fun getRequestUrl(path: String, retryCount: Int): String {
        return identifyUrl("/ms/engine/$path?retryCount=$retryCount")
    }

    override fun getCiToken(): String {
        try {
            val projectId = AgentEnv.getProjectId()
            if (projectId.startsWith("git_")) {
                val gitProjectId = projectId.removePrefix("git_")
                val url = "/ms/repository/api/build/gitci/getToken?gitProjectId=$gitProjectId"
                val request = buildGet(url)
                val responseContent = request(request, "获取工蜂CI项目Token失败！")
                val gitToken = objectMapper.readValue<Result<GitToken>>(responseContent)
                return gitToken.data?.accessToken ?: ""
            }
        } catch (e: Exception) {
            logger.error("get ci token failed.", e)
        }

        return super.getCiToken()
    }

    override fun getCiUrl(): String {
        try {
            val projectId = AgentEnv.getProjectId()
            if (projectId.startsWith("git_")) {
                val url = "/ms/gitci/api/build/getCiUrl?projectId=$projectId"
                val request = buildGet(url)
                val responseContent = request(request, "获取工蜂CI项目Token失败！")
                val gitToken = objectMapper.readValue<Result<GitToken>>(responseContent)
                return gitToken.data?.accessToken ?: ""
            }
        } catch (e: Exception) {
            logger.error("get ci token failed.", e)
        }

        return super.getCiToken()
    }
}
