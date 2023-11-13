package com.tencent.devops.remotedev.service.gitproxy

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.remotedev.pojo.gitproxy.CreateProjectData
import com.tencent.devops.remotedev.pojo.gitproxy.CreateRepoData
import com.tencent.devops.remotedev.pojo.gitproxy.CreateRepoDataConfigProxy
import com.tencent.devops.remotedev.pojo.gitproxy.CreateRepoRespData
import com.tencent.devops.remotedev.pojo.gitproxy.RepoConfig
import com.tencent.devops.remotedev.pojo.gitproxy.RepoInfo
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException

@Suppress("ALL")
@Component
class GitproxyBkRepoClient @Autowired constructor(
    private val objectMapper: ObjectMapper
) {

    @Value("\${bkrepo.bkrepoDevxUrl:#{null}}")
    val bkrepoDevxUrl: String? = null

    @Value("\${bkrepo.bkrepoDevxHeaderUserAuth:#{null}}")
    val bkrepoDevxHeaderUserAuth: String? = null

    fun createRepo(
        userId: String,
        projectId: String,
        repoName: String,
        url: String,
        desc: String?,
        gitType: ScmType,
        category: BkRepoCategory,
        enableLfs: Boolean
    ): CreateRepoRespData? {
        val requestData = CreateRepoData(
            projectId = projectId,
            name = repoName,
            type = if (enableLfs) {
                "LFS"
            } else {
                when (gitType) {
                    ScmType.CODE_SVN -> "SVN"
                    else -> "GIT"
                }
            },
            category = category.name,
            public = false,
            description = desc,
            configuration = RepoConfig(
                type = category.type,
                proxy = when (category) {
                    BkRepoCategory.PROXY -> {
                        CreateRepoDataConfigProxy(
                            public = false,
                            name = "CloudDeskGroup-$repoName-proxy",
                            url = url
                        )
                    }

                    BkRepoCategory.REMOTE -> null
                },
                url = when (category) {
                    BkRepoCategory.PROXY -> null
                    BkRepoCategory.REMOTE -> url
                },
                settings = null
            ),
            display = false
        )
        logger.debug("createRepo request body {}", JsonUtil.toJson(requestData))
        val request = Request.Builder()
            .url("$bkrepoDevxUrl/repository/api/repo/create")
            .headers(getCommonHeaders(userId).toHeaders())
            .post(objectMapper.writeValueAsString(requestData).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return doRequest(request).resolveResponse<Response<CreateRepoRespData>>()?.data
    }

    fun fetchRepo(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int,
        gitType: ScmType?,
        category: BkRepoCategory
    ): Page<RepoInfo> {
        var url = "$bkrepoDevxUrl/repository/api/repo/page/$projectId/$page/$pageSize" +
                "?category=${category.name}&display=false"
        if (gitType != null) {
            url = "$url&${
                when (gitType) {
                    ScmType.CODE_SVN -> "SVN"
                    else -> "GIT"
                }
            }"
        }
        val request = Request.Builder()
            .url(url)
            .headers(getCommonHeaders(userId).toHeaders())
            .get()
            .build()
        return doRequest(request).resolveResponse<Response<Page<RepoInfo>>>()!!.data!!
    }

    fun deleteRepo(userId: String, projectId: String, repoName: String) {
        logger.info("deleteRepo, userId: $userId, projectId: $projectId, repoName: $repoName")
        val url = "$bkrepoDevxUrl/repository/api/repo/delete/$projectId/$repoName?forced=false"
        val request = Request.Builder()
            .url(url)
            .headers(getCommonHeaders(userId).toHeaders())
            .delete()
            .build()
        doRequest(request).resolveResponse<Response<Void>>()
    }

    fun existProject(userId: String, projectId: String): Boolean? {
        val url = "$bkrepoDevxUrl/repository/api/project/exist/$projectId"
        val request = Request.Builder()
            .url(url)
            .headers(getCommonHeaders(BKREPO_ROOT_USERID).toHeaders())
            .get()
            .build()
        return doRequest(request).resolveResponse<Response<Boolean?>>()!!.data
    }

    fun createProject(userId: String, projectId: String) {
        val requestData = CreateProjectData(
            name = projectId,
            displayName = projectId,
            description = ""
        )
        val request = Request.Builder()
            .url("$bkrepoDevxUrl/repository/api/project/create")
            .headers(getCommonHeaders(userId).toHeaders())
            .post(objectMapper.writeValueAsString(requestData).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        doRequest(request).resolveResponse<Response<Void>>()
    }

    private fun getCommonHeaders(userId: String): MutableMap<String, String> {
        val headers = mutableMapOf<String, String>()
        headers["Authorization"] = bkrepoDevxHeaderUserAuth ?: ""
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
            logger.debug("gitproxy request bkrepo {} resp {}", this.request.url, responseContent)
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
        private val JSON_MEDIA_TYPE = MediaTypes.APPLICATION_JSON.toMediaTypeOrNull()
        private const val BKREPO_ROOT_USERID = "admin"
    }
}

enum class BkRepoCategory(val type: String) {
    PROXY("proxy"),
    REMOTE("remote")
}
