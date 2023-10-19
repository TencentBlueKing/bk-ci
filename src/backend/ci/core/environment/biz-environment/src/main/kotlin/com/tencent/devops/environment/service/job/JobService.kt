package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.environment.pojo.job.FileDistributeResult
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.ScriptExecuteReq
import com.tencent.devops.environment.pojo.job.ScriptExecuteResult
import com.tencent.devops.environment.pojo.job.req.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.req.JobCloudExecuteTarget
import com.tencent.devops.environment.pojo.job.req.JobCloudFileDistributeReq
import com.tencent.devops.environment.pojo.job.req.JobCloudHost
import com.tencent.devops.environment.pojo.job.req.JobCloudScriptExecuteReq
import com.tencent.devops.environment.pojo.job.resp.JobCloudResp
import com.tencent.devops.environment.service.job.api.ApigwJobCloudApi
import com.tencent.devops.environment.utils.job.NetworkUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("JobService")
class JobService @Autowired constructor(
    private val authenticationService: AuthenticationService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(JobService::class.java)
    }

    fun executeScript(userId: String, scriptExecuteReq: ScriptExecuteReq): JobResult<ScriptExecuteResult> {
        val dynamicGroupList: List<JobCloudHost> = emptyList()
        val topoNodeList: List<JobCloudHost> = emptyList()
        val jobCloudScriptExecuteReq = JobCloudScriptExecuteReq(
            scriptContent = scriptExecuteReq.scriptContent,
            scriptParam = scriptExecuteReq.scriptParam,
            timeout = scriptExecuteReq.timeout,
            accountAlias = scriptExecuteReq.account,
            isParamSensitive = scriptExecuteReq.isSensiveParam,
            scriptLanguage = scriptExecuteReq.scriptLanguage,
            targetServer = JobCloudExecuteTarget(
                hostList = scriptExecuteReq.executeTarget.hostList.map {
                    JobCloudHost(
                        bkHostId = it.bkHostId,
                        bkCloudId = it.bkCloudId,
                        ip = it.ip
                    )
                }
            ),
            bkUsername = userId
        )

        ApigwJobCloudApi.set(::executeScript.name)

//        val jobCloudAuthenticationReq: JobCloudAuthenticationReq =
//            authenticationService.appAuthentication(jobCloudScriptExecuteReq.bkUsername)
//        jobCloudScriptExecuteReq.bkScopeType = jobCloudAuthenticationReq.bkScopeType
//        jobCloudScriptExecuteReq.bkScopeId = jobCloudAuthenticationReq.bkScopeId
//
//        val jobCloudResp: JobCloudResp<ScriptExecuteResult> =
//            NetworkUtil.executeHttpRequest(
//                httpType = "post",
//                url = jobCloudAuthenticationReq.url,
//                bkAuthorization = jobCloudAuthenticationReq.bkAuthorization,
//                jobCloudReq = jobCloudScriptExecuteReq
//            )
//
//        var jsonData = ""
//        val scriptExecuteResult: ScriptExecuteResult =
//            if (null != jobCloudResp.data) {
//                jsonData = jacksonObjectMapper().writeValueAsString(jobCloudResp.data)
//                jacksonObjectMapper().readValue(jsonData)
//            } else {
//                ScriptExecuteResult()
//            }
//        if (logger.isDebugEnabled) {
//            logger.debug("[executeScript] jobCloudResp.data: ${jobCloudResp.data}")
//            logger.debug("[executeScript] serialized jsonData: $jsonData")
//            logger.debug("[executeScript] scriptExecuteResult: $scriptExecuteResult")
//        }
//
//        return JobResult(
//            code = jobCloudResp.code,
//            result = jobCloudResp.result,
//            jobRequestId = jobCloudResp.jobRequestId,
//            data = scriptExecuteResult
//        )
        return ApigwJobCloudApi().executePostRequest(userId, jobCloudScriptExecuteReq)!!

    }

    fun distributeFile(jobCloudFileDistributeReq: JobCloudFileDistributeReq): JobResult<FileDistributeResult> {
        ApigwJobCloudApi.set("distributeFile")
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
            code = jobCloudResp.code,
            result = jobCloudResp.result,
            jobRequestId = jobCloudResp.jobRequestId,
            data = fileDistributeResult
        )
    }
}