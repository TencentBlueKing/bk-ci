package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.IS_NOT_ADMIN_MEMBER
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.constants.NO_ADMIN_MEMBER_MESSAGE
import com.tencent.devops.turbo.api.IServiceTurboController
import com.tencent.devops.turbo.pojo.TurboRecordModel
import com.tencent.devops.turbo.service.TurboAuthService
import com.tencent.devops.turbo.service.TurboPlanService
import com.tencent.devops.turbo.service.TurboRecordService
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import com.tencent.devops.turbo.vo.TurboPlanStatRowVO
import com.tencent.devops.turbo.vo.TurboRecordHistoryVO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class ServiceTurboController @Autowired constructor(
    private val turboPlanService: TurboPlanService,
    private val turboRecordService: TurboRecordService,
    private val turboAuthService: TurboAuthService
) : IServiceTurboController {

    override fun getTurboPlanByProjectIdAndCreatedDate(
        projectId: String,
        startTime: LocalDate?,
        endTime: LocalDate?,
        pageNum: Int?,
        pageSize: Int?,
        userId: String
    ): Response<Page<TurboPlanStatRowVO>> {
        // 判断是否是管理员
        if (!turboAuthService.getAuthResult(projectId, userId)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return Response.success(
            turboPlanService.getTurboPlanByProjectIdAndCreatedDate(projectId, startTime, endTime, pageNum, pageSize))
    }

    override fun getTurboRecordHistoryList(
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?,
        turboRecordModel: TurboRecordModel,
        projectId: String,
        userId: String
    ): Response<Page<TurboRecordHistoryVO>> {
        // 判断是否是管理员
        if (!turboAuthService.getAuthResult(projectId, userId)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return Response.success(
            turboRecordService.getTurboRecordHistoryList(pageNum, pageSize, sortField, sortType, turboRecordModel))
    }

    override fun getTurboPlanDetailByPlanId(
        planId: String,
        projectId: String,
        userId: String
    ): Response<TurboPlanDetailVO> {
        // 判断是否是管理员
        if (!turboAuthService.getAuthResult(projectId, userId)) {
            throw TurboException(errorCode = IS_NOT_ADMIN_MEMBER, errorMessage = NO_ADMIN_MEMBER_MESSAGE)
        }
        return Response.success(turboPlanService.getTurboPlanDetailByPlanId(planId))
    }
}
