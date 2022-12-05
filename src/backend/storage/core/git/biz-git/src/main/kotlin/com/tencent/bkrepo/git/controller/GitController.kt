package com.tencent.bkrepo.git.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.git.artifact.GitRepositoryArtifactInfo
import com.tencent.bkrepo.git.service.GitService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RequestMapping("{projectId}/{repoName}.git")
@RestController
class GitController(
    private val gitService: GitService
) {
    /**
     * git clone
     * git push
     * */
    @GetMapping("info/refs")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun infoRefs(
        infoRepository: GitRepositoryArtifactInfo,
        @RequestParam("service") svc: String
    ) {
        gitService.infoRefs(infoRepository, svc)
    }

    /**
     * git clone
     * */
    @PostMapping("git-upload-pack")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun gitUploadPack(infoRepository: GitRepositoryArtifactInfo) {
        gitService.gitUploadPack()
    }

    /**
     * git push
     * */
    @PostMapping("git-receive-pack")
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun gitReceivePack(infoRepository: GitRepositoryArtifactInfo) {
        gitService.gitReceivePack()
    }
}
