package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.FileDistributeReq
import com.tencent.devops.environment.pojo.job.FileDistributeResult
import org.springframework.stereotype.Service

@Service("FileDistributeService")
class FileDistributeService {
    fun distributeFile(
        userId: String,
        projectId: String,
        fileDistributeReq: FileDistributeReq
    ): FileDistributeResult {
        TODO()
    }
}