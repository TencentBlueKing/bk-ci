package com.tencent.bkrepo.nuget.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.service.NugetServiceIndexService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@RequestMapping("/{projectId}/{repoName}/v3")
class NugetServiceIndexController(
    private val nugetServiceIndexService: NugetServiceIndexService
) {
    /**
     * 获取服务索引 service index
     */
    @GetMapping("/index.json", produces = [MediaTypes.APPLICATION_JSON])
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun feed(
        artifactInfo: NugetArtifactInfo
    ): ResponseEntity<Any> {
        return nugetServiceIndexService.getFeed(artifactInfo)
    }
}
