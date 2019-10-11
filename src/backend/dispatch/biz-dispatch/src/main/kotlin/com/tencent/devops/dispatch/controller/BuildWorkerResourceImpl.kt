package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.BuildWorkerResource
import com.tencent.devops.dispatch.service.DownloaderService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class BuildWorkerResourceImpl @Autowired constructor(private val downloaderService: DownloaderService) : BuildWorkerResource {
    override fun download(eTag: String?): Response {
        return downloaderService.downloadWorker(eTag)
    }
}