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

package com.tencent.bkrepo.auth.service.bkiam

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.BaseDataResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.bk.sdk.iam.service.TokenService
import com.tencent.bkrepo.auth.constant.BASIC_AUTH_HEADER_PREFIX
import com.tencent.bkrepo.auth.exception.AuthFailedException
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.project.ProjectRangeQueryRequest
import com.tencent.bkrepo.repository.pojo.project.RepoRangeQueryRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class BkiamCallbackService @Autowired constructor(
    private val projectClient: ProjectClient,
    private val repositoryClient: RepositoryClient,
    private val tokenService: TokenService
) {
    @Value("\${auth.iam.callbackUser:}")
    private val callbackUser = ""

    private var bufferedToken = ""

    fun queryProject(token: String, request: CallbackRequestDTO): CallbackBaseResponseDTO? {
        logger.info("queryProject, token: $token, request: $request")
        checkToken(token)
        val method = request.method
        if (method == CallbackMethodEnum.FETCH_INSTANCE_INFO) {
            val ids = request.filter.idList.map { it.toString() }
            return fetchProjectInfo(ids, request.filter.attributeList)
        }
        return listProject(request.page, method)
    }

    fun queryRepo(token: String, request: CallbackRequestDTO): CallbackBaseResponseDTO? {
        logger.info("queryRepo, token: $token, request: $request")
        checkToken(token)
        val method = request.method
        val projectId = request.filter.parent.id
        if (method == CallbackMethodEnum.FETCH_INSTANCE_INFO) {
            val ids = request.filter.idList.map { it.toString() }
            return fetchRepoInfo(projectId, ids, request.filter.attributeList)
        }
        return listRepo(projectId, request.page, request.method)
    }

    private fun listRepo(projectId: String, page: PageInfoDTO?, method: CallbackMethodEnum): CallbackBaseResponseDTO? {
        logger.info("listRepo, projectId: $projectId, page: $page, method: $method")
        var offset = 0L
        var limit = 20
        if (page != null) {
            offset = page.offset
            limit = page.limit.toInt()
        }
        val repoPage = repositoryClient.rangeQuery(RepoRangeQueryRequest(listOf(), projectId, offset, limit)).data!!
        var result = ListInstanceResponseDTO()
        val repos = repoPage.records.map {
            val entity = InstanceInfoDTO()
            entity.id = it!!.name
            entity.displayName = it.name
            entity
        }
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        data.count = repoPage.totalRecords
        data.result = repos
        result.code = 0L
        result.message = ""
        result.data = data
        logger.info("listRepo, result: $result")
        return result
    }

    private fun fetchRepoInfo(projectId: String, idList: List<String>, attrs: List<String>): CallbackBaseResponseDTO? {
        logger.info("fetchRepoInfo, projectId: $projectId, idList: $idList, attrs: $attrs")
        val repoPage = repositoryClient.rangeQuery(RepoRangeQueryRequest(idList, projectId, 0, 10000)).data!!
        val repos = repoPage.records.map {
            val entity = InstanceInfoDTO()
            entity.id = it!!.name
            entity.displayName = it.name
            entity
        }
        val result = FetchInstanceInfoResponseDTO()
        result.code = 0
        result.message = ""
        result.data = repos
        return result
    }

    private fun listProject(page: PageInfoDTO?, method: CallbackMethodEnum): ListInstanceResponseDTO {
        logger.info("listProject, page $method, method $page")
        var offset = 0L
        var limit = 20
        if (page != null) {
            offset = page.offset
            limit = page.limit.toInt()
        }
        val projectPage = projectClient.rangeQuery(ProjectRangeQueryRequest(listOf(), offset, limit)).data!!
        val projects = projectPage.records.map {
            val entity = InstanceInfoDTO()
            entity.id = it!!.name
            entity.displayName = it.displayName
            entity
        }
        val result = ListInstanceResponseDTO()
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        data.count = projectPage.totalRecords
        data.result = projects
        result.code = 0L
        result.message = ""
        result.data = data
        logger.info("listProject, result: $result")
        return result
    }

    private fun fetchProjectInfo(idList: List<String>, attrs: List<String>): FetchInstanceInfoResponseDTO {
        logger.info("fetchProjectInfo, idList: $idList, attrs: $attrs")
        val projectPage = projectClient.rangeQuery(ProjectRangeQueryRequest(idList, 0, 10000)).data!!

        val projects = projectPage.records.map {
            val entity = InstanceInfoDTO()
            entity.id = it!!.name
            entity.displayName = it.displayName
            entity
        }

        val result = FetchInstanceInfoResponseDTO()
        result.code = 0
        result.message = ""
        result.data = projects
        return result
    }

    private fun checkToken(token: String) {
        val credentials = parseCredentials(token)
        val userName = credentials.first
        val password = credentials.second
        if (userName != callbackUser) {
            throw AuthFailedException("invalid iam user: $userName")
        }
        val tokenToCheck = password
        if (bufferedToken.isNotBlank() && bufferedToken == tokenToCheck) {
            return
        }
        bufferedToken = tokenService.token
        if (bufferedToken != tokenToCheck) {
            throw AuthFailedException("[$tokenToCheck] is not a valid credentials")
        }
    }

    private fun parseCredentials(token: String): Pair<String, String> {
        return try {
            val encodedCredentials = token.removePrefix(BASIC_AUTH_HEADER_PREFIX)
            val decodedToken = String(Base64.getDecoder().decode(encodedCredentials))
            val parts = decodedToken.split(StringPool.COLON)
            Pair(parts[0], parts[1])
        } catch (exception: IllegalArgumentException) {
            throw AuthFailedException("[$token] is not a valid token")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkiamCallbackService::class.java)
    }
}
