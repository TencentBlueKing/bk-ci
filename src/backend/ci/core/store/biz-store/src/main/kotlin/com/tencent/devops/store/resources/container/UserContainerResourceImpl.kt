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

package com.tencent.devops.store.resources.container

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.container.UserContainerResource
import com.tencent.devops.store.pojo.container.ContainerResource
import com.tencent.devops.store.pojo.container.ContainerResp
import com.tencent.devops.store.pojo.container.ContainerType
import com.tencent.devops.store.service.container.ContainerService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserContainerResourceImpl @Autowired constructor(private val containerService: ContainerService) :
    UserContainerResource {

    override fun getContainerResource(
        userId: String,
        projectCode: String,
        containerId: String,
        os: OS,
        buildType: BuildType
    ): Result<ContainerResource?> {
        return containerService.getContainerResource(userId, projectCode, containerId, os, buildType)
    }

    override fun getContainerResource(
        userId: String,
        projectCode: String,
        os: OS,
        buildType: BuildType
    ): Result<ContainerResource?> {
        return containerService.getContainerResource(userId, projectCode, null, os, buildType)
    }

    override fun getAllContainerInfos(userId: String, projectCode: String): Result<List<ContainerResp>> {
        return containerService.getAllContainerInfos(userId, projectCode, null, null)
    }

    override fun getContainerInfoByType(
        userId: String,
        projectCode: String,
        type: String
    ): Result<List<ContainerResp>> {
        return containerService.getAllContainerInfos(userId, projectCode, type, null)
    }

    override fun getContainerInfoByTypeAndOs(
        userId: String,
        projectCode: String,
        type: String,
        os: OS
    ): Result<List<ContainerResp>> {
        return containerService.getAllContainerInfos(userId, projectCode, type, os)
    }

    override fun getAllContainers(): Result<List<ContainerType>?> {
        return containerService.getAllContainers()
    }
}
