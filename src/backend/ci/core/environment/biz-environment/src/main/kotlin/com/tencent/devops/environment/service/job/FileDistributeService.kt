package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.JobCloudFileDistributeReq
import com.tencent.devops.environment.pojo.job.FileDistributeResult
import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudResp
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("FileDistributeService")
class FileDistributeService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FileDistributeService::class.java)
    }
    fun distributeFile(jobCloudFileDistributeReq: JobCloudFileDistributeReq): JobResult<FileDistributeResult> {
        AuthenticationService.set("distributeFile")
        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
            authenticationService.appAuthentication(jobCloudFileDistributeReq.bkUsername)
        jobCloudFileDistributeReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
        jobCloudFileDistributeReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId

        val jobCloudResp: JobCloudResp<FileDistributeResult> =
            NetworkUtil.executeHttpRequest(
                httpType = "post",
                url = jobCloudAuthenticationReq.url,
                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
                jobCloudReq = jobCloudFileDistributeReq
            )

        var jsonData = ""
        val fileDistributeResult: FileDistributeResult =
            if (null != jobCloudResp.data) {
                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
                jacksonObjectMapper().readValue(jsonData)
            } else {
                FileDistributeResult()
            }
        if (logger.isDebugEnabled) {
            logger.debug("[distributeFile] jobCloudResp.data: ${jobCloudResp.data}")
            logger.debug("[distributeFile] serialized jsonData: $jsonData")
            logger.debug("[distributeFile] fileDistributeResult: $fileDistributeResult")
        }

        return JobResult(
            status = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = fileDistributeResult
        )
    }
}