package com.tencent.devops.process.permission

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthAuthorizationApi
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverResult
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.service.SubPipelineService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PipelineAuthorizationService constructor(
    val pipelinePermissionService: PipelinePermissionService,
    val authAuthorizationApi: AuthAuthorizationApi,
    val subPipelineService: SubPipelineService
) {
    fun addResourceAuthorization(
        projectId: String,
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    ) {
        authAuthorizationApi.addResourceAuthorization(
            projectId = projectId,
            resourceAuthorizationList = resourceAuthorizationList
        )
    }

    fun resetPipelineAuthorization(
        projectId: String,
        preCheck: Boolean,
        resourceAuthorizationHandoverDTOs: List<ResourceAuthorizationHandoverDTO>
    ): Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>> {
        logger.info("reset pipeline authorization|$preCheck|$projectId|$resourceAuthorizationHandoverDTOs")
        return authAuthorizationApi.resetResourceAuthorization(
            projectId = projectId,
            preCheck = preCheck,
            resourceAuthorizationHandoverDTOs = resourceAuthorizationHandoverDTOs,
            handoverResourceAuthorization = ::handoverPipelineAuthorization
        )
    }

    private fun handoverPipelineAuthorization(
        preCheck: Boolean,
        resourceAuthorizationHandoverDTO: ResourceAuthorizationHandoverDTO
    ): ResourceAuthorizationHandoverResult {
        return with(resourceAuthorizationHandoverDTO) {
            val hasHandoverToPermission = pipelinePermissionService.checkPipelinePermission(
                userId = handoverTo!!,
                projectId = projectCode,
                pipelineId = resourceCode,
                permission = AuthPermission.EXECUTE
            )
            val checkSubPipelinePermission = subPipelineService.checkSubPipelinePermission(
                projectId = projectCode,
                pipelineId = resourceCode,
                userId = handoverTo!!,
                permission = AuthPermission.EXECUTE
            )
            // 1.当前流水线的执行权限
            // 2.有子流水线的执行权限
            when {
                hasHandoverToPermission && checkSubPipelinePermission.isEmpty() -> {
                    ResourceAuthorizationHandoverResult(ResourceAuthorizationHandoverStatus.SUCCESS)
                }

                checkSubPipelinePermission.isNotEmpty() -> {
                    val failTitle = I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_NOT_SUB_PIPELINE_EXECUTE_PERMISSION_ERROR_TITLE,
                        params = arrayOf(handoverTo!!)
                    )
                    val failMsg = checkSubPipelinePermission.map {
                        it.errorMessage
                    }.joinToString(FAIL_MESSAGE_SEPARATOR)
                    ResourceAuthorizationHandoverResult(
                        status = ResourceAuthorizationHandoverStatus.FAILED,
                        message = "$failTitle$FAIL_MESSAGE_SEPARATOR$failMsg"
                    )
                }

                else -> {
                    ResourceAuthorizationHandoverResult(
                        status = ResourceAuthorizationHandoverStatus.FAILED,
                        message = MessageUtil.getMessageByLocale(
                            messageCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                            params = arrayOf(AuthPermission.EXECUTE.getI18n(I18nUtil.getLanguage(handoverTo))),
                            language = I18nUtil.getLanguage(handoverTo)
                        )
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAuthorizationService::class.java)
        const val FAIL_MESSAGE_SEPARATOR = "<br/>"
    }
}
