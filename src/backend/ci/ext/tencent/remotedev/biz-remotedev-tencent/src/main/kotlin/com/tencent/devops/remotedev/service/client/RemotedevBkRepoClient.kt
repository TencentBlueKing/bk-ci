package com.tencent.devops.remotedev.service.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.remotedev.config.BkRepoRegion
import com.tencent.devops.remotedev.config.BkRepoRegionConfig
import com.tencent.devops.remotedev.config.RemoteDevBkRepoConfig
import com.tencent.devops.remotedev.constant.BkRepoConstants
import com.tencent.devops.remotedev.pojo.bkrepo.AutoIndexConfig
import com.tencent.devops.remotedev.pojo.bkrepo.RepoConfiguration
import com.tencent.devops.remotedev.pojo.bkrepo.RepoCreateRequest
import com.tencent.devops.remotedev.pojo.bkrepo.RepoPermissionCreateRequest
import com.tencent.devops.remotedev.pojo.bkrepo.RepoSettings
import com.tencent.devops.remotedev.pojo.bkrepo.RepoToggleRequest
import com.tencent.devops.remotedev.pojo.bkrepo.TemporaryTokenCreateRequest
import com.tencent.devops.remotedev.pojo.bkrepo.TemporaryTokenCreateResponse
import com.tencent.devops.remotedev.pojo.gitproxy.CreateProjectData
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException

@Suppress("ALL")
@Service
class RemotedevBkRepoClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val bkRepoConfig: RemoteDevBkRepoConfig
) {
    fun repoStreamCreate(
        region: BkRepoRegion,
        projectId: String,
        repoName: String,
        userId: String
    ): String? {
        val config = bkRepoConfig.getRegionConfig(region)
        val request = Request.Builder()
            .url("${config.url}/media/api/user/stream/create/$projectId/$repoName?display=false")
            .headers(getCommonHeaders(region, userId).toHeaders())
            .post(
                objectMapper.writeValueAsString(JsonUtil.toJson(mapOf<String, String>()))
                    .toRequestBody(MediaTypes.APPLICATION_JSON.toMediaTypeOrNull())
            )
            .build()
        return doRequest(config, request).resolveResponse<Response<String>>()?.data
    }

    fun existProject(region: BkRepoRegion, projectId: String): Boolean? {
        val config = bkRepoConfig.getRegionConfig(region)
        val url = "${config.url}/repository/api/project/exist/$projectId"
        val request = Request.Builder()
            .url(url)
            .headers(getCommonHeaders(region, BKREPO_ROOT_USERID).toHeaders())
            .get()
            .build()
        return doRequest(config, request).resolveResponse<Response<Boolean?>>()!!.data
    }

    fun createProject(region: BkRepoRegion, userId: String, projectId: String) {
        val config = bkRepoConfig.getRegionConfig(region)
        val requestData = CreateProjectData(
            name = projectId,
            displayName = projectId,
            description = ""
        )
        val request = Request.Builder()
            .url("${config.url}/repository/api/project/create")
            .headers(getCommonHeaders(region, userId).toHeaders())
            .post(objectMapper.writeValueAsString(requestData).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        doRequest(config, request).resolveResponse<Response<Void>>()
    }

    fun pageNodeList(
        region: BkRepoRegion,
        projectId: String,
        userId: String,
        workspaceName: String,
        page: Int,
        pageSize: Int
    ): Page<BkRepoNodeDetail>? {
        val config = bkRepoConfig.getRegionConfig(region)
        val url = "${config.url}/repository/api/node/page/$projectId/$workspaceName/streams?" +
                "pageNumber=$page&pageSize=$pageSize" +
                "&includeFolder=false&includeMetadata=true&sort=true&sortProperty=createdDate&direction=DESC"
        val request = Request.Builder()
            .url(url)
            .headers(getCommonHeaders(region, userId).toHeaders())
            .get()
            .build()
        return doRequest(config, request).resolveResponse<Response<Page<BkRepoNodeDetail>>>()!!.data
    }

    fun nodeSearch(
        region: BkRepoRegion,
        userId: String,
        body: NodeSearchBody
    ): Page<BkRepoNodeDetail>? {
        val config = bkRepoConfig.getRegionConfig(region)
        val url = "${config.url}/repository/api/node/search"
        val request = Request.Builder()
            .url(url)
            .headers(getCommonHeaders(region, userId).toHeaders())
            .post(objectMapper.writeValueAsString(body).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return doRequest(config, request).resolveResponse<Response<Page<BkRepoNodeDetail>>>()!!.data
    }

    /**
     * 检查BkRepo仓库是否存在
     *
     * @param region BkRepo区域配置
     * @param projectId 项目ID
     * @param repoName 仓库名称
     * @param userId 用户ID
     * @return 仓库是否存在
     */
    fun checkRepoExist(
        region: BkRepoRegion,
        projectId: String,
        repoName: String,
        userId: String
    ): Boolean {
        val config = bkRepoConfig.getRegionConfig(region)
        val url = "${config.url}/repository/api/repo/exist/$projectId/$repoName"
        val request = Request.Builder()
            .url(url)
            .headers(getCommonHeaders(region, userId).toHeaders())
            .get()
            .build()
        return doRequest(config, request).resolveResponse<Response<Boolean>>()?.data ?: false
    }

    /**
     * 创建BkRepo仓库
     *
     * @param region BkRepo区域配置
     * @param projectId 项目ID
     * @param repoName 仓库名称
     * @param userId 用户ID
     */
    fun createRepo(
        region: BkRepoRegion,
        projectId: String,
        repoName: String,
        userId: String
    ) {
        val config = bkRepoConfig.getRegionConfig(region)
        val requestData = RepoCreateRequest(
            projectId = projectId,
            type = BkRepoConstants.REPO_TYPE,
            name = repoName,
            public = false,
            display = false,
            description = "云桌面截图仓库",
            category = BkRepoConstants.REPO_CATEGORY,
            configuration = RepoConfiguration(
                type = "local",
                settings = RepoSettings(
                    system = false,
                    autoIndex = AutoIndexConfig(enabled = false)
                )
            )
        )
        val request = Request.Builder()
            .url("${config.url}/repository/api/repo/create")
            .headers(getCommonHeaders(region, userId).toHeaders())
            .post(objectMapper.writeValueAsString(requestData).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        doRequest(config, request).resolveResponse<Response<Void>>()
        logger.info("create bkrepo repo success: projectId=$projectId, repoName=$repoName")
    }

    /**
     * 切换仓库权限模式为STRICT
     *
     * @param region BkRepo区域配置
     * @param projectId 项目ID
     * @param repoName 仓库名称
     * @param userId 用户ID
     */
    fun changeRepoToggle(
        region: BkRepoRegion,
        projectId: String,
        repoName: String,
        userId: String
    ) {
        val config = bkRepoConfig.getRegionConfig(region)
        val requestData = RepoToggleRequest(
            projectId = projectId,
            repoName = repoName,
            accessControlMode = BkRepoConstants.ACCESS_CONTROL_MODE
        )
        val request = Request.Builder()
            .url("${config.url}/auth/api/mode/repo/toggle")
            .headers(getCommonHeaders(region, userId).toHeaders())
            .post(objectMapper.writeValueAsString(requestData).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        doRequest(config, request).resolveResponse<Response<Void>>()
        logger.info("change repo toggle success: projectId=$projectId, repoName=$repoName")
    }

    /**
     * 创建仓库权限
     *
     * @param region BkRepo区域配置
     * @param projectId 项目ID
     * @param repoName 仓库名称
     * @param userId 用户ID
     */
    fun createRepoPermission(
        region: BkRepoRegion,
        projectId: String,
        repoName: String,
        userId: String
    ) {
        val config = bkRepoConfig.getRegionConfig(region)
        val requestData = RepoPermissionCreateRequest(
            resourceType = "NODE",
            permName = "$repoName-permission",
            projectId = projectId,
            repos = listOf(repoName),
            includePattern = listOf("/"),
            users = listOf(userId),
            actions = listOf("MANAGE"),
            createBy = userId,
            updatedBy = userId
        )
        val request = Request.Builder()
            .url("${config.url}/auth/api/permission/create")
            .headers(getCommonHeaders(region, userId).toHeaders())
            .post(objectMapper.writeValueAsString(requestData).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        doRequest(config, request).resolveResponse<Response<Void>>()
        logger.info("create repo permission success: projectId=$projectId, repoName=$repoName")
    }

    /**
     * 创建临时访问Token
     *
     * @param region BkRepo区域配置
     * @param projectId 项目ID
     * @param repoName 仓库名称
     * @param fullPathSet 文件路径集合
     * @param expireSeconds 过期时间（秒，默认600）
     * @param type Token类型（UPLOAD, DOWNLOAD, ALL）
     * @param userId 用户ID
     * @return 临时访问Token
     */
    fun createTemporaryAccessToken(
        region: BkRepoRegion,
        projectId: String,
        repoName: String,
        fullPathSet: List<String>,
        expireSeconds: Int = 600,
        type: String,
        userId: String
    ): String {
        val config = bkRepoConfig.getRegionConfig(region)
        val requestData = TemporaryTokenCreateRequest(
            projectId = projectId,
            repoName = repoName,
            fullPathSet = fullPathSet,
            expireSeconds = expireSeconds,
            type = type
        )
        val request = Request.Builder()
            .url("${config.url}/generic/temporary/token/create")
            .headers(getCommonHeaders(region, userId).toHeaders())
            .post(objectMapper.writeValueAsString(requestData).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val response = doRequest(config, request).resolveResponse<Response<List<TemporaryTokenCreateResponse>>>()
        return response?.data?.firstOrNull()?.token ?: throw RemoteServiceException("create temporary token failed")
    }

    private fun getCommonHeaders(region: BkRepoRegion, userId: String): MutableMap<String, String> {
        val config = bkRepoConfig.getRegionConfig(region)
        val headers = mutableMapOf<String, String>()
        headers["Authorization"] = config.headerUserAuth
        headers["X-BKREPO-UID"] = userId
        return headers
    }

    private fun doRequest(config: BkRepoRegionConfig, request: Request): okhttp3.Response {
        try {
            return if (!config.dnsIp.isNullOrBlank()) {
                val ips = config.dnsIp.split(";").filter { it.isNotBlank() }.map { it.trim() }.toSet()
                val client =
                    OkhttpUtils.genOkHttpClientSupDns(config.url.removePrefix("http://").removePrefix("https://"), ips)
                client.newCall(request).execute()
            } else {
                OkhttpUtils.doHttp(request)
            }
        } catch (e: IOException) {
            throw RemoteServiceException("request api[${request.url.toUrl()}] error: ${e.localizedMessage}")
        }
    }

    private inline fun <reified T> okhttp3.Response.resolveResponse(allowCode: Int? = null): T? {
        this.use {
            val responseContent = this.body!!.string()
            logger.debug("remotedev request bkrepo {} resp {}", this.request.url, responseContent)
            if (this.isSuccessful) {
                return objectMapper.readValue(responseContent, jacksonTypeRef<T>())
            }

            val responseData = try {
                objectMapper.readValue<Response<Void>>(responseContent)
            } catch (e: JacksonException) {
                throw RemoteServiceException(responseContent, this.code)
            }
            if (allowCode == responseData.code) {
                logger.info("request bkrepo api failed but it can be allowed: ${responseData.message}")
                return null
            }
            throw RemoteServiceException(responseData.message ?: responseData.code.toString(), this.code)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoClient::class.java)
        private const val BKREPO_ROOT_USERID = "admin"
        private val JSON_MEDIA_TYPE = MediaTypes.APPLICATION_JSON.toMediaTypeOrNull()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BkRepoNodeDetail(
    val name: String,
    val fullPath: String,
    val metadata: BkRepoNodeDetailMetadata?,
    val size: Long?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BkRepoNodeDetailMetadata(
    @JsonProperty("media.startTime")
    val mediaStartTime: Long?,
    @JsonProperty("media.stopTime")
    val mediaStopTime: Long?
)

// 需要参考bkrepo的节点自定义搜索文档，太复杂这里写不下
data class NodeSearchBody(
    val select: List<String>,
    val page: NodeSearchPage,
    val sort: NodeSearchSort,
    val rule: NodeSearchRule
)

data class NodeSearchPage(
    val pageNumber: Int,
    val pageSize: Int
)

data class NodeSearchSort(
    val properties: List<String>,
    val direction: String
)

data class NodeSearchRule(
    val rules: List<NodeSearchRulesItem>,
    val relation: String
)

data class NodeSearchRulesItem(
    val field: String,
    val value: Any,
    val operation: String
)
