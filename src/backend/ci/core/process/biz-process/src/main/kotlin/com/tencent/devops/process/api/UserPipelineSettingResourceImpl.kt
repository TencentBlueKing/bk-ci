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

package com.tencent.devops.process.api

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bk.audit.annotations.AuditRequestBody
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPipelineSettingResource
import com.tencent.devops.process.pojo.setting.PipelineCommonSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineSettingResourceImpl @Autowired constructor(
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService
) : UserPipelineSettingResource {

    @AuditEntry(actionId = ActionId.PIPELINE_EDIT)
    override fun saveSetting(
        userId: String,
        @AuditRequestBody
        setting: PipelineSetting
    ): Result<String> {
        val savedSetting = pipelineSettingFacadeService.saveSetting(
            userId = userId,
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            setting = setting
        )
        pipelineInfoFacadeService.updatePipelineSettingVersion(
            userId = userId,
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            operationLogType = OperationLogType.UPDATE_PIPELINE_SETTING,
            savedSetting = savedSetting
        )
        return Result(savedSetting.pipelineId)
    }

    override fun getSetting(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Result<PipelineSetting> {
        return Result(pipelineSettingFacadeService.userGetSetting(userId, projectId, pipelineId, version = version))
    }

    override fun getCommonSetting(userId: String): Result<PipelineCommonSetting> {
        return Result(pipelineSettingFacadeService.getCommonSetting(userId))
    }
}
