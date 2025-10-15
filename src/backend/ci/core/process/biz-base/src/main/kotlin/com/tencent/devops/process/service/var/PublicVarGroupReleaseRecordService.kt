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
import com.tencent.devops.process.dao.`var`.PublicVarGroupReleaseRecordDao
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarReleaseDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReleaseDTO
import com.tencent.devops.process.pojo.`var`.enums.OperateTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarGroupReleaseRecordPO
import com.tencent.devops.process.pojo.`var`.po.PublicVarPO
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
    private val client: Client
) {

    fun batchAddPublicVarGroupReleaseRecord(publicVarGroupReleaseDTO: PublicVarGroupReleaseDTO) {
        val userId = publicVarGroupReleaseDTO.userId
        val oldVarPOs = publicVarGroupReleaseDTO.oldVarPOs
        val newVarPOs = publicVarGroupReleaseDTO.newVarPOs
        val oldVarDOs = convertPOToDO(oldVarPOs)
        val newVarDOs = convertPOToDO(newVarPOs)
        val releaseRecords = generateVarChangeRecords(
            oldVars = oldVarDOs,
            newVars = newVarDOs,
            groupName = oldVarPOs.firstOrNull()?.groupName ?: newVarPOs.first().groupName,
            version = publicVarGroupReleaseDTO.version,
            userId = userId,
            pubTime = LocalDateTime.now(),
            versionDesc = publicVarGroupReleaseDTO.versionDesc
        )
        // 批量生成ID
        val segmentIds = if (releaseRecords.isNotEmpty()) {
            client.get(ServiceAllocIdResource::class)
                .batchGenerateSegmentId("T_PIPELINE_PUBLIC_VAR_GROUP_RELEASE_RECORD", releaseRecords.size).data
        } else {
            emptyList()
        }
        if (releaseRecords.isNotEmpty() && segmentIds.isNullOrEmpty()) {
            logger.warn("Failed to generate segment IDs for release records, size: ${releaseRecords.size}")
            throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf("Failed to generate segment IDs")
            )
        }
        var index = 0
        val records = releaseRecords.map { releaseRecord ->
            PipelinePublicVarGroupReleaseRecordPO(
                id = segmentIds?.get(index++) ?: 0,
                projectId = oldVarPOs.firstOrNull()?.projectId ?: newVarPOs.first().projectId,
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
        val count = pipelinePublicVarGroupReleaseRecordDao.countByGroupName(dslContext, projectId, groupName)
        if (count == 0L) {
            return Page(
                page = page,
                pageSize = pageSize,
                count = count,
                records = emptyList()
            )
        }
        return Page(
            page = page,
            pageSize = pageSize,
            count = count,
            records = pipelinePublicVarGroupReleaseRecordDao.listGroupReleaseHistory(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName,
                page = page,
                pageSize = pageSize
            )
        )
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
        
        // 1. 处理删除的变量
        val deletedVars = oldVars.filter { oldVar ->
            newVars.none { it.varName == oldVar.varName }
        }
        deletedVars.forEach { oldVar ->
            val content = JsonUtil.toJson(
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
                    desc = versionDesc
                )
            )
        }
        
        // 2. 处理新增的变量
        val addedVars = newVars.filter { newVar ->
            newVar.varName !in oldVars.map { it.varName }
        }
        addedVars.forEach { newVar ->
            val content = JsonUtil.toJson(
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
                    desc = versionDesc
                )
            )
        }
        
        // 3. 处理修改的变量
        val modifiedVars = newVars.filter { newVar ->
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
                val content = JsonUtil.toJson(
                    mapOf(
                        "operate" to OperateTypeEnum.UPDATE,
                        "varName" to newVar.varName,
                        "changes" to changes,
                        "desc" to newVar.desc,
                        "type" to newVar.type.name,
                        "alias" to newVar.alias,
                        "defaultValue" to newVar.defaultValue
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
     */
    private fun isBuildFormPropertyEqual(prop1: BuildFormProperty, prop2: BuildFormProperty): Boolean {
        return prop1.required == prop2.required &&
                prop1.readOnly == prop2.readOnly &&
                prop1.valueNotEmpty == prop2.valueNotEmpty
    }

    /**
     * 将PublicVarPO转换为PublicVarDO
     */
    fun convertPOToDO(varPOs: List<PublicVarPO>): List<PublicVarDO> {
        return varPOs.map { po ->
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
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupReleaseRecordService::class.java)
    }
}
