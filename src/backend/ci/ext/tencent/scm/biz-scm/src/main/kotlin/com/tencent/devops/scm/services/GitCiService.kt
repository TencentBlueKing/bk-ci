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

package com.tencent.devops.scm.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.repository.pojo.git.GitMember
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitBranchCommit
import com.tencent.devops.scm.code.git.api.GitOauthApi
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import io.swagger.annotations.ApiParam
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import javax.ws.rs.QueryParam

@Service
class GitCiService {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCiService::class.java)
    }

    @Value("\${gitCI.clientId}")
    private lateinit var gitCIClientId: String

    @Value("\${gitCI.clientSecret}")
    private lateinit var gitCIClientSecret: String

    @Value("\${gitCI.url}")
    private lateinit var gitCIUrl: String

    @Value("\${gitCI.oauthUrl}")
    private lateinit var gitCIOauthUrl: String

    fun getGitCIMembers(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?
    ): List<GitMember> {
        val url = "$gitCIUrl/api/v3/projects/${URLEncoder.encode(gitProjectId, "UTF8")}/members" +
                if (search != null) {
                    "?query=$search"
                } else {
                    ""
                } +
                "&page=$page" + "&per_page=$pageSize" +
                "&access_token=$token"
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            val data = it.body()!!.string()
            if (!it.isSuccessful) throw RuntimeException("fail to get the git projects members with: $url($data)")
            return JsonUtil.to(data, object : TypeReference<List<GitMember>>() {})
        }
    }

    fun getBranch(
        token: String,
        gitProjectId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        orderBy: GitCodeBranchesOrder?,
        sort: GitCodeBranchesSort?
    ): List<String> {
        val url = "${gitCIUrl}/projects/${URLEncoder.encode(gitProjectId, "utf-8")}" +
                "/repository/branches?access_token=$token&page=$page&per_page=$pageSize" +
                if (search != null) {
                    "&search=$search"
                } else {
                    ""
                } +
                if (orderBy != null) {
                    "&order_by=${orderBy.value}"
                } else {
                    ""
                } +
                if (sort != null) {
                    "&sort=${sort.value}"
                } else {
                    ""
                }
        val res = mutableListOf<String>()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()?.string() ?: return@use
            val branList = JsonParser.parseString(data).asJsonArray
            if (!branList.isJsonNull) {
                branList.forEach {
                    val branch = it.asJsonObject
                    if (branch.isJsonNull) {
                        return@forEach
                    }
                    res.add(if (branch["name"].isJsonNull) "" else branch["name"].asString)
                }
            }
        }
        return res
    }
}
