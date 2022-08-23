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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.PipelineExportV2YamlData
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.process.service.PipelineListFacadeService
import com.tencent.devops.process.service.TXPipelineExportService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTXPipelineResourceImpl @Autowired constructor(
    private val pipelineExportService: TXPipelineExportService,
    val pipelineListFacadeService: PipelineListFacadeService
) : ServiceTXPipelineResource {
    override fun exportPipelineGitCI(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<PipelineExportV2YamlData> {
        checkParam(userId, projectId)
        checkPipelineId(pipelineId)
        return Result(pipelineExportService.exportV2YamlStr(userId, projectId, pipelineId, true))
    }

    override fun getPipelineInfobyId(id: Int): Result<SimplePipeline> {
        return Result(pipelineListFacadeService.getByAutoIds(listOf(id))[0])
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    private fun checkPipelineId(pipelineId: String) {
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
    }
}
