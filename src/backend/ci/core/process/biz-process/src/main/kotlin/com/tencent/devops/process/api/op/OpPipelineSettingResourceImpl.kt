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

package com.tencent.devops.process.api.op

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.PROJECT_NOT_EXIST
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.project.api.op.OPProjectResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectProperties
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineSettingResourceImpl @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineSettingDao: PipelineSettingDao
) : OpPipelineSettingResource {

    private val logger = LoggerFactory.getLogger(OpPipelineSettingResourceImpl::class.java)

    @AuditEntry(actionId = ActionId.PIPELINE_EDIT)
    override fun updateSetting(userId: String, setting: PipelineSetting): Result<String> {
        return Result(
            pipelineSettingFacadeService.saveSetting(
                userId = userId,
                projectId = setting.projectId,
                pipelineId = setting.pipelineId,
                setting = setting
            ).pipelineId
        )
    }

    override fun getSetting(userId: String, projectId: String, pipelineId: String): Result<PipelineSetting> {
        return Result(
            pipelineSettingFacadeService.userGetSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId
            )
        )
    }

    override fun updateMaxConRunningQueueSize(
        userId: String,
        projectId: String,
        pipelineId: String,
        maxConRunningQueueSize: Int
    ): Result<String> {
        return Result(
            pipelineSettingFacadeService.updateMaxConRunningQueueSize(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                maxConRunningQueueSize = maxConRunningQueueSize
            )
        )
    }

    override fun updatePipelineAsCodeSettings(
        userId: String,
        projectId: String,
        pipelineId: String?,
        pipelineAsCodeSettings: PipelineAsCodeSettings
    ): Result<Int> {
        logger.info(
            "[$projectId]|updatePipelineAsCodeSettings|userId=$userId|" +
                "pipelineId=$pipelineId|$pipelineAsCodeSettings"
        )
        if (pipelineId.isNullOrBlank()) {
            val projectVO = client.get(ServiceProjectResource::class).get(projectId).data
                ?: throw ExecuteException(
                    MessageUtil.getMessageByLocale(PROJECT_NOT_EXIST, I18nUtil.getLanguage(userId))
                )
            val success = client.get(OPProjectResource::class).setProjectProperties(
                userId = userId,
                projectCode = projectId,
                properties = projectVO.properties?.copy(
                    pipelineAsCodeSettings = pipelineAsCodeSettings
                ) ?: ProjectProperties(pipelineAsCodeSettings)
            ).data == true
            if (!success) return Result(0)
        }
        return Result(
            pipelineSettingDao.updatePipelineAsCodeSettings(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineAsCodeSettings = pipelineAsCodeSettings
            )
        )
    }

    override fun updateBuildMetricsSettings(userId: String, projectId: String, enabled: Boolean): Result<Boolean> {
        logger.info(
            "[$projectId]|updateBuildMetricsSettings|userId=$userId|$enabled"
        )
        val projectVO = client.get(ServiceProjectResource::class).get(projectId).data
            ?: throw ExecuteException(
                MessageUtil.getMessageByLocale(PROJECT_NOT_EXIST, I18nUtil.getLanguage(userId))
            )
        val success = client.get(OPProjectResource::class).setProjectProperties(
            userId = userId,
            projectCode = projectId,
            properties = projectVO.properties?.copy(
                buildMetrics = enabled
            ) ?: ProjectProperties(buildMetrics = enabled)
        ).data == true
        return Result(success)
    }
}
