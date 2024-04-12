package com.tencent.devops.repository.sdk.tapd

import com.tencent.devops.repository.sdk.common.util.SdkHttpUtil
import okhttp3.Credentials
import org.slf4j.LoggerFactory
import java.net.URLEncoder

open class DefaultTapdClient(
    // tapd服务域名
    open val serverUrl: String,
    // tapd api域名
    open val apiUrl: String,
    // tapd 应用ID
    open val clientId: String,
    // tapd 应用secret
    open val clientSecret: String
) : TapdClient {

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultTapdClient::class.java)
    }

    override fun <T> execute(oauthToken: String, request: TapdRequest<T>): T {
        val headers = mutableMapOf(
            "Authorization" to "Bearer $oauthToken"
        )
        return SdkHttpUtil.execute(
            apiUrl = apiUrl,
            systemHeaders = headers,
            request = request
        )
    }

    override fun <T> execute(request: TapdRequest<T>): T {
        val headers = mutableMapOf(
            "Authorization" to Credentials.basic(clientId, clientSecret)
        )
        return SdkHttpUtil.execute(
            apiUrl = apiUrl,
            systemHeaders = headers,
            request = request
        )
    }

    override fun appInstallUrl(cb: String, state: String?, test: Int, showInstalled: Int?): String {
        var url =
            "$serverUrl/oauth/open_app_install?test=$test&client_id=$clientId&cb=${URLEncoder.encode(cb, "UTF-8")}"
        if (showInstalled != null) {
            url = "$url&show_installed=$showInstalled"
        }
        if (state != null) {
            url = "$url&state=${URLEncoder.encode(state, "UTF-8")}"
        }
        return url
    }

    override fun oauthUrl(redirectUri: String, scope: String, authBy: TapdAuthType, state: String): String {
        return "$serverUrl/oauth/?response_type=code&client_id=$clientId&" +
            "redirect_uri=$redirectUri&scope=${URLEncoder.encode(scope, "utf-8")}&" +
            "auth_by=${authBy.desc}&state=${URLEncoder.encode(state, "utf-8")}"
    }
}
