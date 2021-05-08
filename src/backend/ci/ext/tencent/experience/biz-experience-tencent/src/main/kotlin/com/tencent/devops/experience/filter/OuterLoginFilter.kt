package com.tencent.devops.experience.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_NAME
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.experience.constant.ExperienceConstant.HEADER_O_TOKEN
import com.tencent.devops.experience.constant.ExperienceConstant.ORGANIZATION_OUTER
import com.tencent.devops.experience.service.ExperienceOuterService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.HeaderParam
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.PreMatching
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.Context
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
class OuterLoginFilter @Autowired constructor(
    private val experienceOuterService: ExperienceOuterService
) : ContainerRequestFilter {
    @Context
    private var resourceInfo: ResourceInfo? = null

    @SuppressWarnings("NestedBlockDepth")
    override fun filter(requestContext: ContainerRequestContext?) {
        if (null != requestContext && null != resourceInfo) {
            val headers = requestContext.headers
            if (headers[AUTH_HEADER_DEVOPS_ORGANIZATION_NAME]?.contains(ORGANIZATION_OUTER) == true) {
                // 安全过滤
                resourceInfo!!.resourceMethod.parameterAnnotations.forEach {
                    it.forEach { annotation ->
                        if (annotation is HeaderParam) {
                            logger.info("HeaderParam : ${annotation.value}")
                        }
                    }
                }
                // 续期token
                headers[HEADER_O_TOKEN]?.get(0)?.let {
                    experienceOuterService.renewToken(it)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OuterLoginFilter::class.java)
    }
}
