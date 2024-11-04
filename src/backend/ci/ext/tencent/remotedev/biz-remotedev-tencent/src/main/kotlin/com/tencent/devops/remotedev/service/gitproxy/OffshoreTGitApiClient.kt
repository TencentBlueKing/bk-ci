package com.tencent.devops.remotedev.service.gitproxy

import com.fasterxml.jackson.core.type.TypeReference
import com.google.gson.JsonParser
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.TGitConfig
import com.tencent.devops.remotedev.pojo.gitproxy.TGitNamespace
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.HttpRetryException
import java.net.InetAddress
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Suppress("ALL")
@Service
class OffshoreTGitApiClient @Autowired constructor(
    private val tGitConfig: TGitConfig
) {
    fun getProjectList(
        token: TGitToken,
        page: Int,
        pageSize: Int,
        search: String?,
        minAccessLevel: GitAccessLevelEnum?,
        type: TGitProjectType,
        throwE: Boolean
    ): List<TGitProjectInfo> {
        logger.info("get /api/v3/projects|$page|$pageSize|$search|$minAccessLevel|$type")
        val url = "${tGitConfig.tGitUrl}/api/v3/projects".addQuery(
            token.key() to token.token,
            "page" to page,
            "per_page" to pageSize,
            "search" to search,
            "min_access_level" to minAccessLevel?.level,
            "type" to type.name
        )
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    if (throwE) {
                        throw RemoteServiceException("${response.code}|$data", response.code)
                    } else {
                        logger.error("getProjectList fail|{}|{}", response.code, data)
                        return emptyList()
                    }
                }
                val repoList = JsonParser.parseString(data).asJsonArray
                if (!repoList.isJsonNull) {
                    return JsonUtil.to(data, object : TypeReference<List<TGitProjectInfo>>() {})
                }
            }
        } catch (e: Exception) {
            logger.error("getProjectList error", e)
            if (throwE) {
                throw e
            }
        }
        return emptyList()
    }

    fun getProjectMemberAll(
        token: TGitToken,
        projectId: String,
        userId: String
    ): List<TGitProjectMember>? {
        logger.info("get /api/v3/projects/$projectId/members/all|$userId")
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/members/all".addQuery(
            token.key() to token.token,
            "query" to userId
        )
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                // 404 说明这个人不存在
                if (response.code == 404) {
                    return null
                }
                if (!response.isSuccessful) {
                    logger.error("getProjectMemberAll fail|{}|{}", response.code, data)
                    return null
                }
                val repoList = JsonParser.parseString(data).asJsonArray
                if (!repoList.isJsonNull) {
                    return JsonUtil.to(data, object : TypeReference<List<TGitProjectMember>>() {})
                }
                return null
            }
        } catch (e: Exception) {
            logger.error("getProjectMemberAll error", e)
            return null
        }
    }

    fun getSvnProjectAuth(
        token: TGitToken,
        projectId: String
    ): TGitSvnAuth? {
        logger.info("get api/v3/svn/projects/$projectId/authority")
        val url = "${tGitConfig.tGitUrl}/api/v3/svn/projects/$projectId/authority".addQuery(
            token.key() to token.token,
            "dir_path" to "/"
        )
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("getSvnProjectAuth fail|{}|{}", response.code, data)
                    return null
                }
                return JsonUtil.to(data, object : TypeReference<TGitSvnAuth>() {})
            }
        } catch (e: Exception) {
            logger.error("getSvnProjectAuth error", e)
            return null
        }
    }

    fun getNamespaces(
        token: TGitToken,
        page: Int,
        pageSize: Int
    ): List<TGitNamespace> {
        logger.info("get /api/v3/namespaces|$page|$pageSize")
        val url = "${tGitConfig.tGitUrl}/api/v3/namespaces".addQuery(
            token.key() to token.token,
            "page" to page,
            "per_page" to pageSize,
            "min_access_level" to GitAccessLevelEnum.MASTER.level
        )
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("getNamespaces fail|{}|{}", response.code, data)
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REQ_TGIT_API_ERROR.errorCode,
                        params = arrayOf("/api/v3/namespaces", data)
                    )
                }
                return JsonUtil.to(data, object : TypeReference<List<TGitNamespace>>() {})
            }
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: Exception) {
            logger.error("getNamespaces error", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REQ_TGIT_API_ERROR.errorCode,
                params = arrayOf("/api/v3/namespaces", e.localizedMessage)
            )
        }
    }

    fun createProject(
        token: TGitToken,
        name: String,
        namespaceId: Long?,
        svnProject: Boolean
    ): TGitProjectInfo {
        val uri = if (svnProject) {
            "/api/v3/svn/projects"
        } else {
            "/api/v3/projects"
        }

        logger.info("create offshore $uri|$name|$namespaceId")

        val url = "${tGitConfig.tGitUrl}$uri".addQuery(
            "policy" to "offshore",
            token.key() to token.token
        )

        val body = mutableMapOf(
            "name" to name
        )
        if (namespaceId != null) {
            body["namespace_id"] = namespaceId.toString()
        }

        val request = Request.Builder()
            .url(url)
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("createProject fail|{}|{}|{}", uri, response.code, data)
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REQ_TGIT_API_ERROR.errorCode,
                        params = arrayOf(uri, data)
                    )
                }
                return JsonUtil.to(data, object : TypeReference<TGitProjectInfo>() {})
            }
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: Exception) {
            logger.error("createProject $uri error", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REQ_TGIT_API_ERROR.errorCode,
                params = arrayOf(uri, e.localizedMessage)
            )
        }
    }

    fun getProjectAcl(
        token: TGitToken,
        projectId: String
    ): TGitAclConfig? {
        logger.info("$LOG_UPDATE_TGIT_ACL_TAG|offshore/acl/config|$projectId")
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/acl/config".addQuery(
            "policy" to "offshore",
            token.key() to token.token
        )
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("$LOG_UPDATE_TGIT_ACL_TAG fail|{}|{}", response.code, data)
                    return null
                }
                return JsonUtil.to(data, object : TypeReference<TGitAclConfig>() {})
            }
        } catch (e: Exception) {
            logger.error("$LOG_UPDATE_TGIT_ACL_TAG error", e)
            return null
        }
    }

    fun updateProjectAclIp(
        token: TGitToken,
        projectId: String,
        ips: Set<String>
    ): Boolean {
        logger.info("$LOG_UPDATE_TGIT_ACL_TAG|offshore|/acl/config/allow_ips|$projectId|$ips")
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/acl/config/allow_ips".addQuery(
            "policy" to "offshore",
            token.key() to token.token
        )
        val body = mapOf(
            "allow_ips" to ips.joinToString(";")
        )
        val request = Request.Builder()
            .url(url)
            .put(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("$LOG_UPDATE_TGIT_ACL_TAG fail|{}|{}", response.code, data)
                    return false
                }
                return true
            }
        } catch (e: Exception) {
            logger.error("$LOG_UPDATE_TGIT_ACL_TAG error", e)
            return false
        }
    }

    fun updateProjectAclUser(
        token: TGitToken,
        projectId: String,
        users: Set<String>
    ): Boolean {
        logger.info("$LOG_UPDATE_TGIT_ACL_TAG|offshore|/acl/config/allow_users|$projectId|$users")
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/acl/config/allow_users".addQuery(
            "policy" to "offshore",
            token.key() to token.token
        )
        val body = mapOf(
            "allow_users" to users.joinToString(";")
        )
        val request = Request.Builder()
            .url(url)
            .put(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("$LOG_UPDATE_TGIT_ACL_TAG fail|{}|{}", response.code, data)
                    return false
                }
                return true
            }
        } catch (e: Exception) {
            logger.error("$LOG_UPDATE_TGIT_ACL_TAG error", e)
            return false
        }
    }

    fun updateProjectAclSpecIps(
        token: TGitToken,
        projectId: String,
        ips: Set<String>
    ): Boolean {
        logger.info("$LOG_UPDATE_TGIT_ACL_TAG|offshore|/acl/config/spec_allow_ips|$projectId|$ips")
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/acl/config/spec_allow_ips".addQuery(
            "policy" to "offshore",
            token.key() to token.token
        )
        val body = mapOf(
            "spec_allow_ips" to ips.joinToString(";")
        )
        val request = Request.Builder()
            .url(url)
            .put(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("$LOG_UPDATE_TGIT_ACL_TAG fail|{}|{}", response.code, data)
                    return false
                }
                return true
            }
        } catch (e: Exception) {
            logger.error("$LOG_UPDATE_TGIT_ACL_TAG error", e)
            return false
        }
    }

    fun updateProjectAclSpecUser(
        token: TGitToken,
        projectId: String,
        users: Set<String>
    ): Boolean {
        logger.info("$LOG_UPDATE_TGIT_ACL_TAG|offshore|/acl/config/spec_hit_users|$projectId|$users")
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/acl/config/spec_hit_users".addQuery(
            "policy" to "offshore",
            token.key() to token.token
        )
        val body = mapOf(
            "spec_hit_users" to users.joinToString(";")
        )
        val request = Request.Builder()
            .url(url)
            .put(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("$LOG_UPDATE_TGIT_ACL_TAG fail|{}|{}", response.code, data)
                    return false
                }
                return true
            }
        } catch (e: Exception) {
            logger.error("$LOG_UPDATE_TGIT_ACL_TAG error", e)
            return false
        }
    }

    private val RETRY_CODE = listOf(429, 500)

    private fun doRetryHttp(request: Request): okhttp3.Response {
        return HttpRetryUtils.retry(retryPeriodMills = 2000) {
            val response = offshoreHttpClient.newCall(request).execute()
            if (RETRY_CODE.contains(response.code)) {
                logger.info("tgit request will be retry |${response.code}|${request.url.toUrl()}")
                throw HttpRetryException(response.message, response.code)
            }
            response
        }
    }

    private fun String.addQuery(vararg pairs: Pair<String, Any?>): String {
        val sb = StringBuilder(this)
        var flag = 0
        pairs.forEach { (name, value) ->
            if (value == null) {
                return@forEach
            }
            flag += 1
            if (flag == 1) {
                sb.append("?$name=$value")
            } else {
                sb.append("&$name=$value")
            }
        }
        return sb.toString()
    }

    private lateinit var offshoreHttpClient: OkHttpClient

    @PostConstruct
    private fun init() {
        offshoreHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(
                sslSocketFactory(),
                trustAllCerts[0] as X509TrustManager
            )
            .hostnameVerifier { _, _ -> true }
            .dns(
                TGitDns(
                    url = tGitConfig.tGitUrl.removePrefix("https://").removePrefix("http://"),
                    ips = tGitConfig.tGitIp.split(";").filter { it.isNotBlank() }.toSet()
                )
            )
            .build()
    }

    companion object {
        // 日志标志常量，方便配置告警或者搜索日志
        const val LOG_UPDATE_TGIT_ACL_TAG = "update_tgit_project_acl"

        private val logger = LoggerFactory.getLogger(OffshoreTGitApiClient::class.java)

        private fun sslSocketFactory(): SSLSocketFactory {
            try {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                return sslContext.socketFactory
            } catch (ingored: Exception) {
                throw RemoteServiceException(ingored.message!!)
            }
        }

        private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                return arrayOf()
            }
        })
    }
}

class TGitDns(
    private val url: String,
    private val ips: Set<String>
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return if (hostname == url) {
            // 返回特殊的IP地址
            ips.map { InetAddress.getByName(it) }
        } else {
            // 对于其他主机名使用系统默认的DNS解析
            Dns.SYSTEM.lookup(hostname)
        }
    }
}

data class TGitToken(
    val token: String,
    val private: Boolean
) {
    fun key() = if (private) {
        "private_token"
    } else {
        "access_token"
    }
}
