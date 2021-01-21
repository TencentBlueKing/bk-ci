package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.PipelineSettingService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineSettingResourceImpl @Autowired constructor(
    private val pipelineSettingService: PipelineSettingService
) : OpPipelineSettingResource {
    override fun updateSetting(userId: String, setting: PipelineSetting): Result<String> {
        return Result(pipelineSettingService.saveSetting(userId = userId, setting = setting))
    }

    override fun getSetting(userId: String, projectId: String, pipelineId: String): Result<PipelineSetting> {
        return Result(pipelineSettingService.userGetSetting(userId = userId, projectId = projectId, pipelineId = pipelineId))
    }
}