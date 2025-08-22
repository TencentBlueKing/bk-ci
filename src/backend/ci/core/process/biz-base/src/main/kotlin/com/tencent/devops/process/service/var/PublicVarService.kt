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

package com.tencent.devops.process.service.`var`

import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_INVALID_PARAM_
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_DUPLICATE
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_FORMAT_ERROR
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReleaseDTO
import com.tencent.devops.process.pojo.`var`.po.PublicVarPO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarVO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
import java.util.concurrent.Executors
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicVarService @Autowired constructor(
    private val dslContext: DSLContext,
    private val publicVarDao: PublicVarDao,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val client: Client,
    private val pipelinePublicVarGroupReleaseRecordService: PipelinePublicVarGroupReleaseRecordService
) {

    fun addGroupPublicVar(context: DSLContext = dslContext, publicVarDTO: PublicVarDTO): Boolean {
        val projectId = publicVarDTO.projectId
        val userId = publicVarDTO.userId
        val groupName = publicVarDTO.groupName
        val publicVarPOs = publicVarDTO.publicVars.map {
            PublicVarPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("PIPELINE_PUBLIC_VAR").data ?: 0,
                projectId = projectId,
                varName = it.varName,
                alias = it.alias,
                type = it.type,
                valueType = it.valueType,
                defaultValue = it.defaultValue,
                desc = it.desc,
                referCount = 0,
                groupName = groupName,
                version = publicVarDTO.version,
                buildFormProperty = JsonUtil.toJson(it.buildFormProperty),
                creator = userId,
                modifier = userId,
                createTime = LocalDateTime.now(),
                updateTime = LocalDateTime.now()
            )
        }
        val oldVarPOs = publicVarDao.listVarBygroupName(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            version = publicVarDTO.version - 1,
            varNameList = publicVarDTO.publicVars.map { it.varName }
        )
        publicVarDao.batchSave(dslContext, publicVarPOs)

        val syncExecutorService = Executors.newFixedThreadPool(1)
        try {
            syncExecutorService.submit {
                pipelinePublicVarGroupReleaseRecordService.batchAddPublicVarGroupReleaseRecord(
                    PublicVarGroupReleaseDTO(
                        projectId = projectId,
                        groupName = groupName,
                        version = publicVarDTO.version,
                        userId = userId,
                        newVarPOs = publicVarPOs,
                        oldVarPOs = oldVarPOs
                    )
                )
            }
        } finally {
            syncExecutorService.shutdown()
        }
        return true
    }

    fun listGroupNamesByVarFilter(
        projectId: String,
        filterByVarName: String?,
        filterByVarAlias: String?
    ): List<String> {
        if (filterByVarName.isNullOrBlank() && filterByVarAlias.isNullOrBlank()) return emptyList()

        val groupNamesByVarName = filterByVarName?.let {
            publicVarDao.listGroupNamesByVarName(dslContext, projectId, it)
        } ?: emptyList()

        val groupNamesByVarAlias = filterByVarAlias?.let {
            publicVarDao.listGroupNamesByVarAlias(dslContext, projectId, it)
        } ?: emptyList()

        return (groupNamesByVarName + groupNamesByVarAlias).distinct()
    }

    fun getGroupPublicVar(projectId: String, groupName: String, version: Int): List<PublicVarPO> {
        return publicVarDao.listVarBygroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = version
        )
    }

    fun getVariables(
        userId: String,
        projectId: String,
        groupName: String,
        version: Int?
    ): List<PublicVarDO> {
        val targetVersion = version ?: publicVarGroupDao.getLatestVersionByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName
        ) ?: throw ErrorCodeException(errorCode = ERROR_INVALID_PARAM_, params = arrayOf(groupName))

            return publicVarDao.listVarBygroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = targetVersion
        ).map { publicVarPO ->
            PublicVarDO(
                varName = publicVarPO.varName,
                alias = publicVarPO.alias,
                type = publicVarPO.type,
                valueType = publicVarPO.valueType,
                defaultValue = publicVarPO.defaultValue,
                desc = publicVarPO.desc,
                referCount = publicVarPO.referCount,
                buildFormProperty = JsonUtil.to(publicVarPO.buildFormProperty, BuildFormProperty::class.java)
            )
        }
    }

    fun checkGroupPublicVar(publicVars: List<PublicVarVO>) {
        if (publicVars.isEmpty()) {
            return
        }
        // 检查变量名是否重复
        val varNames = publicVars.map { it.varName }
        if (varNames.size != varNames.distinct().size) {
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_DUPLICATE)
        }
        // 检查变量名格式是否符合要求
        val verNameRegex = Regex("^[0-9a-zA-Z_]+$")
        if (!publicVars.all { verNameRegex.matches(it.varName) }) {
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_FORMAT_ERROR)
        }
    }
}