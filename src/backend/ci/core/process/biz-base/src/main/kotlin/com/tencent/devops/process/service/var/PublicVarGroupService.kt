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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_INVALID_PARAM_
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT
import com.tencent.devops.process.constant.ProcessMessageCode.PIPELINE_PUBLIC_VAR_GROUP_IS_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.PIPELINE_PUBLIC_VAR_GROUP_REFERENCED
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReleaseRecordDao
import com.tencent.devops.process.pojo.`var`.`do`.PipelinePublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarReleaseDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.enums.OperateTypeEnum
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupPO
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
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao
) {

    companion object {
        const val PUBLIC_VER_GROUP_ADD_LOCK_KEY = "PUBLIC_VER_GROUP_ADD_LOCK"
        const val EXPIRED_TIME_IN_SECONDS = 5L
        private val logger = LoggerFactory.getLogger(PublicVarGroupService::class.java)
    }

    fun addGroup(publicVarGroupDTO: PublicVarGroupDTO): String {
        val projectId = publicVarGroupDTO.projectId
        val userId = publicVarGroupDTO.userId
        val groupName = publicVarGroupDTO.publicVarGroup.groupName
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "${PUBLIC_VER_GROUP_ADD_LOCK_KEY}_${projectId}_$groupName",
            expiredTimeInSeconds = EXPIRED_TIME_IN_SECONDS
        )
        redisLock.lock()
        try {
            publicVarService.checkGroupPublicVar(publicVarGroupDTO.publicVarGroup.publicVars)
            val version = publicVarGroupDao.getLatestVersionByGroupName(dslContext, projectId, groupName) ?: 0
            val operateType = publicVarGroupDTO.operateType
            if (operateType == OperateTypeEnum.CREATE && version > 0) {
                throw ErrorCodeException(
                    errorCode = PIPELINE_PUBLIC_VAR_GROUP_IS_EXIST,
                    params = arrayOf(groupName)
                )
            }
            val publicVarGroupPO = PublicVarGroupPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("PIPELINE_PUBLIC_VAR_GROUP").data ?: 0,
                projectId = projectId,
                groupName = groupName,
                version = version + 1,
                latestFlag = true,
                varCount = publicVarGroupDTO.publicVarGroup.publicVars.size,
                referCount = 0,
                desc = publicVarGroupDTO.publicVarGroup.desc,
                creator = userId,
                modifier = userId,
                createTime = LocalDateTime.now(),
                updateTime = LocalDateTime.now()
            )
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                if (version != 0) {
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
            throw t
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

        val publicVars = varPOs.map { po ->
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

        val totalCount = publicVarGroupDao.countGroupsByProjectId(
            dslContext = dslContext,
            projectId = projectId,
            filterByGroupName = queryReq.filterByGroupName,
            filterByGroupDesc = queryReq.filterByGroupDesc,
            filterByUpdater = queryReq.filterByUpdater,
            groupNames = groupNames
        )

        val records = publicVarGroupDao.listGroupsByProjectIdPage(
            dslContext = dslContext,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            filterByGroupName = queryReq.filterByGroupName,
            filterByGroupDesc = queryReq.filterByGroupDesc,
            filterByUpdater = queryReq.filterByUpdater,
            groupNames = groupNames
        ).map { po ->
            PublicVarGroupDO(
                groupName = po.groupName,
                referCount = po.referCount,
                varCount = po.varCount,
                desc = po.desc,
                modifier = po.modifier,
                updateTime = po.updateTime
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
        operateType: OperateTypeEnum,
        yaml: PublicVarGroupYamlStringVO
    ): String {
        val parserVO = try {
            TransferMapper.getObjectMapper().readValue(
                yaml.yaml,
                object : TypeReference<PublicVarGroupYamlParser>() {}
            )
        } catch (e: Throwable) {
            logger.warn("Failed to parse YAML for public variable group", e)
            throw e
        }

        // 将variables转换为List<BuildFormProperty>
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

        val publicVarGroupVO = PublicVarGroupVO(
            groupName = parserVO.name,
            desc = parserVO.desc,
            publicVars = publicVars
        )

        return addGroup(
            PublicVarGroupDTO(
                projectId = projectId,
                userId = userId,
                publicVarGroup = publicVarGroupVO,
                operateType = operateType
            )
        )
    }

    fun getGroupYaml(groupName: String, version: Int?, projectId: String): String {
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
        val params = varPOs.map { JsonUtil.to(it.buildFormProperty, BuildFormProperty::class.java) }
        val variables = variableTransfer.makeVariableFromBuildParams(params, false)
        val parserVO = PublicVarGroupYamlParser(
            version = "v3.0",
            name = groupInfo.groupName,
            desc = groupInfo.desc,
            variables = variables ?: emptyMap()
        )
        return TransferMapper.getObjectMapper().writeValueAsString(parserVO)
    }

    fun exportGroup(groupName: String, version: Int?, projectId: String): Response {
        val yaml = getGroupYaml(groupName, version, projectId)
        return YamlCommonUtils.exportToFile(yaml, groupName)
    }

    fun deleteGroup(userId: String, projectId: String, groupName: String): Boolean {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "${PUBLIC_VER_GROUP_ADD_LOCK_KEY}_DELETE_${projectId}_$groupName",
            expiredTimeInSeconds = EXPIRED_TIME_IN_SECONDS
        )
        redisLock.lock()
        try {
            val groupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName
            ) ?: throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf(groupName)
            )

            // 检查引用计数是否大于0
            if (groupRecord.referCount > 0) {
                throw ErrorCodeException(
                    errorCode = PIPELINE_PUBLIC_VAR_GROUP_REFERENCED,
                    params = arrayOf(groupName)
                )
            }

            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                publicVarGroupDao.deleteByGroupName(context, projectId, groupName)
                pipelinePublicVarGroupReleaseRecordDao.deleteByGroupName(context, projectId, groupName)
                publicVarDao.deleteByGroupName(context, projectId, groupName)
            }
            return true
        } catch (t: Throwable) {
            logger.warn("Failed to delete variable group $groupName", t)
            throw t
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
        
        // 获取数据库中最新版本的变量组信息
        val latestGroupRecord = publicVarGroupDao.getRecordByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_INVALID_PARAM_,
            params = arrayOf(groupName)
        )
        
        // 获取最新版本的变量列表
        val latestVarPOs = publicVarService.getGroupPublicVar(
            projectId = projectId,
            groupName = groupName,
            version = latestGroupRecord.version
        )
        
        // 将PO转换为DO对象进行比较
        val latestVarDOs = latestVarPOs.map { po ->
            PublicVarDO(
                varName = po.varName,
                alias = po.alias,
                desc = po.desc,
                type = po.type,
                valueType = po.valueType,
                defaultValue = po.defaultValue,
                referCount = po.referCount,
                buildFormProperty = JsonUtil.to(po.buildFormProperty, BuildFormProperty::class.java)
            )
        }
        
        // 将VO转换为DO对象进行比较
        val newVarDOs = publicVarGroup.publicVars.map { vo ->
            PublicVarDO(
                varName = vo.varName,
                alias = vo.alias,
                desc = vo.desc,
                type = vo.type,
                valueType = vo.valueType,
                defaultValue = vo.defaultValue,
                referCount = 0,
                buildFormProperty = vo.buildFormProperty
            )
        }
        
        val releaseRecords = mutableListOf<PublicVarReleaseDO>()
        val version = latestGroupRecord.version + 1
        val pubTime = LocalDateTime.now()
        
        // 1. 处理删除的变量
        val deletedVars = latestVarDOs.filter { oldVar ->
            newVarDOs.none { it.varName == oldVar.varName }
        }
        deletedVars.forEach { oldVar ->
            val content = jacksonObjectMapper().writeValueAsString(
                mapOf(
                    "operate" to OperateTypeEnum.DELETE,
                    "varName" to oldVar.varName,
                    "alias" to oldVar.alias,
                    "defaultValue" to oldVar.defaultValue,
                    "desc" to oldVar.desc,
                    "type" to oldVar.type.name
                )
            )
            
            releaseRecords.add(
                PublicVarReleaseDO(
                    groupName = groupName,
                    version = version,
                    publisher = userId,
                    pubTime = pubTime,
                    content = content,
                    desc = publicVarGroup.versionDesc
                )
            )
        }
        
        // 2. 处理新增的变量
        val addedVars = newVarDOs.filter { newVar ->
            newVar.varName !in latestVarDOs.map { it.varName }
        }
        addedVars.forEach { newVar ->
            val content = jacksonObjectMapper().writeValueAsString(
                mapOf(
                    "operate" to OperateTypeEnum.CREATE,
                    "varName" to newVar.varName,
                    "alias" to newVar.alias,
                    "defaultValue" to newVar.defaultValue,
                    "desc" to newVar.desc,
                    "type" to newVar.type.name
                )
            )
            
            releaseRecords.add(
                PublicVarReleaseDO(
                    groupName = groupName,
                    version = version,
                    publisher = userId,
                    pubTime = pubTime,
                    content = content,
                    desc = publicVarGroup.versionDesc
                )
            )
        }
        
        // 3. 处理修改的变量
        val modifiedVars = newVarDOs.filter { newVar ->
            latestVarDOs.any { oldVar ->
                oldVar.varName == newVar.varName &&
                        (oldVar.alias != newVar.alias ||
                                oldVar.desc != newVar.desc ||
                                oldVar.defaultValue != newVar.defaultValue ||
                                !isBuildFormPropertyEqual(oldVar.buildFormProperty, newVar.buildFormProperty))
            }
        }
        
        modifiedVars.forEach { newVar ->
            val oldVar = latestVarDOs.first { it.varName == newVar.varName }
            
            // 收集所有变更字段
            val changes = mutableMapOf<String, Map<String, Any?>>()
            
            if (oldVar.alias != newVar.alias) {
                changes["alias"] = mapOf("oldValue" to oldVar.alias, "newValue" to newVar.alias)
            }
            if (oldVar.desc != newVar.desc) {
                changes["desc"] = mapOf("oldValue" to oldVar.desc, "newValue" to newVar.desc)
            }
            if (oldVar.defaultValue != newVar.defaultValue) {
                changes["defaultValue"] = mapOf("oldValue" to oldVar.defaultValue, "newValue" to newVar.defaultValue)
            }
            
            // 直接比较BuildFormProperty对象的属性
            val oldBuildFormProperty = oldVar.buildFormProperty
            val newBuildFormProperty = newVar.buildFormProperty
            
            if (oldBuildFormProperty.required != newBuildFormProperty.required) {
                changes["required"] =
                    mapOf("oldValue" to oldBuildFormProperty.required, "newValue" to newBuildFormProperty.required)
            }
            if (oldBuildFormProperty.readOnly != newBuildFormProperty.readOnly) {
                changes["readOnly"] =
                    mapOf("oldValue" to oldBuildFormProperty.readOnly, "newValue" to newBuildFormProperty.readOnly)
            }
            if (oldBuildFormProperty.valueNotEmpty != newBuildFormProperty.valueNotEmpty) {
                changes["valueNotEmpty"] = mapOf(
                    "oldValue" to oldBuildFormProperty.valueNotEmpty,
                    "newValue" to newBuildFormProperty.valueNotEmpty
                )
            }
            
            if (changes.isNotEmpty()) {
                val content = jacksonObjectMapper().writeValueAsString(
                    mapOf(
                        "operate" to OperateTypeEnum.UPDATE,
                        "varName" to newVar.varName,
                        "changes" to changes,
                        "desc" to newVar.desc,
                        "type" to newVar.type.name
                    )
                )
                
                releaseRecords.add(
                    PublicVarReleaseDO(
                        groupName = groupName,
                        version = version,
                        publisher = userId,
                        pubTime = pubTime,
                        content = content,
                        desc = publicVarGroup.versionDesc
                    )
                )
            }
        }
        
        return releaseRecords
    }
    
    /**
     * 比较两个BuildFormProperty对象是否相等
     */
    private fun isBuildFormPropertyEqual(prop1: BuildFormProperty, prop2: BuildFormProperty): Boolean {
        return prop1.id == prop2.id &&
                prop1.name == prop2.name &&
                prop1.type == prop2.type &&
                prop1.defaultValue == prop2.defaultValue &&
                prop1.desc == prop2.desc &&
                prop1.required == prop2.required &&
                prop1.readOnly == prop2.readOnly &&
                prop1.valueNotEmpty == prop2.valueNotEmpty &&
                prop1.constant == prop2.constant
    }

    fun getProjectPublicParamByRef(
        userId: String,
        projectId: String,
        varGroupRefs: List<PublicVarGroupRef>
    ): List<BuildFormProperty> {
        if (varGroupRefs.isEmpty()) {
            return emptyList()
        }

        val buildFormProperties = mutableListOf<BuildFormProperty>()
        val processedVarNames = mutableSetOf<String>()
        varGroupRefs.forEach { varGroupRef ->
            try {
                val groupName = varGroupRef.groupName
                val version = varGroupRef.versionName?.substring(1)?.toIntOrNull()
                val groupRecord = publicVarGroupDao.getRecordByGroupName(
                    dslContext = dslContext,
                    projectId = projectId,
                    groupName = groupName,
                    version = version
                ) ?: run {
                    logger.warn("Variable group $groupName not found in project $projectId")
                    return@forEach
                }

                // 获取变量组中的变量
                val varPOs = publicVarService.getGroupPublicVar(
                    projectId = projectId,
                    groupName = groupName,
                    version = groupRecord.version
                )

                // 转换为BuildFormProperty并检查同名变量
                varPOs.forEach { po ->
                    if (processedVarNames.contains(po.varName)) {
                        throw ErrorCodeException(
                            errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT,
                            params = arrayOf(groupName, po.varName)
                        )
                    }
                    val buildFormProperty = JsonUtil.to(po.buildFormProperty, BuildFormProperty::class.java)
                    buildFormProperty.varGroupName = groupName
                    buildFormProperties.add(buildFormProperty)
                    processedVarNames.add(po.varName)
                }
            } catch (e: Throwable) {
                logger.warn("Failed to get variables from group ${varGroupRef.groupName}", e)
                throw e
            }
        }

        return buildFormProperties
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
        val parserVO = try {
            TransferMapper.getObjectMapper().readValue(
                yaml.yaml,
                object : TypeReference<PublicVarGroupYamlParser>() {}
            )
        } catch (e: Throwable) {
            logger.warn("Failed to parse YAML for public variable group", e)
            throw e
        }

        // 将variables转换为List<BuildFormProperty>
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
        referVersionName: String?
    ): Result<List<PipelinePublicVarGroupDO>> {
        try {
            logger.info("[$projectId|$referId] Get pipeline variables for type: $referType")
            
            // 查询流水线关联的变量组信息
            val referInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType,
                referVersionName = referVersionName ?: VersionStatus.COMMITTING.name
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
                
                PipelinePublicVarGroupDO(
                    groupName = groupRecord.groupName,
                    varCount = groupRecord.varCount,
                    desc = groupRecord.desc,
                    modifier = groupRecord.modifier,
                    updateTime = groupRecord.updateTime
                )
            }.filterNotNull()
            
            return Result(pipelineVarGroups)
        } catch (e: Throwable) {
            logger.error("[$projectId|$referId] Failed to get pipeline variables", e)
            return Result(emptyList())
        }
    }

    fun listProjectVarGroupInfo(userId: String, projectId: String): Result<List<PipelinePublicVarGroupDO>> {
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
                PipelinePublicVarGroupDO(
                    groupName = groupRecord.groupName,
                    varCount = groupRecord.varCount,
                    desc = groupRecord.desc,
                    modifier = groupRecord.modifier,
                    updateTime = groupRecord.updateTime
                )
            }
            
            return Result(pipelineVarGroups)
        } catch (e: Throwable) {
            logger.error("[$projectId] Failed to get project variable groups info", e)
            return Result(emptyList())
        }
    }
}