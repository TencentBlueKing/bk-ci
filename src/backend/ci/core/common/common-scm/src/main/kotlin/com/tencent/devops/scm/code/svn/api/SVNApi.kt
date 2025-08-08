/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.scm.code.svn.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.scm.config.SVNConfig
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.LoginSession
import com.tencent.devops.scm.pojo.SvnTreeInfo
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.net.URLEncoder

@Suppress("ALL")
open class SVNApi {

    private val logger = LoggerFactory.getLogger(SVNApi::class.java)
    private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    fun lock(repname: String, applicant: String, subpath: String, svnConfig: SVNConfig) {
        val url = composeSvnLockPostUrl(svnConfig)
        val requestData = mapOf(
            "repname" to repname,
            "applicant" to applicant,
            "subpath" to listOf(subpath)
        )
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("lock the svn repo, url:$url")
        logger.info("lock the svn repo, body:$requestBody")
        val request = Request.Builder()
            .url(url)
            .addHeader("ApiKey", svnConfig.apiKey).addHeader("Content-type", "application/json")
            .post(RequestBody.create("application/json;charset=utf-8".toMediaTypeOrNull(), requestBody))
            .build()
        val body = getBody(request)
        logger.info("lock the svn repo response $body")
        val response: Boolean = JsonUtil.getObjectMapper().readValue(body)
        if (!response) {
            throw TaskExecuteException(
                errorCode = ErrorCode.THIRD_PARTY_INTERFACE_ERROR,
                errorType = ErrorType.THIRD_PARTY,
                errorMsg = "Fail to lock the svn repo"
            )
        }
    }

    fun unlock(repname: String, applicant: String, subpath: String, svnConfig: SVNConfig) {
        val url = composeSvnUnLockPostUrl(svnConfig)
        val requestData = mapOf(
            "repname" to repname,
            "applicant" to applicant,
            "subpath" to listOf(subpath)
        )
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("unlock the svn repo, url:$url")
        logger.info("unlock the svn repo, body:$requestBody")
        val request = Request.Builder()
            .url(url)
            .addHeader("ApiKey", svnConfig.apiKey).addHeader("Content-type", "application/json")
            .post(RequestBody.create("application/json;charset=utf-8".toMediaTypeOrNull(), requestBody))
            .build()
        val body = getBody(request)
        logger.info("unlock the svn repo response $body")
        val response: Boolean = JsonUtil.getObjectMapper().readValue(body)
        if (!response) {
            throw TaskExecuteException(
                errorCode = ErrorCode.THIRD_PARTY_INTERFACE_ERROR,
                errorType = ErrorType.THIRD_PARTY,
                errorMsg = "Fail to unlock the svn repo"
            )
        }
    }

    private fun getBody(request: Request): String {
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.warn("fail to get response|url=${request.url}|code=${response.code}|body=${response.body}")
                when {
                    response.code == 401 -> throw ScmException(
                        I18nUtil.getCodeLanMessage(CommonMessageCode.ENGINEERING_REPO_UNAUTHORIZED),
                        ScmType.CODE_SVN.name
                    )
                    response.code == 404 -> throw ScmException(
                        I18nUtil.getCodeLanMessage(CommonMessageCode.ENGINEERING_REPO_NOT_EXIST),
                        ScmType.CODE_SVN.name
                    )
                    else -> throw ScmException(
                        I18nUtil.getCodeLanMessage(CommonMessageCode.ENGINEERING_REPO_CALL_ERROR),
                        ScmType.CODE_SVN.name
                    )
                }
            }
            return response.body!!.string()
        }
    }

    /**
     * http://svn.xx.com/project/maven_hello_world_proj -> maven_hello_world_proj
     * http://svn.yy.com/project/maven_hello_world_proj/trunk -> maven_hello_world_proj/trunk
     */
    private fun getPath(url: String): String {
        val result = StringBuilder()

        val split = url.split("/")
        var find = false
        split.forEach {
            if (it.endsWith("_proj")) {
                result.append(it)
                find = true
                return@forEach
            }
            if (find) {
                result.append("/")
                    .append(it)
            }
        }

        return result.toString()
    }

    /**
     * svn+ssh://user@zz-svn.xx.com/project/maven_hello_world_proj ->
     *     http://zz-svn.xx.com/project/maven_hello_world_proj
     */
    private fun toHttpUrl(url: String): String {
        if (!url.startsWith("svn+ssh")) {
            return url
        }

        val suffix = url.split("//")
        if (suffix.size != 2) {
            logger.warn("Unknown svn url - $url")
            return url
        }
        return "http://" + if (suffix[1].contains("@")) {
            suffix[1].substring(suffix[1].indexOf("@") + 1)
        } else {
            suffix[1]
        }
    }

    private fun composeSvnLockPostUrl(svnConfig: SVNConfig) = "${svnConfig.apiUrl}/svn/lock"

    private fun composeSvnUnLockPostUrl(svnConfig: SVNConfig) = "${svnConfig.apiUrl}/svn/unlock"

    private fun request(svnConfig: SVNConfig, url: String) =
        Request.Builder().url(url).header("apiKey", svnConfig.apiKey)

    open fun request(
        host: String,
        token: String,
        url: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Request.Builder {
        return if (page != null && pageSize != null) Request.Builder()
            .url("$host/$url?page=$page&per_page=$pageSize")
            .header("PRIVATE-TOKEN", token)
        else Request.Builder()
            .url("$host/$url")
            .header("PRIVATE-TOKEN", token)
    }

    fun post(host: String, token: String, url: String, body: String) =
        request(host, token, url).post(RequestBody.create(mediaType, body)).build()

    fun getWebhooks(
        host: String,
        projectName: String,
        token: String
    ): List<SvnHook> {
        val fullName = URLEncoder.encode(projectName, "UTF-8")
        val request = request(
            host = host,
            url = "svn/projects/$fullName/hooks",
            token = token,
            page = 1,
            pageSize = MAX_PAGE_SIZE
        ).get().build()
        val body = getBody(request)
        logger.info("Get the webhook($body)")
        return JsonUtil.getObjectMapper().readValue<List<SvnHook>>(body)
    }

    fun addWebhooks(
        host: String,
        projectName: String,
        hookUrl: String,
        token: String,
        eventType: SvnHookEventType,
        path: String
    ): SvnHook {
        val fullName = URLEncoder.encode(projectName, "UTF-8")
        val param = mutableMapOf<String, Any>(
            "path" to path,
            "url" to hookUrl
        ).let {
            it[eventType.value] = true
            it
        }
        val request = request(
            host = host,
            url = "svn/projects/$fullName/hooks",
            token = token
        )
            .post(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(),
                    JsonUtil.toJson(param, false)
                )
            )
            .build()
        val body = getBody(request)
        logger.info("Get the add hook response $body")
        return JsonUtil.getObjectMapper().readValue(body)
    }

    fun getFileList(
        host: String,
        projectName: String,
        token: String,
        revision: String,
        path: String
    ): SvnTreeInfo {
        val fullName = URLEncoder.encode(projectName, "UTF-8")
        val queryParam = "path=$path&revision=$revision"
        val request = request(
            host = host,
            url = "svn/projects/$fullName/tree?$queryParam",
            token = token
        ).get().build()
        val body = getBody(request)
        logger.info("Get the svn file list($body)")
        return JsonUtil.getObjectMapper().readValue<SvnTreeInfo>(body)
    }

    fun getSession(
        host: String,
        username: String,
        password: String
    ): LoginSession? {
        val body = JsonUtil.toJson(
            mapOf(
                "login" to username,
                "password" to password
            ),
            false
        )
        val request = post(host, "", "session", body)
        val responseBody = getBody(request).ifBlank {
            logger.warn("get session is blank, please check the username and password")
            return null
        }
        return JsonUtil.getObjectMapper().readValue(responseBody)
    }

    companion object {
        // 接口分页最大行数
        const val MAX_PAGE_SIZE = 500
    }
}
