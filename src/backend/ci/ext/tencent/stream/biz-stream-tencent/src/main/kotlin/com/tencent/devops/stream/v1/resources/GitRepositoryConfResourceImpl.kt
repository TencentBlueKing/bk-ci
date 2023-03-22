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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.stream.api.service.v1.GitRepositoryConfResource
import com.tencent.devops.stream.constant.StreamCode.BK_PROJECT_CANNOT_OPEN_STREAM
import com.tencent.devops.stream.v1.pojo.V1GitRepositoryConf
import com.tencent.devops.stream.v1.service.V1GitRepositoryConfService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class GitRepositoryConfResourceImpl @Autowired constructor(
    private val repositoryService: V1GitRepositoryConfService
) : GitRepositoryConfResource {

    override fun disableGitCI(userId: String, gitProjectId: Long): Result<Boolean> {
        checkParam(userId, gitProjectId)
        return Result(repositoryService.disableGitCI(gitProjectId))
    }

    override fun getGitCIConf(userId: String, gitProjectId: Long): Result<V1GitRepositoryConf?> {
        checkParam(userId, gitProjectId)
        return Result(repositoryService.getGitCIConf(gitProjectId))
    }

    override fun saveGitCIConf(userId: String, repositoryConf: V1GitRepositoryConf): Result<Boolean> {
        checkParam(userId, repositoryConf.gitProjectId)
        return Result(repositoryService.saveGitCIConf(userId, repositoryConf))
    }

    private fun checkParam(userId: String, gitProjectId: Long) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (!repositoryService.initGitCISetting(userId, gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PROJECT_CANNOT_OPEN_STREAM,
                    language = I18nUtil.getLanguage(userId)
                ))
        }
    }
}
