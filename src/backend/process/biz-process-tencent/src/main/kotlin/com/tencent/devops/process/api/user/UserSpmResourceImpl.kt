package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.third.spm.SpmFileInfo
import com.tencent.devops.process.service.spm.SpmService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserSpmResourceImpl @Autowired constructor(
    private val spmService: SpmService
) : UserSpmResource {

    override fun getFileInfo(projectId: String, globalDownloadUrl: String, downloadUrl: String, cmdbAppId: Int): Result<List<SpmFileInfo>> = spmService.getFileInfo(projectId, globalDownloadUrl, downloadUrl, cmdbAppId)
}