package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceWetestTaskResource
import com.tencent.devops.plugin.pojo.wetest.WetestAutoTestRequest
import com.tencent.devops.plugin.pojo.wetest.WetestInstStatus
import com.tencent.devops.plugin.pojo.wetest.WetestTask
import com.tencent.devops.plugin.pojo.wetest.WetestTaskInst
import com.tencent.devops.plugin.service.WetestService
import com.tencent.devops.plugin.service.WetestTaskInstService
import com.tencent.devops.plugin.service.WetestTaskService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceWetestTaskResourceImpl @Autowired constructor(
    private val wetestTaskService: WetestTaskService,
    private val wetestTaskInstService: WetestTaskInstService,
    private val wetestService: WetestService
) : ServiceWetestTaskResource {
    override fun getTask(taskId: String, projectId: String): Result<WetestTask?> {
        return Result(wetestTaskService.getTask(projectId, taskId.toInt()))
    }

    override fun saveTaskInst(wetestTaskInst: WetestTaskInst): Result<String> {
        return Result(wetestTaskInstService.saveTask(wetestTaskInst))
    }

    override fun updateTaskInstStatus(testId: String, status: WetestInstStatus): Result<String> {
        return Result(wetestTaskInstService.updateTaskInstStatus(testId, status))
    }

    override fun uploadRes(accessId: String, accessToken: String, type: String, fileParams: ArtifactorySearchParam): Result<Map<String, Any>> {
        return Result(wetestService.uploadRes(accessId, accessToken, type, fileParams))
    }

    override fun autoTest(accessId: String, accessToken: String, request: WetestAutoTestRequest): Result<Map<String, Any>> {
        return Result(wetestService.autoTest(accessId, accessToken, request))
    }

    override fun queryTestStatus(accessId: String, accessToken: String, testId: String): Result<Map<String, Any>> {
        return Result(wetestService.queryTestStatus(accessId, accessToken, testId))
    }
}