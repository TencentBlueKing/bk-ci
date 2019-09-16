package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.BuildDockerResource
import com.tencent.devops.dispatch.service.vm.DownloaderService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class BuildDockerResourceImpl @Autowired constructor(private val downloaderService: DownloaderService) : BuildDockerResource {
    override fun download(eTag: String?): Response {
        return downloaderService.downloadDocker(eTag)
    }
}