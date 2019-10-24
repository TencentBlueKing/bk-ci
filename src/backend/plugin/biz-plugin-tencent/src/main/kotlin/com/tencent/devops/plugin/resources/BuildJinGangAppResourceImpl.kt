package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.BSAuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.VSAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.BuildJinGangAppResource
import com.tencent.devops.plugin.service.JinGangService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildJinGangAppResourceImpl @Autowired constructor(
    private val jinGangService: JinGangService,
    private val bkAuthResourceApi: BSAuthResourceApi,
    private val serviceCode: VSAuthServiceCode
) : BuildJinGangAppResource {
    override fun updateTask(buildId: String, md5: String, status: Int, taskId: Long, scanUrl: String, result: String) {
        jinGangService.updateTask(buildId, md5, status, taskId, scanUrl, result)
    }

    override fun createTask(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: Int,
        userId: String,
        path: String,
        md5: String,
        size: Long,
        version: String,
        type: Int
    ): Result<Long> {
        return Result(jinGangService.createTask(projectId, pipelineId, buildId, buildNo, userId, path, md5, size, version, type))
    }

    override fun createResource(
        userId: String,
        projectId: String,
        jinGangTaskId: String,
        resourceName: String
    ): Result<Boolean> {
        // 在权限中心注册资源
        logger.info("register resources started|userId: $userId, projectId: $projectId, jinGangTaskId: $jinGangTaskId,resourceName: $resourceName")
        bkAuthResourceApi.createResource(userId, serviceCode, AuthResourceType.SCAN_TASK, projectId, HashUtil.encodeLongId(jinGangTaskId.toLong()), resourceName)
        return Result(true)
    }

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
        return Result(jinGangService.scanApp(userId, projectId, pipelineId, buildId, buildNo, elementId, file, isCustom, runType))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildJinGangAppResourceImpl::class.java)
    }
}
