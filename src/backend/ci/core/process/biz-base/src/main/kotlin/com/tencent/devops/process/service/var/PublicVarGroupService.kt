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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_INVALID_PARAM_
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupVariable
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_DESERIALIZE_ERROR
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_FORMAT_ERROR
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_MISSING_FIELD
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_NAME_FORMAT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_PARSE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_UNKNOWN_FIELD
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_VARIABLE_NAME_FORMAT
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReleaseRecordDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupVersionSummaryDao
import com.tencent.devops.process.dao.`var`.PublicVarVersionSummaryDao
import com.tencent.devops.process.permission.`var`.PublicVarGroupPermissionService
import com.tencent.devops.process.pojo.`var`.PublicVarGroupPermissions
import com.tencent.devops.process.pojo.`var`.`do`.PipelineRefPublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarReleaseDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupPO
import com.tencent.devops.process.pojo.`var`.po.PublicVarPO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupVO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupYamlStringVO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarVO
import com.tencent.devops.process.yaml.transfer.TransferMapper
import com.tencent.devops.process.yaml.transfer.VariableTransfer
import com.tencent.devops.process.yaml.transfer.pojo.PublicVarGroupYamlParser
import com.tencent.devops.process.yaml.v2.utils.YamlCommonUtils
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import jakarta.ws.rs.core.Response
import java.time.LocalDateTime
import org.checkerframework.checker.units.qual.t
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicVarGroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val client: Client,
    private val publicVarService: PublicVarService,
    private val variableTransfer: VariableTransfer,
    private val publicVarDao: PublicVarDao,
    private val pipelinePublicVarGroupReleaseRecordDao: PublicVarGroupReleaseRecordDao,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
    private val publicVarGroupReleaseRecordService: PublicVarGroupReleaseRecordService,
    private val publicVarGroupPermissionService: PublicVarGroupPermissionService,
    private val publicVarGroupVersionSummaryDao: PublicVarGroupVersionSummaryDao,
    private val publicVarVersionSummaryDao: PublicVarVersionSummaryDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupService::class.java)
        
        // 正则表达式常量
        private val GROUP_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]{2,31}$")
        private val VAR_NAME_REGEX = Regex("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$")
    }

    fun addGroup(publicVarGroupDTO: PublicVarGroupDTO): String {
        val projectId = publicVarGroupDTO.projectId
        val userId = publicVarGroupDTO.userId
        val groupName = publicVarGroupDTO.publicVarGroup.groupName
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "${ProcessMessageCode.PUBLIC_VAR_GROUP_ADD_LOCK_KEY}_${projectId}_$groupName",
            expiredTimeInSeconds = ProcessMessageCode.PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
        )
        redisLock.lock()
        try {
            publicVarService.checkGroupPublicVar(publicVarGroupDTO.publicVarGroup.publicVars)
            val version = publicVarGroupDao.getLatestVersionByGroupName(dslContext, projectId, groupName) ?: 0
            // 通过数据库查询判断操作类型：version为0表示新增，否则为升级版本
            val isCreate = (version == 0)
            val newVersion = version + 1

            val publicVarGroupPO = PublicVarGroupPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("T_RESOURCE_PUBLIC_VAR_GROUP").data ?: 0,
                projectId = projectId,
                groupName = groupName,
                version = newVersion,
                versionName = "v$newVersion",
                latestFlag = true,
                varCount = publicVarGroupDTO.publicVarGroup.publicVars.size,
                desc = publicVarGroupDTO.publicVarGroup.desc,
                creator = userId,
                modifier = userId,
                createTime = LocalDateTime.now(),
                updateTime = LocalDateTime.now()
            )
            // 如果是新建变量组（首次创建），注册到权限中心
            if (isCreate) {
                publicVarGroupPermissionService.createResource(
                    userId = userId,
                    projectId = projectId,
                    groupCode = groupName,
                    name = groupName
                )
            }
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                if (version != 0) {
                    // 更新旧版本的 latest 标志
                    publicVarGroupDao.updateLatestFlag(
                        dslContext = context,
                        projectId = projectId,
                        groupName = groupName,
                        latestFlag = false
                    )
                }
                publicVarGroupDao.save(context, publicVarGroupPO)
                publicVarService.addGroupPublicVar(
                    context = context,
                    publicVarDTO = PublicVarDTO(
                        projectId = projectId,
                        userId = userId,
                        groupName = groupName,
                        version = publicVarGroupPO.version,
                        versionDesc = publicVarGroupDTO.publicVarGroup.versionDesc ?: "",
                        publicVars = publicVarGroupDTO.publicVarGroup.publicVars
                    )
                )
            }
        } catch (t: Throwable) {
            logger.warn("Failed to add variable group $groupName", t)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_ADD_FAILED,
                params = arrayOf(groupName)
            )
        } finally {
            redisLock.unlock()
        }
        return publicVarGroupDTO.publicVarGroup.groupName
    }

    fun getPipelineGroupsVar(projectId: String, groupName: String, version: Int? = null): PublicVarGroupVO {
        // 版本号空值默认取最新版本
        val groupRecord = publicVarGroupDao.getRecordByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = version
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_INVALID_PARAM_,
            params = arrayOf(groupName)
        )

        val varPOs = publicVarService.getGroupPublicVar(
            projectId = projectId,
            groupName = groupName,
            version = groupRecord.version
        )
        val publicVars = publicVarService.convertVarPOsToPublicVarVOs(varPOs)

        return PublicVarGroupVO(
            groupName = groupRecord.groupName,
            desc = groupRecord.desc,
            publicVars = publicVars
        )
    }

    fun getGroups(
        userId: String,
        queryReq: PublicVarGroupInfoQueryReqDTO
    ): Page<PublicVarGroupDO> {
        val projectId = queryReq.projectId
        val page = queryReq.page
        val pageSize = queryReq.pageSize

        val groupNames = publicVarService.listGroupNamesByVarFilter(
            projectId = projectId,
            filterByVarName = queryReq.filterByVarName,
            filterByVarAlias = queryReq.filterByVarAlias
        )

        // 如果用户指定了变量名或别名筛选条件，但没有匹配结果，直接返回空页面
        val hasVarFilter = !queryReq.filterByVarName.isNullOrBlank() || !queryReq.filterByVarAlias.isNullOrBlank()
        if (hasVarFilter && groupNames.isEmpty()) {
            return Page(
                count = 0,
                page = page,
                pageSize = pageSize,
                totalPages = 0,
                records = emptyList()
            )
        }

        val totalCount = publicVarGroupDao.countGroupsByProjectId(
            dslContext = dslContext,
            projectId = projectId,
            filterByGroupName = queryReq.filterByGroupName,
            filterByGroupDesc = queryReq.filterByGroupDesc,
            filterByUpdater = queryReq.filterByUpdater,
            groupNames = groupNames.takeIf { it.isNotEmpty() }
        )

        val groupPOs = publicVarGroupDao.listGroupsByProjectIdPage(
            dslContext = dslContext,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            filterByGroupName = queryReq.filterByGroupName,
            filterByGroupDesc = queryReq.filterByGroupDesc,
            filterByUpdater = queryReq.filterByUpdater,
            groupNames = groupNames.takeIf { it.isNotEmpty() }
        )

        // 批量查询权限
        val groupNameList = groupPOs.map { it.groupName }
        val permissionsMap = if (groupNameList.isNotEmpty()) {
            publicVarGroupPermissionService.filterPublicVarGroups(
                userId = userId,
                projectId = projectId,
                authPermissions = setOf(
                    AuthPermission.EDIT,
                    AuthPermission.VIEW,
                    AuthPermission.DELETE,
                    AuthPermission.USE
                ),
                groupNames = groupNameList
            )
        } else {
            emptyMap()
        }

        // 批量查询所有版本的引用数量（按 referId 去重）
        val referCountMap = if (groupNameList.isNotEmpty()) {
            publicVarGroupVersionSummaryDao.batchGetTotalReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupNames = groupNameList
            )
        } else {
            emptyMap()
        }

        // 批量查询动态版本的引用数量
        val dynamicVersionReferCountMap = if (groupNameList.isNotEmpty()) {
            publicVarGroupVersionSummaryDao.batchGetDynamicVersionReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupNames = groupNameList
            )
        } else {
            emptyMap()
        }

        // 批量查询固定版本的引用数量
        val fixedVersionReferCountMap = if (groupNameList.isNotEmpty()) {
            publicVarGroupVersionSummaryDao.batchGetFixedVersionReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupNames = groupNameList
            )
        } else {
            emptyMap()
        }
        
        val records = groupPOs.map { po ->
            val actualReferCount = referCountMap[po.groupName] ?: 0
            val dynamicVersionReferCount = dynamicVersionReferCountMap[po.groupName] ?: 0
            val fixedVersionReferCount = fixedVersionReferCountMap[po.groupName] ?: 0
            PublicVarGroupDO(
                groupName = po.groupName,
                referCount = actualReferCount,
                dynamicVersionReferCount = dynamicVersionReferCount,
                fixedVersionReferCount = fixedVersionReferCount,
                varCount = po.varCount,
                desc = po.desc,
                modifier = po.modifier,
                updateTime = po.updateTime,
                permission = PublicVarGroupPermissions(
                    canEdit = permissionsMap[AuthPermission.EDIT]?.contains(po.groupName) ?: false,
                    canView = permissionsMap[AuthPermission.VIEW]?.contains(po.groupName) ?: false,
                    canDelete = permissionsMap[AuthPermission.DELETE]?.contains(po.groupName) ?: false,
                    canUse = permissionsMap[AuthPermission.USE]?.contains(po.groupName) ?: false
                )
            )
        }

        return Page(
            count = totalCount,
            page = page,
            pageSize = pageSize,
            totalPages = PageUtil.calTotalPage(pageSize, totalCount),
            records = records
        )
    }

    fun listGroupNames(projectId: String): List<String> {
        return publicVarGroupDao.listGroupsNameByProjectId(dslContext, projectId)
    }

    fun importGroup(
        userId: String,
        projectId: String,
        yaml: PublicVarGroupYamlStringVO
    ): String {
        val publicVarGroupVO = parseYamlToPublicVarGroupVO(yaml)

        return addGroup(
            PublicVarGroupDTO(
                projectId = projectId,
                userId = userId,
                publicVarGroup = publicVarGroupVO
            )
        )
    }

    fun getGroupYaml(
        groupName: String,
        version: Int?,
        projectId: String
    ): String {
        val groupInfo = publicVarGroupDao.getRecordByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = version
        )

        if (groupInfo == null) {
            throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf(groupName)
            )
        }

        val varPOs = publicVarService.getGroupPublicVar(
            projectId = projectId,
            groupName = groupName,
            version = groupInfo.version
        )
        val params = publicVarService.convertVarPOsToBuildFormProperties(varPOs)
        val variables = variableTransfer.makeVariableFromBuildParams(params, false)
        val parserVO = PublicVarGroupYamlParser(
            version = "v3.0",
            name = groupInfo.groupName,
            desc = groupInfo.desc,
            variables = variables ?: emptyMap()
        )
        return TransferMapper.getObjectMapper().writeValueAsString(parserVO)
    }

    fun exportGroup(
        groupName: String,
        version: Int?,
        projectId: String
    ): Response {
        val yaml = getGroupYaml(groupName, version, projectId)
        return YamlCommonUtils.exportToFile(yaml, groupName)
    }

    fun deleteGroup(userId: String, projectId: String, groupName: String): Boolean {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "${ProcessMessageCode.PUBLIC_VAR_GROUP_DELETE_LOCK_KEY}_${projectId}_$groupName",
            expiredTimeInSeconds = ProcessMessageCode.PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
        )
        redisLock.lock()
        try {
            publicVarGroupDao.getRecordByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName
            ) ?: throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf(groupName)
            )

            // 检查变量组是否被引用（从Summary表获取引用总数）
            val referCount = publicVarGroupVersionSummaryDao.getTotalReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName
            )

            if (referCount > 0) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_REFERENCED,
                    params = arrayOf(groupName)
                )
            }

            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                publicVarGroupDao.deleteByGroupName(dslContext = context, projectId = projectId, groupName = groupName)
                pipelinePublicVarGroupReleaseRecordDao.deleteByGroupName(
                    dslContext = context,
                    projectId = projectId,
                    groupName = groupName
                )
                publicVarDao.deleteByGroupName(dslContext = context, projectId = projectId, groupName = groupName)
                // 删除变量组版本概要信息
                publicVarGroupVersionSummaryDao.deleteByGroupName(
                    dslContext = context,
                    projectId = projectId,
                    groupName = groupName
                )
                // 删除变量版本概要信息
                publicVarVersionSummaryDao.deleteByGroupName(
                    dslContext = context,
                    projectId = projectId,
                    groupName = groupName
                )
            }

            // 从权限中心删除变量组资源
            publicVarGroupPermissionService.deleteResource(
                projectId = projectId,
                groupName = groupName
            )

            return true
        } catch (e: ErrorCodeException) {
            logger.warn("Failed to delete variable group $groupName", e)
            throw e
        }
        catch (t: Throwable) {
            logger.warn("Failed to delete variable group $groupName", t)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_DELETE_FAILED,
                params = arrayOf(groupName)
            )
        } finally {
            redisLock.unlock()
        }
    }

    fun getChangePreview(
        userId: String,
        projectId: String,
        publicVarGroup: PublicVarGroupVO
    ): List<PublicVarReleaseDO> {
        val groupName = publicVarGroup.groupName

        val (latestGroupRecord, latestVarPOs) = dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            
            // 获取数据库中最新版本的变量组信息
            val groupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = context,
                projectId = projectId,
                groupName = groupName
            ) ?: throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf(groupName)
            )

            // 获取最新版本的变量列表
            val varPOs = publicVarService.getGroupPublicVar(
                projectId = projectId,
                groupName = groupName,
                version = groupRecord.version,
                context = context
            )

            Pair(groupRecord, varPOs)
        }

        val latestVarDOs = publicVarGroupReleaseRecordService.convertPOToDO(
            varPOs = latestVarPOs,
            projectId = projectId,
            groupName = groupName,
            version = latestGroupRecord.version
        )

        // 批量查询新变量的引用计数（从 T_PIPELINE_PUBLIC_VAR_VERSION_SUMMARY 表读取，汇总所有版本）
        val newVarNames = publicVarGroup.publicVars.map { it.varName }
        val newVarReferCountMap = if (newVarNames.isNotEmpty()) {
            publicVarVersionSummaryDao.batchGetTotalReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                varNames = newVarNames
            )
        } else {
            emptyMap()
        }
        val newVarDOs = publicVarService.convertPublicVarVOsToDOsWithReferCount(
            publicVars = publicVarGroup.publicVars,
            referCountMap = newVarReferCountMap
        )

        val version = latestGroupRecord.version + 1
        val pubTime = LocalDateTime.now()

        return publicVarGroupReleaseRecordService.generateVarChangeRecords(
            PublicVarGroupReleaseRecordService.VarChangeRecordRequest(
                oldVars = latestVarDOs,
                newVars = newVarDOs,
                groupName = groupName,
                version = version,
                userId = userId,
                pubTime = pubTime,
                versionDesc = publicVarGroup.versionDesc
            )
        )
    }

    fun getProjectPublicParamByRef(
        userId: String,
        projectId: String,
        varGroupRefs: List<PublicVarGroupRef>
    ): List<PublicVarGroupVariable> {
        if (varGroupRefs.isEmpty()) {
            return emptyList()
        }
        val publicVarGroupVariables = mutableListOf<PublicVarGroupVariable>()
        val processedVarNames = mutableSetOf<String>()
        var currentIndex = 0

        varGroupRefs.forEach { varGroupRef ->
            currentIndex = processVarGroupRef(
                projectId = projectId,
                varGroupRef = varGroupRef,
                publicVarGroupVariables = publicVarGroupVariables,
                processedVarNames = processedVarNames,
                currentIndex = currentIndex
            )
        }
        return publicVarGroupVariables
    }

    /**
     * 处理单个变量组引用
     */
    private fun processVarGroupRef(
        projectId: String,
        varGroupRef: PublicVarGroupRef,
        publicVarGroupVariables: MutableList<PublicVarGroupVariable>,
        processedVarNames: MutableSet<String>,
        currentIndex: Int
    ): Int {
        try {
            val groupName = varGroupRef.groupName
            val versionName = varGroupRef.versionName
            val groupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                versionName = versionName
            ) ?: run {
                logger.warn("Variable group $groupName not found in project $projectId")
                return currentIndex
            }

            // 获取变量组中的变量
            val varPOs = publicVarService.getGroupPublicVar(
                projectId = projectId,
                groupName = groupName,
                version = groupRecord.version
            )

            // 转换为PublicVarGroupVariable并检查同名变量
            return processVarPOs(
                varPOs = varPOs,
                groupName = groupName,
                groupVersion = groupRecord.version,
                publicVarGroupVariables = publicVarGroupVariables,
                processedVarNames = processedVarNames,
                currentIndex = currentIndex
            )
        } catch (t: Throwable) {
            logger.warn("Failed to get variables from group ${varGroupRef.groupName}", t)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_GET_VARIABLES_FAILED,
                params = arrayOf(varGroupRef.groupName)
            )
        }
    }

    /**
     * 处理变量PO列表，转换为PublicVarGroupVariable
     */
    private fun processVarPOs(
        varPOs: List<PublicVarPO>,
        groupName: String,
        groupVersion: Int,
        publicVarGroupVariables: MutableList<PublicVarGroupVariable>,
        processedVarNames: MutableSet<String>,
        currentIndex: Int
    ): Int {
        val buildFormProperties = publicVarService.convertVarPOsToBuildFormProperties(varPOs)
        var index = currentIndex
        varPOs.forEachIndexed { i, po ->
            val varName = po.varName
            if (processedVarNames.contains(varName)) {
                throw ErrorCodeException(
                    errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT,
                    params = arrayOf(groupName, varName)
                )
            }
            val buildFormProperty = buildFormProperties[i]
            buildFormProperty.varGroupName = groupName
            buildFormProperty.varGroupVersion = groupVersion
            publicVarGroupVariables.add(
                PublicVarGroupVariable(
                    groupName = groupName,
                    groupVersion = groupVersion,
                    buildFormProperty = buildFormProperty,
                    originalIndex = index++
                )
            )
            processedVarNames.add(varName)
        }
        return index
    }

    fun convertGroupYaml(userId: String, projectId: String, publicVarGroup: PublicVarGroupVO): String {
        val params = publicVarGroup.publicVars.map { it.buildFormProperty }
        val variables = variableTransfer.makeVariableFromBuildParams(params, false)
        val parserVO = PublicVarGroupYamlParser(
            version = "v3.0",
            name = publicVarGroup.groupName,
            desc = publicVarGroup.desc ?: "",
            variables = variables ?: emptyMap()
        )
        return TransferMapper.getObjectMapper().writeValueAsString(parserVO)
    }

    fun convertYamlToGroup(userId: String, projectId: String, yaml: PublicVarGroupYamlStringVO): PublicVarGroupVO {
        return parseYamlToPublicVarGroupVO(yaml)
    }

    /**
     * 解析YAML字符串并转换为PublicVarGroupVO对象
     */
    private fun parseYamlToPublicVarGroupVO(yaml: PublicVarGroupYamlStringVO): PublicVarGroupVO {
        val parserVO = parseYamlToParserVO(yaml)
        return convertParserVOToPublicVarGroupVO(parserVO)
    }

    /**
     * 将YAML解析为PublicVarGroupYamlParser，并处理异常包装
     */
    private fun parseYamlToParserVO(yaml: PublicVarGroupYamlStringVO): PublicVarGroupYamlParser {
        return try {
            TransferMapper.getObjectMapper().readValue(
                yaml.yaml,
                object : TypeReference<PublicVarGroupYamlParser>() {}
            )
        } catch (e: Throwable) {
            logger.warn("Failed to parse YAML for public variable group", e)
            val errorMsg = buildErrorMsg(e)
            throw ErrorCodeException(
                errorCode = ERROR_PUBLIC_VAR_GROUP_YAML_PARSE_FAILED,
                params = arrayOf(errorMsg)
            )
        }
    }

    /**
     * 根据异常构建用户友好的错误消息
     */
    private fun buildErrorMsg(e: Throwable): String {
        return when {
            e.message?.contains("Unrecognized field") == true -> {
                val fieldName = e.message!!.substringAfter("Unrecognized field \"").substringBefore("\"")
                I18nUtil.getCodeLanMessage(
                    messageCode = ERROR_PUBLIC_VAR_GROUP_YAML_UNKNOWN_FIELD,
                    params = arrayOf(fieldName)
                )
            }

            e.message?.contains("Cannot deserialize") == true -> {
                I18nUtil.getCodeLanMessage(ERROR_PUBLIC_VAR_GROUP_YAML_DESERIALIZE_ERROR)
            }

            e.message?.contains("missing") == true -> {
                val missingField = e.message!!.let { msg ->
                    when {
                        msg.contains("Missing required creator property") ->
                            msg.substringAfter("Missing required creator property '")
                                .substringBefore("\'")
                        msg.contains("missing property") ->
                            msg.substringAfter("missing property '")
                                .substringBefore("\'")
                        else -> null
                    }
                }
                if (missingField != null) {
                    I18nUtil.getCodeLanMessage(
                        messageCode = ERROR_PUBLIC_VAR_GROUP_YAML_MISSING_FIELD,
                        params = arrayOf(missingField)
                    )
                } else {
                    I18nUtil.getCodeLanMessage(ERROR_PUBLIC_VAR_GROUP_YAML_MISSING_FIELD)
                }
            }

            else -> e.message ?: I18nUtil.getCodeLanMessage(ERROR_PUBLIC_VAR_GROUP_YAML_FORMAT_ERROR)
        }
    }

    /**
     * 将PublicVarGroupYamlParser转换为PublicVarGroupVO，并完成变量格式处理
     */
    private fun convertParserVOToPublicVarGroupVO(parserVO: PublicVarGroupYamlParser): PublicVarGroupVO {
        validateYamlFormat(parserVO)

        parserVO.variables.forEach { variable ->
            if (variable.value.const == true) {
                variable.value.readonly = true
                variable.value.allowModifyAtStartup = null
            }
        }
        val buildFormProperties = variableTransfer.makeVariableFromYaml(parserVO.variables)
        val publicVars = buildFormProperties.map { property ->
            PublicVarVO(
                varName = property.id,
                alias = property.name ?: "",
                type = if (property.constant == true) PublicVarTypeEnum.CONSTANT else PublicVarTypeEnum.VARIABLE,
                valueType = property.type,
                defaultValue = property.defaultValue,
                desc = property.desc,
                buildFormProperty = property
            )
        }

        return PublicVarGroupVO(
            groupName = parserVO.name,
            desc = parserVO.desc,
            publicVars = publicVars
        )
    }

    fun listPipelineVariables(
        userId: String,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int
    ): Result<List<PipelineRefPublicVarGroupDO>> {
        try {
            logger.info("[$projectId|$referId] Get pipeline variables for type: $referType")

            // 查询流水线关联的变量组信息
            val referInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType,
                referVersion = referVersion
            )

            if (referInfos.isEmpty()) {
                return Result(emptyList())
            }

            // 转换为PipelinePublicVarGroupDO列表
            val pipelineVarGroups = referInfos.map { referInfo ->
                val groupRecord = publicVarGroupDao.getRecordByGroupName(
                    dslContext = dslContext,
                    projectId = projectId,
                    groupName = referInfo.groupName,
                    version = referInfo.version
                ) ?: return@map null

                PipelineRefPublicVarGroupDO(
                    groupName = groupRecord.groupName,
                    varCount = groupRecord.varCount,
                    desc = groupRecord.desc,
                    modifier = groupRecord.modifier,
                    updateTime = groupRecord.updateTime
                )
            }.filterNotNull()

            return Result(pipelineVarGroups)
        } catch (t: Throwable) {
            logger.warn("[$projectId|$referId] Failed to get pipeline variables", t)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_LIST_PIPELINE_VARIABLES_FAILED,
                params = arrayOf(projectId, referId)
            )
        }
    }

    fun listProjectVarGroupInfo(userId: String, projectId: String): Result<List<PipelineRefPublicVarGroupDO>> {
        try {
            logger.info("[$projectId] Get all public variable groups info")

            // 获取项目中所有的公共变量组
            val varGroups = publicVarGroupDao.listGroupsByProjectId(
                dslContext = dslContext,
                projectId = projectId
            )

            if (varGroups.isEmpty()) {
                return Result(emptyList())
            }

            // 转换为PipelinePublicVarGroupDO列表
            val pipelineVarGroups = varGroups.map { groupRecord ->
                PipelineRefPublicVarGroupDO(
                    groupName = groupRecord.groupName,
                    varCount = groupRecord.varCount,
                    desc = groupRecord.desc,
                    modifier = groupRecord.modifier,
                    updateTime = groupRecord.updateTime
                )
            }

            return Result(pipelineVarGroups)
        } catch (t: Throwable) {
            logger.warn("[$projectId] Failed to get project variable groups info", t)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_LIST_PROJECT_VAR_GROUP_FAILED,
                params = arrayOf(projectId)
            )
        }
    }

    /**
     * 验证YAML格式
     * 验证规则：
     * 1. 变量组名称：以英文字母开头，由字母、数字、下划线组成，长度3-32字符
     * 2. 变量名：以字母或下划线开头，由字母、数字、下划线组成
     */
    private fun validateYamlFormat(parserVO: PublicVarGroupYamlParser) {
        // 验证变量组名称格式
        if (parserVO.name.isBlank() || !parserVO.name.matches(GROUP_NAME_REGEX)) {
            throw ErrorCodeException(
                errorCode = ERROR_PUBLIC_VAR_GROUP_YAML_NAME_FORMAT
            )
        }

        // 验证变量名格式
        parserVO.variables.keys.forEach { varName ->
            if (varName.isBlank() || !varName.matches(VAR_NAME_REGEX)) {
                throw ErrorCodeException(
                    errorCode = ERROR_PUBLIC_VAR_GROUP_YAML_VARIABLE_NAME_FORMAT,
                    params = arrayOf(varName)
                )
            }
        }
    }
}