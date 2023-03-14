package com.tencent.devops.experience.resources.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.experience.api.service.ServiceExperienceGroupResource
import com.tencent.devops.experience.constant.ExperienceCode.BK_USER_NOT_PERMISSION
import com.tencent.devops.experience.pojo.GroupCreate
import com.tencent.devops.experience.pojo.GroupUpdate
import com.tencent.devops.experience.pojo.GroupUsers
import com.tencent.devops.experience.service.GroupService
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceExperienceGroupResourceImpl @Autowired constructor(
    private val groupService: GroupService,
    private val client: Client
) : ServiceExperienceGroupResource {
    override fun create(userId: String, projectId: String, group: GroupCreate): Result<String> {
        checkParam(userId, projectId)
        val groupId = groupService.create(projectId = projectId, userId = userId, group = group)
        return Result(groupId)
    }

    override fun getUsers(userId: String, projectId: String, groupHashId: String): Result<GroupUsers> {
        checkParam(userId, projectId, groupHashId)
        return Result(groupService.getUsers(userId = userId, projectId = projectId, groupHashId = groupHashId))
    }

    override fun edit(userId: String, projectId: String, groupHashId: String, group: GroupUpdate): Result<Boolean> {
        checkParam(userId, projectId, groupHashId)
        groupService.edit(userId = userId, projectId = projectId, groupHashId = groupHashId, group = group)
        return Result(true)
    }

    override fun delete(userId: String, projectId: String, groupHashId: String): Result<Boolean> {
        checkParam(userId, projectId, groupHashId)
        groupService.delete(userId = userId, projectId = projectId, groupHashId = groupHashId)
        return Result(true)
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (client.get(ServiceProjectResource::class)
                .verifyUserProjectPermission(projectCode = projectId, userId = userId).data != true
        ) {
            throw ErrorCodeException(
                defaultMessage = MessageUtil.getMessageByLocale(
                    messageCode = BK_USER_NOT_PERMISSION,
                    language = I18nUtil.getLanguage(userId)
                ),
                errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION
            )
        }
    }

    private fun checkParam(userId: String, projectId: String, groupHashId: String) {
        checkParam(userId, projectId)
        if (groupHashId.isBlank()) {
            throw ParamBlankException("Invalid groupHashId")
        }
    }
}
