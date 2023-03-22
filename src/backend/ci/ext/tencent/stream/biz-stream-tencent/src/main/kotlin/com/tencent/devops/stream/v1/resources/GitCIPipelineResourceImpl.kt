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

package com.tencent.devops.stream.v1.resources

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.stream.api.service.v1.GitCIPipelineResource
import com.tencent.devops.stream.constant.StreamCode.BK_PROJECT_CANNOT_OPEN_STREAM
import com.tencent.devops.stream.v1.pojo.V1GitProjectPipeline
import com.tencent.devops.stream.v1.service.V1GitCIPipelineService
import com.tencent.devops.stream.v1.service.V1GitRepositoryConfService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class GitCIPipelineResourceImpl @Autowired constructor(
    private val pipelineService: V1GitCIPipelineService,
    private val repositoryConfService: V1GitRepositoryConfService
) : GitCIPipelineResource {

    override fun getPipelineList(
        userId: String,
        gitProjectId: Long,
        keyword: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<V1GitProjectPipeline>> {
        checkParam(userId)
        if (!repositoryConfService.initGitCISetting(userId, gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PROJECT_CANNOT_OPEN_STREAM,
                    language = I18nUtil.getLanguage(userId)
                ))
        }
        return Result(
            pipelineService.getPipelineList(
                userId = userId,
                gitProjectId = gitProjectId,
                keyword = keyword,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getPipeline(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        withHistory: Boolean?
    ): Result<V1GitProjectPipeline?> {
        checkParam(userId)
        if (!repositoryConfService.initGitCISetting(userId, gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PROJECT_CANNOT_OPEN_STREAM,
                    language = I18nUtil.getLanguage(userId)
                ))
        }
        return Result(
            pipelineService.getPipelineById(
                userId = userId,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                withHistory = withHistory
            )
        )
    }

    override fun enablePipeline(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        enabled: Boolean
    ): Result<Boolean> {
        checkParam(userId)
        if (!repositoryConfService.initGitCISetting(userId, gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PROJECT_CANNOT_OPEN_STREAM,
                    language = I18nUtil.getLanguage(userId)
                ))
        }
        return Result(
            pipelineService.enablePipeline(
                userId = userId,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                enabled = enabled
            )
        )
    }

    override fun listPipelineNames(userId: String, gitProjectId: Long): Result<List<V1GitProjectPipeline>> {
        checkParam(userId)
        if (!repositoryConfService.initGitCISetting(userId, gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PROJECT_CANNOT_OPEN_STREAM,
                    language = I18nUtil.getLanguage(userId)
                ))
        }
        return Result(
            pipelineService.getPipelineListWithoutHistory(
                userId = userId,
                gitProjectId = gitProjectId
            )
        )
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
