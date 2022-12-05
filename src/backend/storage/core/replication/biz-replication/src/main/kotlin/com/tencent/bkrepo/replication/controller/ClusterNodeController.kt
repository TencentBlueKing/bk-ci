/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.replication.controller

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.replication.pojo.cluster.ClusterListOption
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeCreateRequest
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeInfo
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeType
import com.tencent.bkrepo.replication.service.ClusterNodeService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("集群节点用户接口")
@RestController
@RequestMapping("/api/cluster")
@Principal(PrincipalType.ADMIN)
class ClusterNodeController(
    private val clusterNodeService: ClusterNodeService
) {

    @ApiOperation("根据id查询节点详情")
    @GetMapping("/info/{id}")
    fun getByClusterId(@PathVariable id: String): Response<ClusterNodeInfo?> {
        return ResponseBuilder.success(clusterNodeService.getByClusterId(id))
    }

    @ApiOperation("根据name查询节点详情")
    @GetMapping("/info")
    fun getByClusterName(@RequestParam name: String): Response<ClusterNodeInfo?> {
        return ResponseBuilder.success(clusterNodeService.getByClusterName(name))
    }

    @ApiOperation("获取中心节点")
    @GetMapping("/info/center")
    fun queryCenterNode(): Response<ClusterNodeInfo> {
        return ResponseBuilder.success(clusterNodeService.getCenterNode())
    }

    @ApiOperation("查询边缘节点列表")
    @GetMapping("/list/edge")
    fun listEdgeNodes(): Response<List<ClusterNodeInfo>> {
        return ResponseBuilder.success(clusterNodeService.listEdgeNodes())
    }

    @ApiOperation("查询所有的集群节点")
    @GetMapping("/list")
    fun listClusterNodes(
        name: String? = null,
        type: ClusterNodeType? = null
    ): Response<List<ClusterNodeInfo>> {
        return ResponseBuilder.success(clusterNodeService.listClusterNodes(name, type))
    }

    @ApiOperation("查询所有的集群节点")
    @GetMapping("/page")
    fun listClusterNodesPage(
        option: ClusterListOption
    ): Response<Page<ClusterNodeInfo>> {
        return ResponseBuilder.success(clusterNodeService.listClusterNodesPage(option))
    }

    @ApiOperation("根据名称判断集群节点是否存在")
    @GetMapping("/exist")
    fun existClusterName(
        @RequestParam name: String
    ): Response<Boolean> {
        return ResponseBuilder.success(clusterNodeService.existClusterName(name))
    }

    @ApiOperation("创建集群节点")
    @PostMapping("/create")
    fun create(
        @RequestAttribute userId: String,
        @RequestBody request: ClusterNodeCreateRequest
    ): Response<Void> {
        clusterNodeService.create(userId, request)
        return ResponseBuilder.success()
    }

    @ApiOperation("根据id删除集群节点")
    @DeleteMapping("/delete/{id}")
    fun deleteClusterNode(
        @PathVariable id: String
    ): Response<Void> {
        clusterNodeService.deleteById(id)
        return ResponseBuilder.success()
    }

    @ApiOperation("测试集群间通信")
    @PostMapping("/tryConnect")
    fun tryConnect(
        @RequestParam name: String
    ): Response<Void> {
        clusterNodeService.tryConnect(name)
        return ResponseBuilder.success()
    }
}
