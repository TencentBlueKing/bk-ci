/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.template.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.engine.service.template.TemplateService
import com.tencent.devops.process.pojo.PipelineTemplate
import com.tencent.devops.process.pojo.template.TemplateDetailInfo
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.template.dao.PTemplateDao
import com.tencent.devops.process.template.dao.PipelineTemplateDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineTemplateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineTemplateDao: PipelineTemplateDao,
    private val templateDao: PTemplateDao,
    private val objectMapper: ObjectMapper
) {
    @Suppress("UNCHECKED_CAST")
    fun listTemplate(projectCode: String): Map<String, PipelineTemplate> {
        val map = HashMap<String, PipelineTemplate>()

        val templates = pipelineTemplateDao.listTemplates(dslContext, projectCode)

        val srcTemplateIdList = mutableListOf<String>()
        templates.forEach {
            if (!it.srcTemplateId.isNullOrEmpty()) {
                srcTemplateIdList.add(it.srcTemplateId)
            }
        }
        val srcTemplates = mutableMapOf<String, String>()
        templateDao.listLatestTemplateByIds(dslContext, srcTemplateIdList).forEach {
            srcTemplates[it["ID"] as String] = it["TEMPLATE"] as String
        }

        templates.forEach {
            val flag = !it.srcTemplateId.isNullOrEmpty()

            val model = if (flag) {
                objectMapper.readValue(srcTemplates[it.srcTemplateId], Model::class.java)
            } else {
                objectMapper.readValue(it.template, Model::class.java)
            }
            if (flag) {
                model.srcTemplateId = it.srcTemplateId
            }

            val key = if (flag) it.srcTemplateId else it.id.toString()
            map[key] = PipelineTemplate(
                name = if (flag) it.templateName else model.name,
                desc = model.desc,
                category = if (!it.category.isNullOrBlank()) JsonUtil.getObjectMapper().readValue(
                    it.category,
                    List::class.java
                ) as List<String> else listOf(),
                icon = it.icon,
                logoUrl = it.logoUrl,
                author = it.author,
                atomNum = it.atomnum,
                publicFlag = it.publicFlag,
                srcTemplateId = it.srcTemplateId,
                stages = model.stages
            )
        }

        return map
    }

    fun addTemplate(
        userId: String,
        author: String,
        name: String,
        type: TemplateType,
        category: String,
        icon: String?,
        logoUrl: String?,
        projectCode: String,
        model: Model,
        srcTemplateId: String?
    ) {
        val atomNum = generateAtomNum(model)
        pipelineTemplateDao.addTemplate(
            dslContext, name, userId, type.name, category, icon, logoUrl, projectCode, author,
            atomNum, objectMapper.writeValueAsString(model), srcTemplateId
        )
    }

    private fun generateAtomNum(model: Model): Int {
        var atomNum = 0
        model.stages.forEach { s ->
            s.containers.forEach { c ->
                atomNum += c.elements.size
            }
        }
        return atomNum
    }

    fun getTemplateDetailInfo(templateCode: String, publicFlag: Boolean): Result<TemplateDetailInfo?> {
        logger.info("the userId is:$templateCode,publicFlag is:$publicFlag")
        if (publicFlag) {
            val publicTemplateRecord = pipelineTemplateDao.getTemplate(dslContext, templateCode.toInt())
                ?: return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_INVALID,
                    arrayOf(templateCode)
                )
            logger.info("the publicTemplateRecord is:$publicTemplateRecord")
            return Result(
                TemplateDetailInfo(
                    templateCode = publicTemplateRecord.id.toString(),
                    templateName = publicTemplateRecord.templateName,
                    templateModel = if (publicTemplateRecord.template.isNotEmpty()) JsonUtil.to(
                        publicTemplateRecord.template,
                        Model::class.java
                    ) else null
                )
            )
        } else {
            val customizeTemplateRecord = templateDao.getLatestTemplate(dslContext, templateCode)
            logger.info("the customizeTemplateRecord is:$customizeTemplateRecord")
            return Result(
                TemplateDetailInfo(
                    templateCode = customizeTemplateRecord.id,
                    templateName = customizeTemplateRecord.templateName,
                    templateModel = if (customizeTemplateRecord.template.isNotEmpty()) JsonUtil.to(
                        customizeTemplateRecord.template,
                        Model::class.java
                    ) else null
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateService::class.java)
    }
}