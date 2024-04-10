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

package com.tencent.devops.environment.service

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.pojo.ProjectConfig
import com.tencent.devops.environment.pojo.ProjectConfigParam
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EnvProjectConfigService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectConfigDao: ProjectConfigDao
) {

    fun saveProjectConfig(projectConfigParam: ProjectConfigParam) {
        projectConfigDao.saveProjectConfig(
            dslContext = dslContext,
            projectId = projectConfigParam.projectId,
            userId = projectConfigParam.updatedUser,
            bcsVmEnabled = projectConfigParam.bcsVmEnabled,
            bcsVmQuota = projectConfigParam.bcsVmQuota,
            importQuota = projectConfigParam.importQuota,
            devCloudEnabled = projectConfigParam.devCloudEnable,
            devCloudQuota = projectConfigParam.devCloudQuota
        )
    }

    fun listProjectConfig(): List<ProjectConfig> {
        return projectConfigDao.listProjectConfig(dslContext).map {
            ProjectConfig(
                projectId = it.projectId,
                updatedUser = it.updatedUser,
                updatedTime = it.updatedTime.timestamp(),
                bcsVmEnabled = it.bcsvmEnalbed,
                bcsVmQuota = it.bcsvmQuota,
                importQuota = it.importQuota,
                devCloudEnable = it.devCloudEnalbed,
                devCloudQuota = it.devCloudQuota
            )
        }
    }

    fun list(page: Int, pageSize: Int, projectId: String?): List<ProjectConfig> {
        return projectConfigDao.list(dslContext, page, pageSize, projectId).map {
            ProjectConfig(
                projectId = it.projectId,
                updatedUser = it.updatedUser,
                updatedTime = it.updatedTime.timestamp(),
                bcsVmEnabled = it.bcsvmEnalbed,
                bcsVmQuota = it.bcsvmQuota,
                importQuota = it.importQuota,
                devCloudEnable = it.devCloudEnalbed,
                devCloudQuota = it.devCloudQuota
            )
        }
    }

    fun countProjectConfig(projectId: String?): Int {
        return projectConfigDao.count(dslContext, projectId)
    }
}
