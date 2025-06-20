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
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.MAXIMUM_NUMBER_QUEUES_ILLEGAL
import com.tencent.devops.process.constant.ProcessMessageCode.MAXIMUM_QUEUE_LENGTH_ILLEGAL
import com.tencent.devops.process.service.DockerBuildService
import com.tencent.devops.process.service.TXPipelineService
import com.tencent.devops.process.service.pipelineExport.TXPipelineExportService
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN
import jakarta.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired

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

    override fun exportPipelinePreCI(
        userId: String,
        projectId: String,
        pipelineId: String,
        archiveFlag: Boolean?
    ): Response {
        checkParam(userId, projectId)
        checkPipelineId(pipelineId)
        return pipelineService.exportYaml(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            archiveFlag = archiveFlag
        )
    }

    override fun exportPipelineGitCI(
        userId: String,
        projectId: String,
        pipelineId: String,
        archiveFlag: Boolean?
    ): Response {
        checkParam(userId, projectId)
        checkPipelineId(pipelineId)
        return pipelineExportService.exportV2Yaml(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            isGitCI = true,
            archiveFlag = archiveFlag
        )
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
        if (setting.runLockType == PipelineRunLockType.SINGLE ||
                setting.runLockType == PipelineRunLockType.SINGLE_LOCK
        ) {
            validateQueueTime(setting.waitQueueTimeMinute)
            validateQueueSize(setting.maxQueueSize)
        }
    }

    /**
     * 校验等待队列时间是否在有效范围内
     * @param minutes 等待时间(分钟)
     * @throws InvalidParamException 当时间不在有效范围内时抛出
     */
    private fun validateQueueTime(minutes: Int) {
        if (
                minutes !in PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN..
                PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
        ) {
            throw InvalidParamException(
                I18nUtil.getCodeLanMessage(MAXIMUM_QUEUE_LENGTH_ILLEGAL)
            )
        }
    }

    /**
     * 校验最大队列大小是否在有效范围内
     * @param size 队列大小
     * @throws InvalidParamException 当大小不在有效范围内时抛出
     */
    private fun validateQueueSize(size: Int) {
        if (size !in PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN..PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX) {
            throw InvalidParamException(
                I18nUtil.getCodeLanMessage(MAXIMUM_NUMBER_QUEUES_ILLEGAL)
            )
        }
    }
}
