/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.project.resources

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.TokenForbiddenException
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.project.api.open.OpenProjectResource
import com.tencent.devops.project.pojo.OperationalProductVO
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ProjectService
import org.slf4j.LoggerFactory

@RestResource
class OpenProjectResourceImpl constructor(
    private val projectService: ProjectService,
    private val clientTokenService: ClientTokenService
) : OpenProjectResource {
    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun get(
        token: String,
        projectId: String
    ): Result<ProjectVO> {
        check(token)
        return Result(
            projectService.getByEnglishName(
                englishName = projectId
            ) ?: throw OperationException("project $projectId not found")
        )
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun listByProjectCodes(
        token: String,
        projectCodes: Set<String>
    ): Result<List<ProjectVO>> {
        check(token)
        return Result(
            projectService.list(
                projectCodes = projectCodes,
                enabled = null
            )
        )
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun getOperationalProducts(token: String): Result<List<OperationalProductVO>> {
        check(token)
        return Result(projectService.getOperationalProducts())
    }

    private fun check(token: String) {
        if (token != clientTokenService.getSystemToken()) {
            logger.warn("auth token fail: $token")
            throw TokenForbiddenException("token check fail")
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OpenProjectResourceImpl::class.java)
    }
}
