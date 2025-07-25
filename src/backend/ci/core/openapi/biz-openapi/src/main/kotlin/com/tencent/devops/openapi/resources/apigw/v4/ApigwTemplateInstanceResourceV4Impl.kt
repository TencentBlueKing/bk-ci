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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwTemplateInstanceResourceV4
import com.tencent.devops.openapi.utils.ApigwParamUtil
import com.tencent.devops.process.api.template.ServiceTemplateInstanceResource
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstancePage
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTemplateInstanceResourceV4Impl @Autowired constructor(private val client: Client) :
    ApigwTemplateInstanceResourceV4 {

    override fun createTemplateInstances(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceCreate>
    ): TemplateOperationRet {
        logger.info(
            "OPENAPI_TEMPLATE_INSTANCE_V4|$userId|create template instances|$projectId|$templateId|$version" +
                "|$useTemplateSettings|$instances"
        )
        return client.get(ServiceTemplateInstanceResource::class).createTemplateInstances(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version,
            useTemplateSettings = useTemplateSettings,
            instances = instances
        )
    }

    override fun updateTemplateInstances(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet {
        logger.info(
            "OPENAPI_TEMPLATE_INSTANCE_V4|$userId|update template instances by version|$projectId|$templateId" +
                "|$version|$useTemplateSettings|$instances"
        )
        return client.get(ServiceTemplateInstanceResource::class).updateTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version,
            useTemplateSettings = useTemplateSettings,
            instances = instances
        )
    }

    override fun updateTemplateInstances(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        templateId: String,
        versionName: String,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet {
        logger.info(
            "OPENAPI_TEMPLATE_INSTANCE_V4|$userId|update template instances by versionName|$projectId|$templateId" +
                "|$versionName|$useTemplateSettings|$instances"
        )
        return client.get(ServiceTemplateInstanceResource::class).updateTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            versionName = versionName,
            useTemplateSettings = useTemplateSettings,
            instances = instances
        )
    }

    override fun listTemplateInstances(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        templateId: String,
        page: Int?,
        pageSize: Int?,
        searchKey: String?,
        sortType: TemplateSortTypeEnum?,
        desc: Boolean?
    ): Result<TemplateInstancePage> {
        logger.info(
            "OPENAPI_TEMPLATE_INSTANCE_V4|$userId|list template instances|$projectId|$templateId|$page" +
                "|$pageSize|$searchKey|$sortType|$desc"
        )
        return client.get(ServiceTemplateInstanceResource::class).listTemplate(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            page = page ?: 1,
            pageSize = ApigwParamUtil.standardSize(pageSize) ?: 20,
            searchKey = searchKey,
            sortType = sortType,
            desc = desc
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTemplateInstanceResourceV4Impl::class.java)
    }
}
