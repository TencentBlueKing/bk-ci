/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

@Suppress("ALL")
object SVNApi {

    private val logger = LoggerFactory.getLogger(SVNApi::class.java)

    fun getWebhooks(svnConfig: SVNConfig, url: String): List<String> {
        val request = request(svnConfig, composeGetUrl(svnConfig, toHttpUrl(url))).get().build()
        val body = getBody(request)
        logger.info("Get the webhook($body) of url - $url")
        val webhooks: SVNWebHook = JsonUtil.getObjectMapper().readValue(body)
        if (webhooks.webhooks.isEmpty()) {
            return emptyList()
        }

        val hooks = mutableListOf<String>()
        val path = getPath(url)
        logger.info("Get the path($path) of the svn url($url)")
        webhooks.webhooks.forEach {
            if (it.path == path) {
                val hookUrl = it.callBack
                if (hookUrl.isNotBlank()) {
                    hooks.addAll(hookUrl.split(","))
                }
            } else {
                logger.info("The path(${it.path}) is not match the expect one($path)")
            }
        }
        return hooks
    }

    fun addWebhooks(svnConfig: SVNConfig, username: String, url: String, hookUrl: String) {
        val request = request(svnConfig, composePostUrl(svnConfig, toHttpUrl(url), hookUrl, username))
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), ""))
            .build()
        val body = getBody(request)
        logger.info("Get the add hook response $body")

        val hookResponse: HookResponse = JsonUtil.getObjectMapper().readValue(body)
        if (hookResponse.status != "200") {
            logger.info("Fail to add the hook. ${hookResponse.message}")
            throw ScmException("add Svn Webhook fail，cause：${hookResponse.message}", ScmType.CODE_SVN.name)
        }
    }

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

    private fun composeGetUrl(svnConfig: SVNConfig, url: String) =
        "${svnConfig.webhookApiUrl}?event=1&apiKey=${svnConfig.apiKey}&svnUrl=$url"

    private fun composePostUrl(svnConfig: SVNConfig, url: String, hookUrl: String, userName: String) =
        "${svnConfig.webhookApiUrl}?event=1&apiKey=${svnConfig.apiKey}&svnUrl=$url&url=$hookUrl&userName=$userName"

    private fun composeSvnLockPostUrl(svnConfig: SVNConfig) = "${svnConfig.apiUrl}/svn/lock"

    private fun composeSvnUnLockPostUrl(svnConfig: SVNConfig) = "${svnConfig.apiUrl}/svn/unlock"

    private fun request(svnConfig: SVNConfig, url: String) =
        Request.Builder().url(url).header("apiKey", svnConfig.apiKey)
}
