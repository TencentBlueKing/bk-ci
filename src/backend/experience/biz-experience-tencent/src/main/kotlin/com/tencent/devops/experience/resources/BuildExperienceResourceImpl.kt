package com.tencent.devops.experience.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.BuildExperienceResource
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import com.tencent.devops.experience.service.ExperienceService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildExperienceResourceImpl @Autowired constructor(private val experienceService: ExperienceService) :
    BuildExperienceResource {

    override fun create(userId: String, projectId: String, experience: ExperienceServiceCreate): Result<Boolean> {
        checkParam(userId, projectId)
        experienceService.serviceCreate(userId, projectId, experience)
        return Result(true)
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}