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

package com.tencent.bkrepo.repository.service.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.repository.dao.ProjectDao
import com.tencent.bkrepo.repository.listener.event.project.ProjectCreatedEvent
import com.tencent.bkrepo.repository.model.TProject
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import com.tencent.bkrepo.repository.pojo.project.ProjectRangeQueryRequest
import com.tencent.bkrepo.repository.service.ProjectService
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * 仓库服务实现类
 */
@Service
class ProjectServiceImpl(
    private val projectDao: ProjectDao
) : AbstractService(), ProjectService {

    override fun getProjectInfo(name: String): ProjectInfo? {
        return convert(projectDao.findByName(name))
    }

    override fun listProject(): List<ProjectInfo> {
        return projectDao.findAll().map { convert(it)!! }
    }

    override fun rangeQuery(request: ProjectRangeQueryRequest): Page<ProjectInfo?> {
        val limit = request.limit
        val skip = request.offset
        return if (request.projectIds.isEmpty()) {
            val query = Query()
            val totalCount = projectDao.count(query)
            val records = projectDao.find(query.skip(skip).limit(limit)).map { convert(it) }
            Page(0, limit, totalCount, records)
        } else {
            val criteria = TProject::name.inValues(request.projectIds)
            val query = Query(criteria)
            val totalCount = projectDao.count(query)
            val records = projectDao.find(query.limit(limit).skip(skip)).map { convert(it) }
            Page(0, limit, totalCount, records)
        }
    }

    override fun checkExist(name: String): Boolean {
        return projectDao.findByName(name) != null
    }

    override fun createProject(request: ProjectCreateRequest): ProjectInfo {
        with(request) {
            validateParameter(this)
            if (checkExist(name)) {
                throw ErrorCodeException(ArtifactMessageCode.PROJECT_EXISTED, name)
            }
            val project = TProject(
                name = name,
                displayName = displayName,
                description = description.orEmpty(),
                createdBy = operator,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = operator,
                lastModifiedDate = LocalDateTime.now()
            )
            return try {
                projectDao.insert(project)
                    .also { publishEvent(ProjectCreatedEvent(request)) }
                    .also { logger.info("Create project [$name] success.") }
                    .let { convert(it)!! }
            } catch (exception: DuplicateKeyException) {
                logger.warn("Insert project[$name] error: [${exception.message}]")
                getProjectInfo(name)!!
            }
        }
    }

    private fun validateParameter(request: ProjectCreateRequest) {
        with(request) {
            if (!Pattern.matches(PROJECT_NAME_PATTERN, name)) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, request::name.name)
            }
            if (displayName.isBlank() ||
                displayName.length < DISPLAY_NAME_LENGTH_MIN ||
                displayName.length > DISPLAY_NAME_LENGTH_MAX
            ) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, request::displayName.name)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectServiceImpl::class.java)
        private const val PROJECT_NAME_PATTERN = "[a-zA-Z_][a-zA-Z0-9\\-_]{1,31}"
        private const val DISPLAY_NAME_LENGTH_MIN = 2
        private const val DISPLAY_NAME_LENGTH_MAX = 32

        private fun convert(tProject: TProject?): ProjectInfo? {
            return tProject?.let {
                ProjectInfo(
                    name = it.name,
                    displayName = it.displayName,
                    description = it.description,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME),
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME)
                )
            }
        }
    }
}
