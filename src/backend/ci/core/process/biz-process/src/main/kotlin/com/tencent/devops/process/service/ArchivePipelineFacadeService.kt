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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.service.pipeline.ArchivePipelineManageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList")
class ArchivePipelineFacadeService @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val archivePipelineManageService: ArchivePipelineManageService
) {
    fun getDownloadAllPipelines(userId: String, projectId: String): List<Map<String, String>> {
        return pipelineListFacadeService.listPermissionPipelineName(projectId, userId)
    }

    fun getAllBuildNo(
        userId: String,
        pipelineId: String,
        projectId: String,
        debugVersion: Int?
    ): List<Map<String, String>> {
        checkPermission(userId, projectId, pipelineId)

        return pipelineListFacadeService.getAllBuildNo(projectId, pipelineId, debugVersion)
    }

    fun migrateArchivePipelineData(
        userId: String,
        projectId: String,
        pipelineId: String,
        cancelFlag: Boolean = false
    ): Boolean {
        // 检查用户是否有迁移归档流水线数据的权限
        val permission = AuthPermission.ARCHIVE
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission
            )
        ) {
            val language = I18nUtil.getLanguage()
            throw PermissionForbiddenException(
                I18nUtil.getCodeLanMessage(
                    messageCode = CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    language = language,
                    params = arrayOf(userId, projectId, permission.getI18n(language), pipelineId)
                )
            )
        }
        return archivePipelineManageService.migrateData(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            cancelFlag = cancelFlag
        )
    }

    fun getArchivedPipelineList(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int,
        filterByPipelineName: String?,
        filterByCreator: String?,
        filterByLabels: String?,
        sortType: PipelineSortType?,
        collation: PipelineCollation?
    ): Page<PipelineInfo> {
        // 检查用户是否有归档流水线数据的查看权限
        val permission = AuthPermission.MANAGE_ARCHIVED_PIPELINE
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                permission = permission,
                authResourceType = AuthResourceType.PROJECT
            )
        ) {
            val language = I18nUtil.getLanguage()
            throw PermissionForbiddenException(
                I18nUtil.getCodeLanMessage(
                    messageCode = CommonMessageCode.USER_NO_PIPELINE_PERMISSION,
                    language = language,
                    params = arrayOf(permission.getI18n(language))
                )
            )
        }
        return archivePipelineManageService.getArchivedPipelineList(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            filterByPipelineName = filterByPipelineName,
            filterByCreator = filterByCreator,
            filterByLabels = filterByLabels,
            sortType = sortType,
            collation = collation
        )
    }

    private fun checkPermission(userId: String, projectId: String, pipelineId: String) {
        val hasCreatePermission = pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW
        )
        if (!hasCreatePermission) {
            throw PermissionForbiddenException("user[$userId] does not has permission on project[$projectId]")
        }
    }
}
