package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.GetAccountListResult
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.req.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.resp.JobCloudResp
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("GetAccountListService")
class GetAccountListService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GetAccountListService::class.java)
        private const val GET_ACCOUNT_LIST_URL_SUFFIX =
            "/?bk_scope_type=%s&bk_scope_id=%s&category=%s&account=%s&alias=%s&start=%s&length=%s"
    }
    fun getAccountList(
        userId: String,
        projectId: String,
        account: String?,
        alias: String?,
        category: Int?,
        start: Int?,
        length: Int?
    ): JobResult<GetAccountListResult> {
        AuthenticationService.set("getAccountList")
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq = authenticationService.appAuthentication(userId)
        val jobCloudResp: JobCloudResp<GetAccountListResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "get",
                url = jobCloudAuthenticationReq.url + String.format(
                    GET_ACCOUNT_LIST_URL_SUFFIX,
                    jobCloudAuthenticationReq.bkScopeType,
                    jobCloudAuthenticationReq.bkScopeId,
                    category, account, alias, start, length
                ),
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = null
            )

        var jsonData = ""
        val getAccountListResult: GetAccountListResult =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                GetAccountListResult()
            }
        if (logger.isDebugEnabled) {
            logger.debug("[getAccountList] jobCloudResp.data: ${jobCloudResp.data}")
            logger.debug("[getAccountList] serialized jsonData: $jsonData")
            logger.debug("[getAccountList] getAccountListResult: $getAccountListResult")
        }

        return JobResult(
            code = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = getAccountListResult
        )
    }
}