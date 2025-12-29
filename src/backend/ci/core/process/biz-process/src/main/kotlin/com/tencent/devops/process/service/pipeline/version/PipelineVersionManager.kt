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

package com.tencent.devops.process.service.pipeline.version

import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.version.PipelineVersionCreateReq
import com.tencent.devops.process.service.pipeline.version.convert.PipelineVersionCreateReqConverter
import com.tencent.devops.process.service.pipeline.version.handler.PipelineVersionCreateHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineVersionManager @Autowired constructor(
    private val versionValidator: PipelineVersionValidator,
    private val versionCreateReqConverters: List<PipelineVersionCreateReqConverter>,
    private val versionCreateHandlers: List<PipelineVersionCreateHandler>
) {

    fun deployPipeline(
        userId: String,
        projectId: String,
        pipelineId: String? = null,
        version: Int? = null,
        request: PipelineVersionCreateReq
    ): DeployPipelineResult {
        val context = getConverter(request).convert(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            request = request
        )
        versionValidator.validate(context = context)
        return getHandler(context).handle(context = context)
    }

    private fun getHandler(context: PipelineVersionCreateContext): PipelineVersionCreateHandler {
        return versionCreateHandlers.find { it.support(context) }
            ?: throw IllegalArgumentException("Unsupported version event: ${context.versionAction}")
    }

    private fun getConverter(request: PipelineVersionCreateReq): PipelineVersionCreateReqConverter {
        return versionCreateReqConverters.find { it.support(request) }
            ?: throw IllegalArgumentException("Unsupported version request: $request")
    }
}
