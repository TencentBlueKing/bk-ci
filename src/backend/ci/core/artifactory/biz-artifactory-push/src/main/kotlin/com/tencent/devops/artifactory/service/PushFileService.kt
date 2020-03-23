package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.FileResourceInfo
import com.tencent.devops.artifactory.pojo.RemoteResourceInfo
import com.tencent.devops.common.api.pojo.Result

interface PushFileService {
    fun pushFileByJob(
        userId: String,
        pushResourceInfo: RemoteResourceInfo,
        fileResourceInfo: FileResourceInfo
    ): Result<Long>
}