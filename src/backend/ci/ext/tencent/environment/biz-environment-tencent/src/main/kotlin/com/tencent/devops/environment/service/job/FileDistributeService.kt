package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.job.JobCloudFileDistributeReq
import com.tencent.devops.environment.pojo.job.FileDistributeResult
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("FileDistributeService")
class FileDistributeService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    fun distributeFile(jobCloudFileDistributeReq: JobCloudFileDistributeReq): Result<FileDistributeResult> {
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(
                operationName = "distributeFile",
                operationEnv = "stag",
                bkUsername = jobCloudFileDistributeReq.bkUsername
            )
        jobCloudFileDistributeReq.bkAppCode = jobCloudAuthenticationReq.bkAppCode
        jobCloudFileDistributeReq.bkAppSecret = jobCloudAuthenticationReq.bkAppSecret
        jobCloudFileDistributeReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloudFileDistributeReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val jobCloudResp: JobCloudResp<FileDistributeResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "post",
                operateName = "distributeFile",
                url = jobCloudAuthenticationReq.url,
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = jobCloudFileDistributeReq.javaClass
            )

        val fileDistributeResult = FileDistributeResult(
            jobInstanceId = jobCloudResp.data?.jobInstanceId ?: 0L,
            jobInstanceName = jobCloudResp.data?.jobInstanceName ?: "",
            stepInstanceId = jobCloudResp.data?.stepInstanceId ?: 0L
        )
        return Result(
            status = jobCloudResp.code,
            message = jobCloudResp.message,
            data = fileDistributeResult
        )
    }
}