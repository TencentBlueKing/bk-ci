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
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.VarRefDetail
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_DUPLICATE
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_FORMAT_ERROR
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.pojo.`var`.VarGroupDiffResult
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReleaseDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarPO
import com.tencent.devops.process.pojo.`var`.po.PublicVarPositionPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarReferPO
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
    private val client: Client,
    private val publicVarDao: PublicVarDao,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val publicVarGroupReleaseRecordService: PublicVarGroupReleaseRecordService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarService::class.java)
    }

    fun addGroupPublicVar(context: DSLContext = dslContext, publicVarDTO: PublicVarDTO): Boolean {
        val projectId = publicVarDTO.projectId
        val userId = publicVarDTO.userId
        val groupName = publicVarDTO.groupName

        // 批量生成ID
        val segmentIds = client.get(ServiceAllocIdResource::class)
            .batchGenerateSegmentId("T_RESOURCE_PUBLIC_VAR", publicVarDTO.publicVars.size).data
        if (segmentIds.isNullOrEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf("Failed to generate segment IDs")
            )
        }

        var index = 0
        val publicVarPOs = publicVarDTO.publicVars.map {
            it.buildFormProperty.varGroupName = groupName
            it.buildFormProperty.varGroupVersion = publicVarDTO.version

            if (it.type == PublicVarTypeEnum.CONSTANT) {
                it.buildFormProperty.constant = true
            }

            PublicVarPO(
                id = segmentIds[index++] ?: 0,
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

    fun handleModelParams(
        projectId: String,
        model: Model,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int
    ) {
        val publicVarGroups = model.publicVarGroups?.toMutableList()
        if (publicVarGroups.isNullOrEmpty()) return
        // 筛选出需要更新到最新版本的变量组
        val groupsToUpdate = publicVarGroups.filter {
            it.version == null
        }
        if (groupsToUpdate.isEmpty()) return
        // 批量获取所有非固定版本组的最新版本信息
        val groupNames = groupsToUpdate.map { it.groupName }
        val latestGroupVersionMap = getLatestVersionsForGroups(projectId, groupNames)
        // 批量获取所有变量组最新版本的变量
        val latestVars = getAllLatestVarsForGroups(projectId, latestGroupVersionMap)
        val groupReferInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
            dslContext = dslContext,
            projectId = projectId,
            referType = referType,
            referId = referId,
            referVersion = referVersion
        )
        val params = model.getTriggerContainer().params

        // 为每个变量组处理并设置 variables
        publicVarGroups.forEach { varGroup ->
            val groupName = varGroup.groupName
            // 获取变量组最新版本的变量
            val latestGroupVars = latestVars[groupName] ?: emptyList()
            // 获取变量组保存时的版本的变量
            val groupReferInfo = groupReferInfos.find { it.groupName == groupName }
            val positionInfo = groupReferInfo?.positionInfo
            positionInfo?.let {
                val latestGroupVarNames = latestGroupVars.map { it.id }.toSet()
                val savedGroupVarNames = positionInfo.map { it.varName }.toSet()
                // 对比版本差异
                val diffResult = compareVarGroupVersions(
                    savedGroupVarNames,
                    latestGroupVarNames
                )
                // 处理变量差异并获取已移除的变量
                val removedVars = processVarGroupDiff(
                    diffResult = diffResult,
                    groupReferInfo = groupReferInfo,
                    latestGroupVars = latestGroupVars,
                    params = params
                )
                // 将已移除的变量设置到 variables 中
                varGroup.variables = removedVars
            }
        }
        model.publicVarGroups = publicVarGroups
    }

    /**
     * 对比变量组版本差异
     */
    private fun compareVarGroupVersions(
        oldVarNames: Set<String>,
        newVarNames: Set<String>
    ): VarGroupDiffResult {
        return VarGroupDiffResult(
            varsToRemove = oldVarNames - newVarNames,
            varsToUpdate = oldVarNames intersect newVarNames,
            varsToAdd = newVarNames - oldVarNames
        )
    }

    /**
     * 处理变量组差异
     */
    private fun processVarGroupDiff(
        diffResult: VarGroupDiffResult,
        groupReferInfo: ResourcePublicVarGroupReferPO,
        latestGroupVars: List<BuildFormProperty>,
        params: MutableList<BuildFormProperty>
    ): List<BuildFormProperty> {
        val positionInfo = groupReferInfo.positionInfo ?: return emptyList()
        val newVarMap = latestGroupVars.associateBy { it.id }
        val positionInfoMap = positionInfo.associateBy { it.varName }

        // 1. 更新已存在的变量
        updateExistingVars(diffResult.varsToUpdate, positionInfoMap, newVarMap, params)

        // 2. 移除不再存在的变量
        removeObsoleteVars(diffResult.varsToRemove, positionInfoMap, params)

        // 3. 添加新增的变量到末尾
        addNewVars(diffResult.varsToAdd, newVarMap, params)

        // 4. 构建已移除的变量列表
        return buildRemovedVarsList(diffResult.varsToRemove, positionInfoMap, groupReferInfo)
    }

    /**
     * 更新已存在的变量
     */
    private fun updateExistingVars(
        varsToUpdate: Set<String>,
        positionInfoMap: Map<String, PublicVarPositionPO>,
        newVarMap: Map<String, BuildFormProperty>,
        params: MutableList<BuildFormProperty>
    ) {
        varsToUpdate.forEach { varName ->
            val pos = positionInfoMap[varName] ?: return@forEach
            val newVar = newVarMap[varName] ?: return@forEach

            val targetIndex = findVarIndexInParams(varName, pos.index, params)
            if (targetIndex >= 0) {
                params[targetIndex] = newVar
            }
        }
    }

    /**
     * 移除不再存在的变量
     */
    private fun removeObsoleteVars(
        varsToRemove: Set<String>,
        positionInfoMap: Map<String, PublicVarPositionPO>,
        params: MutableList<BuildFormProperty>
    ) {
        val indicesToRemove = varsToRemove
            .mapNotNull { varName ->
                val pos = positionInfoMap[varName] ?: return@mapNotNull null
                findVarIndexInParams(varName, pos.index, params)
            }
            .sortedDescending() // 降序排序，确保先删除索引大的

        indicesToRemove.forEach { index ->
            params.removeAt(index)
        }
    }

    /**
     * 添加新增的变量
     */
    private fun addNewVars(
        varsToAdd: Set<String>,
        newVarMap: Map<String, BuildFormProperty>,
        params: MutableList<BuildFormProperty>
    ) {
        val currentParamVarIds = params.map { it.id }.toSet()
        varsToAdd
            .mapNotNull { newVarMap[it] }
            .filter { it.id !in currentParamVarIds }
            .forEach { newVar ->
                params.add(newVar)
            }
    }

    /**
     * 构建已移除的变量列表
     */
    private fun buildRemovedVarsList(
        varsToRemove: Set<String>,
        positionInfoMap: Map<String, PublicVarPositionPO>,
        groupReferInfo: ResourcePublicVarGroupReferPO
    ): List<BuildFormProperty> {
        return varsToRemove.mapNotNull { varName ->
            positionInfoMap[varName]?.let { pos ->
                BuildFormProperty(
                    id = pos.varName,
                    required = pos.required,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = "",
                    options = null,
                    desc = "",
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null,
                    varGroupName = groupReferInfo.groupName,
                    varGroupVersion = groupReferInfo.version,
                    constant = pos.type == PublicVarTypeEnum.CONSTANT
                ).apply {
                    this.removeFlag = true
                }
            }
        }
    }

    /**
     * 在params中查找变量的实际索引位置
     * 先检查预期位置是否匹配，不匹配则遍历查找
     */
    private fun findVarIndexInParams(
        varName: String,
        expectedIndex: Int,
        params: List<BuildFormProperty>
    ): Int {
        // 先检查预期位置是否匹配
        if (expectedIndex in params.indices && params[expectedIndex].id == varName) {
            return expectedIndex
        }
        // 预期位置不匹配，遍历查找实际位置
        return params.indexOfFirst { it.id == varName }
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
     * 处理公共变量组引用
     * 根据Model中的publicVarGroups和varRefDetails，同步T_RESOURCE_PUBLIC_VAR_REFER_INFO表的引用记录
     * 并更新T_RESOURCE_PUBLIC_VAR表的REFER_COUNT字段
     *
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param model 流水线模型
     * @param resourceId 资源ID（如流水线ID）
     * @param resourceType 资源类型
     * @param resourceVersion 资源版本
     * @param varRefDetails 变量引用详情列表
     */
    fun handlePublicVarGroupReferences(
        userId: String,
        projectId: String,
        model: Model,
        resourceId: String,
        resourceType: String,
        resourceVersion: Int,
        varRefDetails: List<VarRefDetail>
    ) {
        logger.info("Start handling public var group references for resource: $resourceId|$resourceVersion")

        val referType = PublicVerGroupReferenceTypeEnum.valueOf(resourceType)
        
        // 1. 查询所有已存在的引用记录（按groupName和version分组）
        val existingGroupMap = publicVarReferInfoDao.listVarGroupsByReferIdAndVersion(
            dslContext = dslContext,
            projectId = projectId,
            referId = resourceId,
            referType = referType,
            referVersion = resourceVersion
        )

        // 2. 构建Model中的变量组列表
        val modelVarGroups = model.publicVarGroups ?: emptyList()

        // 3. 如果Model中没有变量组，清理所有已存在的引用记录
        if (modelVarGroups.isEmpty()) {
            if (existingGroupMap.isNotEmpty()) {
                cleanupRemovedVarGroupReferences(
                    context = dslContext,
                    projectId = projectId,
                    resourceId = resourceId,
                    referType = referType,
                    resourceVersion = resourceVersion,
                    groupsToCleanup = existingGroupMap.keys,
                    existingGroupMap = existingGroupMap
                )
            }
            return
        }

        // 4. 从varRefDetails中提取被引用的变量名集合
        val referencedVarNames = varRefDetails.map { it.varName }.toSet()

        // 5. 从triggerContainer.params中筛选出公共变量（通过varGroupName判断）
        // 构建 Map<varGroupName, Set<varName>>
        val triggerContainer = model.getTriggerContainer()
        val publicVarMap = triggerContainer.params
            .filter { !it.varGroupName.isNullOrBlank() } // 有varGroupName的是公共变量
            .groupBy { it.varGroupName!! }
            .mapValues { (_, vars) -> vars.map { it.id }.toSet() }

        // 6. 识别需要清理的变量组（在已存在但不在Model的publicVarGroups中的）
        val modelGroupKeys = modelVarGroups.map { 
            val versionKey = it.version ?: -1
            "${it.groupName}:$versionKey"
        }.toSet()
        val groupsToCleanup = existingGroupMap.keys - modelGroupKeys

        // 7. 先清理不在Model中的公共变量引用
        if (groupsToCleanup.isNotEmpty()) {
            cleanupRemovedVarGroupReferences(
                context = dslContext,
                projectId = projectId,
                resourceId = resourceId,
                referType = referType,
                resourceVersion = resourceVersion,
                groupsToCleanup = groupsToCleanup,
                existingGroupMap = existingGroupMap
            )
        }

        // 8. 批量查询需要的最新版本号（减少数据库查询次数）
        val groupsNeedLatestVersion = modelVarGroups
            .filter { it.version == null }
            .map { it.groupName }
        val latestVersionMap = if (groupsNeedLatestVersion.isNotEmpty()) {
            publicVarGroupDao.getLatestVersionsByGroupNames(
                dslContext = dslContext,
                projectId = projectId,
                groupNames = groupsNeedLatestVersion
            )
        } else {
            emptyMap()
        }

        // 9. 批量查询所有变量组的已存在变量名（减少数据库查询次数）
        val allExistingVarNames = publicVarReferInfoDao.listVarReferInfoByReferIdAndVersion(
            dslContext = dslContext,
            projectId = projectId,
            referId = resourceId,
            referType = referType,
            referVersion = resourceVersion
        ).groupBy { "${it.groupName}:${it.version}" }
         .mapValues { (_, records) -> records.map { it.varName }.toSet() }

        // 10. 使用Map收集引用计数变化，最后批量更新
        // key: "groupName:version:varName", value: 计数变化量（正数表示增加，负数表示减少）
        val referCountChanges = mutableMapOf<String, Int>()

        // 收集需要批量删除的变量引用 Triple(groupName, versionForDb, varNames)
        val varsToDeleteBatch = mutableListOf<Triple<String, Int, List<String>>>()

        // 收集需要批量新增的变量引用
        val referRecordsToAdd = mutableListOf<ResourcePublicVarReferPO>()

        // 11. 处理Model中的变量组引用
        modelVarGroups.forEach { varGroup ->
            val groupName = varGroup.groupName
            val versionForDb = varGroup.version ?: -1
            val groupKey = "$groupName:$versionForDb"

            // 从publicVarMap中获取该变量组的所有变量
            val groupVarNames = publicVarMap[groupName] ?: emptySet()

            // 筛选出被引用的变量（在referencedVarNames中存在的）
            val referencedVarNameSet = groupVarNames.filter {
                referencedVarNames.contains(it)
            }.toSet()

            // 如果该变量组没有被引用的变量，跳过处理
            if (referencedVarNameSet.isEmpty()) {
                return@forEach
            }

            // 确定实际版本号（用于更新引用计数）
            val actualVersion = if (versionForDb == -1) {
                latestVersionMap[groupName] ?: run {
                    return@forEach
                }
            } else {
                versionForDb
            }

            // 获取该组已存在的引用变量名
            val existingVarNames = allExistingVarNames[groupKey] ?: emptySet()

            // 识别需要新增和删除的变量
            val varsToAdd = referencedVarNameSet - existingVarNames
            val varsToRemove = existingVarNames - referencedVarNameSet

            // 收集需要删除的变量引用
            if (varsToRemove.isNotEmpty()) {
                varsToDeleteBatch.add(Triple(groupName, versionForDb, varsToRemove.toList()))

                // 收集引用计数变化（减少）
                varsToRemove.forEach { varName ->
                    val countKey = "$groupName:$actualVersion:$varName"
                    referCountChanges[countKey] = (referCountChanges[countKey] ?: 0) - 1
                }
            }

            // 收集需要新增的变量引用
            if (varsToAdd.isNotEmpty()) {
                // 批量生成ID
                val segmentIds = client.get(ServiceAllocIdResource::class)
                    .batchGenerateSegmentId("T_RESOURCE_PUBLIC_VAR_REFER_INFO", varsToAdd.size).data
                if (segmentIds.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = ERROR_INVALID_PARAM_,
                        params = arrayOf("Failed to generate segment IDs for var refer info")
                    )
                }

                // 构建引用记录
                varsToAdd.forEachIndexed { index, varName ->
                    referRecordsToAdd.add(
                        ResourcePublicVarReferPO(
                            id = segmentIds[index] ?: 0,
                            projectId = projectId,
                            groupName = groupName,
                            varName = varName,
                            version = versionForDb,
                            referId = resourceId,
                            referType = referType,
                            referVersion = resourceVersion,
                            referVersionName = resourceVersion.toString(),
                            creator = userId,
                            modifier = userId,
                            createTime = LocalDateTime.now(),
                            updateTime = LocalDateTime.now()
                        )
                    )

                    // 收集引用计数变化（增加）
                    val countKey = "$groupName:$actualVersion:$varName"
                    referCountChanges[countKey] = (referCountChanges[countKey] ?: 0) + 1
                }
            }
        }

        // 12. 批量执行删除操作
        varsToDeleteBatch.forEach { (groupName, versionForDb, varNames) ->
            publicVarReferInfoDao.deleteByReferIdAndGroupAndVarNames(
                dslContext = dslContext,
                projectId = projectId,
                referId = resourceId,
                referType = referType,
                groupName = groupName,
                referVersion = resourceVersion,
                varNames = varNames
            )
        }

        // 13. 批量插入新增的引用记录
        if (referRecordsToAdd.isNotEmpty()) {
            publicVarReferInfoDao.batchSave(dslContext, referRecordsToAdd)
        }

        // 14. 批量更新引用计数
        if (referCountChanges.isNotEmpty()) {
            publicVarDao.batchUpdateReferCount(
                dslContext = dslContext,
                projectId = projectId,
                referCountChanges = referCountChanges
            )
        }
    }

    /**
     * 清理已移除的变量组引用
     */
    fun cleanupRemovedVarGroupReferences(
        context: DSLContext,
        projectId: String,
        resourceId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        resourceVersion: Int,
        groupsToCleanup: Set<String>,
        existingGroupMap: Map<String, Pair<String, Int>>
    ) {
        // 收集引用计数变化
        val referCountChanges = mutableMapOf<String, Int>()

        groupsToCleanup.forEach { groupKey ->
            val groupInfo = existingGroupMap[groupKey] ?: return@forEach
            val (groupName, version) = groupInfo

            logger.info("Cleaning up var group: $groupName, version: $version")

            // 删除引用记录（按groupName和version删除）
            publicVarReferInfoDao.deleteByReferIdAndGroup(
                dslContext = context,
                projectId = projectId,
                referId = resourceId,
                referType = referType,
                groupName = groupName,
                referVersion = resourceVersion
            )

            // 确定实际版本号用于更新引用计数
            val actualVersion = if (version == -1) {
                // 查询最新版本号
                publicVarGroupDao.getLatestVersionByGroupName(
                    dslContext = context,
                    projectId = projectId,
                    groupName = groupName
                ) ?: run {
                    logger.warn("Cannot find latest version for group: $groupName")
                    return@forEach
                }
            } else {
                version
            }

            // 查询该变量组的所有变量名
            val varNames = publicVarDao.queryVarNamesByGroupName(
                dslContext = context,
                projectId = projectId,
                groupName = groupName,
                version = actualVersion
            )

            // 收集引用计数变化（减少）
            varNames.forEach { varName ->
                val countKey = "$groupName:$actualVersion:$varName"
                referCountChanges[countKey] = (referCountChanges[countKey] ?: 0) - 1
            }

            logger.info("Cleaned up var group: $groupName, actual version: $actualVersion, ${varNames.size} vars")
        }

        // 批量更新引用计数
        publicVarDao.batchUpdateReferCount(
            dslContext = context,
            projectId = projectId,
            referCountChanges = referCountChanges
        )
    }
}
