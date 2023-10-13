package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("AuthenticationService")
class AuthenticationService {
    // @Value("\${auth.appCode:}")
    @Value("\${job.bkAppCode:}")
    private val bkAppCode = ""

    // @Value("\${auth.appSecret:}")
    @Value("\${job.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${job.bkScopeType:#{null}}")
    val bkScopeType: String? = null

    @Value("\${job.bkScopeId:#{null}}")
    val bkScopeId: String? = null

    @Value("\${job.executeScriptProdUrl:#{null}}")
    val executeScriptProdUrl: String? = null

    @Value("\${job.distributeFileProdUrl:#{null}}")
    val distributeFileProdUrl: String? = null

    @Value("\${job.terminateTaskProdUrl:#{null}}")
    val terminateTaskProdUrl: String? = null

    @Value("\${job.queryJobInstanceStatusProdUrl:#{null}}")
    val queryJobInstanceStatusProdUrl: String? = null

    @Value("\${job.queryJobInstanceLogsProdUrl:#{null}}")
    val queryJobInstanceLogsProdUrl: String? = null

    fun appAuthentication(
        operationName: String,
        bkUsername: String
    ): JobCloudAuthenticationReq {
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"bk_username\": \"${bkUsername}\"}"
        val url = when (operationName) {
            "executeScript" -> executeScriptProdUrl
            "distributeFile" -> distributeFileProdUrl
            "terminateTask" -> terminateTaskProdUrl
            "queryJobInstanceStatus" -> queryJobInstanceStatusProdUrl
            "queryJobInstanceLogs" -> queryJobInstanceLogsProdUrl
            else -> ""
        }
        return JobCloudAuthenticationReq(
            url = url ?: "",
            bkAuthorization = bkAuthorization,
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            bkScopeType = bkScopeType ?: "",
            bkScopeId = bkScopeId ?: ""
        )
    }
}
