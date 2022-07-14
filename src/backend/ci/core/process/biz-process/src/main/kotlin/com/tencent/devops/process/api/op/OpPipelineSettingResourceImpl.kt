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

package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineSettingResourceImpl @Autowired constructor(
    private val pipelineSettingFacadeService: PipelineSettingFacadeService
) : OpPipelineSettingResource {
    override fun updateSetting(userId: String, setting: PipelineSetting): Result<String> {
        return Result(pipelineSettingFacadeService.saveSetting(userId = userId, setting = setting))
    }

    override fun getSetting(userId: String, projectId: String, pipelineId: String): Result<PipelineSetting> {
        return Result(pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        ))
    }

    override fun updateMaxConRunningQueueSize(pipelineId: String, maxConRunningQueueSize: Int): Result<String> {
        checkMaxConRunningQueueSize(maxConRunningQueueSize)
        return Result(pipelineSettingFacadeService.updateMaxConRunningQueueSize(
            pipelineId = pipelineId,
            maxConRunningQueueSize = maxConRunningQueueSize
        ))
    }

    private fun checkMaxConRunningQueueSize(maxConRunningQueueSize: Int) {
        if (maxConRunningQueueSize <= PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN ||
            maxConRunningQueueSize > PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
        ) {
            throw InvalidParamException("最大并发数量非法", params = arrayOf("maxConRunningQueueSize"))
        }
    }
}
