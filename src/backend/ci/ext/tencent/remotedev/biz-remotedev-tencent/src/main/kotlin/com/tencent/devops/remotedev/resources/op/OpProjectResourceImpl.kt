package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpProjectResource
import com.tencent.devops.remotedev.service.RemotedevProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpProjectResourceImpl @Autowired constructor(
    private val remotedevProjectService: RemotedevProjectService
) : OpProjectResource {
    override fun enableOrDisableRemotedev(userId: String, projectId: String, enable: Boolean): Result<Boolean> {
        return Result(remotedevProjectService.enableRemotedev(userId, projectId, enable))
    }

    override fun migrateOldData(userId: String, projectId: String?): Result<Boolean> {
        remotedevProjectService.migrateOldData(projectId)
        return Result(true)
    }
}
