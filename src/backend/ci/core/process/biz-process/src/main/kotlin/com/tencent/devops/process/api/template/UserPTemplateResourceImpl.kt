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

package com.tencent.devops.process.api.template

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.template.CopyTemplateReq
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.SaveAsTemplateReq
import com.tencent.devops.process.pojo.template.TemplateId
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN
import org.springframework.beans.factory.annotation.Autowired

/**
 * deng
 * 2019-01-08
 */
@RestResource
class UserPTemplateResourceImpl @Autowired constructor(private val templateFacadeService: TemplateFacadeService) :
    UserPTemplateResource {

    override fun createTemplate(userId: String, projectId: String, template: Model): Result<TemplateId> {
        return Result(TemplateId(templateFacadeService.createTemplate(projectId, userId, template)))
    }

    override fun deleteTemplate(userId: String, projectId: String, templateId: String): Result<Boolean> {
        return Result(templateFacadeService.deleteTemplate(projectId, userId, templateId))
    }

    override fun deleteTemplate(userId: String, projectId: String, templateId: String, version: Long): Result<Boolean> {
        return Result(templateFacadeService.deleteTemplate(
            projectId = projectId,
            userId = userId,
            templateId = templateId,
            version = version
        ))
    }

    override fun deleteTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String
    ): Result<Boolean> {
        return Result(templateFacadeService.deleteTemplate(
            projectId = projectId,
            userId = userId,
            templateId = templateId,
            versionName = versionName
        ))
    }

    override fun updateTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String,
        template: Model
    ): Result<Boolean> {
        return Result(templateFacadeService.updateTemplate(projectId, userId, templateId, versionName, template) > 0)
    }

    override fun listTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<TemplateListModel> {
        return Result(templateFacadeService.listTemplate(projectId, userId, templateType, storeFlag, page, pageSize))
    }

    override fun listAllTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        page: Int?,
        pageSize: Int?
    ): Result<OptionalTemplateList> {
        return Result(templateFacadeService.listAllTemplate(projectId, templateType, null, page, pageSize))
    }

    override fun getTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?
    ): Result<TemplateModelDetail> {
        return Result(templateFacadeService.getTemplate(projectId, userId, templateId, version))
    }

    @Suppress("ALL")
    override fun updateTemplateSetting(
        userId: String,
        projectId: String,
        templateId: String,
        setting: PipelineSetting
    ): Result<Boolean> {
        if (setting.runLockType == PipelineRunLockType.SINGLE ||
            setting.runLockType == PipelineRunLockType.SINGLE_LOCK) {
            if (setting.waitQueueTimeMinute < PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN ||
                setting.waitQueueTimeMinute > PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
            ) {
                throw InvalidParamException(I18nUtil.getCodeLanMessage(ProcessMessageCode.MAXIMUM_QUEUE_LENGTH_ILLEGAL))
            }
            if (setting.maxQueueSize < PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN ||
                setting.maxQueueSize > PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
            ) {
                throw InvalidParamException(
                    I18nUtil.getCodeLanMessage(ProcessMessageCode.MAXIMUM_NUMBER_QUEUES_ILLEGAL)
                )
            }
        }
        return Result(templateFacadeService.updateTemplateSetting(projectId, userId, templateId, setting))
    }

    override fun getTemplateSetting(
        userId: String,
        projectId: String,
        templateId: String
    ): Result<PipelineSetting> {
        return Result(templateFacadeService.getTemplateSetting(projectId, userId, templateId))
    }

    override fun copyTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        copyTemplateReq: CopyTemplateReq
    ): Result<TemplateId> {
        return Result(TemplateId(templateFacadeService.copyTemplate(userId, projectId, templateId, copyTemplateReq)))
    }

    override fun saveAsTemplate(
        userId: String,
        projectId: String,
        saveAsTemplateReq: SaveAsTemplateReq
    ): Result<TemplateId> {
        return Result(TemplateId(templateFacadeService.saveAsTemplate(userId, projectId, saveAsTemplateReq)))
    }

    override fun hasManagerPermission(userId: String, projectId: String): Result<Boolean> {
        return Result(templateFacadeService.hasManagerPermission(projectId, userId))
    }
}
