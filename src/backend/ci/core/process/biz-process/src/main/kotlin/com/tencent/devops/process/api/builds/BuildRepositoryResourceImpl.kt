package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.service.PipelineBuildMaterialService
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildRepositoryResourceImpl @Autowired constructor(
    private val pipelineBuildMaterialService: PipelineBuildMaterialService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService
) : BuildRepositoryResource {
    override fun saveBuildMaterial(
        buildId: String,
        projectId: String,
        pipelineId: String,
        pipelineBuildMaterials: List<PipelineBuildMaterial>
    ): Result<Int> {
        checkParam(buildId = buildId, projectId = projectId, pipelineId = pipelineId)
        checkPermission(projectId = projectId, pipelineId = pipelineId)
        return Result(pipelineBuildMaterialService.saveBuildMaterial(buildId, projectId, pipelineId, pipelineBuildMaterials))
    }

    fun checkPermission(projectId: String, pipelineId: String) {
        val userId = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)?.lastModifyUser ?: ""
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE))
            throw PermissionForbiddenException("用户${userId}无权获取此流水线构建信息")
    }

    fun checkParam(buildId: String, projectId: String, pipelineId: String) {
        if (StringUtils.isBlank(buildId))
            throw ParamBlankException("build Id is null or blank")
        if (StringUtils.isBlank(pipelineId))
            throw ParamBlankException("pipeline Id is null or blank")
    }
}