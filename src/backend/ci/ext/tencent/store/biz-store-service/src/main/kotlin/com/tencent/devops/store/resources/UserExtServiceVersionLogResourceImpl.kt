package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceVersionLogResource
import com.tencent.devops.store.pojo.VersionLog
import com.tencent.devops.store.pojo.vo.VersionLogVO
import com.tencent.devops.store.service.ExtServiceVersionLogService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceVersionLogResourceImpl @Autowired constructor(
    private val extServiceVersionLogService: ExtServiceVersionLogService
) : UserExtServiceVersionLogResource {

    override fun getVersionLogList(userId: String, serviceId: String): Result<VersionLogVO?> {
        return extServiceVersionLogService.listVersionLog(serviceId)
    }

    override fun getVersionLog(userId: String, logId: String): Result<VersionLog> {
        return extServiceVersionLogService.getVersionLog(logId)
    }
}