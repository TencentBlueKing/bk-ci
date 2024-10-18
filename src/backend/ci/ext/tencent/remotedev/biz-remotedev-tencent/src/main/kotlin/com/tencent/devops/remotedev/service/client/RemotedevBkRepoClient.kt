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
import com.tencent.devops.remotedev.config.RemoteDevBkRepoConfig
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
        return doRequest(request).resolveResponse<Response<String>>()?.data
    }

    fun existProject(region: BkRepoRegion, projectId: String): Boolean? {
        val config = bkRepoConfig.getRegionConfig(region)
        val url = "${config.url}/repository/api/project/exist/$projectId"
        val request = Request.Builder()
            .url(url)
            .headers(getCommonHeaders(region, BKREPO_ROOT_USERID).toHeaders())
            .get()
            .build()
        return doRequest(request).resolveResponse<Response<Boolean?>>()!!.data
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
        doRequest(request).resolveResponse<Response<Void>>()
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
        return doRequest(request).resolveResponse<Response<Page<BkRepoNodeDetail>>>()!!.data
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
        return doRequest(request).resolveResponse<Response<Page<BkRepoNodeDetail>>>()!!.data
    }

    private fun getCommonHeaders(region: BkRepoRegion, userId: String): MutableMap<String, String> {
        val config = bkRepoConfig.getRegionConfig(region)
        val headers = mutableMapOf<String, String>()
        headers["Authorization"] = config.headerUserAuth
        headers["X-BKREPO-UID"] = userId
        return headers
    }

    private fun doRequest(request: Request): okhttp3.Response {
        try {
            return OkhttpUtils.doHttp(request)
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
    val metadata: BkRepoNodeDetailMetadata?
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