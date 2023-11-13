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

package com.tencent.devops.project.resources

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.callback.AuthConstants
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.service.ProjectService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AuthProjectService @Autowired constructor(
    val projectService: ProjectService,
    val authTokenApi: AuthTokenApi,
    val client: Client,
    val tokenService: ClientTokenService,
    val bkTag: BkTag
) {

    // 项目-管理员列表 缓存， 5分钟有效时间
    private val projectManager = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, List<String>>()

    fun getProjectList(page: PageInfoDTO?, token: String): ListInstanceResponseDTO {
        logger.info("getProjectList page $page, token: $token ")
        authTokenApi.checkToken(token)
        var offset = 0
        var limit = AuthConstants.MAX_LIMIT
        if (page != null) {
            offset = page.offset.toInt()
            limit = page.limit.toInt()
        }
        val projectRecords = projectService.listByChannel(limit, offset, ProjectChannelCode.BS)
        val count = projectRecords?.count
        val projectInfo = mutableListOf<InstanceInfoDTO>()
        projectRecords?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.projectName
            projectInfo.add(entity)
        }
        logger.info("projectInfo $projectInfo")
        val result = ListInstanceInfo()
        return result.buildListInstanceResult(projectInfo, count)
    }

    fun getProjectInfo(
        idList: List<String>,
        token: String,
        attribute: List<String>
    ): FetchInstanceInfoResponseDTO {
        logger.info("getProjectInfo ids[$idList], attribute[$attribute]")
        authTokenApi.checkToken(token)
        val ids = idList.toSet()
        val projectInfo = projectService.list(ids)
        val entityList = mutableListOf<InstanceInfoDTO>()

        projectInfo?.map {
            val approve = if (attribute.contains(APPROVE_KEY)) {
                getProjectManager(it.projectCode)
            } else {
                emptyList()
            }
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.projectName
            entity.iamApprover = approve
            entityList.add(entity)
        }
        logger.info("entityInfo $entityList")
        val result = FetchInstanceInfo()
        return result.buildFetchInstanceResult(entityList)
    }

    fun searchProjectInstances(keyword: String, page: PageInfoDTO?, token: String): SearchInstanceResponseDTO {
        logger.info("searchInstance keyword[$keyword] page[$page]")
        authTokenApi.checkToken(token)
        val projectRecords = projectService.searchProjectByProjectName(
            projectName = keyword,
            limit = page!!.limit.toInt(),
            offset = page!!.offset.toInt()
        )
        val count = projectRecords?.count ?: 0L
        val projectInfo = mutableListOf<InstanceInfoDTO>()
        projectRecords?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.projectName
            projectInfo.add(entity)
        }
        logger.info("projectInfo $projectInfo")
        val result = SearchInstanceInfo()
        return result.buildSearchInstanceResult(projectInfo, count)
    }

    private fun getProjectManager(projectCode: String): List<String> {
        if (projectManager.getIfPresent(projectCode) != null) {
            return projectManager.getIfPresent(projectCode)!!
        }

        val routerTag = projectService.getByEnglishName(projectCode)!!.routerTag
        val managerUser = bkTag.invokeByTag(routerTag) {
            client.getGateway(ServiceProjectAuthResource::class).getProjectUsers(
                token = tokenService.getSystemToken(null)!!,
                projectCode = projectCode,
                group = BkAuthGroup.MANAGER
            ).data ?: emptyList()
        }
        projectManager.put(projectCode, managerUser)
        return managerUser
    }

    companion object {
        val logger = LoggerFactory.getLogger(AuthProjectService::class.java)
        const val APPROVE_KEY = "_bk_iam_approver_"
    }
}
