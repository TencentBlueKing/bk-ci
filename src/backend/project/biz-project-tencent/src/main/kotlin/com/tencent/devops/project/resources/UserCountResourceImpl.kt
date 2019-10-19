package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.UserCountResource
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.CountService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCountResourceImpl @Autowired constructor(
    private val countService: CountService
) : UserCountResource {
    override fun login(userId: String, xRealIp: String?, xForwardedFor: String?, userAgent: String?): Result<Boolean> {
        countService.countLogin(userId, xRealIp, xForwardedFor, userAgent)
        return Result(true)
    }
}