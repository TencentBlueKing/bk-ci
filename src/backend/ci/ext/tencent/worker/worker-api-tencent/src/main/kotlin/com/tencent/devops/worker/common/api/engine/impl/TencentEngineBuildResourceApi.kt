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
import com.tencent.devops.common.api.constant.I18NConstant.BK_FAILED_GET_WORKER_BEE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.worker.common.CI_TOKEN_CONTEXT
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.api.engine.EngineBuildSDKApi
import com.tencent.devops.worker.common.env.AgentEnv

@Suppress("UNUSED")
@ApiPriority(priority = 9)
class TencentEngineBuildResourceApi : EngineBuildResourceApi(), EngineBuildSDKApi {

    override fun getRequestUrl(path: String, retryCount: Int, executeCount: Int): String {
        return "/ms/engine/$path?retryCount=$retryCount&executeCount=$executeCount&buildId=$buildId"
    }

    override fun getJobContext(): Map<String, String> {
        // 在此方法累加需要传入的上下文变量，可以调多处接口获取并累加
        val context = mutableMapOf<String, String>()
        try {
            val projectId = AgentEnv.getProjectId()
            if (projectId.startsWith("git_")) {
                val gitProjectId = projectId.removePrefix("git_")
                val url = "/ms/repository/api/build/gitci/getToken?gitProjectId=$gitProjectId"
                val request = buildGet(url)
                val responseContent = request(request,
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_FAILED_GET_WORKER_BEE,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ))
                val gitToken = objectMapper.readValue<Result<GitToken>>(responseContent)
                context[CI_TOKEN_CONTEXT] = gitToken.data?.accessToken ?: ""
            }
        } catch (e: Exception) {
            logger.error("get context failed: ", e)
        }
        return context
    }

    override fun endTask(variables: Map<String, String>, envBuildId: String, retryCount: Int): Result<Boolean> {
        // #5277 对所有job下变量做收尾处理，可以在try区域内逐步追加
        try {
            val projectId = AgentEnv.getProjectId()
            val gitToken = variables[CI_TOKEN_CONTEXT]
            if (projectId.startsWith("git_") && !gitToken.isNullOrBlank()) {
                val url = "/ms/repository/api/build/gitci/clearToken?token=$gitToken"
                val request = buildDelete(url)
                val responseContent = request(request,
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_FAILED_GET_WORKER_BEE,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ))
                val result = objectMapper.readValue<Result<Boolean>>(responseContent)
                if (result.data == true) {
                    logger.info("ci token for project[$projectId] is cleared.")
                }
            }
        } catch (e: Exception) {
            logger.error("get context failed: ", e)
        }
        return super.endTask(variables, envBuildId, retryCount)
    }
}
