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
import com.tencent.bk.sdk.iam.dto.SelectionDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceTypeChainDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceTypeDTO
import com.tencent.bk.sdk.iam.service.IamResourceService
import com.tencent.devops.auth.dao.ResourceDao
import com.tencent.devops.auth.pojo.resource.CreateResourceDTO
import com.tencent.devops.auth.pojo.resource.ResourceInfo
import com.tencent.devops.auth.pojo.resource.UpdateResourceDTO
import com.tencent.devops.common.api.util.DateTimeUtil
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

    @Value("\${iam.selector.project:#{null}}")
    val projectCallbackPath = "/api/service/auth/resource/projects"

    @Value("\${iam.selector.other:#{null}}")
    val otherResourceCallbackPath = "/api/service/auth/resource/instances/list"

    override fun createExtSystem(resource: CreateResourceDTO) {
        logger.info("createExtSystem $resource")
        val resourceInfo = ResourceInfo(
            resourceId = resource.resourceId,
            name = resource.name,
            englishName = resource.englishName,
            desc = resource.desc,
            englishDes = resource.englishDes,
            parent = resource.parent,
            system = resource.system,
            creator = "",
            updator = null,
            creatorTime = 0L,
            updateTime = null
        )
        // 1. 创建资源类型
        createIamResource(resource)

        // 2. 资源视图
        buildIamResourceSelectorInstance(resourceInfo)
    }

    override fun updateExtSystem(resource: UpdateResourceDTO, resourceType: String) {
        val updateResourceInfo = ResourceTypeDTO()
        updateResourceInfo.id = resourceType
        updateResourceInfo.name = resource.name
        updateResourceInfo.englishName = resource.englishName
        updateResourceInfo.description = resource.desc
        updateResourceInfo.englishDescription = resource.englishDes
        val result = resourceService.updateResource(updateResourceInfo, resourceType)
        logger.info("updateExtSystem createResource:$result")

        val resourceInfo = ResourceInfo(
            resourceId = resourceType,
            name = resource.name,
            englishName = resource.englishName,
            desc = resource.desc,
            englishDes = resource.englishDes,
            parent = resource.parent,
            system = resource.system,
            creator = "",
            updator = null,
            creatorTime = 0L,
            updateTime = null
        )
        // 2. 资源视图
        buildIamResourceSelectorInstance(resourceInfo)
    }

    private fun createIamResource(resource: CreateResourceDTO) {
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
                // TODO: 系统id换回ci
                .system(systemId)
//                .system(iamConfiguration.systemId)
                .id(AuthResourceType.PROJECT.value)
                .build()
            resourceInfo.parent = arrayListOf(projectResource)
            val path = ProviderConfigDTO()
            path.path = otherResourceCallbackPath
            resourceInfo.providerConfig = path
        }
        val resourceInfos = mutableListOf<ResourceTypeDTO>()
        logger.info("createIamResource $resourceInfo")
        resourceInfos.add(resourceInfo)
        val result = resourceService.createResource(resourceInfos)
        logger.info("createExtSystem createResource:$result")
    }

    private fun buildIamResourceSelectorInstance(resource: ResourceInfo) {
        val selectInstance = resourceService.systemInstanceSelector
        val resourceSelectId = resource.resourceId + INSTANCELABLE
        val projectSelect = ResourceTypeChainDTO()
        projectSelect.id = AuthResourceType.PROJECT.value + INSTANCELABLE
        projectSelect.systemId = systemId

        var create = true
        // 如果存在视图做修改,不存在做新增
        selectInstance?.forEach {
            if (it.id.equals(resourceSelectId)) {
                create = false
                return@forEach
            }
        }

        if (create) {
            val createSelectionDTO = buildResourceSelector(resource, projectSelect)
            logger.info("buildIamResourceSelectorInstance create $createSelectionDTO")
            resourceService.createResourceInstanceSelector(arrayListOf(createSelectionDTO))
        } else {
            val updateSelectionDTO = buildResourceSelector(resource, projectSelect)
            logger.info("buildIamResourceSelectorInstance update $updateSelectionDTO")
            resourceService.updateResourceInstanceSelector(resourceSelectId, updateSelectionDTO)
        }
    }

    private fun buildResourceSelector(
        resource: ResourceInfo,
        projectChain: ResourceTypeChainDTO
    ): SelectionDTO {
        val resourceTypeChains = mutableListOf<ResourceTypeChainDTO>()
        // 所有的视图第一级都是project
        resourceTypeChains.add(projectChain)
        val selectionDTO = SelectionDTO()
        selectionDTO.id = resource.resourceId + INSTANCELABLE
        selectionDTO.name = resource.name
        selectionDTO.englishName = resource.englishName
        // 非project资源，追加对应资源的二级视图
        if (resource.resourceId != AuthResourceType.PROJECT.value) {
            val otherSelect = ResourceTypeChainDTO()
            otherSelect.id = resource.resourceId
            otherSelect.systemId = systemId
            resourceTypeChains.add(otherSelect)
        }
        selectionDTO.resourceTypeChain = resourceTypeChains
        logger.info("buildResourceSelector $selectionDTO")
        return selectionDTO
    }

    companion object {
        val systemId = "fitz_test"
        val INSTANCELABLE = "_instance"
        val logger = LoggerFactory.getLogger(IamBkResourceServiceImpl::class.java)
    }
}