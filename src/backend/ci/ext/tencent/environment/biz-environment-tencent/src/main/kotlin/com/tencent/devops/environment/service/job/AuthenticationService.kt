package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import com.tencent.devops.environment.pojo.job.JobCloudScriptExecuteReq
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("AuthenticationService")
class AuthenticationService {
    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""

    fun appAuthentication(
        operationName: String,
        operationEnv: String,
        bkUsername: String
    ): JobCloudAuthenticationReq {
        val bkAppCode = appCode
        val bkAppSecret = appSecret
        val bkScopeType = "" // TODO：改为配置项
        val bkScopeId = "" // TODO：改为配置项

        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"userId\": \"${bkUsername}\"}"

        val url: String = when (operationName) {
            "executeScript" -> {
                when (operationEnv) {
                    "prod" -> "https://jobv3-cloud.apigw.o.woa.com/prod/api/v3/fast_execute_script"
                    else -> "https://jobv3-cloud.apigw.o.woa.com/stag/api/v3/fast_execute_script"
                }
            }

            "distributeFile" -> {
                when (operationEnv) {
                    "prod" -> "https://jobv3-cloud.apigw.o.woa.com/prod/api/v3/fast_transfer_file"
                    else -> "https://jobv3-cloud.apigw.o.woa.com/stag/api/v3/fast_transfer_file"
                }
            }

            "queryJobInstanceStatus" -> {
                when (operationEnv) {
                    "prod" -> "https://jobv3-cloud.apigw.o.woa.com/prod/api/v3/get_job_instance_status"
                    else -> "https://jobv3-cloud.apigw.o.woa.com/stag/api/v3/get_job_instance_status"
                }
            }

            "queryJobInstanceLogs" -> {
                when (operationEnv) {
                    "prod" -> "https://jobv3-cloud.apigw.o.woa.com/prod/api/v3/batch_get_job_instance_ip_log"
                    else -> "https://jobv3-cloud.apigw.o.woa.com/stag/api/v3/batch_get_job_instance_ip_log"
                }
            }

            else -> ""
        }
        return JobCloudAuthenticationReq(
            url = url,
            bkAuthorization = bkAuthorization,
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            bkScopeType = bkScopeType,
            bkScopeId = bkScopeId
        )
    }
}
