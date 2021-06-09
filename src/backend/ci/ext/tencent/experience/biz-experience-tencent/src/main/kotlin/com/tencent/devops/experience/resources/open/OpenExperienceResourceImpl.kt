package com.tencent.devops.experience.resources.open

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.open.OpenExperienceResource
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.job.ExperienceHotJob
import com.tencent.devops.experience.pojo.outer.OuterLoginParam
import com.tencent.devops.experience.pojo.outer.OuterProfileVO
import com.tencent.devops.experience.service.ExperienceOuterService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI
import javax.ws.rs.core.Response

@RestResource
class OpenExperienceResourceImpl @Autowired constructor(
    private val experienceOuterService: ExperienceOuterService,
    private val experiencePublicDao: ExperiencePublicDao,
    private val dslContext: DSLContext
) : OpenExperienceResource {
    override fun outerLogin(
        platform: Int,
        appVersion: String?,
        realIp: String,
        params: OuterLoginParam
    ): Result<String> {
        return Result(experienceOuterService.outerLogin(platform, appVersion, realIp, params))
    }

    override fun outerAuth(token: String): Result<OuterProfileVO> {
        return Result(experienceOuterService.outerAuth(token))
    }

    override fun appStoreRedirect(id: String): Response {
        val publicRecord = experiencePublicDao.getById(dslContext, HashUtil.decodeIdToLong(id))
            ?: return Response.status(404).build()
        // TODO 记录
        return Response
            .temporaryRedirect(URI.create(publicRecord.externalLink))
            .build()
    }
}
