package com.tencent.bkrepo.git.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.git.artifact.GitContentArtifactInfo
import com.tencent.bkrepo.git.artifact.GitRepositoryArtifactInfo
import com.tencent.bkrepo.git.service.GitService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RequestMapping("{projectId}/{repoName}", produces = [MediaTypes.APPLICATION_JSON])
@RestController
class GitApiController(
    private val gitService: GitService
) {

    /**
     * 同步仓库
     * 同步仓库前，需要先配置好远程仓库配置，包括url,认证信息等
     * */
    @PostMapping("sync")
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun sync(gitRepositoryArtifactInfo: GitRepositoryArtifactInfo) {
        gitService.sync(gitRepositoryArtifactInfo)
    }

    /**
     * 获取仓库内容
     * @param ref The name of the commit/branch/tag.
     * Default: the repository’s default branch (usually master)
     * @param path path parameter
     * */
    @GetMapping("/raw/{ref}/**")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun getContent(gitContentArtifactInfo: GitContentArtifactInfo) {
        gitService.getContent(gitContentArtifactInfo)
    }
}
