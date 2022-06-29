/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.service.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.scanner.configuration.ScannerProperties.Companion.DEFAULT_PROJECT_SCAN_PRIORITY
import com.tencent.bkrepo.scanner.configuration.ScannerProperties.Companion.DEFAULT_SCAN_TASK_COUNT_LIMIT
import com.tencent.bkrepo.scanner.configuration.ScannerProperties.Companion.DEFAULT_SUB_SCAN_TASK_COUNT_LIMIT
import com.tencent.bkrepo.scanner.dao.ProjectScanConfigurationDao
import com.tencent.bkrepo.scanner.model.TProjectScanConfiguration
import com.tencent.bkrepo.scanner.pojo.ProjectScanConfiguration
import com.tencent.bkrepo.scanner.pojo.request.ProjectScanConfigurationPageRequest
import com.tencent.bkrepo.scanner.service.ProjectScanConfigurationService
import com.tencent.bkrepo.scanner.utils.Converter
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ProjectScanConfigurationServiceImpl(
    private val projectScanConfigurationDao: ProjectScanConfigurationDao
) : ProjectScanConfigurationService {
    override fun create(request: ProjectScanConfiguration): ProjectScanConfiguration {
        with(request) {
            if (projectScanConfigurationDao.existsByProjectId(projectId)) {
                throw ErrorCodeException(CommonMessageCode.RESOURCE_EXISTED, projectId)
            }

            val userId = SecurityUtils.getUserId()
            val now = LocalDateTime.now()

            val priority = priority ?: DEFAULT_PROJECT_SCAN_PRIORITY
            val scanTaskCountLimit = scanTaskCountLimit ?: DEFAULT_SCAN_TASK_COUNT_LIMIT
            val subScanTaskCountLimit = subScanTaskCountLimit
                ?: DEFAULT_SUB_SCAN_TASK_COUNT_LIMIT
            val configuration = TProjectScanConfiguration(
                createdBy = userId,
                createdDate = now,
                lastModifiedBy = userId,
                lastModifiedDate = now,
                projectId = projectId,
                priority = priority,
                scanTaskCountLimit = scanTaskCountLimit,
                subScanTaskCountLimit = subScanTaskCountLimit,
                autoScanConfiguration = autoScanConfiguration ?: emptyMap()
            )
            return Converter.convert(projectScanConfigurationDao.insert(configuration))
        }
    }

    override fun update(request: ProjectScanConfiguration): ProjectScanConfiguration {
        with(request) {
            val oldConfiguration = projectScanConfigurationDao.findByProjectId(projectId)
                ?: throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND)
            val lastModifiedBy = SecurityUtils.getUserId()
            val lastModifiedDate = LocalDateTime.now()
            val newConfiguration = oldConfiguration.copy(
                priority = priority ?: oldConfiguration.priority,
                scanTaskCountLimit = scanTaskCountLimit ?: oldConfiguration.scanTaskCountLimit,
                subScanTaskCountLimit = subScanTaskCountLimit ?: oldConfiguration.subScanTaskCountLimit,
                autoScanConfiguration = autoScanConfiguration ?: oldConfiguration.autoScanConfiguration,
                lastModifiedBy = lastModifiedBy,
                lastModifiedDate = lastModifiedDate
            )
            return Converter.convert(projectScanConfigurationDao.save(newConfiguration))
        }
    }

    override fun page(request: ProjectScanConfigurationPageRequest): Page<ProjectScanConfiguration> {
        with(request) {
            val pageRequest = Pages.ofRequest(pageNumber, pageSize)
            val page = projectScanConfigurationDao.page(projectId, pageRequest)
            return Pages.ofResponse(
                Pages.ofRequest(pageNumber, pageSize),
                page.totalRecords,
                page.records.map { Converter.convert(it) }
            )
        }
    }

    override fun get(projectId: String): ProjectScanConfiguration {
        return projectScanConfigurationDao.findByProjectId(projectId)
            ?.let { Converter.convert(it) }
            ?: throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND)
    }
}
