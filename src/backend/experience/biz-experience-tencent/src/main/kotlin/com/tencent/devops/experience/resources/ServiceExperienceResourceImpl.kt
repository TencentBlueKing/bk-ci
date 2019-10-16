package com.tencent.devops.experience.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.ServiceExperienceResource
import com.tencent.devops.experience.pojo.Experience
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import com.tencent.devops.experience.service.ExperienceService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceExperienceResourceImpl @Autowired constructor(private val experienceService: ExperienceService) : ServiceExperienceResource {

    override fun create(userId: String, projectId: String, experience: ExperienceServiceCreate): Result<Boolean> {
        checkParam(userId, projectId)
        experienceService.serviceCreate(userId, projectId, experience)
        return Result(true)
    }

    override fun count(projectIds: Set<String>?, expired: Boolean?): Result<Map<String, Int>> {
        return Result(experienceService.count(projectIds ?: setOf(), expired))
    }

    override fun get(userId: String, projectId: String, experienceHashId: String): Result<Experience> {
        checkParam(userId, projectId)
        return Result(experienceService.get(userId, projectId, experienceHashId, false))
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}