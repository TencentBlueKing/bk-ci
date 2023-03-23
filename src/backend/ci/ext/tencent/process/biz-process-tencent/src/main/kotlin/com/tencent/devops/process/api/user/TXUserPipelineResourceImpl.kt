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

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.ILLEGAL_MAXIMUM_NUMBER
import com.tencent.devops.process.constant.ProcessMessageCode.ILLEGAL_MAXIMUM_QUEUE_LENGTH
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.DockerBuildService
import com.tencent.devops.process.service.TXPipelineService
import com.tencent.devops.process.service.pipelineExport.TXPipelineExportService
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class TXUserPipelineResourceImpl @Autowired constructor(
    private val dockerBuildService: DockerBuildService,
    private val pipelineService: TXPipelineService,
    private val pipelineExportService: TXPipelineExportService
) : TXUserPipelineResource {

    override fun enableDockerBuild(userId: String, projectId: String): Result<Boolean> {
        checkParam(userId, projectId)
        return Result(dockerBuildService.isEnable(userId, projectId))
    }

    override fun exportPipelinePreCI(userId: String, projectId: String, pipelineId: String): Response {
        checkParam(userId, projectId)
        checkPipelineId(pipelineId)
        return pipelineService.exportYaml(userId, projectId, pipelineId)
    }

    override fun exportPipelineGitCI(userId: String, projectId: String, pipelineId: String): Response {
        checkParam(userId, projectId)
        checkPipelineId(pipelineId)
        return pipelineExportService.exportV2Yaml(userId, projectId, pipelineId, true)
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

    private fun checkParam(setting: PipelineSetting) {
        if (setting.runLockType == PipelineRunLockType.SINGLE || setting.runLockType == PipelineRunLockType.SINGLE_LOCK) {
            if (setting.waitQueueTimeMinute < PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN ||
                    setting.waitQueueTimeMinute > PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
            ) {
                throw InvalidParamException(
                    MessageUtil.getMessageByLocale(
                        messageCode = ILLEGAL_MAXIMUM_QUEUE_LENGTH,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    )
                )
            }
            if (setting.maxQueueSize < PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN ||
                    setting.maxQueueSize > PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
            ) {
                throw InvalidParamException(
                    MessageUtil.getMessageByLocale(
                        messageCode = ILLEGAL_MAXIMUM_NUMBER,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    )
                )
            }
        }
    }
}
