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

package com.tencent.devops.auth.service.action.impl

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.ProviderConfigDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceTypeDTO
import com.tencent.bk.sdk.iam.service.IamResourceService
import com.tencent.devops.auth.dao.ResourceDao
import com.tencent.devops.auth.pojo.resource.CreateResourceDTO
import com.tencent.devops.auth.pojo.resource.UpdateResourceDTO
import com.tencent.devops.common.auth.api.AuthResourceType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class IamBkResourceServiceImpl @Autowired constructor(
    override val dslContext: DSLContext,
    override val resourceDao: ResourceDao,
    val iamConfiguration: IamConfiguration,
    val resourceService: IamResourceService
): BkResourceServiceImpl(dslContext, resourceDao) {

    @Value("")
    val projectCallbackPath = ""

    @Value("")
    val otherResourceCallbackPath = ""

    override fun createExtSystem(resource: CreateResourceDTO) {
        logger.info("createExtSystem $resource")
        // 1. 创建资源类型
        val resourceInfo = ResourceTypeDTO()
        resourceInfo.id = resource.resourceId
        resourceInfo.name = resource.name
        resourceInfo.englishName = resource.englishName
        resourceInfo.description = resource.desc
        resourceInfo.englishDescription = resource.englishDes
        if (resource.resourceId == AuthResourceType.PROJECT.value) {
            resourceInfo.parent = null
            val path = ProviderConfigDTO()
            path.path = projectCallbackPath
            resourceInfo.providerConfig = path
        } else {
            val projectResource = ResourceDTO.builder()
                .system(iamConfiguration.systemId)
                .id(AuthResourceType.PROJECT.value)
                .build()
            resourceInfo.parent = arrayListOf(projectResource)
            val path = ProviderConfigDTO()
            path.path = otherResourceCallbackPath
            resourceInfo.providerConfig = path
        }
        val result = resourceService.createResource(resourceInfo)
        logger.info("createExtSystem createResource:$result")
        // 2. 资源视图
    }

    override fun updateExtSystem(resource: UpdateResourceDTO, resourceType: String) {
        val resourceInfo = ResourceTypeDTO()
        resourceInfo.id = resourceType
        resourceInfo.name = resource.name
        resourceInfo.englishName = resource.englishName
        resourceInfo.description = resource.desc
        resourceInfo.englishDescription = resource.englishDes
        val result = resourceService.updateResource(resourceInfo, resourceType)
        logger.info("updateExtSystem createResource:$result")
    }

    companion object {
        val logger = LoggerFactory.getLogger(IamBkResourceServiceImpl::class.java)
    }
}