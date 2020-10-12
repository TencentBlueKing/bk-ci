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

package com.tencent.bkrepo.replication.controller

import com.tencent.bkrepo.auth.api.ServicePermissionResource
import com.tencent.bkrepo.auth.api.ServiceRoleResource
import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.auth.pojo.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.Permission
import com.tencent.bkrepo.auth.pojo.Role
import com.tencent.bkrepo.auth.pojo.User
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.replication.api.ReplicationClient
import com.tencent.bkrepo.replication.config.DEFAULT_VERSION
import com.tencent.bkrepo.replication.pojo.request.NodeExistCheckRequest
import com.tencent.bkrepo.replication.pojo.request.NodeReplicaRequest
import com.tencent.bkrepo.replication.pojo.request.RoleReplicaRequest
import com.tencent.bkrepo.replication.pojo.request.UserReplicaRequest
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.metadata.MetadataDeleteRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Principal(type = PrincipalType.ADMIN)
@RestController
class ReplicationController : ReplicationClient {

    @Autowired
    private lateinit var projectClient: ProjectClient

    @Autowired
    private lateinit var repositoryClient: RepositoryClient

    @Autowired
    private lateinit var nodeClient: NodeClient

    @Autowired
    private lateinit var metadataClient: MetadataClient

    @Autowired
    private lateinit var permissionResource: ServicePermissionResource

    @Autowired
    private lateinit var userResource: ServiceUserResource

    @Autowired
    private lateinit var roleResource: ServiceRoleResource

    @Autowired
    private lateinit var storageService: StorageService

    @Value("\${spring.application.version}")
    private var version: String = DEFAULT_VERSION

    override fun ping(token: String) = ResponseBuilder.success()

    override fun version(token: String) = ResponseBuilder.success(version)

    override fun checkNodeExist(
        token: String,
        projectId: String,
        repoName: String,
        fullPath: String
    ): Response<Boolean> {
        return nodeClient.exist(projectId, repoName, fullPath)
    }

    override fun checkNodeExistList(
        token: String,
        nodeExistCheckRequest: NodeExistCheckRequest
    ): Response<List<String>> {
        return nodeClient.listExistFullPath(
            nodeExistCheckRequest.projectId,
            nodeExistCheckRequest.repoName,
            nodeExistCheckRequest.fullPathList
        )
    }

    override fun replicaUser(token: String, userReplicaRequest: UserReplicaRequest): Response<User> {
        with(userReplicaRequest) {
            val userInfo = userResource.detail(userId).data ?: run {
                val request = CreateUserRequest(userId, name, pwd, admin)
                userResource.createUser(request)
                userResource.detail(userId).data!!
            }
            val remoteTokenStringList = this.tokens.map { it.id }
            val selfTokenStringList = userInfo.tokens.map { it.id }
            remoteTokenStringList.forEach {
                if (!selfTokenStringList.contains(it)) {
                    userResource.addUserToken(userId, token)
                }
            }
            return ResponseBuilder.success(userInfo)
        }
    }

    override fun replicaRole(token: String, roleReplicaRequest: RoleReplicaRequest): Response<Role> {
        with(roleReplicaRequest) {
            val existRole = if (repoName == null) {
                roleResource.detailByRidAndProjectId(roleId, projectId).data
            } else {
                roleResource.detailByRidAndProjectIdAndRepoName(roleId, projectId, repoName!!).data
            }
            val roleInfo = existRole ?: run {
                val request = CreateRoleRequest(roleId, name, type, projectId, repoName, admin)
                val id = roleResource.createRole(request).data!!
                roleResource.detail(id).data!!
            }

            return ResponseBuilder.success(roleInfo)
        }
    }

    override fun replicaPermission(token: String, permissionCreateRequest: CreatePermissionRequest): Response<Void> {
        permissionResource.createPermission(permissionCreateRequest)
        return ResponseBuilder.success()
    }

    override fun replicaUserRoleRelationShip(token: String, rid: String, userIdList: List<String>): Response<Void> {
        userResource.addUserRoleBatch(rid, userIdList)
        return ResponseBuilder.success()
    }

    override fun listPermission(
        token: String,
        resourceType: ResourceType,
        projectId: String,
        repoName: String?
    ): Response<List<Permission>> {
        return permissionResource.listPermission(resourceType, projectId, repoName)
    }

    @PostMapping(FILE_MAPPING_URI, consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun replicaFile(
        @ArtifactPathVariable
        artifactInfo: DefaultArtifactInfo,
        fileMap: ArtifactFileMap,
        nodeReplicaRequest: NodeReplicaRequest
    ): Response<NodeDetail> {
        with(nodeReplicaRequest) {
            val file = fileMap["file"]!!
            // 校验
            if (file.getSize() != size) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "size")
            }
            if (sha256.isBlank()) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "sha256")
            }
            if (md5.isBlank()) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "md5")
            }
            // 保存文件
            val projectId = artifactInfo.projectId
            val repoName = artifactInfo.repoName
            val fullPath = artifactInfo.artifactUri
            val repoInfo = repositoryClient.detail(projectId, repoName).data!!
            storageService.store(sha256, file, repoInfo.storageCredentials)
            // 保存节点
            val request = NodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = fullPath,
                folder = false,
                expires = expires,
                overwrite = true,
                size = size,
                sha256 = sha256,
                md5 = md5,
                metadata = metadata,
                operator = userId
            )
            return nodeClient.create(request)
        }
    }

    override fun replicaNodeCreateRequest(token: String, nodeCreateRequest: NodeCreateRequest): Response<NodeDetail> {
        return nodeClient.create(nodeCreateRequest)
    }

    override fun replicaNodeRenameRequest(token: String, nodeRenameRequest: NodeRenameRequest): Response<Void> {
        return nodeClient.rename(nodeRenameRequest)
    }

    override fun replicaNodeUpdateRequest(token: String, nodeUpdateRequest: NodeUpdateRequest): Response<Void> {
        return nodeClient.update(nodeUpdateRequest)
    }

    override fun replicaNodeCopyRequest(token: String, nodeCopyRequest: NodeCopyRequest): Response<Void> {
        return nodeClient.copy(nodeCopyRequest)
    }

    override fun replicaNodeMoveRequest(token: String, nodeMoveRequest: NodeMoveRequest): Response<Void> {
        return nodeClient.move(nodeMoveRequest)
    }

    override fun replicaNodeDeleteRequest(token: String, nodeDeleteRequest: NodeDeleteRequest): Response<Void> {
        return nodeClient.delete(nodeDeleteRequest)
    }

    override fun replicaRepoCreateRequest(token: String, request: RepoCreateRequest): Response<RepositoryInfo> {
        return repositoryClient.detail(request.projectId, request.name).data?.let { ResponseBuilder.success(it) }
            ?: repositoryClient.create(request)
    }

    override fun replicaRepoUpdateRequest(token: String, request: RepoUpdateRequest): Response<Void> {
        return repositoryClient.update(request)
    }

    override fun replicaRepoDeleteRequest(token: String, request: RepoDeleteRequest): Response<Void> {
        return repositoryClient.delete(request)
    }

    override fun replicaProjectCreateRequest(token: String, request: ProjectCreateRequest): Response<ProjectInfo> {
        return projectClient.query(request.name).data?.let { ResponseBuilder.success(it) }
            ?: projectClient.create(request)
    }

    override fun replicaMetadataSaveRequest(token: String, request: MetadataSaveRequest): Response<Void> {
        return metadataClient.save(request)
    }

    override fun replicaMetadataDeleteRequest(token: String, request: MetadataDeleteRequest): Response<Void> {
        return metadataClient.delete(request)
    }

    companion object {
        private const val FILE_MAPPING_URI = "/file/{projectId}/{repoName}/**"
    }
}
