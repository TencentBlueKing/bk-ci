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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT
import com.tencent.devops.process.constant.ProcessMessageCode.PIPELINE_PUBLIC_VAR_GROUP_IS_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.PIPELINE_PUBLIC_VAR_GROUP_REFERENCED
import com.tencent.devops.process.dao.`var`.PipelinePublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PipelinePublicVarGroupReleseRecordDao
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarChangePreviewDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarReleaseDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarVariableReferenceDO
import com.tencent.devops.process.pojo.`var`.dto.PipelinePublicVarGroupReferDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.enums.OperateTypeEnum
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarGroupReferPO
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupPO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupVO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupYamlStringVO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarVO
import com.tencent.devops.process.template.service.TemplateService
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
    private val pipelinePublicVarGroupReleaseRecordDao: PipelinePublicVarGroupReleseRecordDao,
    private val pipelinePublicVarGroupReferInfoDao: PipelinePublicVarGroupReferInfoDao,
    private val templateService: TemplateService
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

    fun getGroupYaml(groupName: String, version: Int, projectId: String): String {
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
            version = version
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

    fun exportGroup(groupName: String, version: Int, projectId: String): Response {
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

    fun listVarGroupReferInfo(queryReq: PublicVarGroupInfoQueryReqDTO): Page<PublicVarVariableReferenceDO> {
        val projectId = queryReq.projectId
        val groupName = queryReq.groupName!!
        val version = queryReq.version
        val page = queryReq.page
        val pageSize = queryReq.pageSize
        val pipelinePublicVarGroupInfo = publicVarGroupDao.getRecordByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = version
        ) ?: return Page(
            count = 0,
            page = page,
            pageSize = pageSize,
            records = emptyList()
        )

        val totalCount = pipelinePublicVarGroupReferInfoDao.countByGroupName(dslContext, projectId, groupName)
        val varGroupReferInfo = pipelinePublicVarGroupReferInfoDao.listVarGroupReferInfo(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            page = page,
            pageSize = pageSize
        )
        return Page(
            count = totalCount.toLong(),
            page = page,
            pageSize = pageSize,
            records = varGroupReferInfo.map {
                PublicVarVariableReferenceDO(
                    referId = it.referId,
                    referType = it.referType,
                    referName = it.referName,
                    referCount = pipelinePublicVarGroupInfo.referCount,
                    referUrl = getVarGroupReferUrl(projectId, it.referType, it.referId),
                    modifier = it.modifier,
                    updateTime = it.updateTime
                )
            }
        )
    }

    private fun getVarGroupReferUrl(
        projectId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referId: String
    ): String {
        return when (referType) {
            PublicVerGroupReferenceTypeEnum.PIPELINE -> "/console/pipeline/$projectId/$referId/history/pipeline"
            PublicVerGroupReferenceTypeEnum.TEMPLATE -> {
                val version = templateService.getTemplateLatestVersion(referId) ?: return ""
                "/console/pipeline/$projectId/template/$referId/$version/pipeline"
            }
        }
    }

    fun getChangePreview(
        userId: String,
        queryReq: PublicVarGroupInfoQueryReqDTO
    ): Page<PublicVarChangePreviewDO> {
        val projectId = queryReq.projectId
        val groupName = queryReq.groupName!!
        val page = queryReq.page
        val pageSize = queryReq.pageSize
        val count = pipelinePublicVarGroupReleaseRecordDao.countByGroupName(dslContext, projectId, groupName)
        val publicVarGroupReleaseRecordPOS = pipelinePublicVarGroupReleaseRecordDao.listByGroupNamePage(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            page = page,
            pageSize = pageSize
        )
        return Page(
            count = count.toLong(),
            page = page,
            pageSize = pageSize,
            records = publicVarGroupReleaseRecordPOS.map {
                PublicVarChangePreviewDO(
                    publisher = it.publisher,
                    pubTime = it.pubTime,
                    desc = it.desc,
                    content = it.content ?: ""
                )
            }
        )
    }

    fun getReleaseHistory(
        userId: String,
        queryReq: PublicVarGroupInfoQueryReqDTO
    ): Page<PublicVarReleaseDO> {
        val projectId = queryReq.projectId
        val groupName = queryReq.groupName!!
        val page = queryReq.page
        val pageSize = queryReq.pageSize
        val count = pipelinePublicVarGroupReleaseRecordDao.countByGroupName(dslContext, projectId, groupName)
        val publicVarReleaseDOS = pipelinePublicVarGroupReleaseRecordDao.listGroupReleaseHistory(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            page = page,
            pageSize = pageSize
        )
        return Page(
            count = count.toLong(),
            page = page,
            pageSize = pageSize,
            records = publicVarReleaseDOS
        )
    }

    fun addPipelineGroupRefer(
        userId: String,
        projectId: String,
        pipelinePublicVarGroupReferInfo: PipelinePublicVarGroupReferDTO
    ): Boolean {
        val referId = pipelinePublicVarGroupReferInfo.referId
        val referType = pipelinePublicVarGroupReferInfo.referType
        val groupNames = pipelinePublicVarGroupReferInfo.groupNames
        val referName = pipelinePublicVarGroupReferInfo.referName

        if (groupNames.isEmpty()) {
            return true
        }

        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "${PUBLIC_VER_GROUP_ADD_LOCK_KEY}_${projectId}_${referId}_${referType.name}",
            expiredTimeInSeconds = EXPIRED_TIME_IN_SECONDS
        )

        redisLock.lock()
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)

                groupNames.forEach { groupName ->
                    // 检查变量组是否存在
                    val groupRecord = publicVarGroupDao.getRecordByGroupName(
                        dslContext = context,
                        projectId = projectId,
                        groupName = groupName
                    ) ?: throw ErrorCodeException(
                        errorCode = ERROR_INVALID_PARAM_,
                        params = arrayOf(groupName)
                    )

                    // 检查该流水线是否已与该变量组（groupName+version）建立引用
                    val existingReferCount = pipelinePublicVarGroupReferInfoDao.countByReferId(
                        dslContext = context,
                        projectId = projectId,
                        referId = referId,
                        referType = referType,
                        groupName = groupName,
                        version = groupRecord.version
                    )

                    if (existingReferCount > 0) {
                        return@forEach
                    }

                    // 插入引用记录
                    pipelinePublicVarGroupReferInfoDao.save(
                        dslContext = context,
                        PipelinePublicVarGroupReferPO(
                            id = client.get(ServiceAllocIdResource::class)
                                .generateSegmentId("T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO").data ?: 0,
                            projectId = projectId,
                            groupName = groupName,
                            version = groupRecord.version,
                            referId = referId,
                            referName = referName,
                            referType = referType,
                            modifier = userId,
                            updateTime = LocalDateTime.now(),
                            creator = userId,
                            createTime = LocalDateTime.now()
                        )
                    )

                    val currentCount = pipelinePublicVarGroupReferInfoDao.countByGroupName(
                        dslContext = context,
                        projectId = projectId,
                        groupName = groupName
                    )

                    // 更新变量组引用计数
                    publicVarGroupDao.updateReferCount(
                        dslContext = context,
                        projectId = projectId,
                        groupName = groupName,
                        version = groupRecord.version,
                        referCount = currentCount
                    )
                }
            }
        } catch (t: Throwable) {
            logger.warn("Failed to add pipeline group refer for $referId", t)
            throw t
        } finally {
            redisLock.unlock()
        }

        return true
    }

fun getProjectPublicParam(userId: String, projectId: String, groupNames: List<String>): List<BuildFormProperty> {
        if (groupNames.isEmpty()) {
            return emptyList()
        }

        val buildFormProperties = mutableListOf<BuildFormProperty>()
        val processedVarNames = mutableSetOf<String>()

        groupNames.forEach { groupName ->
            try {
                // 获取变量组信息（默认获取最新版本）
                val groupRecord = publicVarGroupDao.getRecordByGroupName(
                    dslContext = dslContext,
                    projectId = projectId,
                    groupName = groupName
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
                logger.warn("Failed to get variables from group $groupName", e)
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
}