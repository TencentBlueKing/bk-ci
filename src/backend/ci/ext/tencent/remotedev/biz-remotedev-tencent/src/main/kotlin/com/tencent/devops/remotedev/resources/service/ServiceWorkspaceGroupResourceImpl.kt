/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.ServiceWorkspaceGroupResource
import com.tencent.devops.remotedev.dao.WorkspaceGroupRelationDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceWorkspaceGroupResourceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceGroupRelationDao: WorkspaceGroupRelationDao
) : ServiceWorkspaceGroupResource {

    override fun getWorkspacesByGroups(projectId: String, groupIds: List<Long>): Result<List<String>> {
        val workspaceNames = workspaceGroupRelationDao.listWorkspaceNamesByGroups(
            dslContext = dslContext,
            projectId = projectId,
            groupIds = groupIds
        )
        return Result(workspaceNames)
    }

    override fun getGroupsByWorkspace(projectId: String, workspaceName: String): Result<List<Long>> {
        val groupIds = workspaceGroupRelationDao.listGroupIdsByWorkspace(
            dslContext = dslContext,
            projectId = projectId,
            workspaceName = workspaceName
        )
        return Result(groupIds)
    }
}
