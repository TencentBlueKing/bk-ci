package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.turbo.api.IOpTurboEngineConfigController
import com.tencent.devops.turbo.pojo.ParamEnumModel
import com.tencent.devops.turbo.pojo.ParamEnumSimpleModel
import com.tencent.devops.turbo.pojo.TurboEngineConfigModel
import com.tencent.devops.turbo.pojo.TurboEngineConfigPriorityModel
import com.tencent.devops.turbo.service.TurboEngineConfigService
import com.tencent.devops.turbo.vo.TurboEngineConfigVO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class OpTurboEngineConfigController @Autowired constructor(
    private val turboEngineConfigService: TurboEngineConfigService
) : IOpTurboEngineConfigController {

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

    override fun getAllEngineConfigList(): Response<List<TurboEngineConfigVO>> {
        return Response.success(turboEngineConfigService.getAllEngineConfigList())
    }

    override fun getEngineConfigByEngineCode(engineCode: String): Response<TurboEngineConfigVO> {
        return Response.success(turboEngineConfigService.queryEngineConfigInfo(engineCode))
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

    override fun updateEngineConfigPriority(
        turboPriorityList: List<TurboEngineConfigPriorityModel>,
        user: String
    ): Response<Boolean> {
        turboEngineConfigService.updateEngineConfigPriority(turboPriorityList, user)
        return Response.success(true)
    }

    override fun addWorkVersion(
        engineCode: String,
        paramEnum: ParamEnumModel
    ): Response<Boolean> {
        turboEngineConfigService.addWorkerVersion(engineCode, "worker_version", paramEnum)
        return Response.success(true)
    }

    override fun deleteWorkVersion(
        engineCode: String,
        paramValue: String
    ): Response<Boolean> {
        turboEngineConfigService.deleteWorkerVersion(engineCode, "worker_version", paramValue)
        return Response.success(true)
    }

    override fun updateWorkVersion(
        engineCode: String,
        paramValue: String,
        paramEnum: ParamEnumSimpleModel
    ): Response<Boolean> {
        turboEngineConfigService.updateWorkerVersion(engineCode, "worker_version", paramValue, paramEnum)
        return Response.success(true)
    }
}
