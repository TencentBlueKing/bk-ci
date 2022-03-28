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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwTemplateResourceV4
import com.tencent.devops.process.api.template.ServicePTemplateResource
import com.tencent.devops.process.api.template.UserPTemplateResource
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.TemplateId
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTemplateResourceV4Impl @Autowired constructor(private val client: Client) : ApigwTemplateResourceV4 {
    override fun listTemplate(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?
    ): Result<TemplateListModel> {
        logger.info("get project's pipeline all template, projectId($projectId) by user $userId")
        return client.get(ServicePTemplateResource::class).listTemplate(
            userId = userId,
            projectId = projectId,
            templateType = templateType,
            storeFlag = storeFlag
        )
    }

    override fun getTemplate(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?
    ): Result<TemplateModelDetail> {
        logger.info("get template, projectId($projectId) templateId($templateId) version($version) by $userId")
        return client.get(ServicePTemplateResource::class).getTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version
        )
    }

    override fun listAllTemplate(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<OptionalTemplateList> {
        logger.info("get project's pipeline all template, projectId($projectId) by user $userId")
        return client.get(ServicePTemplateResource::class).listAllTemplate(
            userId = userId,
            projectId = projectId,
            templateType = null
        )
    }

    override fun createTemplate(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        template: Model
    ): Result<TemplateId> {
        logger.info("createTemplate|userId=$userId|projectId=$projectId")
        return client.get(UserPTemplateResource::class).createTemplate(
            userId = userId,
            projectId = projectId,
            template = template)
    }

    override fun deleteTemplate(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        templateId: String
    ): Result<Boolean> {
        logger.info("DeleteTemplate|userId=$userId|projectId=$projectId|templateId=$templateId")
        return client.get(UserPTemplateResource::class).deleteTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId)
    }

    override fun deleteTemplateVersion(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        templateId: String,
        version: Long
    ): Result<Boolean> {
        logger.info("deleteTemplateVersion|userId=$userId|projectId=$projectId|templateId=$templateId|ver=$version")
        return client.get(UserPTemplateResource::class).deleteTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version
        )
    }

    override fun updateTemplate(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String,
        template: Model
    ): Result<Boolean> {
        logger.info("updateTemplate|userId=$userId|projectId=$projectId|templateId=$templateId|verName=$versionName")
        return client.get(UserPTemplateResource::class).updateTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            versionName = versionName,
            template = template
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTemplateResourceV4Impl::class.java)
    }
}
