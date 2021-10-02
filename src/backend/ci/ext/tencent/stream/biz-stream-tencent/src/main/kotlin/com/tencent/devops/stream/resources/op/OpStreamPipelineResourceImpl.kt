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

package com.tencent.devops.stream.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.stream.api.op.OpStreamPipelineResource
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.trigger.GitCIEventService
import com.tencent.devops.stream.v2.service.StreamPipelineBranchService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpStreamPipelineResourceImpl @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val gitCIEventService: GitCIEventService
) : OpStreamPipelineResource {
    override fun checkBranches(userId: String, gitProjectId: Long, pipelineId: String): Result<Boolean> {
        streamPipelineBranchService.deleteBranch(
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            branch = null
        )
        pipelineResourceDao.deleteByPipelineId(dslContext, pipelineId)
        client.get(ServicePipelineResource::class).delete(
            userId = userId, projectId = "git_$gitProjectId", pipelineId = pipelineId,
            channelCode = ChannelCode.GIT
        )
        // 删除相关的构建记录
        gitCIEventService.deletePipelineBuildHistory(setOf(pipelineId))
        return Result(true)
    }
}
