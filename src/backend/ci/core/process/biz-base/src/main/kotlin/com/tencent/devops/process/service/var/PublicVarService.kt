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
import com.tencent.devops.common.pipeline.ModelPublicVarHandleContext
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_DUPLICATE
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_FORMAT_ERROR
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarVersionSummaryDao
import com.tencent.devops.process.pojo.`var`.VarGroupDiffResult
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReleaseDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarPO
import com.tencent.devops.process.pojo.`var`.po.PublicVarPositionPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarVO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PublicVarService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val publicVarDao: PublicVarDao,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
    private val publicVarVersionSummaryDao: PublicVarVersionSummaryDao,
    private val publicVarGroupReleaseRecordService: PublicVarGroupReleaseRecordService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarService::class.java)

        // 正则表达式常量
        private val VAR_NAME_REGEX = Regex("^[0-9a-zA-Z_]+$")
    }

    fun addGroupPublicVar(context: DSLContext = dslContext, publicVarDTO: PublicVarDTO): Boolean {
        val projectId = publicVarDTO.projectId
        val userId = publicVarDTO.userId
        val groupName = publicVarDTO.groupName

        // 批量生成ID
        val segmentIds = client.get(ServiceAllocIdResource::class)
            .batchGenerateSegmentId("T_RESOURCE_PUBLIC_VAR", publicVarDTO.publicVars.size).data
        if (segmentIds.isNullOrEmpty() || segmentIds.size != publicVarDTO.publicVars.size) {
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
                groupName = groupName,
                version = publicVarDTO.version,
                buildFormProperty = JsonUtil.toJson(it.buildFormProperty),
                creator = userId,
                modifier = userId,
                createTime = LocalDateTime.now(),
                updateTime = LocalDateTime.now()
            )
        }
        
        // 批量保存和发布记录（在事务中执行查询和保存）
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            
            // 获取前一个版本号（处理版本不连续和第一个版本的情况）
            val previousVersion = publicVarGroupDao.getPreviousVersion(
                dslContext = transactionContext,
                projectId = projectId,
                groupName = groupName,
                currentVersion = publicVarDTO.version
            )
            
            // 如果存在前一个版本，则查询前一个版本的变量；否则返回空列表
            val oldVarPOs = if (previousVersion != null) {
                publicVarDao.listVarByGroupName(
                    dslContext = transactionContext,
                    projectId = projectId,
                    groupName = groupName,
                    version = previousVersion
                )
            } else {
                emptyList()
            }
            
            publicVarDao.batchSave(dslContext = transactionContext, publicVarGroupPOs = publicVarPOs)
            
            publicVarGroupReleaseRecordService.batchAddPublicVarGroupReleaseRecord(
                dslContext = transactionContext,
                publicVarGroupReleaseDTO = PublicVarGroupReleaseDTO(
                    projectId = projectId,
                    groupName = groupName,
                    version = publicVarDTO.version,
                    versionDesc = publicVarDTO.versionDesc,
                    userId = userId,
                    newVarPOs = publicVarPOs,
                    oldVarPOs = oldVarPOs
                )
            )

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

    fun getGroupPublicVar(
        projectId: String,
        groupName: String,
        version: Int,
        context: DSLContext = dslContext
    ): List<PublicVarPO> {
        return publicVarDao.listVarByGroupName(
            dslContext = context,
            projectId = projectId,
            groupName = groupName,
            version = version
        )
    }

    /**
     * 将PublicVarPO列表转换为PublicVarDO列表，并批量查询引用计数
     * 
     * @param varPOs 变量PO列表
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param version 版本号（用于设置buildFormProperty.varGroupVersion，可为null）
     * @return 变量DO列表
     */
    private fun convertVarPOsToDOsWithReferCount(
        varPOs: List<PublicVarPO>,
        projectId: String,
        groupName: String,
        version: Int?
    ): List<PublicVarDO> {
        if (varPOs.isEmpty()) {
            return emptyList()
        }

        // 批量查询所有变量的引用数量（从 T_PIPELINE_PUBLIC_VAR_VERSION_SUMMARY 表读取，汇总所有版本）
        val varNames = varPOs.map { it.varName }
        val referCountMap = publicVarVersionSummaryDao.batchGetTotalReferCount(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            varNames = varNames
        )
        return convertVarPOsToPublicVarDOs(varPOs, referCountMap, version)
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

        val publicVarPOs = publicVarDao.listVarByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = targetVersion
        )

        return convertVarPOsToDOsWithReferCount(
            varPOs = publicVarPOs,
            projectId = projectId,
            groupName = groupName,
            version = version
        )
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
        // 检查变量名格式是否符合要求（由字母、数字、下划线组成）
        if (!publicVars.all { VAR_NAME_REGEX.matches(it.varName) }) {
            throw ErrorCodeException(errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_FORMAT_ERROR)
        }
    }

    /**
     * 将 PublicVarPO 列表转换为 BuildFormProperty 列表（供 PublicVarGroupService 等复用）
     */
    fun convertVarPOsToBuildFormProperties(varPOs: List<PublicVarPO>): List<BuildFormProperty> {
        return varPOs.map { JsonUtil.to(it.buildFormProperty, BuildFormProperty::class.java) }
    }

    /**
     * 将 PublicVarPO 列表转换为 PublicVarVO 列表（供 PublicVarGroupService 等复用）
     */
    fun convertVarPOsToPublicVarVOs(varPOs: List<PublicVarPO>): List<PublicVarVO> {
        return varPOs.map { po ->
            PublicVarVO(
                varName = po.varName,
                alias = po.alias,
                type = po.type,
                valueType = po.valueType,
                defaultValue = po.defaultValue,
                desc = po.desc,
                buildFormProperty = JsonUtil.to(po.buildFormProperty, BuildFormProperty::class.java)
            )
        }
    }

    /**
     * 将 PublicVarVO 列表转换为 PublicVarDO 列表并设置引用计数（供 PublicVarGroupService.getChangePreview 等复用）
     */
    fun convertPublicVarVOsToDOsWithReferCount(
        publicVars: List<PublicVarVO>,
        referCountMap: Map<String, Int>
    ): List<PublicVarDO> {
        return publicVars.map { vo ->
            PublicVarDO(
                varName = vo.varName,
                alias = vo.alias,
                desc = vo.desc,
                type = vo.type,
                valueType = vo.valueType,
                defaultValue = vo.defaultValue,
                buildFormProperty = vo.buildFormProperty,
                referCount = referCountMap[vo.varName] ?: 0
            )
        }
    }

    /**
     * 将 PublicVarPO 列表转换为 PublicVarDO 列表，并设置引用计数与变量组版本（供 PublicVarGroupReferQueryService 等复用）
     */
    fun convertVarPOsToPublicVarDOs(
        varPOs: List<PublicVarPO>,
        referCountMap: Map<String, Int>,
        varGroupVersion: Int?
    ): List<PublicVarDO> {
        return varPOs.map { varPO ->
            val buildFormProperty = JsonUtil.to(varPO.buildFormProperty, BuildFormProperty::class.java)
            buildFormProperty.varGroupVersion = varGroupVersion
            val actualReferCount = referCountMap[varPO.varName] ?: 0
            PublicVarDO(
                varName = varPO.varName,
                alias = varPO.alias,
                type = varPO.type,
                valueType = varPO.valueType,
                defaultValue = varPO.defaultValue,
                desc = varPO.desc,
                buildFormProperty = buildFormProperty,
                referCount = actualReferCount
            )
        }
    }

    fun handleModelParams(
        projectId: String,
        modelPublicVarHandleContext: ModelPublicVarHandleContext
    ): List<BuildFormProperty> {
        val publicVarGroups = modelPublicVarHandleContext.publicVarGroups.toMutableList()
        if (publicVarGroups.isEmpty()) return modelPublicVarHandleContext.params
        // 筛选出需要更新到最新版本的变量组
        val groupsToUpdate = publicVarGroups.filter {
            it.version == null
        }
        // 获取引用信息
        val referId = modelPublicVarHandleContext.referId
        val referType = modelPublicVarHandleContext.referType

        // 查询引用信息
        val groupReferInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
            dslContext = dslContext,
            projectId = projectId,
            referType = referType,
            referId = referId,
            referVersion = modelPublicVarHandleContext.referVersion
        )

        // 批量查询最新版本
        val latestGroupVersionMap = getLatestVersionsForGroups(
            projectId = projectId,
            groupNames = groupsToUpdate.map { it.groupName }
        )
        // 批量获取变量
        val latestVars = getAllLatestVarsForGroups(
            projectId = projectId,
            groupToVersion = latestGroupVersionMap
        )

        val params = modelPublicVarHandleContext.params.toMutableList()
        // 获取流水线中所有非变量组的变量名集合
        val pipelineVarNames = params.filter { it.varGroupName.isNullOrBlank() }.map { it.id }.toSet()

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
                logger.info("handleModelParams " +
                        "latestGroupVarNames: $latestGroupVarNames|savedGroupVarNames: $savedGroupVarNames")
                // 对比版本差异
                val diffResult = compareVarGroupVersions(savedGroupVarNames, latestGroupVarNames)
                // 处理变量差异并获取已移除的变量
                val removedVars = processVarGroupDiff(
                    diffResult = diffResult,
                    groupReferInfo = groupReferInfo,
                    latestGroupVars = latestGroupVars,
                    params = params,
                    pipelineVarNames = pipelineVarNames
                )
                // 将已移除的变量设置到 variables 中
                varGroup.variables = removedVars
            }
        }
        return params
    }

    /**
     * 对比变量组版本差异
     * @param oldVarNames 旧版本变量名集合
     * @param newVarNames 新版本变量名集合
     * @return 差异结果，包含需要删除、更新和新增的变量
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
        params: MutableList<BuildFormProperty>,
        pipelineVarNames: Set<String>
    ): List<BuildFormProperty> {
        val positionInfo = groupReferInfo.positionInfo ?: return emptyList()
        val newVarMap = latestGroupVars.associateBy { it.id }
        val positionInfoMap = positionInfo.associateBy { it.varName }

        // 更新已存在的变量（排除与流水线变量同名的）
        updateExistingVars(diffResult.varsToUpdate, positionInfoMap, newVarMap, params, pipelineVarNames)

        // 移除不再存在的变量
        removeObsoleteVars(diffResult.varsToRemove, positionInfoMap, params)

        // 添加新增的变量到末尾（排除与流水线变量同名的）
        addNewVars(diffResult.varsToAdd, newVarMap, params, pipelineVarNames)

        // 构建已移除的变量列表
        return buildRemovedVarsList(diffResult.varsToRemove, positionInfoMap, groupReferInfo)
    }

    /**
     * 更新已存在的变量
     */
    private fun updateExistingVars(
        varsToUpdate: Set<String>,
        positionInfoMap: Map<String, PublicVarPositionPO>,
        newVarMap: Map<String, BuildFormProperty>,
        params: MutableList<BuildFormProperty>,
        pipelineVarNames: Set<String>
    ) {
        varsToUpdate.forEach { varName ->
            // 如果变量名与流水线变量同名，则跳过
            if (varName in pipelineVarNames) {
                return@forEach
            }
            
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
                val index = findVarIndexInParams(varName, pos.index, params)
                if (index >= 0) index else null // 过滤掉未找到的变量
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
        params: MutableList<BuildFormProperty>,
        pipelineVarNames: Set<String>
    ) {
        val currentParamVarIds = params.map { it.id }.toSet()
        varsToAdd
            .mapNotNull { newVarMap[it] }
            .filter { it.id !in currentParamVarIds }
            .filter { it.id !in pipelineVarNames } // 排除与流水线变量同名的
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
        if (groupNames.isEmpty()) return emptyMap()
        val versionMap = publicVarGroupDao.getLatestVersionsByGroupNames(
            dslContext = dslContext,
            projectId = projectId,
            groupNames = groupNames
        )
        val missingGroups = groupNames.filter { it !in versionMap }
        if (missingGroups.isNotEmpty()) {
            throw ErrorCodeException(errorCode = ERROR_INVALID_PARAM_, params = arrayOf(missingGroups.first()))
        }
        return versionMap
    }

    /**
     * 批量获取变量
     * 
     * @param projectId 项目ID
     * @param groupToVersion 变量组名 -> 版本号
     * @return 变量组名 -> 变量 BuildFormProperty 列表
     */
    private fun getAllLatestVarsForGroups(
        projectId: String,
        groupToVersion: Map<String, Int>
    ): Map<String, List<BuildFormProperty>> {
        return groupToVersion.mapValues { (groupName, version) ->
            publicVarDao.listVarByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                version = version
            ).map { publicVarPO ->
                JsonUtil.to(publicVarPO.buildFormProperty, BuildFormProperty::class.java)
            }
        }
    }
}
