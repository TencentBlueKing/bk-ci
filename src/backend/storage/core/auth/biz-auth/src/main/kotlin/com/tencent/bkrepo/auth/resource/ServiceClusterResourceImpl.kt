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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.auth.resource

import com.tencent.bkrepo.auth.api.ServiceClusterResource
import com.tencent.bkrepo.auth.pojo.AddClusterRequest
import com.tencent.bkrepo.auth.pojo.Cluster
import com.tencent.bkrepo.auth.pojo.UpdateClusterRequest
import com.tencent.bkrepo.auth.service.ClusterService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceClusterResourceImpl @Autowired constructor(
    private val clusterService: ClusterService
) : ServiceClusterResource {

    override fun add(request: AddClusterRequest): Response<Boolean> {
        return ResponseBuilder.success(clusterService.addCluster(request))
    }

    override fun list(): Response<List<Cluster>> {
        return ResponseBuilder.success(clusterService.listCluster())
    }

    override fun ping(clusterId: String): Response<Boolean> {
        return ResponseBuilder.success(clusterService.ping(clusterId))
    }

    override fun delete(clusterId: String): Response<Boolean> {
        return ResponseBuilder.success(clusterService.delete(clusterId))
    }

    override fun update(clusterId: String, request: UpdateClusterRequest): Response<Boolean> {
        return ResponseBuilder.success(clusterService.updateCluster(clusterId, request))
    }

    override fun credential(): Response<Boolean> {
        return ResponseBuilder.success(true)
    }
}
