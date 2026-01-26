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

    fun batchAddPublicVarGroupReleaseRecord(
        dslContext: DSLContext,
        publicVarGroupReleaseDTO: PublicVarGroupReleaseDTO
    ) {
        val userId = publicVarGroupReleaseDTO.userId
        val oldVarPOs = publicVarGroupReleaseDTO.oldVarPOs
        val newVarPOs = publicVarGroupReleaseDTO.newVarPOs
        val projectId = oldVarPOs.firstOrNull()?.projectId ?: newVarPOs.firstOrNull()?.projectId
            ?: throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf("projectId")
            )
        val groupName = oldVarPOs.firstOrNull()?.groupName ?: newVarPOs.firstOrNull()?.groupName
            ?: throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf("groupName")
            )
        val oldVersion = oldVarPOs.firstOrNull()?.version
        val newVersion = newVarPOs.firstOrNull()?.version

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
        val releaseRecords = generateVarChangeRecords(
            oldVars = oldVarDOs,
            newVars = newVarDOs,
            groupName = groupName,
            version = publicVarGroupReleaseDTO.version,
            userId = userId,
            pubTime = LocalDateTime.now(),
            versionDesc = publicVarGroupReleaseDTO.versionDesc
        )
        // 批量生成ID
        val segmentIds = if (releaseRecords.isNotEmpty()) {
            client.get(ServiceAllocIdResource::class)
                .batchGenerateSegmentId("T_RESOURCE_PUBLIC_VAR_GROUP_RELEASE_RECORD", releaseRecords.size).data
        } else {
            emptyList()
        }
        val listSize = segmentIds?.size ?: 0
        if (segmentIds.isNullOrEmpty() || listSize != releaseRecords.size) {
            logger.warn("Failed to generate segment IDs for release records, size: ${releaseRecords.size}")
            throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf("Failed to generate segment IDs")
            )
        }
        var index = 0
        val records = releaseRecords.map { releaseRecord ->
            ResourcePublicVarGroupReleaseRecordPO(
                id = segmentIds.get(index++) ?: 0,
                projectId = projectId,
                groupName = releaseRecord.groupName,
                version = releaseRecord.version,
                publisher = releaseRecord.publisher,
                pubTime = releaseRecord.pubTime,
                desc = releaseRecord.desc,
                content = releaseRecord.content,
                creator = userId,
                modifier = userId,
                createTime = LocalDateTime.now(),
                updateTime = LocalDateTime.now()
            )
        }
        if (records.isNotEmpty()) {
            pipelinePublicVarGroupReleaseRecordDao.batchInsert(dslContext, records)
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
     * 生成变量变更记录
     * @param oldVars 旧版本变量列表
     * @param newVars 新版本变量列表
     * @param groupName 变量组名称
     * @param version 版本号
     * @param userId 用户ID
     * @param pubTime 发布时间
     * @param versionDesc 版本描述
     * @return 变更记录列表
     */
    fun generateVarChangeRecords(
        oldVars: List<PublicVarDO>,
        newVars: List<PublicVarDO>,
        groupName: String,
        version: Int,
        userId: String,
        pubTime: LocalDateTime,
        versionDesc: String?
    ): List<PublicVarReleaseDO> {
        val releaseRecords = mutableListOf<PublicVarReleaseDO>()

        val newVarNameSet = newVars.map { it.varName }.toSet()
        val oldVarNameSet = oldVars.map { it.varName }.toSet()

        // 处理删除的变量
        val deletedVars = oldVars.filter { it.varName !in newVarNameSet }
        deletedVars.forEach { oldVar ->
            val showInfo = mapOf(
                PublicVarDO::alias.name to oldVar.alias,
                PublicVarDO::desc.name to oldVar.desc,
                PublicVarDO::defaultValue.name to oldVar.defaultValue,
                BuildFormProperty::readOnly.name to oldVar.buildFormProperty.readOnly,
                BuildFormProperty::required.name to oldVar.buildFormProperty.required,
                BuildFormProperty::valueNotEmpty.name to oldVar.buildFormProperty.valueNotEmpty
            )
            val content = JsonUtil.toJson(
                mapOf(
                    OPERATE to OperateTypeEnum.DELETE,
                    PublicVarDO::varName.name to oldVar.varName,
                    TYPE to oldVar.type.name,
                    SHOW_INFO to showInfo
                )
            )

            releaseRecords.add(
                PublicVarReleaseDO(
                    groupName = groupName,
                    version = version,
                    publisher = userId,
                    pubTime = pubTime,
                    content = content,
                    desc = versionDesc
                )
            )
        }

        // 处理新增的变量
        val addedVars = newVars.filter { it.varName !in oldVarNameSet }
        addedVars.forEach { newVar ->
            val showInfo = mapOf(
                PublicVarDO::alias.name to newVar.alias,
                PublicVarDO::desc.name to newVar.desc,
                PublicVarDO::defaultValue.name to newVar.defaultValue,
                BuildFormProperty::required.name to newVar.buildFormProperty.required,
                BuildFormProperty::readOnly.name to newVar.buildFormProperty.readOnly,
                BuildFormProperty::valueNotEmpty.name to newVar.buildFormProperty.valueNotEmpty
            )
            val content = JsonUtil.toJson(
                mapOf(
                    OPERATE to OperateTypeEnum.CREATE,
                    PublicVarDO::varName.name to newVar.varName,
                    TYPE to newVar.type.name,
                    SHOW_INFO to showInfo
                )
            )

            releaseRecords.add(
                PublicVarReleaseDO(
                    groupName = groupName,
                    version = version,
                    publisher = userId,
                    pubTime = pubTime,
                    content = content,
                    desc = versionDesc
                )
            )
        }

        // 处理修改的变量
        val modifiedVars = newVars.filter { newVar ->
            newVar.varName in oldVarNameSet &&
                    oldVars.any { oldVar ->
                        oldVar.varName == newVar.varName &&
                                (oldVar.alias != newVar.alias ||
                                        oldVar.desc != newVar.desc ||
                                        oldVar.defaultValue != newVar.defaultValue ||
                                        !isBuildFormPropertyEqual(oldVar.buildFormProperty, newVar.buildFormProperty))
                    }
        }
        modifiedVars.forEach { newVar ->
            val oldVar = oldVars.first { it.varName == newVar.varName }

            // 收集所有变更字段
            val changes = mutableMapOf<String, Map<String, Any?>>()

            if (oldVar.alias != newVar.alias) {
                changes[PublicVarDO::alias.name] = mapOf(OLD_VALUE to oldVar.alias, NEW_VALUE to newVar.alias)
            }
            if (oldVar.desc != newVar.desc) {
                changes[PublicVarDO::desc.name] = mapOf(OLD_VALUE to oldVar.desc, NEW_VALUE to newVar.desc)
            }
            if (oldVar.defaultValue != newVar.defaultValue) {
                changes[PublicVarDO::defaultValue.name] = mapOf(OLD_VALUE to oldVar.defaultValue, NEW_VALUE to newVar.defaultValue)
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
                    OLD_VALUE to oldBuildFormProperty.valueNotEmpty,
                    NEW_VALUE to newBuildFormProperty.valueNotEmpty
                )
            }

            if (changes.isNotEmpty()) {
                val showInfo = mapOf(
                    PublicVarDO::alias.name to newVar.alias,
                    PublicVarDO::desc.name to newVar.desc,
                    BuildFormProperty::required.name to newVar.buildFormProperty.required,
                    BuildFormProperty::readOnly.name to newVar.buildFormProperty.readOnly,
                    PublicVarDO::defaultValue.name to newVar.defaultValue,
                    BuildFormProperty::valueNotEmpty.name to newVar.buildFormProperty.valueNotEmpty
                )
                val content = JsonUtil.toJson(
                    mapOf(
                        OPERATE to OperateTypeEnum.UPDATE,
                        PublicVarDO::varName.name to newVar.varName,
                        CHANGES to changes,
                        TYPE to newVar.type.name,
                        SHOW_INFO to showInfo
                    )
                )

                releaseRecords.add(
                    PublicVarReleaseDO(
                        groupName = groupName,
                        version = version,
                        publisher = userId,
                        pubTime = pubTime,
                        content = content,
                        desc = versionDesc
                    )
                )
            }
        }

        return releaseRecords
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
     * 将数据库实体对象转换为业务对象，包括解析buildFormProperty JSON字符串
     * @param varPOs 数据库实体对象列表
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param version 版本号（可为null）
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
        val referCountMap = if (varNames.isNotEmpty()) {
            publicVarVersionSummaryDao.batchGetTotalReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                varNames = varNames
            )
        } else {
            emptyMap()
        }

        return varPOs.map { po ->
            val actualReferCount = referCountMap[po.varName] ?: 0
            PublicVarDO(
                varName = po.varName,
                alias = po.alias,
                desc = po.desc,
                type = po.type,
                valueType = po.valueType,
                defaultValue = po.defaultValue,
                buildFormProperty = JsonUtil.to(po.buildFormProperty, BuildFormProperty::class.java),
                referCount = actualReferCount
            )
        }
    }
}