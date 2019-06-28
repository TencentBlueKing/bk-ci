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

package com.tencent.devops.store.service.container

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.store.pojo.container.Container
import com.tencent.devops.store.pojo.container.ContainerRequest
import com.tencent.devops.store.pojo.container.ContainerResource
import com.tencent.devops.store.pojo.container.ContainerResourceValue
import com.tencent.devops.store.pojo.container.ContainerResp

/**
 * 构建容器逻辑类
 *
 * since: 2018-12-20
 */
interface ContainerService {

    /**
     * 获取所有构建容器信息
     */
    fun getAllPipelineContainer(): Result<List<Container>>

    /**
     * 获取构建容器信息
     */
    fun getAllContainerInfos(userId: String, projectCode: String, type: String?, os: OS?): Result<List<ContainerResp>>

    /**
     * 获取容器构建资源信息
     */
    fun getContainerResource(
        userId: String,
        projectCode: String,
        containerOS: OS,
        buildType: BuildType
    ): Result<ContainerResourceValue?>

    /**
     * 获取容器构建资源信息
     */
    fun getContainerResource(
        userId: String,
        projectCode: String,
        containerId: String?,
        containerOS: OS,
        buildType: BuildType
    ): Result<ContainerResource?>

    /**
     * 获取构建容器信息
     */
    fun getPipelineContainer(id: String): Result<Container?>

    /**
     * 保存构建容器信息
     */
    fun savePipelineContainer(containerRequest: ContainerRequest): Result<Boolean>

    /**
     * 更新构建容器信息
     */
    fun updatePipelineContainer(id: String, containerRequest: ContainerRequest): Result<Boolean>

    /**
     * 删除构建容器信息
     */
    fun deletePipelineContainer(id: String): Result<Boolean>
}