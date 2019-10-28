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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryId
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.Permission
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.service.RepoFileService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.repository.service.RepositoryUserService
import org.springframework.beans.factory.annotation.Autowired
import java.net.URLDecoder

@RestResource
class ServiceRepositoryResourceImpl @Autowired constructor(
        private val repoFileService: RepoFileService,
        private val repositoryService: RepositoryService,
        private val repositoryUserService: RepositoryUserService
) : ServiceRepositoryResource {

    override fun create(userId: String, projectId: String, repository: Repository): Result<RepositoryId> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repository.aliasName.isBlank()) {
            throw ParamBlankException("Invalid repository aliasName")
        }
        if (repository.url.isBlank()) {
            throw ParamBlankException("Invalid repository url")
        }
        return Result(RepositoryId(repositoryService.userCreate(userId, projectId, repository)))
    }

    override fun list(projectId: String, repositoryType: ScmType?): Result<List<RepositoryInfoWithPermission>> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        return Result(repositoryService.serviceList(projectId, repositoryType))
    }

    /**
     * @param repositoryId 代表的是hashId或者代码库名，依赖repositoryType
     */
    override fun get(projectId: String, repositoryId: String, repositoryType: RepositoryType?): Result<Repository> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repositoryId.isBlank()) {
            throw ParamBlankException("Invalid repositoryHashId")
        }
        return Result(
            repositoryService.serviceGet(
                projectId,
                buildConfig(URLDecoder.decode(repositoryId, "UTF-8"), repositoryType)
            )
        )
    }

    override fun count(
        projectId: Set<String>,
        repositoryHashId: String?,
        repositoryType: ScmType?,
        aliasName: String?
    ): Result<Long> {
        val data = repositoryService.serviceCount(projectId, repositoryHashId ?: "", repositoryType, aliasName ?: "")
        return Result(data)
    }

    override fun hasPermissionList(userId: String, projectId: String, repositoryType: ScmType?, permission: Permission): Result<Page<RepositoryInfo>> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val bkAuthPermission = when (permission) {
            Permission.DELETE -> AuthPermission.DELETE
            Permission.LIST -> AuthPermission.LIST
            Permission.VIEW -> AuthPermission.VIEW
            Permission.EDIT -> AuthPermission.EDIT
            Permission.USE -> AuthPermission.USE
        }
        val limit = PageUtil.convertPageSizeToSQLLimit(0, 9999)
        val result = repositoryService.hasPermissionList(userId, projectId, repositoryType, bkAuthPermission, limit.offset, limit.limit)
        return Result(Page(0, 9999, result.count, result.records))
    }
//
//    override fun createV2(userId: String, projectId: String, repository: Repository): Result<RepositoryId> {
//        if (userId.isBlank()) {
//            throw ParamBlankException("Invalid userId")
//        }
//        if (projectId.isBlank()) {
//            throw ParamBlankException("Invalid projectId")
//        }
//        if (repository.aliasName.isBlank()) {
//            throw ParamBlankException("Invalid repository aliasName")
//        }
//        if (repository.url.isBlank()) {
//            throw ParamBlankException("Invalid repository url")
//        }
//        return Result(RepositoryId(repositoryService.userCreate(userId, projectId, repository)))
//    }
//
//    override fun listV2(projectId: String, repositoryType: ScmType?): Result<List<RepositoryInfoWithPermission>> {
//        if (projectId.isBlank()) {
//            throw ParamBlankException("Invalid projectId")
//        }
//        return Result(repositoryService.serviceList(projectId, repositoryType))
//    }
//
//    override fun getV2(projectId: String, repositoryId: String, repositoryType: RepositoryType?): Result<Repository> {
//        if (projectId.isBlank()) {
//            throw ParamBlankException("Invalid projectId")
//        }
//        if (repositoryId.isBlank()) {
//            throw ParamBlankException("Invalid repositoryHashId")
//        }
//        return Result(
//                repositoryService.serviceGet(
//                        projectId,
//                        buildConfig(URLDecoder.decode(repositoryId, "UTF-8"), repositoryType)
//                )
//        )
//    }

    override fun createGitCodeRepository(userId: String, projectCode: String, repositoryName: String, sampleProjectPath: String, namespaceId: Int?, visibilityLevel: VisibilityLevelEnum?, tokenType: TokenTypeEnum): Result<RepositoryInfo?> {
        return repositoryService.createGitCodeRepository(userId, projectCode, repositoryName, sampleProjectPath, namespaceId, visibilityLevel, tokenType)
    }

    override fun updateGitCodeRepository(userId: String, repoId: String, updateGitProjectInfo: UpdateGitProjectInfo, tokenType: TokenTypeEnum): Result<Boolean> {
        return repositoryService.updateGitCodeRepository(userId, buildConfig(repoId, null), updateGitProjectInfo, tokenType)
    }

    override fun addGitProjectMember(userId: String, userIdList: List<String>, repoId: String, gitAccessLevel: GitAccessLevelEnum, tokenType: TokenTypeEnum): Result<Boolean> {
        return repositoryService.addGitProjectMember(userId, userIdList, buildConfig(repoId, null), gitAccessLevel, tokenType)
    }

    override fun deleteGitProjectMember(userId: String, userIdList: List<String>, repoId: String, tokenType: TokenTypeEnum): Result<Boolean> {
        return repositoryService.deleteGitProjectMember(userId, userIdList, buildConfig(repoId, null), tokenType)
    }

    override fun updateRepositoryUserInfo(userId: String, projectCode: String, repositoryHashId: String): Result<Boolean> {
        return repositoryUserService.updateRepositoryUserInfo(userId, projectCode, repositoryHashId)
    }

    override fun moveGitProjectToGroup(userId: String, groupCode: String?, repoId: String, tokenType: TokenTypeEnum): Result<GitProjectInfo?> {
        return repositoryService.moveGitProjectToGroup(userId, groupCode, buildConfig(repoId, null), tokenType)
    }

    override fun getFileContent(repoId: String, filePath: String, reversion: String?, branch: String?, repositoryType: RepositoryType?): Result<String> {
        return Result(repoFileService.getFileContent(buildConfig(repoId, repositoryType), filePath, reversion, branch))
    }
//
//    override fun getFileContentV2(repoId: String, filePath: String, reversion: String?, branch: String?, repositoryType: RepositoryType?): Result<String> {
//        return Result(repoFileService.getFileContent(buildConfig(repoId, repositoryType), filePath, reversion, branch))
//    }

    override fun delete(userId: String, projectId: String, repositoryHashId: String): Result<Boolean> {
        repositoryService.userDelete(userId, projectId, repositoryHashId)
        return Result(true)
    }
//
//    override fun hasPermissionListV2(userId: String, projectId: String, repositoryType: ScmType?, permission: Permission): Result<Page<RepositoryInfo>> {
//        if (userId.isBlank()) {
//            throw ParamBlankException("Invalid userId")
//        }
//        if (projectId.isBlank()) {
//            throw ParamBlankException("Invalid projectId")
//        }
//        val bkAuthPermission = when (permission) {
//            Permission.DELETE -> AuthPermission.DELETE
//            Permission.LIST -> AuthPermission.LIST
//            Permission.VIEW -> AuthPermission.VIEW
//            Permission.EDIT -> AuthPermission.EDIT
//            Permission.USE -> AuthPermission.USE
//        }
//        val limit = PageUtil.convertPageSizeToSQLLimit(0, 9999)
//        val result = repositoryService.hasPermissionList(userId, projectId, repositoryType, bkAuthPermission, limit.offset, limit.limit)
//        return Result(Page(0, 9999, result.count, result.records))
//    }
//
//    override fun deleteV2(userId: String, projectId: String, repositoryHashId: String): Result<Boolean> {
//        repositoryService.userDelete(userId, projectId, repositoryHashId)
//        return Result(true)
//    }
}