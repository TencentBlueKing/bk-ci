package com.tencent.devops.remotedev.service.gitproxy

import com.fasterxml.jackson.core.type.TypeReference
import com.google.gson.JsonParser
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.remotedev.config.TGitConfig
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
        accessToken: String,
        page: Int,
        pageSize: Int,
        search: String?,
        minAccessLevel: GitAccessLevelEnum?,
        type: TGitProjectType
    ): List<TGitProjectInfo> {
        val url = "${tGitConfig.tGitUrl}/api/v3/projects?access_token=$accessToken&page=$page&per_page=$pageSize"
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

    fun getProjectMemberAll(
        accessToken: String,
        projectId: String,
        userId: String
    ): List<TGitProjectMember>? {
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/members/all?access_token=$accessToken&query=$userId"
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
            logger.error("getProjectList error", e)
            return null
        }
    }

    fun getSvnProjectAuth(
        accessToken: String,
        projectId: String
    ): TGitSvnAuth? {
        val url = "${tGitConfig.tGitUrl}/api/v3/svn/projects/$projectId/authority?access_token=$accessToken&dir_path=/"
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
            logger.error("getProjectList error", e)
            return null
        }
    }

    fun getProjectAcl(
        accessToken: String,
        projectId: String
    ): TGitAclConfig? {
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/acl/config" +
                "?policy=offshore&access_token=$accessToken"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            doRetryHttp(request).use { response ->
                val data = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("getProjectAcl fail|{}|{}", response.code, data)
                    return null
                }
                return JsonUtil.to(data, object : TypeReference<TGitAclConfig>() {})
            }
        } catch (e: Exception) {
            logger.error("getProjectList error", e)
            return null
        }
    }

    fun updateProjectAclIp(
        accessToken: String,
        projectId: String,
        ips: Set<String>
    ): Boolean {
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/acl/config/allow_ips" +
                "?policy=offshore&access_token=$accessToken"
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

    fun updateProjectAclUser(
        accessToken: String,
        projectId: String,
        users: Set<String>
    ): Boolean {
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/acl/config/allow_users" +
                "?policy=offshore&access_token=$accessToken"
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
                    logger.error("addProjectAclUser fail|{}|{}", response.code, data)
                    return false
                }
                return true
            }
        } catch (e: Exception) {
            logger.error("addProjectAclUser error", e)
            return false
        }
    }

    fun updateProjectAclSpecIps(
        accessToken: String,
        projectId: String,
        ips: Set<String>
    ): Boolean {
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/acl/config/spec_allow_ips" +
                "?policy=offshore&access_token=$accessToken"
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
                    logger.error("updateProjectAclSpecIps fail|{}|{}", response.code, data)
                    return false
                }
                return true
            }
        } catch (e: Exception) {
            logger.error("updateProjectAclSpecIps error", e)
            return false
        }
    }

    fun updateProjectAclSpecUser(
        accessToken: String,
        projectId: String,
        users: Set<String>
    ): Boolean {
        val url = "${tGitConfig.tGitUrl}/api/v3/projects/$projectId/acl/config/spec_hit_users" +
                "?policy=offshore&access_token=$accessToken"
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
                    logger.error("updateProjectAclSpecUser fail|{}|{}", response.code, data)
                    return false
                }
                return true
            }
        } catch (e: Exception) {
            logger.error("updateProjectAclSpecUser error", e)
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

    private fun String.addParams(args: Map<String, Any?>): String {
        val sb = StringBuilder(this)
        args.forEach { (name, value) ->
            if (value != null) {
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
