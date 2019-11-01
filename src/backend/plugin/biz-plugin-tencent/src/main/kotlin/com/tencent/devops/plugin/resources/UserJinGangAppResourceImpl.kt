package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.UserJinGangAppResource
import com.tencent.devops.plugin.pojo.JinGangAppResponse
import com.tencent.devops.plugin.pojo.JinGangAppResultReponse
import com.tencent.devops.plugin.service.JinGangService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserJinGangAppResourceImpl @Autowired constructor(
    private val jinGangService: JinGangService
) : UserJinGangAppResource {
    override fun getList(projectId: String, page: Int, pageSize: Int): Result<JinGangAppResponse?> {
        val resultList = jinGangService.getList(projectId, page, pageSize)
        val resultCount = jinGangService.getCount(projectId)
        return Result(data = JinGangAppResponse(
                count = resultCount,
                page = page,
                pageSize = pageSize,
                totalPages = resultCount / pageSize + 1,
                records = resultList
        ))
    }

    override fun getAppResult(userId: String, taskId: Long): Result<JinGangAppResultReponse?> {
        return Result(data = jinGangService.getAppResult(userId, taskId))
    }

    override fun scanApp(userId: String, projectId: String, pipelineId: String, buildId: String, buildNo: Int, file: String, isCustom: Boolean, runType: String): Result<String> {
        return Result(jinGangService.scanApp(userId, projectId, pipelineId, buildId, buildNo, "",
                file, isCustom, runType, true))
    }
}