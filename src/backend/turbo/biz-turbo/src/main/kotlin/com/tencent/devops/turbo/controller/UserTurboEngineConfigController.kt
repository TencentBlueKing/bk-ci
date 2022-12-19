package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.turbo.api.IUserTurboEngineConfigController
import com.tencent.devops.turbo.pojo.ParamEnumModel
import com.tencent.devops.turbo.pojo.TurboEngineConfigModel
import com.tencent.devops.turbo.service.TurboEngineConfigService
import com.tencent.devops.turbo.service.TurboPlanService
import com.tencent.devops.turbo.vo.TurboEngineConfigVO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class UserTurboEngineConfigController @Autowired constructor(
    private val turboEngineConfigService: TurboEngineConfigService,
    private val turboPlanService: TurboPlanService
) : IUserTurboEngineConfigController {

    companion object {
        private val logger = LoggerFactory.getLogger(UserTurboEngineConfigController::class.java)
    }

    override fun getEngineConfigList(
        projectId: String
    ): Response<List<TurboEngineConfigVO>> {
        return Response.success(turboEngineConfigService.getEngineConfigList(projectId))
    }

    override fun addNewEngineConfig(
        turboEngineConfigModel: TurboEngineConfigModel,
        user: String
    ): Response<Long?> {
        return Response.success(turboEngineConfigService.addNewEngineConfig(turboEngineConfigModel, user))
    }

    override fun deleteEngineConfig(engineCode: String, user: String): Response<Boolean> {
        turboEngineConfigService.deleteEngineConfig(engineCode, user)
        return Response.success(true)
    }

    override fun updateEngineConfig(engineCode: String, status: Boolean, user: String): Response<Boolean> {
        if (status) {
            turboEngineConfigService.resumeEngineConfig(engineCode, user)
        } else {
            turboEngineConfigService.disableEngineConfig(engineCode, user)
        }
        return Response.success(true)
    }

    override fun getEngineConfigByEngineCode(engineCode: String): Response<TurboEngineConfigVO> {
        return Response.success(turboEngineConfigService.queryEngineConfigInfo(engineCode))
    }

    override fun getRecommendEngineConfig(): Response<List<TurboEngineConfigVO>> {
        return Response.success(turboEngineConfigService.queryRecommendEngineConfig())
    }

    override fun updateEngineConfig(
        engineCode: String,
        turboEngineConfigModel: TurboEngineConfigModel,
        user: String
    ): Response<Boolean> {
        turboEngineConfigService.updateEngineConfig(
            engineCode, turboEngineConfigModel, user
        )
        return Response.success(true)
    }

    override fun getEngineInfoByPlanId(
        planId: String
    ): Response<TurboEngineConfigVO?> {
        val turboPlanDetailVO = turboPlanService.getTurboPlanDetailByPlanId(planId)
        logger.info("get turbo plan info, plan id: $planId, engine code: ${turboPlanDetailVO.engineCode}")
        if (turboPlanDetailVO.engineCode.isBlank()) {
            return Response.success(null)
        }
        return Response.success(turboEngineConfigService.queryEngineConfigInfo(turboPlanDetailVO.engineCode))
    }

    override fun getCompilerVersionListByQueueName(
        engineCode: String,
        projectId: String,
        queueName: String?
    ): Response<List<ParamEnumModel>> {
        return Response.success(
            turboEngineConfigService.getCompilerVersionListByQueueName(
                engineCode = engineCode,
                projectId = projectId,
                queueName = queueName
            )
        )
    }
}
