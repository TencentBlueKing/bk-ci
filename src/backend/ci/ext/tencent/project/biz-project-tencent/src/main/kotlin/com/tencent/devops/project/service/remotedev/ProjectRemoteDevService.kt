package com.tencent.devops.project.service.remotedev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.devops.auth.api.service.ServiceMonitorSpaceResource
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 将 project 中 remotedev 相关业务逻辑放到这里，避免污染
 */
@Service
class ProjectRemoteDevService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val client: Client
) {

    @Value("\${remoteDev.appCode:}")
    val appCode = ""

    @Value("\${remoteDev.appToken:}")
    val appSecret = ""

    @Value("\${remoteDev.bkMonitorUrl:}")
    val bkMonitorUrl = ""

    @Value("\${remoteDev.bkrepoDevxUrl:}")
    val bkrepoDevxUrl = ""

    @Value("\${remoteDev.bkrepoDevxHeaderUserAuth:}")
    val bkrepoDevxHeaderUserAuth = ""

    // 开启 remotedev 相关逻辑
    fun enableRemoteDev(
        userId: String,
        projectCode: String,
        projectName: String
    ) {
        // 迁移监控权限, 从 auth 获取 bizid
        if (migrateMonitorResource(listOf(projectCode))) {
            val bizId = try {
                val data = client.get(ServiceMonitorSpaceResource::class)
                    .getMonitorSpaceBizId(userId, projectCode).data
                if (data.isNullOrBlank()) {
                    logger.warn("enableRemoteDev getMonitorSpaceBizId null or blank")
                    null
                } else {
                    data.toLong()
                }
            } catch (e: Exception) {
                logger.error("enableRemoteDev getMonitorSpaceBizId error", e)
                null
            }

            // 创建监控表盘
            if (bizId != null) {
                quickImportDashboard(bizId)
            }
        }

        // 创建 lsync generic类型的仓库，做单向同步盘
        if (existRepoProject(projectCode) != true) {
            createRepoProject(projectCode, projectName)
        }
        createLsyncGeneric(projectCode)
    }

    private fun quickImportDashboard(
        bizId: Long
    ) {
        val url = "$bkMonitorUrl/quick_import_dashboard/"
        val headerStr = objectMapper.writeValueAsString(
            mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret)
        ).replace("\\s".toRegex(), "")
        val requestBody = objectMapper.writeValueAsString(QuickImportDashboardReq(bkBizId = bizId))
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("X-Bkapi-Authorization", headerStr)
            .build()

        try {
            OkhttpUtils.doHttp(request).use {
                val responseStr = it.body!!.string()
                if (!it.isSuccessful) {
                    logger.warn("quickImportDashboard request failed, uri:($url)|response: ($requestBody)")
                    return
                }
                val resp = objectMapper.readValue<BkMonitorResp>(responseStr)
                if (resp.code != 200L || !resp.result) {
                    logger.warn("quickImportDashboard request failed, url:($url)|response:($responseStr)")
                    return
                }
                return
            }
        } catch (e: Exception) {
            logger.warn("quickImportDashboard request failed", e)
        }
    }

    private fun migrateMonitorResource(projectList: List<String>): Boolean {
        return try {
            logger.debug("migrateMonitorResource projects {}", projectList)
            client.get(ServiceMonitorSpaceResource::class).migrateMonitorResource(projectList).data ?: false
        } catch (e: Exception) {
            logger.warn("migrateMonitorResource projects {} error", projectList, e)
            false
        }
    }

    private fun createLsyncGeneric(
        projectId: String
    ) {
        val requestData = CreateRepoData(
            projectId = projectId,
            name = "lsync",
            type = "GENERIC",
            category = "LOCAL",
            public = false,
            description = "repo",
            configuration = null,
            storageCredentialsKey = null
        )
        val url = "$bkrepoDevxUrl/repository/api/repo/create"
        val requestBody = objectMapper.writeValueAsString(requestData)
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .headers(getBkrepoCommonHeaders().toHeaders())
            .post(requestBody)
            .build()
        try {
            OkhttpUtils.doHttp(request).use {
                val responseStr = it.body!!.string()
                if (!it.isSuccessful) {
                    logger.warn("createLsyncGeneric request failed, uri:($url)|response: ($responseStr)")
                    return
                }
            }
        } catch (e: Exception) {
            logger.error("createLsyncGeneric request api[${request.url.toUrl()}] error: ${e.localizedMessage}")
        }
    }

    private fun existRepoProject(projectId: String): Boolean? {
        val url = "$bkrepoDevxUrl/repository/api/project/exist/$projectId"
        val request = Request.Builder()
            .url(url)
            .headers(getBkrepoCommonHeaders().toHeaders())
            .get()
            .build()
        try {
            OkhttpUtils.doHttp(request).use {
                val responseStr = it.body!!.string()
                if (!it.isSuccessful) {
                    logger.warn("existRepoProject request failed, uri:($url)|response: ($responseStr)")
                    return false
                }
                val resp = objectMapper.readValue<Response<Boolean?>>(responseStr)
                return resp.data
            }
        } catch (e: Exception) {
            logger.error("existRepoProject request api[${request.url.toUrl()}] error: ${e.localizedMessage}")
        }

        return false
    }

    private fun createRepoProject(projectId: String, projectName: String) {
        val requestData = CreateProjectData(
            name = projectId,
            displayName = projectName,
            description = ""
        )
        val url = "$bkrepoDevxUrl/repository/api/project/create"
        val requestBody = objectMapper.writeValueAsString(requestData)
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .headers(getBkrepoCommonHeaders().toHeaders())
            .post(requestBody)
            .build()
        try {
            OkhttpUtils.doHttp(request).use {
                val responseStr = it.body!!.string()
                if (!it.isSuccessful) {
                    logger.warn("createRepoProject request failed, uri:($url)|response: ($responseStr)")
                    return
                }
            }
        } catch (e: Exception) {
            logger.error("createRepoProject request api[${request.url.toUrl()}] error: ${e.localizedMessage}")
        }
    }

    private fun getBkrepoCommonHeaders(): MutableMap<String, String> {
        val headers = mutableMapOf<String, String>()
        headers["Authorization"] = bkrepoDevxHeaderUserAuth
        headers["X-BKREPO-UID"] = BKREPO_ROOT_USERID
        return headers
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectRemoteDevService::class.java)
        private const val BKREPO_ROOT_USERID = "admin"
    }
}

data class QuickImportDashboardReq(
    @JsonProperty("bk_biz_id")
    val bkBizId: Long,
    @JsonProperty("dash_name")
    val dashName: String = "bkci/BKCI-云桌面"
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BkMonitorResp(
    val result: Boolean,
    val code: Long,
    val message: String
)

data class CreateRepoData(
    val projectId: String,
    val name: String,
    val type: String,
    val category: String,
    val public: Boolean,
    val description: String?,
    val configuration: Any?,
    val storageCredentialsKey: Any?
)

data class CreateProjectData(
    val name: String,
    val displayName: String,
    val description: String
)
