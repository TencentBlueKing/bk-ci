package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserRemoteDevSupResource
import com.tencent.devops.remotedev.pojo.remotedevsup.DevcloudCVMData
import com.tencent.devops.remotedev.service.devcloud.DevcloudService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserRemoteDevSupResourceImpl @Autowired constructor(
    private val devcloudService: DevcloudService
) : UserRemoteDevSupResource {
    override fun cvmList(userId: String, projectId: String, page: Int, pageSize: Int): Result<Page<DevcloudCVMData>?> {
        return Result(devcloudService.fetchCVMList(userId, projectId, page, pageSize))
    }
}
