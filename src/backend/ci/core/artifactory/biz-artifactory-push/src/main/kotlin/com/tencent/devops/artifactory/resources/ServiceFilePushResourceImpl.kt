package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.ServiceFilePushResource
import com.tencent.devops.artifactory.pojo.dto.PushFileDTO
import com.tencent.devops.artifactory.service.PushFileServiceExt
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceFilePushResourceImpl @Autowired constructor(
    private val pushFileService: PushFileServiceExt
) : ServiceFilePushResource {
    override fun pushFile(userId: String, pushInfo: PushFileDTO): Result<Boolean?> {
        return pushFileService.pushFileByJob(userId, pushInfo.remoteResourceInfo, pushInfo.fileInfo)
    }
}