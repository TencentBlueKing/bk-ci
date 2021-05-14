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

package com.tencent.devops.process.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineContextTencentService@Autowired constructor(
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val buildVariableService: BuildVariableService,
    private val client: Client
): PipelineContextService(pipelineBuildDetailService, buildVariableService) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineContextTencentService::class.java)
    }

    fun getGitToken(
        projectId: String,
        buildId: String,
        containerId: String
    ): String {
        return if (projectId.startsWith("git_")) {
            val gitToken = try {
                val gitProjectId = projectId.removePrefix("git_")
                val accessToken = client.getScm(ServiceGitCiResource::class).getToken(gitProjectId).data!!.accessToken
                logger.info("get token from scm success, token: $accessToken")
                accessToken
            } catch (e: Exception) {
                logger.error("get token from scm exception:", e)
                ""
            }

            gitToken
        } else ""
    }

    fun deleteGitToken(token: String) {
        try {
            client.getScm(ServiceGitCiResource::class).clearToken(token)
        } catch (e: Exception) {
            logger.warn("Delete git token($token) failed, msg: ${e.message}")
        }
    }
}
