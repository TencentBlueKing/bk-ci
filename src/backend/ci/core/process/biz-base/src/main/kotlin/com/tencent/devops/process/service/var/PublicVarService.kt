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
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_DUPLICATE
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_VAR_NAME_FORMAT_ERROR
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.pojo.`var`.VarGroupDiffResult
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReleaseDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarGroupReferPO
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
    private val client: Client,
    private val publicVarDao: PublicVarDao,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
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
            .batchGenerateSegmentId("T_PIPELINE_PUBLIC_VAR", publicVarDTO.publicVars.size).data
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
        groupReferInfo: PipelinePublicVarGroupReferPO,
        latestGroupVars: List<BuildFormProperty>,
        params: MutableList<BuildFormProperty>
    ): List<BuildFormProperty> {
        val newVarMap = latestGroupVars.associateBy { it.id }
        val positionInfo = groupReferInfo.positionInfo ?: return emptyList()
        val positionInfoMap = positionInfo.associateBy { it.varName }

        // 1. 更新已存在的变量（先检查索引位置是否是同一变量，不是则在params中查找）
        diffResult.varsToUpdate.forEach { varName ->
            positionInfoMap[varName]?.let { pos ->
                val newVar = newVarMap[varName]
                if (newVar != null) {
                    // 先检查positionInfo记录的索引位置是否是同一变量
                    if (pos.index >= 0 && pos.index < params.size && params[pos.index].id == varName) {
                        // 索引位置匹配，直接更新
                        params[pos.index] = newVar
                    } else {
                        // 索引位置不匹配（可能变量顺序被调整），在params中查找实际位置
                        val actualIndex = params.indexOfFirst { it.id == varName }
                        if (actualIndex >= 0) {
                            params[actualIndex] = newVar
                        }
                    }
                }
            }
        }

        // 2. 移除不再存在的变量（先检查索引位置是否是同一变量，不是则在params中查找）
        val indicesToRemove = diffResult.varsToRemove.mapNotNull { varName ->
            val pos = positionInfoMap[varName]
            if (pos != null) {
                // 先检查positionInfo记录的索引位置是否是同一变量
                if (pos.index >= 0 && pos.index < params.size && params[pos.index].id == varName) {
                    logger.info("processVarGroupDiff will remove var: $varName at saved index: ${pos.index}")
                    pos.index
                } else {
                    // 索引位置不匹配，在params中查找实际位置
                    val actualIndex = params.indexOfFirst { it.id == varName }
                    if (actualIndex >= 0) {
                        logger.info("processVarGroupDiff will remove var: $varName at actual index: $actualIndex (saved index was ${pos.index})")
                        actualIndex
                    } else {
                        logger.warn("processVarGroupDiff var to remove not found in params: $varName")
                        null
                    }
                }
            } else {
                null
            }
        }.sortedDescending() // 降序排序，确保先删除索引大的

        indicesToRemove.forEach { index ->
            val removedVarName = params[index].id
            params.removeAt(index)
            logger.info("processVarGroupDiff removed var: $removedVarName from index: $index")
        }

        // 3. 添加新增的变量到末尾（先检查是否已存在，避免重复）
        val currentParamVarIds = params.map { it.id }.toSet()
        diffResult.varsToAdd
            .mapNotNull { newVarMap[it] }
            .filter { it.id !in currentParamVarIds } // 过滤已存在的变量
            .forEach { newVar ->
                params.add(newVar)
                logger.info("processVarGroupDiff added new var: ${newVar.id}")
            }

        // 4. 使用diffResult.varsToRemove来构建已移除的变量列表
        val removedVars = diffResult.varsToRemove.mapNotNull { varName ->
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
                    constant = if (pos.type == PublicVarTypeEnum.CONSTANT) true else false,
                ).apply {
                    this.removeFlag = true
                }
            }
        }

        return removedVars
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
}
