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

import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.callback.AuthConstants
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.service.ProjectService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthProjectService @Autowired constructor(
    val projectService: ProjectService,
    val authTokenApi: AuthTokenApi
) {

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

    fun getProjectInfo(idList: List<String>, token: String): FetchInstanceInfoResponseDTO {
        logger.info("getProjectInfo ids[$idList]")
        authTokenApi.checkToken(token)
        val ids = idList.toSet()
        val projectInfo = projectService.list(ids)
        val entityList = mutableListOf<InstanceInfoDTO>()
        projectInfo?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.projectName
            entity.iamApprover = arrayListOf(it.creator)
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

    companion object {
        val logger = LoggerFactory.getLogger(AuthProjectService::class.java)
    }
}
