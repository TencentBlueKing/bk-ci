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

package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.repository.constant.SERVICE_NAME
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import com.tencent.bkrepo.repository.pojo.share.ShareRecordInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * 资源节点服务接口
 */
@Api("节点服务接口")
@Primary
@FeignClient(SERVICE_NAME, contextId = "NodeResource")
@RequestMapping("/service/node")
interface NodeClient {

    @ApiOperation("根据路径查看节点详情")
    @GetMapping("/query/{projectId}/{repoName}/{repoType}")
    fun detail(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "仓库类型", required = true)
        @PathVariable repoType: String,
        @ApiParam(value = "节点完整路径", required = true)
        @RequestParam fullPath: String
    ): Response<NodeDetail?>

    @ApiOperation("根据路径查看节点详情")
    @GetMapping("/query/{projectId}/{repoName}")
    fun detail(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "节点完整路径", required = true)
        @RequestParam fullPath: String
    ): Response<NodeDetail?>

    @ApiOperation("根据路径查看节点是否存在")
    @GetMapping("/exist/{projectId}/{repoName}")
    fun exist(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "节点完整路径", required = true)
        @RequestParam fullPath: String
    ): Response<Boolean>

    @ApiOperation("列出仓库中已存在的节点")
    @PostMapping("/exist/list/{projectId}/{repoName}")
    fun listExistFullPath(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @RequestBody fullPathList: List<String>
    ): Response<List<String>>

    @ApiOperation("列表查询指定目录下所有节点")
    @GetMapping("/list/{projectId}/{repoName}")
    fun list(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "所属目录", required = true)
        @RequestParam path: String,
        @ApiParam(value = "是否包含目录", required = false, defaultValue = "true")
        @RequestParam includeFolder: Boolean = true,
        @ApiParam(value = "是否深度查询文件", required = false, defaultValue = "false")
        @RequestParam deep: Boolean = false
    ): Response<List<NodeInfo>>

    @ApiOperation("分页查询指定目录下所有节点")
    @GetMapping("/page/{projectId}/{repoName}/{page}/{size}")
    fun page(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "当前页", required = true, example = "0")
        @PathVariable page: Int,
        @ApiParam(value = "分页大小", required = true, example = "20")
        @PathVariable size: Int,
        @ApiParam(value = "所属目录", required = true)
        @RequestParam path: String,
        @ApiParam(value = "是否包含目录", required = false, defaultValue = "true")
        @RequestParam includeFolder: Boolean = true,
        @ApiParam("是否包含元数据", required = false, defaultValue = "false")
        @RequestParam includeMetadata: Boolean = false,
        @ApiParam(value = "是否深度查询文件", required = false, defaultValue = "false")
        @RequestParam deep: Boolean = false
    ): Response<Page<NodeInfo>>

    @ApiOperation("创建节点")
    @PostMapping
    fun create(
        @RequestBody nodeCreateRequest: NodeCreateRequest
    ): Response<NodeDetail>

    @ApiOperation("重命名节点")
    @PutMapping("/rename")
    fun rename(
        @RequestBody nodeRenameRequest: NodeRenameRequest
    ): Response<Void>

    @ApiOperation("更新节点")
    @PutMapping("/update")
    fun update(
        @RequestBody nodeUpdateRequest: NodeUpdateRequest
    ): Response<Void>

    @ApiOperation("移动节点")
    @PutMapping("/move")
    fun move(
        @RequestBody nodeMoveRequest: NodeMoveRequest
    ): Response<Void>

    @ApiOperation("复制节点")
    @PutMapping("/copy")
    fun copy(
        @RequestBody nodeCopyRequest: NodeCopyRequest
    ): Response<Void>

    @ApiOperation("删除节点")
    @DeleteMapping("/delete")
    fun delete(
        @RequestBody nodeDeleteRequest: NodeDeleteRequest
    ): Response<Void>

    @ApiOperation("查询节点大小信息")
    @GetMapping("/size/{projectId}/{repoName}")
    fun computeSize(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "节点完整路径", required = true)
        @RequestParam fullPath: String
    ): Response<NodeSizeInfo>

    @ApiOperation("查询文件节点数量")
    @GetMapping("/file/{projectId}/{repoName}")
    fun countFileNode(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "节点完整路径", required = true)
        @RequestParam path: String
    ): Response<Long>

    @ApiOperation("列表查询分享链接")
    @GetMapping("/share/list/{projectId}/{repoName}")
    fun listShareRecord(
        @ApiParam(value = "所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam(value = "仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam(value = "节点完整路径", required = true)
        @RequestParam fullPath: String
    ): Response<List<ShareRecordInfo>>

    @ApiOperation("自定义查询节点")
    @PostMapping("/query")
    fun query(@RequestBody queryModel: QueryModel): Response<Page<Map<String, Any>>>
}
