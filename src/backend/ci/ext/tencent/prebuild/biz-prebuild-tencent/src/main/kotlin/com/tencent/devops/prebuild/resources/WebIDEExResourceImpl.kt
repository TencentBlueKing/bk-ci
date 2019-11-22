package com.tencent.devops.prebuild.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.prebuild.api.WebIDEExResource
import com.tencent.devops.prebuild.service.WebIDEService
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.api.pojo.Result

@RestResource
class WebIDEExResourceImpl @Autowired constructor(private val webIDEService: WebIDEService) : WebIDEExResource {
    override fun heartBeat(userId: String, ip: String): Result<Boolean> {
        return Result(webIDEService.heartBeat(userId, ip))
    }
}