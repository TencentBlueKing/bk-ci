package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.ServiceFilePushResource
import com.tencent.devops.artifactory.pojo.dto.PushFileDTO
import com.tencent.devops.artifactory.pojo.vo.PushResultVO
import com.tencent.devops.artifactory.service.PushFileServiceExt
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceFilePushResourceImpl @Autowired constructor(
    private val pushFileService: PushFileServiceExt
) : ServiceFilePushResource {
    override fun pushFile(userId: String, pushInfo: PushFileDTO): Result<Long?> {
        return pushFileService.pushFileByJob(userId, pushInfo.remoteResourceInfo, pushInfo.fileInfo)
    }

    override fun checkPushStatus(userId: String, projectId: String, jobInstanceId: Long): Result<PushResultVO?> {
        return pushFileService.checkStatus(userId, projectId, jobInstanceId)
    }
}