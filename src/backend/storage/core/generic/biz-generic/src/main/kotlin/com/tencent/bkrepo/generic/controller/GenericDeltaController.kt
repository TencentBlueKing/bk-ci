package com.tencent.bkrepo.generic.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo.Companion.DELTA_MAPPING_URI
import com.tencent.bkrepo.generic.constant.HEADER_OLD_FILE_PATH
import com.tencent.bkrepo.generic.service.DeltaSyncService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class GenericDeltaController(private val deltaSyncService: DeltaSyncService) {

    @GetMapping(DELTA_MAPPING_URI)
    @Permission(ResourceType.NODE, PermissionAction.READ)
    fun sign(
        @ArtifactPathVariable artifactInfo: GenericArtifactInfo
    ) {
        return deltaSyncService.sign()
    }

    @PatchMapping(DELTA_MAPPING_URI)
    @Permission(ResourceType.NODE, PermissionAction.WRITE)
    fun patch(
        @ArtifactPathVariable artifactInfo: GenericArtifactInfo,
        @RequestHeader(HEADER_OLD_FILE_PATH) oldFilePath: String,
        deltaFile: ArtifactFile
    ): SseEmitter {
        return deltaSyncService.patch(oldFilePath, deltaFile)
    }
}
