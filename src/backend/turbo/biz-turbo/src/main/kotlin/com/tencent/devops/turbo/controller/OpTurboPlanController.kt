package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.turbo.api.IOpTurboPlanController
import com.tencent.devops.turbo.pojo.TurboPlanModel
import com.tencent.devops.turbo.service.TurboPlanService
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@Suppress("MaxLineLength")
@RestController
class OpTurboPlanController @Autowired constructor(
    private val turboPlanService: TurboPlanService
) : IOpTurboPlanController {

    override fun updateTurboPlan(
        turboPlanModel: TurboPlanModel,
        planId: String,
        user: String
    ): Response<Boolean> {
        return Response.success(turboPlanService.updateTurboPlanInfo(turboPlanModel, planId, user))
    }

    override fun getAllTurboPlanList(
        turboPlanId: String?,
        planName: String?,
        projectId: String?,
        pageNum: Int?,
        pageSize: Int?
    ): Response<Page<TurboPlanDetailVO>> {
        return Response.success(turboPlanService.getAllTurboPlanList(turboPlanId, planName, projectId, pageNum, pageSize))
    }
}
