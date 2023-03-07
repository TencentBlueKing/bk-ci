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

import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.dao.WorkspaceTemplateDao
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WorkspaceTemplateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val workspaceTemplateDao: WorkspaceTemplateDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceTemplateService::class.java)
    }

    // 新增模板
    fun addWorkspaceTemplate(userId: String, workspaceTemplate: WorkspaceTemplate): Boolean {
        logger.info(
            "WorkspaceTemplateService|addWorkspaceTemplate|userId" +
                "|$userId|workspaceTemplate|$workspaceTemplate"
        )

        // 模板信息写入DB
        workspaceTemplateDao.createWorkspaceTemplate(
            userId = userId,
            workspaceTemplate = workspaceTemplate,
            dslContext = dslContext
        )

        return true
    }

    // 修改模板
    fun updateWorkspaceTemplate(
        wsTemplateId: Long,
        workspaceTemplate: WorkspaceTemplate
    ): Boolean {
        logger.info(
            "WorkspaceTemplateService|updateWorkspaceTemplate|" +
                "workspaceTemplateId|$wsTemplateId|workspaceTemplate|$workspaceTemplate"
        )

        // 更新模板信息
        workspaceTemplateDao.updateWorkspaceTemplate(
            wsTemplateId = wsTemplateId,
            workspaceTemplate = workspaceTemplate,
            dslContext = dslContext
        )

        return true
    }

    // 删除模板
    fun deleteWorkspaceTemplate(
        wsTemplateId: Long
    ): Boolean {
        logger.info("WorkspaceTemplateService|deleteWorkspaceTemplate|wsTemplateId|$wsTemplateId")
        // 删除模板信息
        workspaceTemplateDao.deleteWorkspaceTemplate(
            wsTemplateId = wsTemplateId,
            dslContext = dslContext
        )

        return true
    }

    // 获取工作空间模板
    fun getWorkspaceTemplateList(): List<WorkspaceTemplate> {
        val result = mutableListOf<WorkspaceTemplate>()
        workspaceTemplateDao.queryWorkspaceTemplate(
            wsTemplateId = null,
            dslContext = dslContext
        ).forEach {
            result.add(
                WorkspaceTemplate(
                    wsTemplateId = it.id.toInt(),
                    image = it.image,
                    name = it.name,
                    source = it.source,
                    logo = it.logo,
                    description = it.description
                )
            )
        }
        return result
    }
}
