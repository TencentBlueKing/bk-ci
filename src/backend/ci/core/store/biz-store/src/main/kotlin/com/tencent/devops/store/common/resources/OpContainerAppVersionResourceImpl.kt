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

package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.container.OpContainerAppVersionResource
import com.tencent.devops.store.pojo.app.ContainerAppVersion
import com.tencent.devops.store.pojo.app.ContainerAppVersionCreate
import com.tencent.devops.store.common.service.ContainerAppVersionService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpContainerAppVersionResourceImpl @Autowired constructor(
    private val containerAppVersionService: ContainerAppVersionService
) : OpContainerAppVersionResource {

    override fun addContainerAppVersion(containerAppVersionRequest: ContainerAppVersionCreate): Result<Boolean> {
        return containerAppVersionService.saveContainerAppVersion(containerAppVersionRequest)
    }

    override fun deleteContainerAppVersionById(id: Int): Result<Boolean> {
        return containerAppVersionService.deleteContainerAppVersion(id)
    }

    override fun updateContainerAppVersion(
        id: Int,
        containerAppVersionRequest: ContainerAppVersionCreate
    ): Result<Boolean> {
        return containerAppVersionService.updateContainerAppVersion(id, containerAppVersionRequest)
    }

    override fun listContainerAppVersionsByAppId(appId: Int): Result<List<ContainerAppVersion>> {
        return containerAppVersionService.listByAppId(appId)
    }

    override fun getContainerAppVersionById(id: Int): Result<ContainerAppVersion?> {
        return containerAppVersionService.getContainerAppVersion(id)
    }
}
