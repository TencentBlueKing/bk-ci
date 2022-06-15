package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.IS_NOT_ADMIN_MEMBER
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.constants.NO_ADMIN_MEMBER_MESSAGE
import com.tencent.devops.turbo.api.IUserTurboPlanController
import com.tencent.devops.turbo.pojo.TurboPlanModel
import com.tencent.devops.turbo.service.TurboAuthService
import com.tencent.devops.turbo.service.TurboPlanService
import com.tencent.devops.turbo.vo.TurboMigratedPlanVO
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import com.tencent.devops.turbo.vo.TurboPlanPageVO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@Suppress("MaxLineLength")
@RestController
class UserTurboPlanController @Autowired constructor(
    private val turboPlanService: TurboPlanService,
    private val turboAuthService: TurboAuthService
) : IUserTurboPlanController {

    override fun addNewTurboPlan(turboPlanModel: TurboPlanModel, projectId: String, user: String): Response<String?> {
        // 判断是否是管理员
        if (!turboAuthService.getAuthResult(projectId, user)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return Response.success(turboPlanService.addNewTurboPlan(turboPlanModel, user))
    }

    override fun getTurboPlanStatRowData(projectId: String, pageNum: Int?, pageSize: Int?, user: String): Response<TurboPlanPageVO> {
        // 判断是否是管理员
        if (!turboAuthService.getAuthResult(projectId, user)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return Response.success(turboPlanService.getTurboPlanStatRowData(projectId, pageNum, pageSize))
    }

    override fun getTurboPlanDetailByPlanId(planId: String, projectId: String, user: String): Response<TurboPlanDetailVO> {
        // 判断是否是管理员
        if (!turboAuthService.getAuthResult(projectId, user)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return Response.success(turboPlanService.getTurboPlanDetailByPlanId(planId))
    }

    override fun putTurboPlanDetailNameAndOpenStatus(turboPlanModel: TurboPlanModel, planId: String, user: String, projectId: String): Response<Boolean> {
        // 判断是否是管理员
        if (!turboAuthService.getAuthResult(projectId, user)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return Response.success(turboPlanService.putTurboPlanDetailNameAndOpenStatus(turboPlanModel, planId, user))
    }

    override fun putTurboPlanConfigParam(turboPlanModel: TurboPlanModel, planId: String, user: String, projectId: String): Response<Boolean> {
        // 判断是否是管理员
        if (!turboAuthService.getAuthResult(projectId, user)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return Response.success(turboPlanService.putTurboPlanConfigParam(turboPlanModel, planId, user))
    }

    override fun putTurboPlanTopStatus(planId: String, topStatus: String, user: String): Response<Boolean> {
        return Response.success(turboPlanService.putTurboPlanTopStatus(planId, topStatus, user))
    }

    override fun getAvailableTurboPlanList(projectId: String, pageNum: Int?, pageSize: Int?): Response<Page<TurboPlanDetailVO>> {
        return Response.success(turboPlanService.getAvailableProjectIdList(projectId, pageNum, pageSize))
    }

    override fun findTurboPlanIdByProjectIdAndPipelineInfo(projectId: String, pipelineId: String, pipelineElementId: String): Response<TurboMigratedPlanVO?> {
        return Response.success(turboPlanService.findMigratedTurboPlanByPipelineInfo(projectId, pipelineId, pipelineElementId))
    }
}
