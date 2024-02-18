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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.constant.HTTP_400
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.op.OPProjectResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.remotedev.dao.WindowsResourceTypeDao
import com.tencent.devops.remotedev.dao.WindowsResourceZoneDao
import com.tencent.devops.remotedev.dao.WindowsSpecResourceDao
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.op.WindowsSpecResInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WindowsResourceConfigService @Autowired constructor(
    private val dslContext: DSLContext,
    private val windowsResourceTypeDao: WindowsResourceTypeDao,
    private val windowsResourceZoneDao: WindowsResourceZoneDao,
    private val windowsSpecResourceDao: WindowsSpecResourceDao,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WindowsResourceConfigService::class.java)
    }

    fun getAllZone(): List<WindowsResourceZoneConfig> {
        logger.info("get all windows resource zone")
        return windowsResourceZoneDao.fetchAll(dslContext, true)
    }

    fun getAllType(withUnavailable: Boolean?, onlySpecModel: Boolean?): List<WindowsResourceTypeConfig> {
        logger.info("get all windows resource type")
        return windowsResourceTypeDao.fetchAll(dslContext, withUnavailable ?: false, onlySpecModel)
    }

    fun getTypeConfig(
        machineType: String
    ): WindowsResourceTypeConfig? {
        logger.info("get windows resource config type $machineType")
        return windowsResourceTypeDao.fetchAny(dslContext, machineType)
    }

    fun getZoneConfig(
        zone: String
    ): WindowsResourceZoneConfig? {
        logger.info("get windows resource config zone $zone")
        return windowsResourceZoneDao.fetchAny(dslContext, zone)
    }

    // 新增windows硬件资源配置
    fun addWindowsResource(windowsResourceConfig: WindowsResourceTypeConfig): Boolean {
        logger.info("WorkspaceTemplateService|addWindowsResource|windowsResourceConfig|$windowsResourceConfig")
        // 模板信息写入DB
        windowsResourceTypeDao.save(dslContext, windowsResourceConfig)
        return true
    }

    // 新增windows硬件资源配置
    fun addWindowsResourceZone(windowsResourceConfig: WindowsResourceZoneConfig): Boolean {
        logger.info("WorkspaceTemplateService|addWindowsResourceZone|windowsResourceConfig|$windowsResourceConfig")
        // 模板信息写入DB
        windowsResourceZoneDao.save(dslContext, windowsResourceConfig)
        return true
    }

    // 更新windows硬件资源配置
    fun updateWindowsResource(
        id: Long,
        windowsResourceConfig: WindowsResourceTypeConfig
    ): Boolean {
        logger.info(
            "WorkspaceTemplateService|updateWorkspaceTemplate|" +
                    "id|$id|windowsResourceConfig|$windowsResourceConfig"
        )

        // 更新模板信息
        windowsResourceTypeDao.updateWindowsResourceConfig(
            id = id,
            config = windowsResourceConfig,
            dslContext = dslContext
        )
        return true
    }

    // 更新windows硬件资源配置
    fun updateWindowsResourceZone(
        id: Long,
        windowsResourceConfig: WindowsResourceZoneConfig
    ): Boolean {
        logger.info(
            "WorkspaceTemplateService|updateWindowsResourceZone|" +
                    "id|$id|windowsResourceConfig|$windowsResourceConfig"
        )

        // 更新模板信息
        windowsResourceZoneDao.updateWindowsResourceZoneConfig(
            id = id,
            config = windowsResourceConfig,
            dslContext = dslContext
        )
        return true
    }

    fun deleteWindowsResource(
        id: Long
    ): Boolean {
        logger.info("WindowsResourceConfigService|deleteWindowsResource|id|$id")
        // 删除模板信息
        windowsResourceTypeDao.deleteWindowsResource(
            id = id,
            dslContext = dslContext
        )

        return true
    }

    fun deleteWindowsResourceZone(
        id: Long
    ): Boolean {
        logger.info("WindowsResourceConfigService|deleteWindowsResourceZone|id|$id")
        // 删除模板信息
        windowsResourceZoneDao.deleteWindowsResource(
            id = id,
            dslContext = dslContext
        )

        return true
    }

    fun createOrUpdateSpec(
        data: WindowsSpecResInfo
    ): Boolean {
        return windowsSpecResourceDao.createOrUpdateSpecRes(
            dslContext = dslContext,
            projectId = data.projectId,
            size = data.size,
            quota = data.quota
        )
    }

    fun deleteSpec(
        projectId: String,
        size: String
    ): Boolean {
        return windowsSpecResourceDao.delete(dslContext = dslContext, projectId = projectId, size = size)
    }

    fun fetchSpec(
        projectId: String?,
        machineType: String?,
        page: Int?,
        pageSize: Int?
    ): Page<WindowsSpecResInfo> {
        val limit = PageUtil.convertPageSizeToSQLLimit(page ?: 1, pageSize ?: 20)
        val count = windowsSpecResourceDao.fetchSpecCount(projectId, machineType, dslContext)
        val recode = windowsSpecResourceDao.fetchSpec(projectId, machineType, dslContext, limit).map {
            WindowsSpecResInfo(
                projectId = it.projectId,
                size = it.size,
                quota = it.quota
            )
        }
        return Page(
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            count = count,
            records = recode
        )
    }

    // 追加项目的云桌面配额
    fun addProjectTotalQuota(
        userId: String,
        projectId: String,
        quota: Int
    ): Boolean {
        logger.info("addProjectTotalQuota|projectId|$projectId|quota|$quota")
        // 先获取当前项目的properties配置获取当前配额，再追加申请的配额，更新
        val projectInfo = kotlin.runCatching {
            client.get(ServiceProjectResource::class).get(projectId)
        }.onFailure { logger.warn("get project $projectId info error|${it.message}") }
            .getOrElse { null }?.data ?: throw RemoteServiceException(
            "not find project $projectId", HTTP_400
        )
        val projectProperties = projectInfo.properties
        if (projectProperties?.remotedev == null || projectProperties.remotedev == false) {
            logger.info("addProjectTotalQuota|$projectId|not open remotedev")
            return false
        }
        val curQuota = projectProperties.cloudDesktopNum
        return client.get(OPProjectResource::class).setProjectProperties(
            userId = userId,
            projectCode = projectId,
            properties = projectProperties.copy(
                cloudDesktopNum = (curQuota + quota)
            )
        ).data == true
    }
}
