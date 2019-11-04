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

package com.tencent.devops.worker.common.api.scm

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.commit.CommitData
import com.tencent.devops.ticket.pojo.CertIOS
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi

class CommitResourceApi : AbstractBuildResourceApi(), CommitSDKApi {

    override fun addCommit(commits: List<CommitData>): Result<CertIOS> {
        val path = "/ms/repository/api/build/commit/addCommit"
        val request = buildPost(path, getJsonRequest(commits))
        val responseContent = request(request, "添加代码库commit信息失败")
        return objectMapper.readValue(responseContent)
    }

    override fun getLatestCommit(
        pipelineId: String,
        elementId: String,
        repositoryConfig: RepositoryConfig
    ): Result<CommitData> {
        val repositoryId = repositoryConfig.getRepositoryId()
        val name = repositoryConfig.repositoryType.name
        val path = "/ms/repository/api/build/commit/getLatestCommit?pipelineId=$pipelineId" +
            "&elementId=$elementId&repoId=$repositoryId&repositoryType=$name"
        val request = buildGet(path)
        val responseContent = request(request, "获取最后一次代码commit信息失败")
        return objectMapper.readValue(responseContent)
    }
}