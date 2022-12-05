package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.IS_NOT_ADMIN_MEMBER
import com.tencent.devops.common.api.exception.code.TURBO_PARAM_INVALID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.constants.NO_ADMIN_MEMBER_MESSAGE
import com.tencent.devops.turbo.api.IUserTurboRecordController
import com.tencent.devops.turbo.enums.EnumDistccTaskStatus
import com.tencent.devops.turbo.pojo.TurboRecordModel
import com.tencent.devops.turbo.service.TurboAuthService
import com.tencent.devops.turbo.service.TurboPlanInstanceService
import com.tencent.devops.turbo.service.TurboPlanService
import com.tencent.devops.turbo.service.TurboRecordService
import com.tencent.devops.turbo.vo.TurboListSelectVO
import com.tencent.devops.turbo.vo.TurboRecordDisplayVO
import com.tencent.devops.turbo.vo.TurboRecordHistoryVO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@Suppress("MaxLineLength")
@RestController
class UserTurboRecordController @Autowired constructor(
    private val turboPlanService: TurboPlanService,
    private val turboRecordService: TurboRecordService,
    private val turboPlanInstanceService: TurboPlanInstanceService,
    private val turboAuthService: TurboAuthService
) : IUserTurboRecordController {

    companion object {
        private val logger = LoggerFactory.getLogger(UserTurboRecordController::class.java)
    }

    override fun getTurboRecordHistoryList(
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?,
        turboRecordModel: TurboRecordModel,
        projectId: String,
        user: String
    ): Response<Page<TurboRecordHistoryVO>> {
        // 判断是否是管理员
        if (!turboAuthService.getAuthResult(projectId, user)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return Response.success(turboRecordService.getTurboRecordHistoryList(pageNum, pageSize, sortField, sortType, turboRecordModel))
    }

    override fun getPipelineAndPlanAndStatusList(projectId: String): Response<TurboListSelectVO> {
        val turboPlanInstanceList = turboPlanInstanceService.findPipelineInfoByProjectId(projectId)
        return Response.success(
            TurboListSelectVO(
                planInfo = turboPlanService.getByProjectId(projectId).associate { it.id!! to it.planName },
                pipelineInfo = turboPlanInstanceList.filter { !it.pipelineId.isNullOrBlank() }.associate { it.pipelineId!! to it.pipelineName },
                clientIpInfo = turboPlanInstanceList.filter { !it.clientIp.isNullOrBlank() }.distinctBy { it.clientIp }.map { it.clientIp!! },
                statusInfo = EnumDistccTaskStatus.values().associate { it.getTBSStatus() to it.getStatusName() }
            )
        )
    }

    override fun getTurboRecordStatInfo(turboRecordId: String): Response<String?> {
        return Response.success(
            turboRecordService.getTurboRecordStatInfo(turboRecordId)
        )
    }

    override fun getTurboDisplayInfoById(turboRecordId: String, projectId: String, user: String): Response<TurboRecordDisplayVO> {
        // 判断是否是管理员
        if (!turboAuthService.getAuthResult(projectId, user)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }

        val turboRecordEntity = turboRecordService.findByRecordId(turboRecordId)
        if (null == turboRecordEntity || turboRecordEntity.turboPlanId.isNullOrBlank()) {
            logger.info("no turbo record found with id: $turboRecordId")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "no turbo record found")
        }
        val turboPlanEntity = turboPlanService.findTurboPlanById(turboRecordEntity.turboPlanId!!)
        if (null == turboPlanEntity) {
            logger.info("no turbo plan found with id: ${turboRecordEntity.turboPlanId}")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "no turbo plan found")
        }
        val turboInstanceEntity = turboPlanInstanceService.findInstanceById(turboRecordEntity.turboPlanInstanceId)
        if (null == turboInstanceEntity) {
            logger.info("no turbo instance found with id: ${turboRecordEntity.turboPlanInstanceId}")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "no turbo instance found")
        }
        val turboRecordDisplayVO = turboRecordService.getTurboRecordDisplayInfo(turboRecordEntity, turboPlanEntity)
        turboRecordDisplayVO.executeCount = turboInstanceEntity.executeCount
        return Response.success(turboRecordDisplayVO)
    }
}
