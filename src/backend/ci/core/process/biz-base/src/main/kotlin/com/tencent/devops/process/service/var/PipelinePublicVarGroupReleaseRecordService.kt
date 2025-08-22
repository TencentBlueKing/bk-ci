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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.dao.`var`.PipelinePublicVarGroupReleseRecordDao
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReleaseDTO
import com.tencent.devops.process.pojo.`var`.enums.OperateTypeEnum
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarGroupReleaseRecordPO
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelinePublicVarGroupReleaseRecordService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelinePublicVarGroupReleaseRecordDao: PipelinePublicVarGroupReleseRecordDao,
    private val client: Client
) {

    fun batchAddPublicVarGroupReleaseRecord(publicVarGroupReleaseDTO: PublicVarGroupReleaseDTO) {
        val userId = publicVarGroupReleaseDTO.userId
        val oldVarPOs = publicVarGroupReleaseDTO.oldVarPOs
        val newVarPOs = publicVarGroupReleaseDTO.newVarPOs

        val records = mutableListOf<PipelinePublicVarGroupReleaseRecordPO>()

        // 1. 处理删除的变量
        val deletedVars = oldVarPOs.filter { oldVar ->
            newVarPOs.none { it.varName == oldVar.varName }
        }
        deletedVars.forEach {
            val typeDesc = PublicVarTypeEnum.getTypeDescription(it.type)
            val desc = "${OperateTypeEnum.DELETE.getI18n(I18nUtil.getLanguage())}$typeDesc ${it.varName}"

            records.add(
                PipelinePublicVarGroupReleaseRecordPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("PIPELINE_PUBLIC_VAR_GROUP_RELEASE_RECORD").data ?: 0,
                    projectId = it.projectId,
                    groupName = it.groupName,
                    version = publicVarGroupReleaseDTO.version,
                    publisher = userId,
                    pubTime = LocalDateTime.now(),
                    desc = desc,
                    content = jacksonObjectMapper().writeValueAsString(
                        mapOf(
                            "operate" to "delete",
                            "varName" to it.varName,
                            "alias" to it.alias,
                            "defaultValue" to it.defaultValue,
                            "desc" to it.desc,
                            "type" to it.type.name
                        )
                    ),
                    creator = userId,
                    modifier = userId,
                    createTime = LocalDateTime.now(),
                    updateTime = LocalDateTime.now()
                )
            )
        }

        // 2. 处理新增的变量
        val addedVars = newVarPOs.filter { newVar ->
            newVar.varName !in oldVarPOs.map { it.varName }
        }
        addedVars.forEach {
            val typeDesc = PublicVarTypeEnum.getTypeDescription(it.type)
            val desc = "${OperateTypeEnum.CREATE.getI18n(I18nUtil.getLanguage())}$typeDesc ${it.varName}"

            records.add(
                PipelinePublicVarGroupReleaseRecordPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("PIPELINE_PUBLIC_VAR_GROUP_RELEASE_RECORD").data ?: 0,
                    projectId = it.projectId,
                    groupName = it.groupName,
                    version = publicVarGroupReleaseDTO.version,
                    publisher = userId,
                    pubTime = LocalDateTime.now(),
                    desc = desc,
                    content = jacksonObjectMapper().writeValueAsString(
                        mapOf(
                            "operate" to OperateTypeEnum.CREATE,
                            "varName" to it.varName,
                            "alias" to it.alias,
                            "defaultValue" to it.defaultValue,
                            "desc" to it.desc,
                            "type" to it.type.name
                        )
                    ),
                    creator = userId,
                    modifier = userId,
                    createTime = LocalDateTime.now(),
                    updateTime = LocalDateTime.now()
                )
            )
        }

        // 3. 处理修改的变量
        val modifiedVars = newVarPOs.filter { newVar ->
            oldVarPOs.any { oldVar ->
                oldVar.varName == newVar.varName &&
                        (oldVar.alias != newVar.alias ||
                                oldVar.desc != newVar.desc ||
                                oldVar.defaultValue != newVar.defaultValue)
            }
        }
        logger.info("modifiedVars: $modifiedVars")
        modifiedVars.forEach { newVar ->
            val oldVar = oldVarPOs.first { it.varName == newVar.varName }
            val typeDesc = PublicVarTypeEnum.getTypeDescription(newVar.type)

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

            if (oldVar.buildFormProperty != newVar.buildFormProperty) {
                val oldBuildFormProperty = JsonUtil.to(oldVar.buildFormProperty, BuildFormProperty::class.java)
                val newBuildFormProperty = JsonUtil.to(newVar.buildFormProperty, BuildFormProperty::class.java)
                if (oldBuildFormProperty.required != newBuildFormProperty.required) {
                    changes["required"] =
                        mapOf("oldValue" to newBuildFormProperty.required, "newValue" to newBuildFormProperty.required)
                }
                if (oldBuildFormProperty.readOnly != newBuildFormProperty.readOnly) {
                    changes["readOnly"] =
                        mapOf("oldValue" to newBuildFormProperty.readOnly, "newValue" to newBuildFormProperty.readOnly)
                }
                if (oldBuildFormProperty.valueNotEmpty != newBuildFormProperty.valueNotEmpty) {
                    changes["valueNotEmpty"] = mapOf(
                        "oldValue" to newBuildFormProperty.valueNotEmpty,
                        "newValue" to newBuildFormProperty.valueNotEmpty
                    )
                }
            }

            if (changes.isNotEmpty()) {
                val desc = "${OperateTypeEnum.UPDATE.getI18n(I18nUtil.getLanguage())}$typeDesc ${newVar.varName}"
                records.add(
                    PipelinePublicVarGroupReleaseRecordPO(
                        id = client.get(ServiceAllocIdResource::class)
                            .generateSegmentId("PIPELINE_PUBLIC_VAR_GROUP_RELEASE_RECORD").data ?: 0,
                        projectId = newVar.projectId,
                        groupName = newVar.groupName,
                        version = publicVarGroupReleaseDTO.version,
                        publisher = userId,
                        pubTime = LocalDateTime.now(),
                        desc = desc,
                        content = jacksonObjectMapper().writeValueAsString(
                            mapOf(
                                "operate" to OperateTypeEnum.UPDATE,
                                "varName" to newVar.varName,
                                "changes" to changes,
                                "desc" to newVar.desc,
                                "type" to newVar.type.name
                            )
                        ),
                        creator = userId,
                        modifier = userId,
                        createTime = LocalDateTime.now(),
                        updateTime = LocalDateTime.now()
                    )
                )
            }
        }
        if (records.isNotEmpty()) {
            pipelinePublicVarGroupReleaseRecordDao.batchInsert(dslContext, records)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelinePublicVarGroupReleaseRecordService::class.java)
    }
}
