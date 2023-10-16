package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.pojo.job.JobCloudAuthenticationReq
import org.slf4j.LoggerFactory
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

    @Value("\${job.jobCloudProdUrlPrefix:#{null}}")
    val jobCloudProdUrlPrefix: String? = null

    @Value("\${job.executeScriptPath:#{null}}")
    val executeScriptPath: String? = null

    @Value("\${job.distributeFilePath:#{null}}")
    val distributeFilePath: String? = null

    @Value("\${job.terminateTaskPath:#{null}}")
    val terminateTaskPath: String? = null

    @Value("\${job.queryJobInstanceStatusPath:#{null}}")
    val queryJobInstanceStatusPath: String? = null

    @Value("\${job.queryJobInstanceLogsPath:#{null}}")
    val queryJobInstanceLogsPath: String? = null

    fun appAuthentication(bkUsername: String): JobCloudAuthenticationReq {
        val logger = LoggerFactory.getLogger(AuthenticationService::class.java)

        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"bk_username\": \"${bkUsername}\"}"

        val operationName = get()
        logger.debug("[appAuthentication] operationName: $operationName")
        val url = jobCloudProdUrlPrefix + when (operationName) {
            "executeScript" -> executeScriptPath
            "distributeFile" -> distributeFilePath
            "terminateTask" -> terminateTaskPath
            "queryJobInstanceStatus" -> queryJobInstanceStatusPath
            "queryJobInstanceLogs" -> queryJobInstanceLogsPath
            else -> ""
        }
        logger.debug("[appAuthentication] url: $url")
        return JobCloudAuthenticationReq(
            url = url,
            bkAuthorization = bkAuthorization,
            bkScopeType = bkScopeType ?: "",
            bkScopeId = bkScopeId ?: ""
        )
    }

    companion object {
        private val threadLocal = ThreadLocal<String>()
        fun set(value: String) {
            threadLocal.set(value)
        }
        fun get(): String? {
            return threadLocal.get()
        }
        fun remove() {
            threadLocal.remove()
        }
    }

}
