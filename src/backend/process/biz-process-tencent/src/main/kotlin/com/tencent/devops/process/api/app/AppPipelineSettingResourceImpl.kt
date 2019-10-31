package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.setting.PipelineSetting
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppPipelineSettingResourceImpl @Autowired constructor(
    private val pipelineSettingService: PipelineSettingService
) : AppPipelineSettingResource {
    override fun getPipelineSetting(userId: String, projectId: String, pipelineId: String): Result<PipelineSetting> {
        return Result(pipelineSettingService.userGetSetting(userId, projectId, pipelineId))
    }

    override fun saveSetting(
        userId: String,
        projectId: String,
        pipelineId: String,
        setting: PipelineSetting
    ): Result<String> {
        return Result(pipelineSettingService.saveSetting(userId, setting, false))
    }
}