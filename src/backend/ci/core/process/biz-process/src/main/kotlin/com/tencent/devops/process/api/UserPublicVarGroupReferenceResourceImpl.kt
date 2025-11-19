/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.api

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPublicVarGroupReferenceResource
import com.tencent.devops.process.pojo.`var`.`do`.PipelineRefPublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicGroupVarRefDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarReleaseDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupVO
import com.tencent.devops.process.service.`var`.PublicVarGroupReferInfoService
import com.tencent.devops.process.service.`var`.PublicVarGroupService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPublicVarGroupReferenceResourceImpl @Autowired constructor(
    private val publicVarGroupService: PublicVarGroupService,
    private val publicVarGroupReferInfoService: PublicVarGroupReferInfoService,
    private val publicVarReferInfoService: PublicVarGroupReferInfoService
) : UserPublicVarGroupReferenceResource {

    @AuditEntry(actionId = ActionId.PUBLIC_VARIABLE_VIEW)
    override fun listVarReferInfo(
        userId: String,
        projectId: String,
        groupName: String,
        varName: String?,
        referType: PublicVerGroupReferenceTypeEnum?,
        version: Int?,
        page: Int,
        pageSize: Int
    ): Result<Page<PublicGroupVarRefDO>> {
        return Result(publicVarGroupReferInfoService.listVarReferInfo(
            PublicVarGroupInfoQueryReqDTO(
                projectId = projectId,
                groupName = groupName,
                varName = varName,
                referType = referType,
                version = version,
                page = page,
                pageSize = pageSize
            )
        ))
    }

    @AuditEntry(actionId = ActionId.PUBLIC_VARIABLE_EDIT)
    override fun getChangePreview(
        userId: String,
        projectId: String,
        publicVarGroup: PublicVarGroupVO
    ): Result<List<PublicVarReleaseDO>> {
        return Result(publicVarGroupService.getChangePreview(
            userId = userId,
            projectId = projectId,
            publicVarGroup = publicVarGroup
        ))
    }

    @AuditEntry(actionId = ActionId.PUBLIC_VARIABLE_USE)
    override fun listPipelineVarGroupInfo(
        userId: String,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int
    ): Result<List<PipelineRefPublicVarGroupDO>> {
        return publicVarGroupService.listPipelineVariables(
            userId = userId,
            projectId = projectId,
            referId = referId,
            referType = referType,
            referVersion = referVersion
        )
    }

    @AuditEntry(actionId = ActionId.PUBLIC_VARIABLE_LIST)
    override fun listProjectVarGroupInfo(userId: String, projectId: String): Result<List<PipelineRefPublicVarGroupDO>> {
        return publicVarGroupService.listProjectVarGroupInfo(
            userId = userId,
            projectId = projectId
        )
    }

    @AuditEntry(actionId = ActionId.PUBLIC_VARIABLE_USE)
    override fun listResourceVarReferInfo(
        userId: String,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int,
        groupName: String,
        version: Int?
    ): Result<List<PublicVarDO>> {
        return Result(publicVarReferInfoService.listResourceVarReferInfo(
            projectId = projectId,
            referId = referId,
            referType = referType,
            referVersion = referVersion,
            groupName = groupName,
            version = version
        ))
    }
}
