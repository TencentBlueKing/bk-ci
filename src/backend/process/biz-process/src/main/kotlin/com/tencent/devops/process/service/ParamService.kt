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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.store.api.ServiceContainerResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * deng
 * 2019-03-04
 */
@Service
class ParamService @Autowired constructor(
    private val client: Client
) {

    fun filterParams(userId: String?, projectId: String, params: List<BuildFormProperty>): List<BuildFormProperty> {
        val filterParams = mutableListOf<BuildFormProperty>()
        params.forEach {
            if (it.type == BuildFormPropertyType.CONTAINER_TYPE && it.containerType != null) {
                filterParams.add(addContainerTypeProperties(userId, projectId, it))
            } else {
                filterParams.add(it)
            }
        }

        return filterParams
    }

    private fun addContainerTypeProperties(
        userId: String?,
        projectId: String,
        property: BuildFormProperty
    ): BuildFormProperty {
        try {
            if (userId.isNullOrBlank()) {
                logger.warn("The user id if empty for the container type properties")
                return property
            }
            val containerType = property.containerType!!
            val containers = client.get(ServiceContainerResource::class)
                .getContainers(userId!!, projectId, containerType.buildType, containerType.os)
            if (containers.data == null || containers.data!!.resources == null) {
                logger.warn("[$userId|$projectId|$property] Fail to get the container properties")
                return property
            }
            val containerValue = containers.data!!.resources!!.map {
                BuildFormValue(it, it)
            }.toList()
            return copyFormProperty(property, containerValue)
        } catch (ignored: Throwable) {
            logger.warn("[$userId|$projectId|$property] Fail to get the pcg container images", ignored)
        }
        return property
    }

    private fun copyFormProperty(property: BuildFormProperty, options: List<BuildFormValue>): BuildFormProperty {
        return BuildFormProperty(
            property.id,
            property.required,
            property.type,
            property.defaultValue,
            options,
            property.desc,
            property.repoHashId,
            property.relativePath,
            property.scmType,
            property.containerType
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ParamService::class.java)
    }
}