/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.dockerapi.client

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.auth.pojo.user.CreateUserToProjectRequest
import com.tencent.bkrepo.common.api.constant.HttpHeaders.AUTHORIZATION
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.api.util.HumanReadable
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.dockerapi.config.BkRepoProperties
import com.tencent.bkrepo.dockerapi.constant.DEFAULT_DOCKER_REPO_NAME
import com.tencent.bkrepo.dockerapi.pojo.DockerRepo
import com.tencent.bkrepo.dockerapi.pojo.DockerTag
import com.tencent.bkrepo.dockerapi.pojo.ImageAccount
import com.tencent.bkrepo.dockerapi.pojo.QueryImageTagRequest
import com.tencent.bkrepo.dockerapi.pojo.QueryProjectImageRequest
import com.tencent.bkrepo.dockerapi.util.AccountUtils
import com.tencent.bkrepo.dockerapi.util.HttpUtils
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.project.UserProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.UserRepoCreateRequest
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.util.Random

@Component
class BkRepoClient(
    private val bkRepoProperties: BkRepoProperties
) {
    fun createProjectUser(projectId: String): ImageAccount {
        logger.info("createProjectUser, projectId: $projectId")
        val username = System.currentTimeMillis().toString() + Random().nextInt(1000).toString()
        val password = AccountUtils.generateRandomPassword(8)
        val url = "${bkRepoProperties.url}/auth/api/user/create/project"
        val reqData = CreateUserToProjectRequest(
            projectId = projectId,
            userId = username,
            name = username,
            pwd = password
        )
        val httpRequest = Request.Builder().url(url)
            .addHeader(AUTHORIZATION, bkRepoProperties.authorization)
            .addHeader(AUTH_HEADER_UID, ADMIN)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(reqData)
                )
            ).build()
        HttpUtils.doRequest(httpRequest, 1)
        return ImageAccount(username, password)
    }

    fun projectExist(projectId: String): Boolean {
        logger.info("checkProjectExist, projectId: $projectId")
        val url = "${bkRepoProperties.url}/repository/api/project/exist/$projectId"
        val httpRequest = Request.Builder().url(url)
            .addHeader(AUTHORIZATION, bkRepoProperties.authorization)
            .addHeader(AUTH_HEADER_UID, ADMIN)
            .get().build()
        val apiResponse = HttpUtils.doRequest(httpRequest, 1)
        return objectMapper.readValue<Response<Boolean>>(apiResponse.content).data!!
    }

    fun repoExist(projectId: String, repoName: String = DEFAULT_DOCKER_REPO_NAME): Boolean {
        logger.info("repoExist, projectId: $projectId")
        val url = "${bkRepoProperties.url}/repository/api/repo//exist/$projectId/$repoName"
        val httpRequest = Request.Builder().url(url)
            .addHeader(AUTHORIZATION, bkRepoProperties.authorization)
            .addHeader(AUTH_HEADER_UID, ADMIN)
            .get().build()
        val apiResponse = HttpUtils.doRequest(httpRequest, 1)
        return objectMapper.readValue<Response<Boolean>>(apiResponse.content).data!!
    }

    fun createProject(projectId: String) {
        logger.info("createProject, projectId: $projectId")
        val url = "${bkRepoProperties.url}/repository/api/project/create"
        val httpRequest = Request.Builder().url(url)
            .addHeader(AUTHORIZATION, bkRepoProperties.authorization)
            .addHeader(AUTH_HEADER_UID, ADMIN)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(UserProjectCreateRequest(projectId, projectId, ""))
                )
            ).build()
        HttpUtils.doRequest(httpRequest, 1)
    }

    fun createRepo(projectId: String, repoName: String = DEFAULT_DOCKER_REPO_NAME) {
        logger.info("createRepo, projectId: $projectId, repoName: $repoName")
        val url = "${bkRepoProperties.url}/repository/api/repo/create"
        val httpRequest = Request.Builder().url(url)
            .addHeader(AUTHORIZATION, bkRepoProperties.authorization)
            .addHeader(AUTH_HEADER_UID, ADMIN)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(UserRepoCreateRequest(projectId, repoName, RepositoryType.DOCKER))
                )
            ).build()
        HttpUtils.doRequest(httpRequest, 1)
    }

    fun queryProjectImage(request: QueryProjectImageRequest): Page<DockerRepo> {
        logger.info("queryProjectImage: $request")
        val projectId = request.projectId
        val repoName = request.repoName ?: DEFAULT_DOCKER_REPO_NAME
        val requestParams = mapOf(
            "pageNumber" to request.pageNumber.toString(),
            "pageSize" to request.pageSize.toString(),
            "packageName" to (request.searchKey ?: "")
        )
        val url = "${bkRepoProperties.url}" +
            "/repository/api/package/page/$projectId/$repoName?${HttpUtils.getQueryStr(requestParams)}"
        val httpRequest = Request.Builder().url(url)
            .addHeader(AUTHORIZATION, bkRepoProperties.authorization)
            .addHeader(AUTH_HEADER_UID, ADMIN)
            .get().build()
        val apiResponse = HttpUtils.doRequest(httpRequest, 2)
        val data = objectMapper.readValue<Response<Page<PackageSummary>>>(apiResponse.content).data!!
        val type = when (projectId) {
            "public" -> "public"
            else -> "private"
        }
        return Page(
            data.pageNumber, data.pageSize, data.totalRecords,
            data.records.map {
                DockerRepo(
                    type = type,
                    createdBy = it.createdBy,
                    created = ISO_DATE_TIME.format(it.createdDate),
                    modifiedBy = it.lastModifiedBy,
                    modified = ISO_DATE_TIME.format(it.lastModifiedDate),
                    imageName = it.name,
                    imagePath = "$projectId/$repoName/${it.name}",
                    tagCount = it.versions,
                    downloadCount = it.downloads
                )
            }
        )
    }

    fun queryImageTag(request: QueryImageTagRequest): Page<DockerTag> {
        logger.info("queryImageTag: $request")
        val projectId = request.projectId
        val repoName = request.repoName ?: DEFAULT_DOCKER_REPO_NAME
        val imageRepo = request.imageRepo
        val requestParams = mapOf(
            "pageNumber" to request.pageNumber.toString(),
            "pageSize" to request.pageSize.toString(),
            "packageKey" to "docker://${request.imageRepo}",
            "version" to ""
        )
        val url = "${bkRepoProperties.url}" +
            "/repository/api/version/page/$projectId/$repoName?${HttpUtils.getQueryStr(requestParams)}"
        val httpRequest = Request.Builder().url(url)
            .addHeader(AUTHORIZATION, bkRepoProperties.authorization)
            .addHeader(AUTH_HEADER_UID, ADMIN)
            .get().build()
        val apiResponse = HttpUtils.doRequest(httpRequest, 2)
        val data = objectMapper.readValue<Response<Page<PackageVersion>>>(apiResponse.content).data!!
        return Page(
            data.pageNumber, data.pageSize, data.totalRecords,
            data.records.map {
                DockerTag(
                    imageName = imageRepo,
                    imagePath = "$projectId/$repoName/$imageRepo",
                    tag = it.name,
                    image = "${bkRepoProperties.domain}/$projectId/$repoName/$imageRepo:${it.name}",
                    createdBy = it.createdBy,
                    created = ISO_DATE_TIME.format(it.createdDate),
                    size = HumanReadable.size(it.size),
                    modified = ISO_DATE_TIME.format(it.lastModifiedDate),
                    modifiedBy = it.lastModifiedBy
                )
            }
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoClient::class.java)
        private const val AUTH_HEADER_UID = "X-BKREPO-UID"
        private const val ADMIN = "admin"
    }
}
