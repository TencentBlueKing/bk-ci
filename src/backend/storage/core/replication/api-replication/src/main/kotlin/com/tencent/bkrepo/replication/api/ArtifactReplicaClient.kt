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

package com.tencent.bkrepo.replication.api

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.REPLICATION_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.replication.pojo.request.NodeExistCheckRequest
import com.tencent.bkrepo.replication.pojo.request.PackageVersionExistCheckRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataDeleteRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping("/replica")
@FeignClient(REPLICATION_SERVICE_NAME, contextId = "ArtifactReplicaClient")
interface ArtifactReplicaClient {

    @GetMapping("/ping")
    fun ping(@RequestHeader(HttpHeaders.AUTHORIZATION) token: String): Response<Void>

    @GetMapping("/version")
    fun version(): Response<String>

    @PostMapping("/node/exist/list")
    fun checkNodeExistList(
        @RequestBody request: NodeExistCheckRequest
    ): Response<List<String>>

    @GetMapping("/node/exist")
    fun checkNodeExist(
        @RequestParam projectId: String,
        @RequestParam repoName: String,
        @RequestParam fullPath: String
    ): Response<Boolean>

    @PostMapping("/node/create")
    fun replicaNodeCreateRequest(
        @RequestBody request: NodeCreateRequest
    ): Response<NodeDetail>

    @PostMapping("/node/rename")
    fun replicaNodeRenameRequest(
        @RequestBody request: NodeRenameRequest
    ): Response<Void>

    @PostMapping("/node/update")
    fun replicaNodeUpdateRequest(
        @RequestBody request: NodeUpdateRequest
    ): Response<Void>

    @PostMapping("/node/copy")
    fun replicaNodeCopyRequest(
        @RequestBody request: NodeMoveCopyRequest
    ): Response<Void>

    @PostMapping("/node/move")
    fun replicaNodeMoveRequest(
        @RequestBody request: NodeMoveCopyRequest
    ): Response<Void>

    @PostMapping("/node/delete")
    fun replicaNodeDeleteRequest(
        @RequestBody request: NodeDeleteRequest
    ): Response<Void>

    @PostMapping("/repo/create")
    fun replicaRepoCreateRequest(
        @RequestBody request: RepoCreateRequest
    ): Response<RepositoryDetail>

    @PostMapping("/repo/update")
    fun replicaRepoUpdateRequest(
        @RequestBody request: RepoUpdateRequest
    ): Response<Void>

    @PostMapping("/repo/delete")
    fun replicaRepoDeleteRequest(
        @RequestBody request: RepoDeleteRequest
    ): Response<Void>

    @PostMapping("/project/create")
    fun replicaProjectCreateRequest(
        @RequestBody request: ProjectCreateRequest
    ): Response<ProjectInfo>

    @PostMapping("/metadata/save")
    fun replicaMetadataSaveRequest(
        @RequestBody request: MetadataSaveRequest
    ): Response<Void>

    @PostMapping("/metadata/delete")
    fun replicaMetadataDeleteRequest(
        @RequestBody request: MetadataDeleteRequest
    ): Response<Void>

    @PostMapping("/package/version/exist")
    fun checkPackageVersionExist(
        @RequestBody request: PackageVersionExistCheckRequest
    ): Response<Boolean>

    @PostMapping("/package/version/create")
    fun replicaPackageVersionCreatedRequest(
        @RequestBody request: PackageVersionCreateRequest
    ): Response<Void>
}
