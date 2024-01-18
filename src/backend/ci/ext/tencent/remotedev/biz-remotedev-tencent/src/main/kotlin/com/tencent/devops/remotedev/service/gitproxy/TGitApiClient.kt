package com.tencent.devops.remotedev.service.gitproxy

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.google.gson.JsonParser
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.net.HttpRetryException

object TGitApiClient {
    private val logger = LoggerFactory.getLogger(TGitApiClient::class.java)

    fun getProjectList(
        accessToken: String,
        gitUrl: String,
        page: Int,
        pageSize: Int,
        search: String?,
        minAccessLevel: GitAccessLevelEnum?,
        type: TGitProjectType
    ): List<TGitProjectInfo> {
        val url = "$gitUrl/api/v3/projects?access_token=$accessToken&page=$page&per_page=$pageSize"
            .addParams(
                mapOf(
                    "search" to search,
                    "min_access_level" to minAccessLevel?.level,
                    "type" to type.name
                )
            )
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("getProjectList fail|{}|{}", response.code, data)
                    return emptyList()
                }
                val repoList = JsonParser.parseString(data).asJsonArray
                if (!repoList.isJsonNull) {
                    return JsonUtil.to(data, object : TypeReference<List<TGitProjectInfo>>() {})
                }
            }
        } catch (e: Exception) {
            logger.error("getProjectList error", e)
        }
        return emptyList()
    }

    fun addProjectAclIp(
        gitUrl: String,
        accessToken: String,
        projectId: String,
        ips: Set<String>
    ): Boolean {
        val url = "$gitUrl/api/v3/projects/$projectId/acl/config/allow_ips?access_token=$accessToken"
        val body = mapOf(
            "allow_ips" to ips.joinToString(";")
        )
        val request = Request.Builder()
            .url(url)
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("addProjectAclIp fail|{}|{}", response.code, data)
                    return false
                }
                return true
            }
        } catch (e: Exception) {
            logger.error("addProjectAclIp error", e)
            return false
        }
    }

    private val RETRY_CODE = listOf(429, 500)

    private fun doRetryHttp(request: Request): okhttp3.Response {
        return HttpRetryUtils.retry(retryPeriodMills = 2000) {
            val response = OkhttpUtils.doHttp(request)
            if (RETRY_CODE.contains(response.code)) {
                logger.info("tgit request will be retry |${response.code}|${request.url.toUrl()}")
                throw HttpRetryException(response.message, response.code)
            }
            response
        }
    }

    private fun String.addParams(args: Map<String, Any?>): String {
        val sb = StringBuilder(this)
        args.forEach { (name, value) ->
            if (value != null) {
                sb.append("&$name=$value")
            }
        }
        return sb.toString()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitProjectInfo(
    @JsonProperty("http_url_to_repo")
    val httpUrlToRepo: String?,
    @JsonProperty("https_url_to_repo")
    val httpsUrlToRepo: String?,
    val permissions: TGitProjectInfoPermissions?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitProjectInfoPermissions(
    @JsonProperty("project_access")
    val projectAccess: TGitProjectInfoPermissionsAccess?,
    @JsonProperty("share_group_access")
    val shareGroupAccess: TGitProjectInfoPermissionsAccess?,
    @JsonProperty("group_access")
    val groupAccess: TGitProjectInfoPermissionsAccess?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TGitProjectInfoPermissionsAccess(
    @JsonProperty("access_level")
    val accessLevel: Int?
)

enum class TGitProjectType {
    SVN, GIT
}
