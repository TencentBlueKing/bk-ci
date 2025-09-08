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
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
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
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicVarService @Autowired constructor(
    private val dslContext: DSLContext,
    private val publicVarDao: PublicVarDao,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val client: Client,
    private val publicVarGroupReleaseRecordService: PublicVarGroupReleaseRecordService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarService::class.java)
    }

    fun addGroupPublicVar(context: DSLContext = dslContext, publicVarDTO: PublicVarDTO): Boolean {
        val projectId = publicVarDTO.projectId
        val userId = publicVarDTO.userId
        val groupName = publicVarDTO.groupName
        val publicVarPOs = publicVarDTO.publicVars.map {
            it.buildFormProperty.varGroupName = groupName
            it.buildFormProperty.varGroupVersion= publicVarDTO.version
            logger.info("buildFormProperty.varGroupName:${it.buildFormProperty.varGroupName}")
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
        val oldVarPOs = publicVarDao.listVarByGroupName(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            version = publicVarDTO.version - 1,
            varNameList = publicVarDTO.publicVars.map { it.varName }
        )
        publicVarDao.batchSave(dslContext, publicVarPOs)
        publicVarGroupReleaseRecordService.batchAddPublicVarGroupReleaseRecord(
            PublicVarGroupReleaseDTO(
                projectId = projectId,
                groupName = groupName,
                version = publicVarDTO.version,
                versionDesc = publicVarDTO.versionDesc,
                userId = userId,
                newVarPOs = publicVarPOs,
                oldVarPOs = oldVarPOs
            )
        )
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
        return publicVarDao.listVarByGroupName(
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

            return publicVarDao.listVarByGroupName(
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

    fun listPublicVarByLatest(
        projectId: String,
        params: List<BuildFormProperty>
    ): List<BuildFormProperty> {
        if (params.isEmpty()) return emptyList()
        
        val result = params.toMutableList()
        
        // 按组名分组处理
        val groupedVars = params.filter {
            !it.varGroupName.isNullOrBlank() && it.varGroupVersion == null
        }.groupBy { it.varGroupName }

        groupedVars.forEach { (groupName, vars) ->

            // 获取该组的最新版本
            val latestVersion = publicVarGroupDao.getLatestVersionByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName!!
            ) ?: throw ErrorCodeException(errorCode = ERROR_INVALID_PARAM_, params = arrayOf(groupName))

            // 获取最新版本的变量列表
            val latestVarPOs = publicVarDao.listVarByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                version = latestVersion
            )

            // 转换最新变量为BuildFormProperty
            val latestVars = latestVarPOs.map { publicVarPO ->
                JsonUtil.to(publicVarPO.buildFormProperty, BuildFormProperty::class.java)
            }

            // 1. 先对相同ID的变量进行替换
            val replacedIds = mutableSetOf<String>()
            latestVars.forEach { latestVar ->
                val existingIndex = result.indexOfFirst { it.id == latestVar.id }
                if (existingIndex >= 0) {
                    // 同ID的进行替换
                    result[existingIndex] = latestVar
                    replacedIds.add(latestVar.id)
                }
            }

            // 2. 移除该组中未被替换的旧变量（只移除属于当前组的变量）
            val varsToRemove = vars.filter { it.id !in replacedIds }
            result.removeAll(varsToRemove)

            // 3. 添加新变量到列表末尾
            latestVars.forEach { latestVar ->
                if (latestVar.id !in replacedIds) {
                    result.add(latestVar)
                }
            }
        }

        // 清除版本信息，确保不会绑定到具体版本
        result.forEach {
            it.varGroupVersion = null
        }

        return result
    }

    /**
     * 更新公共变量组引用列表到最新版本
     * @param projectId 项目ID
     * @param publicVarGroups 变量组引用列表
     * @return 更新后的BuildFormProperty列表
     */
    fun updatePublicVarGroupsToLatest(
        projectId: String,
        publicVarGroups: List<PublicVarGroupRef>
    ): List<BuildFormProperty> {
        if (publicVarGroups.isEmpty()) return emptyList()
        
        val result = mutableListOf<BuildFormProperty>()
        
        // 收集所有最新版本变量用于检查同名
        val allLatestVars = mutableListOf<BuildFormProperty>()

        publicVarGroups.forEach { groupRef ->
            val groupName = groupRef.groupName
            val version = groupRef.versionName?.substring(1)?.toIntOrNull()
            // 获取该组的最新版本（如果versionName为空则使用最新版本）
            val targetVersion = version ?: publicVarGroupDao.getLatestVersionByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName
            ) ?: throw ErrorCodeException(errorCode = ERROR_INVALID_PARAM_, params = arrayOf(groupName))

            // 获取目标版本的变量列表
            val varPOs = publicVarDao.listVarByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                version = targetVersion
            )

            // 转换变量为BuildFormProperty
            val vars = varPOs.map { publicVarPO ->
                JsonUtil.to(publicVarPO.buildFormProperty, BuildFormProperty::class.java)
            }

            // 收集最新版本变量
            allLatestVars.addAll(vars)
            
            // 添加变量到结果列表
            result.addAll(vars)
        }

        // 检查所有变量是否存在同名
        val allVarNames = allLatestVars.map { it.name }
        if (allVarNames.size != allVarNames.distinct().size) {
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_DUPLICATE)
        }

        return result
    }
}