package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.CreateAccountResult
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.req.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.req.JobCloudCreateAccountReq
import com.tencent.devops.environment.pojo.job.resp.JobCloudResp
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("CreateAccountService")
class CreateAccountService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CreateAccountService::class.java)
    }

    fun createAccount(jobCloudCreateAccountReq: JobCloudCreateAccountReq): JobResult<CreateAccountResult> {
        AuthenticationService.set("createAccount")
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(jobCloudCreateAccountReq.bkUsername)
        jobCloudCreateAccountReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloudCreateAccountReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val jobCloudResp: JobCloudResp<CreateAccountResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "post",
                url = jobCloudAuthenticationReq.url,
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = jobCloudCreateAccountReq
            )

        var jsonData = ""
        val createAccountResult: CreateAccountResult =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                CreateAccountResult()
            }
        if (logger.isDebugEnabled) {
            logger.debug("[createAccount] jobCloudResp.data: ${jobCloudResp.data}")
            logger.debug("[createAccount] serialized jsonData: $jsonData")
            logger.debug("[createAccount] createAccountResult: $createAccountResult")
        }

        return JobResult(
            code = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = createAccountResult
        )
    }
}