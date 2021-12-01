package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.turbo.api.IBuildTurboRecordController
import com.tencent.devops.turbo.service.TurboRecordService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class BuildTurboRecordController @Autowired constructor(
    private val turboRecordService: TurboRecordService
) : IBuildTurboRecordController {
    override fun updateRecordStatusForPlugin(buildId: String, user: String): Response<String?> {
        return Response.success(turboRecordService.processAfterPluginFinish(buildId, user))
    }
}
