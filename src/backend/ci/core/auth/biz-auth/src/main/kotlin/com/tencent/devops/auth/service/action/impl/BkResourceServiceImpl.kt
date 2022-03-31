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

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.ResourceDao
import com.tencent.devops.auth.pojo.resource.CreateResourceDTO
import com.tencent.devops.auth.pojo.resource.ResourceInfo
import com.tencent.devops.auth.pojo.resource.UpdateResourceDTO
import com.tencent.devops.auth.service.action.BkResourceService
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class BkResourceServiceImpl @Autowired constructor(
    open val dslContext: DSLContext,
    open val resourceDao: ResourceDao
): BkResourceService{
    override fun createResource(userId: String, resource: CreateResourceDTO): Boolean {
        // 判断此资源是否存在, 存在直接报错
        if (resourceDao.getResourceById(dslContext, resource.resourceId) != null) {
            logger.warn("createResource $resource exist")
        }
        // 添加资源类数据
        resourceDao.createResource(dslContext, userId, resource)

        // 此处为扩展类,操作蓝盾外的其他系统
        createExtSystem(resource)
        return true
    }

    override fun updateResource(
        userId: String,
        resourceId: String,
        resource: UpdateResourceDTO
    ): Boolean {
        // 判断此资源是否存在
        resourceDao.getResourceById(dslContext, resourceId)
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.RESOURCE_NOT_EXSIT
            )

        // 修改资源类数据
        resourceDao.updateResource(dslContext, userId, resourceId, resource)
        updateExtSystem(resource, resourceId)
        return true
    }

    override fun getResource(resourceType: String): ResourceInfo? {
        return resourceDao.getResourceById(dslContext, resourceType)
    }

    override fun getResourceBySystem(systemId: String): List<ResourceInfo>? {
        val records = resourceDao.getResourceBySystemId(dslContext, systemId) ?: return emptyList()
        val result = mutableListOf<ResourceInfo>()
        records.map {
            val resourceInfo = resourceDao.convert(it)
            result.add(resourceInfo!!)
        }
        return result
    }

    override fun resourceList(): List<ResourceInfo>? {
        val records = resourceDao.getAllResource(dslContext) ?: return emptyList()
        val result = mutableListOf<ResourceInfo>()
        records.map {
            val resourceInfo = resourceDao.convert(it)
            result.add(resourceInfo!!)
        }
        return result
    }

    abstract fun createExtSystem(resource: CreateResourceDTO)

    abstract fun updateExtSystem(resource: UpdateResourceDTO, resourceType: String)

    companion object {
        private val logger = LoggerFactory.getLogger(BkResourceServiceImpl::class.java)
    }
}