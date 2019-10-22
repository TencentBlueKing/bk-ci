package com.tencent.devops.plugin.codecc.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.codecc.api.ServiceCodeccResource
import com.tencent.devops.plugin.codecc.pojo.BlueShieldResponse
import com.tencent.devops.plugin.codecc.pojo.CodeccBuildInfo
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.plugin.codecc.service.CodeccService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCodeccResourceImpl @Autowired constructor(
    private val codeccService: CodeccService
) : ServiceCodeccResource {

    override fun callback(callback: CodeccCallback): Result<String> {
        return Result(codeccService.callback(callback))
    }

    override fun getCodeccBuildInfo(buildIds: Set<String>): Result<Map<String, CodeccBuildInfo>> {
        return Result(codeccService.getCodeccBuildInfo(buildIds))
    }

    override fun getCodeccTaskByProject(
        beginDate: Long?,
        endDate: Long?,
        projectIds: Set<String>
    ): Result<Map<String, BlueShieldResponse.Item>> {
        return Result(codeccService.getCodeccTaskByProject(beginDate, endDate, projectIds))
    }

    override fun getCodeccTaskByPipeline(
        beginDate: Long?,
        endDate: Long?,
        pipelineIds: Set<String>
    ): Result<Map<String, BlueShieldResponse.Item>> {
        return Result(codeccService.getCodeccTaskByPipeline(beginDate, endDate, pipelineIds))
    }

    override fun getCodeccTaskResult(
        beginDate: Long?,
        endDate: Long?,
        pipelineIds: Set<String>
    ): Result<Map<String, CodeccCallback>> {
        return Result(codeccService.getCodeccTaskResult(beginDate, endDate, pipelineIds))
    }

    override fun getCodeccTaskResult(buildIds: Set<String>): Result<Map<String, CodeccCallback>> {
        return Result(codeccService.getCodeccTaskResultByBuildIds(buildIds))
    }
}