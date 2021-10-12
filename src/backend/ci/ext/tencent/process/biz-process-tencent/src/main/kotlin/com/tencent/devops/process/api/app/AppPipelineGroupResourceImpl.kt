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

package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineGroupUpdate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelUpdate
import com.tencent.devops.process.service.label.PipelineGroupService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppPipelineGroupResourceImpl @Autowired constructor(private val pipelineGroupService: PipelineGroupService) :
    AppPipelineGroupResource {
    override fun getGroups(userId: String, projectId: String): Result<List<PipelineGroup>> {
        return Result(pipelineGroupService.getGroups(userId, projectId))
    }

    override fun addGroup(userId: String, pipelineGroup: PipelineGroupCreate): Result<Boolean> {
        return Result(pipelineGroupService.addGroup(userId, pipelineGroup))
    }

    override fun updateGroup(userId: String, pipelineGroup: PipelineGroupUpdate): Result<Boolean> {
        return Result(pipelineGroupService.updateGroup(userId, pipelineGroup))
    }

    override fun deleteGroup(userId: String, projectId: String, groupId: String): Result<Boolean> {
        return Result(pipelineGroupService.deleteGroup(userId, projectId, groupId))
    }

    override fun addLabel(userId: String, projectId: String, pipelineLabel: PipelineLabelCreate): Result<Boolean> {
        return Result(pipelineGroupService.addLabel(userId, projectId, pipelineLabel))
    }

    override fun deleteLabel(userId: String, projectId: String, labelId: String): Result<Boolean> {
        return Result(pipelineGroupService.deleteLabel(userId, projectId, labelId))
    }

    override fun updateLabel(userId: String, projectId: String, pipelineLabel: PipelineLabelUpdate): Result<Boolean> {
        return Result(pipelineGroupService.updateLabel(userId, projectId, pipelineLabel))
    }
}
