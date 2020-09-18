package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.RemoteEnvResource
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.service.EnvService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class RemoteEnvResourceImpl @Autowired constructor(
    private val envService: EnvService
) : RemoteEnvResource {

    override fun listEnvForAuth(projectId: String, offset: Int?, limit: Int?): Result<Page<EnvWithPermission>> {
        return Result(envService.listEnvironmentByLimit(projectId, offset, limit))
    }

    override fun getEnvInfos(envIds: List<String>): Result<List<EnvWithPermission>> {
        return Result(envService.listRawEnvByHashIdsAllType(envIds))
    }
}