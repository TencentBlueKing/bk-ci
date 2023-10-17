package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.DeleteAccountResult
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.req.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.req.JobCloudDeleteAccountReq
import com.tencent.devops.environment.pojo.job.resp.JobCloudResp
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("DeleteAccountService")
class DeleteAccountService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DeleteAccountService::class.java)
    }

    fun deleteAccount(jobCloudDeleteAccountReq: JobCloudDeleteAccountReq): JobResult<DeleteAccountResult> {
        AuthenticationService.set("deleteAccount")
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(jobCloudDeleteAccountReq.bkUsername)
        jobCloudDeleteAccountReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloudDeleteAccountReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val jobCloudResp: JobCloudResp<DeleteAccountResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "post",
                url = jobCloudAuthenticationReq.url,
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = jobCloudDeleteAccountReq
            )

        var jsonData = ""
        val deleteAccountResult: DeleteAccountResult =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                DeleteAccountResult()
            }
        if (logger.isDebugEnabled) {
            logger.debug("[deleteAccount] jobCloudResp.data: ${jobCloudResp.data}")
            logger.debug("[deleteAccount] serialized jsonData: $jsonData")
            logger.debug("[deleteAccount] deleteAccountResult: $deleteAccountResult")
        }

        return JobResult(
            status = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = deleteAccountResult
        )
    }
}