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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthGroupDao
import com.tencent.devops.auth.entity.GroupCreateInfo
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.auth.pojo.dto.ProjectRoleDTO
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.auth.tables.records.TAuthGroupInfoRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthGroupService @Autowired constructor(
    val dslContext: DSLContext,
    val groupDao: AuthGroupDao
) {
    fun createGroup(
        userId: String,
        projectCode: String,
        groupInfo: GroupDTO
    ): Int {
        logger.info("createGroup : userId = $userId| projectCode = $projectCode | groupInfo = $groupInfo")
        val groupRecord = groupDao.getGroup(
            dslContext = dslContext,
            projectCode = projectCode,
            groupCode = groupInfo.groupCode
        )
        if (groupRecord != null) {
            // 项目下分组已存在,不能重复创建
            logger.warn(
                "group is exsit, don't create repeatedly : userId = $userId | " +
                    "projectCode = $projectCode | groupInfo = $groupInfo "
            )
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = AuthMessageCode.GROUP_EXIST,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        val groupCreateInfo = GroupCreateInfo(
            groupCode = groupInfo.groupCode,
            groupType = groupInfo.groupType,
            groupName = groupInfo.groupName,
            projectCode = projectCode,
            relationId = groupInfo.relationId,
            displayName = groupInfo.displayName,
            user = userId
        )
        return groupDao.createGroup(dslContext, groupCreateInfo)
    }

    fun batchCreate(
        userId: String,
        projectCode: String,
        groupInfos: List<GroupDTO>
    ): Result<Boolean> {
        val groupCodes = groupInfos.map { it.groupCode }
        val groupRecord = groupDao.getGroupByCodes(
            dslContext = dslContext,
            projectCode = projectCode,
            groupCodes = groupCodes
        )
        if (groupRecord.isNotEmpty) {
            // 项目下分组已存在,不能重复创建
            logger.warn(
                "group is exsit, don't create repeatedly : userId = $userId | " +
                    "projectCode = $projectCode | groupInfo = $groupCodes "
            )
            throw OperationException(
                I18nUtil.getCodeLanMessage(AuthMessageCode.GROUP_EXIST, language = I18nUtil.getLanguage(userId))
            )
        }
        val groupCreateInfos = mutableListOf<GroupCreateInfo>()
        groupInfos.forEach {
            val groupCreateInfo = GroupCreateInfo(
                groupCode = it.groupCode,
                groupType = it.groupType,
                groupName = it.groupName,
                projectCode = projectCode,
                relationId = it.relationId,
                displayName = it.displayName,
                user = userId
            )
            groupCreateInfos.add(groupCreateInfo)
        }
        groupDao.batchCreateGroups(dslContext, groupCreateInfos)
        return Result(true)
    }

    fun updateGroupName(userId: String, groupId: Int, groupInfo: ProjectRoleDTO): Int {
        val groupEntity = groupDao.getGroupById(dslContext, groupId)
            ?: throw ParamBlankException("group not exist : groupId = $groupId")

        if (DefaultGroupType.contains(groupEntity.groupCode)) {
            throw ParamBlankException(AuthMessageCode.DEFAULT_GROUP_UPDATE_NAME_ERROR)
        }

        return groupDao.update(
            dslContext,
            groupEntity.id,
            groupInfo.name,
            groupInfo.displayName ?: groupInfo.name,
            userId
        )
    }

    fun getGroupCode(groupId: Int): TAuthGroupInfoRecord? {
        return groupDao.getGroupById(dslContext, groupId)
    }

    fun getGroupByName(projectCode: String, groupName: String): TAuthGroupInfoRecord? {
        return groupDao.getGroupByName(dslContext, projectCode, groupName)
    }

    fun getGroupByCode(projectCode: String, groupCode: String): TAuthGroupInfoRecord? {
        return groupDao.getGroup(dslContext, projectCode, groupCode)
    }

    fun getGroupByProject(projectCode: String): List<TAuthGroupInfoRecord>? {
        return groupDao.getGroupByProject(dslContext, projectCode)
    }

    fun bindRelationId(id: Int, relationId: String): Int {
        return groupDao.updateRelationId(dslContext, id, relationId)
    }

    fun getRelationId(roleId: Int): String? {
        val groupInfo = groupDao.getRelationId(dslContext, roleId) ?: return null
        return groupInfo.relationId!!
    }

    fun deleteGroup(id: Int, softDelete: Boolean? = true) {
        if (softDelete!!) {
            groupDao.softDelete(dslContext, id)
        } else {
            groupDao.deleteRole(dslContext, id)
        }
    }

    fun getGroupByRelationIds(relationIds: List<Int>): List<TAuthGroupInfoRecord> {
        return groupDao.getGroupByRelationIds(
            dslContext = dslContext,
            relationIds = relationIds
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthGroupService::class.java)
    }
}
