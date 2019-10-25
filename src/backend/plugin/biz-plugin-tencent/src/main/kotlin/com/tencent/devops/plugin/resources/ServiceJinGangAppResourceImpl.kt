package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceJinGangAppResource
import com.tencent.devops.plugin.pojo.JinGangBugCount
import com.tencent.devops.plugin.service.JinGangService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceJinGangAppResourceImpl @Autowired constructor(
    private val jinGangService: JinGangService
) : ServiceJinGangAppResource {

    override fun scanApp(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: Int,
        elementId: String,
        file: String,
        isCustom: Boolean,
        runType: String
    ): Result<String> {
//        if (!jinGangService.checkLimit(projectId, pipelineId)) return Result("$projectId 超过了当天运行次数了")
        return Result(jinGangService.scanApp(userId, projectId, pipelineId, buildId, buildNo, elementId, file, isCustom, runType))
    }

    override fun countBug(projectIds: Set<String>?): Result<Map<String, JinGangBugCount>> {
        return Result(jinGangService.countBug(projectIds ?: setOf()))
    }

    override fun countRisk(projectIds: Set<String>?): Result<Map<String, Int>> {
        return Result(jinGangService.countRisk(projectIds ?: setOf()))
    }
}