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
            val buildFormProperty = JsonUtil.to(publicVarPO.buildFormProperty, BuildFormProperty::class.java)
            buildFormProperty.varGroupVersion = version
            PublicVarDO(
                varName = publicVarPO.varName,
                alias = publicVarPO.alias,
                type = publicVarPO.type,
                valueType = publicVarPO.valueType,
                defaultValue = publicVarPO.defaultValue,
                desc = publicVarPO.desc,
                referCount = publicVarPO.referCount,
                buildFormProperty = buildFormProperty
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

    /**
     * 将变量列表中的变量组变量更新到最新版本
     * @param projectId 项目ID
     * @param params 原始变量列表
     * @param maintainOrder 是否保持原始变量的顺序，默认为true
     *                     - true: 保持原始顺序，适用于需要维持变量位置的场景（如UI展示）
     *                     - false: 不保持顺序，性能更优，适用于只关心变量内容的场景（如构建执行）
     * @return 更新后的变量列表
     */
    fun listPublicVarByLatest(
        projectId: String,
        params: List<BuildFormProperty>,
        maintainOrder: Boolean = true
    ): List<BuildFormProperty> {
        if (params.isEmpty()) return emptyList()
        
        // 筛选出需要更新到最新版本的变量组
        val groupsToUpdate = params.filter {
            !it.varGroupName.isNullOrBlank() && it.varGroupVersion == null
        }.groupBy { it.varGroupName!! }
        
        if (groupsToUpdate.isEmpty()) return params
        
        // 批量获取所有组的最新版本信息
        val groupNames = groupsToUpdate.keys.toList()
        val latestVersionMap = getLatestVersionsForGroups(projectId, groupNames)
        
        // 批量获取所有最新版本的变量
        val allLatestVars = getAllLatestVarsForGroups(projectId, latestVersionMap)
        
        // 根据是否需要保持顺序选择不同的构建策略
        return if (maintainOrder) {
            buildOptimizedResult(params, groupsToUpdate, allLatestVars)
        } else {
            buildFastResult(params, groupsToUpdate, allLatestVars)
        }
    }
    
    /**
     * 批量获取多个组的最新版本
     */
    private fun getLatestVersionsForGroups(
        projectId: String,
        groupNames: List<String>
    ): Map<String, Int> {
        val versionMap = publicVarGroupDao.getLatestVersionsByGroupNames(
            dslContext = dslContext,
            projectId = projectId,
            groupNames = groupNames
        )
        
        // 检查是否有组名不存在
        val missingGroups = groupNames.filter { it !in versionMap }
        if (missingGroups.isNotEmpty()) {
            throw ErrorCodeException(errorCode = ERROR_INVALID_PARAM_, params = arrayOf(missingGroups.first()))
        }
        
        return versionMap
    }
    
    /**
     * 批量获取所有组的最新版本变量
     */
    private fun getAllLatestVarsForGroups(
        projectId: String,
        latestVersionMap: Map<String, Int>
    ): Map<String, List<BuildFormProperty>> {
        return latestVersionMap.mapValues { (groupName, version) ->
            publicVarDao.listVarByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                version = version
            ).map { publicVarPO ->
                JsonUtil.to(publicVarPO.buildFormProperty, BuildFormProperty::class.java).apply {
                    varGroupVersion = null // 清除版本信息
                }
            }
        }
    }
    
    /**
     * 构建优化后的结果列表（保持顺序）
     */
    private fun buildOptimizedResult(
        originalParams: List<BuildFormProperty>,
        groupsToUpdate: Map<String, List<BuildFormProperty>>,
        allLatestVars: Map<String, List<BuildFormProperty>>
    ): List<BuildFormProperty> {
        val result = mutableListOf<BuildFormProperty>()
        val processedIds = mutableSetOf<String>()
        
        // 为每个变量组构建ID到变量的映射，提高查找效率
        val latestVarMaps = allLatestVars.mapValues { (_, vars) ->
            vars.associateBy { it.id }
        }
        
        // 按照原始参数的顺序处理每个变量
        originalParams.forEach { param ->
            val groupName = param.varGroupName
            
            if (groupName in groupsToUpdate) {
                // 需要更新到最新版本的变量
                val latestGroupVarMap = latestVarMaps[groupName] ?: emptyMap()
                val matchedVar = latestGroupVarMap[param.id]
                
                if (matchedVar != null) {
                    // 找到匹配的最新版本变量，替换
                    result.add(matchedVar)
                    processedIds.add(matchedVar.id)
                } else {
                    // 在最新版本中已删除的变量，标记为删除
                    result.add(param.copy().apply { delete = true })
                }
            } else {
                // 不需要更新的变量（包括指定版本的变量和非变量组变量），直接添加到原位置
                result.add(param)
            }
        }
        
        // 添加新增的变量（在最新版本中存在但原列表中不存在的变量）
        groupsToUpdate.keys.forEach { groupName ->
            val latestVars = allLatestVars[groupName] ?: emptyList()
            result.addAll(latestVars.filter { it.id !in processedIds })
        }
        
        return result
    }
    
    /**
     * 构建快速结果列表（不保持顺序，效率更高）
     */
    private fun buildFastResult(
        originalParams: List<BuildFormProperty>,
        groupsToUpdate: Map<String, List<BuildFormProperty>>,
        allLatestVars: Map<String, List<BuildFormProperty>>
    ): List<BuildFormProperty> {
        val result = mutableListOf<BuildFormProperty>()
        
        // 收集所有原始变量的ID，用于快速判断是否为新增变量
        val originalVarIds = originalParams.mapTo(mutableSetOf()) { it.id }
        
        // 直接添加不需要更新的变量（非变量组变量和指定版本的变量组变量）
        originalParams.filter { param ->
            param.varGroupName.isNullOrBlank() || param.varGroupVersion != null
        }.let { result.addAll(it) }
        
        // 处理需要更新的变量组
        groupsToUpdate.forEach { (groupName, originalGroupVars) ->
            val latestVars = allLatestVars[groupName] ?: emptyList()
            val originalGroupVarIds = originalGroupVars.mapTo(mutableSetOf()) { it.id }
            
            // 添加最新版本中存在的变量（包括更新的和新增的）
            latestVars.forEach { latestVar ->
                if (latestVar.id in originalGroupVarIds) {
                    // 更新的变量，直接添加最新版本
                    result.add(latestVar)
                } else {
                    // 新增的变量，直接添加
                    result.add(latestVar)
                }
            }
            
            // 添加已删除的变量（在原始列表中存在但最新版本中不存在）
            val latestVarIds = latestVars.mapTo(mutableSetOf()) { it.id }
            originalGroupVars.forEach { originalVar ->
                if (originalVar.id !in latestVarIds) {
                    // 已删除的变量，标记为删除
                    result.add(originalVar.copy().apply { delete = true })
                }
            }
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
            val versionName = groupRef.versionName
            // 获取该组的最新版本（如果versionName为空则使用最新版本）
            val targetVersion = publicVarGroupDao.getRecordByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                versionName = versionName
            )?.version

            targetVersion ?: throw ErrorCodeException(errorCode = ERROR_INVALID_PARAM_, params = arrayOf(groupName))

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