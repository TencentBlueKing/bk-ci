package com.tencent.devops.experience.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_NAME
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.experience.constant.ExperienceConstant.HEADER_O_TOKEN
import com.tencent.devops.experience.service.ExperienceOuterService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.PreMatching
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
class OuterLoginFilter @Autowired constructor(
    private val experienceOuterService: ExperienceOuterService
) : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext?) {
        if (null != requestContext) {
            val headers = requestContext.headers
            if (headers[AUTH_HEADER_DEVOPS_ORGANIZATION_NAME]?.contains("outer") == true) {
                // TODO 安全过滤
                // 续期token
                headers[HEADER_O_TOKEN]?.get(0)?.let {
                    experienceOuterService.renewToken(it)
                }
            }
        }
    }
}
