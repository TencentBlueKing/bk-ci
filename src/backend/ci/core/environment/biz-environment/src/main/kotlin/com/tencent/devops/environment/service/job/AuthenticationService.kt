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

    fun appAuthentication(
        operationName: String,
        bkUsername: String
    ): JobCloudAuthenticationReq {
        val logger = LoggerFactory.getLogger(AuthenticationService::class.java)

        val bkAuthorization = "{\"bk_app_code\": \"${bkAppCode}\", " +
            "\"bk_app_secret\": \"${bkAppSecret}\", \"bk_username\": \"${bkUsername}\"}"

        val threadLocal = ThreadLocal<String>()
        logger.debug("[appAuthentication] threadLocal01: $threadLocal")
        threadLocal.set("executeScript")
        logger.debug("[appAuthentication] threadLocal02: $threadLocal")
        val localVal = threadLocal.get()
        logger.debug("[appAuthentication] localVal: $localVal")
//        val localVal0 = ThreadLocal<String>().set("executeScript")
//        val localVal = ThreadLocal<String>().get()
        val localVal2 = ThreadLocal<String>().toString()
        logger.debug("[appAuthentication] localVal2: $localVal2")
        val localVal3 = threadLocal.toString()
        logger.debug("[appAuthentication] localVal3: $localVal3")
        ThreadLocal<String>().remove()
        logger.debug("[appAuthentication] localVal11: $localVal")
        logger.debug("[appAuthentication] threadLocal11: $threadLocal")
        threadLocal.remove()
        logger.debug("[appAuthentication] localVal22: $localVal")
        logger.debug("[appAuthentication] threadLocal22: $threadLocal")
//        logger.debug("[appAuthentication] thread local localval.get(): $localVal, operationName: $operationName")
//        logger.debug("[appAuthentication] thread local localval.toString(): $localVal2")
//        logger.debug("[appAuthentication] current thread id: ${Thread.currentThread().id}")
//        logger.debug("[appAuthentication] thread local localval.remove(): $localVal2")
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
}
