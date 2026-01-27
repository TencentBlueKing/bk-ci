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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST
import com.tencent.devops.process.dao.`var`.PublicVarGroupReleaseRecordDao
import com.tencent.devops.process.dao.`var`.PublicVarVersionSummaryDao
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarReleaseDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReleaseDTO
import com.tencent.devops.process.pojo.`var`.enums.OperateTypeEnum
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReleaseRecordPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicVarGroupReleaseRecordService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelinePublicVarGroupReleaseRecordDao: PublicVarGroupReleaseRecordDao,
    private val publicVarVersionSummaryDao: PublicVarVersionSummaryDao,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReleaseRecordService::class.java)
        const val OLD_VALUE = "oldValue"
        const val NEW_VALUE = "newValue"
        const val OPERATE = "operate"
        const val TYPE = "type"
        const val CHANGES = "changes"
        const val SHOW_INFO = "showInfo"
    }

    /**
     * 批量添加公共变量组发布记录
     * 
     * @param dslContext 数据库上下文
     * @param publicVarGroupReleaseDTO 发布记录DTO
     */
    fun batchAddPublicVarGroupReleaseRecord(
        dslContext: DSLContext,
        publicVarGroupReleaseDTO: PublicVarGroupReleaseDTO
    ) {
        val userId = publicVarGroupReleaseDTO.userId
        val oldVarPOs = publicVarGroupReleaseDTO.oldVarPOs
        val newVarPOs = publicVarGroupReleaseDTO.newVarPOs
        
        // 验证并提取公共参数
        val (projectId, groupName) = extractCommonParams(oldVarPOs, newVarPOs)
        val oldVersion = oldVarPOs.firstOrNull()?.version
        val newVersion = newVarPOs.firstOrNull()?.version

        // 转换PO到DO
        val oldVarDOs = convertPOToDO(
            varPOs = oldVarPOs,
            projectId = projectId,
            groupName = groupName,
            version = oldVersion
        )
        val newVarDOs = convertPOToDO(
            varPOs = newVarPOs,
            projectId = projectId,
            groupName = groupName,
            version = newVersion
        )
        
        // 生成变更记录
        val releaseRecords = generateVarChangeRecords(
            VarChangeRecordRequest(
                oldVars = oldVarDOs,
                newVars = newVarDOs,
                groupName = groupName,
                version = publicVarGroupReleaseDTO.version,
                userId = userId,
                pubTime = LocalDateTime.now(),
                versionDesc = publicVarGroupReleaseDTO.versionDesc
            )
        )
        
        // 如果没有变更记录，直接返回
        if (releaseRecords.isEmpty()) {
            logger.info("No changes detected for group: $groupName, version: ${publicVarGroupReleaseDTO.version}")
            return
        }
        
        // 批量生成ID并转换为PO
        val records = convertReleaseRecordsToPO(
            releaseRecords = releaseRecords,
            projectId = projectId,
            userId = userId
        )
        
        // 批量插入数据库
        pipelinePublicVarGroupReleaseRecordDao.batchInsert(dslContext, records)
    }

    /**
     * 从变量PO列表中提取公共参数（projectId和groupName）
     * 
     * @param oldVarPOs 旧版本变量PO列表
     * @param newVarPOs 新版本变量PO列表
     * @return Pair<projectId, groupName>
     * @throws ErrorCodeException 如果无法提取projectId或groupName
     */
    private fun extractCommonParams(
        oldVarPOs: List<PublicVarPO>,
        newVarPOs: List<PublicVarPO>
    ): Pair<String, String> {
        // 优先从newVarPOs获取，如果为空则从oldVarPOs获取
        val projectId = newVarPOs.firstOrNull()?.projectId 
            ?: oldVarPOs.firstOrNull()?.projectId
            ?: throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf("projectId")
            )
        
        val groupName = newVarPOs.firstOrNull()?.groupName
            ?: oldVarPOs.firstOrNull()?.groupName
            ?: throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf("groupName")
            )
        
        return Pair(projectId, groupName)
    }

    /**
     * 将发布记录DO转换为PO，并批量生成ID
     * 
     * @param releaseRecords 发布记录DO列表
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 发布记录PO列表
     * @throws ErrorCodeException 如果ID生成失败
     */
    private fun convertReleaseRecordsToPO(
        releaseRecords: List<PublicVarReleaseDO>,
        projectId: String,
        userId: String
    ): List<ResourcePublicVarGroupReleaseRecordPO> {
        // 批量生成ID
        val segmentIds = client.get(ServiceAllocIdResource::class)
            .batchGenerateSegmentId("T_RESOURCE_PUBLIC_VAR_GROUP_RELEASE_RECORD", releaseRecords.size).data
        
        if (segmentIds.isNullOrEmpty() || segmentIds.size != releaseRecords.size) {
            logger.warn("Failed to generate segment IDs for release records, expected: ${releaseRecords.size}, actual: ${segmentIds?.size ?: 0}")
            throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf("Failed to generate segment IDs")
            )
        }
        
        val currentTime = LocalDateTime.now()
        return releaseRecords.mapIndexed { index, releaseRecord ->
            ResourcePublicVarGroupReleaseRecordPO(
                id = segmentIds[index] ?: 0,
                projectId = projectId,
                groupName = releaseRecord.groupName,
                version = releaseRecord.version,
                publisher = releaseRecord.publisher,
                pubTime = releaseRecord.pubTime,
                desc = releaseRecord.desc,
                content = releaseRecord.content,
                creator = userId,
                modifier = userId,
                createTime = currentTime,
                updateTime = currentTime
            )
        }
    }

    /**
     * 获取公共变量组版本历史
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param page 页码
     * @param pageSize 每页数量
     * @return 版本历史列表
     */
    fun getReleaseHistory(
        projectId: String,
        groupName: String,
        page: Int,
        pageSize: Int
    ): Page<PublicVarReleaseDO> {
        // 按版本分组统计数量
        val count = pipelinePublicVarGroupReleaseRecordDao.countVersionsByGroupName(dslContext, projectId, groupName)
        if (count == 0L) {
            return Page(
                page = page,
                pageSize = pageSize,
                count = count,
                records = emptyList()
            )
        }

        // 获取分页的版本号列表
        val versions = pipelinePublicVarGroupReleaseRecordDao.listDistinctVersions(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            page = page,
            pageSize = pageSize
        )

        // 对每个版本，查询所有记录并组合 content
        val records = versions.map { version ->
            val versionRecords = pipelinePublicVarGroupReleaseRecordDao.listAllRecordsByVersion(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                version = version
            )

            // 取第一条记录的基本信息
            val firstRecord = versionRecords.firstOrNull()
            if (firstRecord == null) {
                throw ErrorCodeException(
                    errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_NOT_EXIST,
                    params = arrayOf(groupName)
                )
            }

            // 解析并组合所有记录的 content
            val combinedContent = combineReleaseContents(versionRecords)

            PublicVarReleaseDO(
                groupName = firstRecord.groupName,
                version = firstRecord.version,
                pubTime = firstRecord.pubTime,
                publisher = firstRecord.publisher,
                content = combinedContent,
                desc = firstRecord.desc
            )
        }

        return Page(
            page = page,
            pageSize = pageSize,
            count = count,
            records = records
        )
    }

    /**
     * 组合多条发布记录的content
     * 将多条发布记录按操作类型和变量类型分类汇总，生成统一的描述文本
     * 输出格式：新增变量: var1、var2，新增常量: const1、const2，修改变量: var3、var4，删除变量: var5
     * @param records 发布记录列表
     * @return 组合后的描述文本
     */
    private fun combineReleaseContents(records: List<PublicVarReleaseDO>): String {
        // 使用 Map 结构来组织变量分类，key 为 "操作类型_变量类型"
        val varMap = mutableMapOf<String, MutableList<String>>()

        // 解析所有记录，按操作类型和变量类型分类
        records.forEach { record ->
            parseAndClassifyRecord(record, varMap)
        }

        // 构建输出文本
        return buildOutputText(varMap)
    }

    /**
     * 解析单条记录并分类到varMap中
     * 从记录的content中提取操作类型、变量名和变量类型，按照"操作类型_变量类型"的格式分类存储
     * @param record 发布记录
     * @param varMap 分类存储的Map，键为"操作类型_变量类型"，值为变量名列表
     */
    private fun parseAndClassifyRecord(
        record: PublicVarReleaseDO,
        varMap: MutableMap<String, MutableList<String>>
    ) {
        try {
            val contentMap = JsonUtil.toMap(record.content)
            val operate = contentMap[OPERATE]?.toString() ?: return
            val varName = contentMap[PublicVarDO::varName.name]?.toString() ?: return
            val type = contentMap[TYPE]?.toString() ?: PublicVarTypeEnum.VARIABLE.name

            val key = "${operate}_${type}"
            varMap.getOrPut(key) { mutableListOf() }.add(varName)
        } catch (e: Exception) {
            logger.warn("Failed to parse content: ${record.content}", e)
        }
    }

    /**
     * 构建输出文本
     * 按照预定义的顺序（新增变量、新增常量、修改变量、修改常量、删除变量、删除常量）
     * 将分类好的变量组合成一条描述文本
     * @param varMap 分类后的变量映射
     * @return 格式化后的描述文本
     */
    private fun buildOutputText(varMap: Map<String, List<String>>): String {
        // 提前获取国际化文本，避免重复调用
        val createText = OperateTypeEnum.CREATE.getI18n()
        val updateText = OperateTypeEnum.UPDATE.getI18n()
        val deleteText = OperateTypeEnum.DELETE.getI18n()
        val variableText = PublicVarTypeEnum.VARIABLE.getI18n()
        val constantText = PublicVarTypeEnum.CONSTANT.getI18n()

        // 定义输出顺序和对应的文本
        val outputOrder = listOf(
            Triple("${OperateTypeEnum.CREATE.name}_${PublicVarTypeEnum.VARIABLE.name}", createText, variableText),
            Triple("${OperateTypeEnum.CREATE.name}_${PublicVarTypeEnum.CONSTANT.name}", createText, constantText),
            Triple("${OperateTypeEnum.UPDATE.name}_${PublicVarTypeEnum.VARIABLE.name}", updateText, variableText),
            Triple("${OperateTypeEnum.UPDATE.name}_${PublicVarTypeEnum.CONSTANT.name}", updateText, constantText),
            Triple("${OperateTypeEnum.DELETE.name}_${PublicVarTypeEnum.VARIABLE.name}", deleteText, variableText),
            Triple("${OperateTypeEnum.DELETE.name}_${PublicVarTypeEnum.CONSTANT.name}", deleteText, constantText)
        )

        // 按顺序构建输出文本，只包含非空的变量列表
        return outputOrder.mapNotNull { (key, operateText, typeText) ->
            varMap[key]?.takeIf { it.isNotEmpty() }?.let { varNames ->
                "$operateText$typeText: ${varNames.joinToString("、")}"
            }
        }.joinToString("，")
    }

    /**
     * 变量变更记录生成请求DTO
     */
    data class VarChangeRecordRequest(
        val oldVars: List<PublicVarDO>,
        val newVars: List<PublicVarDO>,
        val groupName: String,
        val version: Int,
        val userId: String,
        val pubTime: LocalDateTime,
        val versionDesc: String?
    )

    /**
     * 生成变量变更记录
     * @param request 变量变更记录生成请求
     * @return 变更记录列表
     */
    fun generateVarChangeRecords(request: VarChangeRecordRequest): List<PublicVarReleaseDO> {
        val oldVars = request.oldVars
        val newVars = request.newVars
        val groupName = request.groupName
        val version = request.version
        val userId = request.userId
        val pubTime = request.pubTime
        val versionDesc = request.versionDesc
        val releaseRecords = mutableListOf<PublicVarReleaseDO>()

        val newVarNameSet = newVars.map { it.varName }.toSet()
        val oldVarNameSet = oldVars.map { it.varName }.toSet()
        val oldVarByName = oldVars.associateBy { it.varName }

        // 处理删除的变量
        val deletedVars = oldVars.filter { it.varName !in newVarNameSet }
        deletedVars.forEach { oldVar ->
            releaseRecords.add(
                buildReleaseDO(
                    groupName = groupName,
                    version = version,
                    userId = userId,
                    pubTime = pubTime,
                    versionDesc = versionDesc,
                    operate = OperateTypeEnum.DELETE,
                    varName = oldVar.varName,
                    type = oldVar.type.name,
                    showInfo = buildShowInfoFromVar(oldVar)
                )
            )
        }

        // 处理新增的变量
        val addedVars = newVars.filter { it.varName !in oldVarNameSet }
        addedVars.forEach { newVar ->
            releaseRecords.add(
                buildReleaseDO(
                    groupName = groupName,
                    version = version,
                    userId = userId,
                    pubTime = pubTime,
                    versionDesc = versionDesc,
                    operate = OperateTypeEnum.CREATE,
                    varName = newVar.varName,
                    type = newVar.type.name,
                    showInfo = buildShowInfoFromVar(newVar)
                )
            )
        }

        // 处理修改的变量（存在旧集合中且至少有一个字段发生变化）
        val modifiedVars = newVars.filter { newVar ->
            newVar.varName in oldVarNameSet && isVarContentModified(oldVarByName[newVar.varName]!!, newVar)
        }
        modifiedVars.forEach { newVar ->
            val oldVar = oldVarByName[newVar.varName]!!

            // 收集所有变更字段
            val changes = mutableMapOf<String, Map<String, Any?>>()

            if (oldVar.alias != newVar.alias) {
                changes[PublicVarDO::alias.name] = mapOf(OLD_VALUE to oldVar.alias, NEW_VALUE to newVar.alias)
            }
            if (oldVar.desc != newVar.desc) {
                changes[PublicVarDO::desc.name] = mapOf(OLD_VALUE to oldVar.desc, NEW_VALUE to newVar.desc)
            }
            if (oldVar.defaultValue != newVar.defaultValue) {
                changes[PublicVarDO::defaultValue.name] =
                    mapOf(OLD_VALUE to oldVar.defaultValue, NEW_VALUE to newVar.defaultValue)
            }

            // 直接比较BuildFormProperty对象的属性
            val oldBuildFormProperty = oldVar.buildFormProperty
            val newBuildFormProperty = newVar.buildFormProperty

            if (oldBuildFormProperty.required != newBuildFormProperty.required) {
                changes[BuildFormProperty::required.name] =
                    mapOf(OLD_VALUE to oldBuildFormProperty.required, NEW_VALUE to newBuildFormProperty.required)
            }
            if (oldBuildFormProperty.readOnly != newBuildFormProperty.readOnly) {
                changes[BuildFormProperty::readOnly.name] =
                    mapOf(OLD_VALUE to oldBuildFormProperty.readOnly, NEW_VALUE to newBuildFormProperty.readOnly)
            }
            if (oldBuildFormProperty.valueNotEmpty != newBuildFormProperty.valueNotEmpty) {
                changes[BuildFormProperty::valueNotEmpty.name] = mapOf(
                    OLD_VALUE to oldBuildFormProperty.valueNotEmpty, NEW_VALUE to newBuildFormProperty.valueNotEmpty
                )
            }

            if (changes.isNotEmpty()) {
                val showInfo = buildShowInfoFromVar(newVar)
                releaseRecords.add(
                    buildReleaseDOForUpdate(
                        groupName = groupName,
                        version = version,
                        userId = userId,
                        pubTime = pubTime,
                        versionDesc = versionDesc,
                        varName = newVar.varName,
                        type = newVar.type.name,
                        changes = changes,
                        showInfo = showInfo
                    )
                )
            }
        }

        return releaseRecords
    }

    /**
     * 从 PublicVarDO 构建 SHOW_INFO 映射（用于发布记录 content）
     */
    private fun buildShowInfoFromVar(varDO: PublicVarDO): Map<String, Any?> {
        return mapOf(
            PublicVarDO::alias.name to varDO.alias,
            PublicVarDO::desc.name to varDO.desc,
            PublicVarDO::defaultValue.name to varDO.defaultValue,
            BuildFormProperty::required.name to varDO.buildFormProperty.required,
            BuildFormProperty::readOnly.name to varDO.buildFormProperty.readOnly,
            BuildFormProperty::valueNotEmpty.name to varDO.buildFormProperty.valueNotEmpty
        )
    }

    /**
     * 构建单条发布记录 DO（新增/删除场景）
     */
    private fun buildReleaseDO(
        groupName: String,
        version: Int,
        userId: String,
        pubTime: LocalDateTime,
        versionDesc: String?,
        operate: OperateTypeEnum,
        varName: String,
        type: String,
        showInfo: Map<String, Any?>
    ): PublicVarReleaseDO {
        val content = JsonUtil.toJson(
            mapOf(
                OPERATE to operate,
                PublicVarDO::varName.name to varName,
                TYPE to type,
                SHOW_INFO to showInfo
            )
        )
        return PublicVarReleaseDO(
            groupName = groupName,
            version = version,
            publisher = userId,
            pubTime = pubTime,
            content = content,
            desc = versionDesc
        )
    }

    /**
     * 构建单条发布记录 DO（修改场景）
     */
    private fun buildReleaseDOForUpdate(
        groupName: String,
        version: Int,
        userId: String,
        pubTime: LocalDateTime,
        versionDesc: String?,
        varName: String,
        type: String,
        changes: Map<String, Map<String, Any?>>,
        showInfo: Map<String, Any?>
    ): PublicVarReleaseDO {
        val content = JsonUtil.toJson(
            mapOf(
                OPERATE to OperateTypeEnum.UPDATE,
                PublicVarDO::varName.name to varName,
                CHANGES to changes,
                TYPE to type,
                SHOW_INFO to showInfo
            )
        )
        return PublicVarReleaseDO(
            groupName = groupName,
            version = version,
            publisher = userId,
            pubTime = pubTime,
            content = content,
            desc = versionDesc
        )
    }

    /**
     * 判断变量内容是否有变更（别名、描述、默认值、buildFormProperty 任一不同即视为已修改）
     */
    private fun isVarContentModified(oldVar: PublicVarDO, newVar: PublicVarDO): Boolean {
        return oldVar.alias != newVar.alias ||
                oldVar.desc != newVar.desc ||
                oldVar.defaultValue != newVar.defaultValue ||
                !isBuildFormPropertyEqual(oldVar.buildFormProperty, newVar.buildFormProperty)
    }

    /**
     * 比较两个BuildFormProperty对象是否相等
     * 只比较关键属性：required、readOnly、valueNotEmpty
     * @param prop1 第一个属性对象
     * @param prop2 第二个属性对象
     * @return 是否相等
     */
    private fun isBuildFormPropertyEqual(prop1: BuildFormProperty, prop2: BuildFormProperty): Boolean {
        return prop1.required == prop2.required &&
                prop1.readOnly == prop2.readOnly &&
                prop1.valueNotEmpty == prop2.valueNotEmpty
    }

    /**
     * 将PublicVarPO转换为PublicVarDO
     * 将数据库实体对象转换为业务对象，包括解析buildFormProperty JSON字符串和批量查询引用计数
     * 
     * @param varPOs 数据库实体对象列表
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param version 版本号（可为null，用于设置buildFormProperty.varGroupVersion）
     * @return 业务对象列表
     */
    fun convertPOToDO(
        varPOs: List<PublicVarPO>,
        projectId: String,
        groupName: String,
        version: Int?
    ): List<PublicVarDO> {
        if (varPOs.isEmpty()) return emptyList()

        // 批量查询引用计数（从 T_PIPELINE_PUBLIC_VAR_VERSION_SUMMARY 表读取，汇总所有版本）
        val varNames = varPOs.map { it.varName }
        val referCountMap = publicVarVersionSummaryDao.batchGetTotalReferCount(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            varNames = varNames
        )

        return varPOs.map { po ->
            val buildFormProperty = JsonUtil.to(po.buildFormProperty, BuildFormProperty::class.java)
            // 如果提供了version，设置到buildFormProperty中
            if (version != null) {
                buildFormProperty.varGroupVersion = version
            }
            val actualReferCount = referCountMap[po.varName] ?: 0
            PublicVarDO(
                varName = po.varName,
                alias = po.alias,
                desc = po.desc,
                type = po.type,
                valueType = po.valueType,
                defaultValue = po.defaultValue,
                buildFormProperty = buildFormProperty,
                referCount = actualReferCount
            )
        }
    }
}