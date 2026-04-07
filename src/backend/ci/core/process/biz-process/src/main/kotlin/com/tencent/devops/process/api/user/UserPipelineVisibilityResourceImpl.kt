package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.constant.CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineVisibility
import com.tencent.devops.process.service.PipelineVisibilityService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineVisibilityResourceImpl @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineVisibilityService: PipelineVisibilityService
) : UserPipelineVisibilityResource {

    override fun addVisibility(
        userId: String,
        projectId: String,
        pipelineId: String,
        visibilityList: List<PipelineVisibility>
    ): Result<Boolean> {
        checkPermission(userId, projectId, pipelineId, AuthPermission.EDIT)
        pipelineVisibilityService.addVisibility(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            visibilityList = visibilityList
        )
        return Result(true)
    }

    override fun listVisibility(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<PipelineVisibility>> {
        checkPermission(userId, projectId, pipelineId, AuthPermission.VIEW)
        return Result(
            pipelineVisibilityService.listVisibility(
                projectId = projectId,
                pipelineId = pipelineId,
                page = page ?: 1,
                pageSize = pageSize ?: 20
            )
        )
    }

    override fun deleteVisibility(
        userId: String,
        projectId: String,
        pipelineId: String,
        scopeIds: List<String>
    ): Result<Boolean> {
        checkPermission(userId, projectId, pipelineId, AuthPermission.EDIT)
        pipelineVisibilityService.deleteVisibility(
            projectId = projectId,
            pipelineId = pipelineId,
            scopeIds = scopeIds
        )
        return Result(true)
    }

    private fun checkPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission
    ) {
        val language = I18nUtil.getLanguage(userId)
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                language,
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(language),
                    pipelineId
                )
            )
        )
    }
}
