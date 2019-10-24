package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.plugin.api.BuildEnvironmentResource
import com.tencent.devops.plugin.service.JobService
import org.springframework.beans.factory.annotation.Autowired

/**
 * @Description 为插件【作业平台-执行脚本】提供build接口
 * @author Jsonwan
 * @Date 2019/8/15
 * @version 1.0
 */

@RestResource
class BuildEnvironmentResourceImpl @Autowired constructor(
    val jobService: JobService
) : BuildEnvironmentResource {

    override fun listUsableServerEnvs(projectId: String, buildId: String): Result<List<EnvWithPermission>> {
        return jobService.listUsableServerEnvs(projectId, buildId)
    }

    override fun listRawByEnvHashIds(projectId: String, buildId: String, envHashIds: List<String>): Result<List<EnvWithPermission>> {
        return jobService.listRawByEnvHashIds(projectId, buildId, envHashIds)
    }

    override fun listRawByEnvNames(projectId: String, buildId: String, envNames: List<String>): Result<List<EnvWithPermission>> {
        return jobService.listRawByEnvNames(projectId, buildId, envNames)
    }
}