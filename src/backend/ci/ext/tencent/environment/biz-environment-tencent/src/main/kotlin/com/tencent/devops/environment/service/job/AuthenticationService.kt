package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("AuthenticationService")
class AuthenticationService {
    @Value("\${auth.appCode:}")
    private val bkAppCode = ""

    @Value("\${auth.appSecret:}")
    private val bkAppSecret = ""

    @Value("\${job.bkScopeType:#{null}}")
    val bkScopeType: String? = null

    @Value("\${job.bkScopeId:#{null}}")
    val bkScopeId: String? = null

    @Value("\${job.executeScriptProdUrl:#{null}}")
    val executeScriptProdUrl: String? = null

    @Value("\${job.executeScriptStagUrl:#{null}}")
    val executeScriptStagUrl: String? = null

    @Value("\${job.distributeFileProdUrl:#{null}}")
    val distributeFileProdUrl: String? = null

    @Value("\${job.distributeFileStagUrl:#{null}}")
    val distributeFileStagUrl: String? = null

    @Value("\${job.queryJobInstanceStatusProdUrl:#{null}}")
    val queryJobInstanceStatusProdUrl: String? = null

    @Value("\${job.queryJobInstanceStatusStagUrl:#{null}}")
    val queryJobInstanceStatusStagUrl: String? = null

    @Value("\${job.queryJobInstanceLogsProdUrl:#{null}}")
    val queryJobInstanceLogsProdUrl: String? = null

    @Value("\${job.queryJobInstanceLogsStagUrl:#{null}}")
    val queryJobInstanceLogsStagUrl: String? = null

    fun appAuthentication(
        operationName: String,
        operationEnv: String,
        bkUsername: String
    ): JobCloudAuthenticationReq {
        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"userId\": \"${bkUsername}\"}"

        var url: String?
        when (operationName) {
            "executeScript" -> {
                if ("prod" == operationEnv) {
                    url = executeScriptProdUrl
                } else {
                    url = executeScriptStagUrl
                }
            }

            "distributeFile" -> {
                if ("prod" == operationEnv) {
                    url = distributeFileProdUrl
                } else {
                    url = distributeFileStagUrl
                }
            }

            "queryJobInstanceStatus" -> {
                if ("prod" == operationEnv) {
                    url = queryJobInstanceStatusProdUrl
                } else {
                    url = queryJobInstanceStatusStagUrl
                }
            }

            "queryJobInstanceLogs" -> {
                if ("prod" == operationEnv) {
                    url = queryJobInstanceLogsProdUrl
                } else {
                    url = queryJobInstanceLogsStagUrl
                }
            }

            else -> url = ""
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
