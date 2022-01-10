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

package com.tencent.devops.process.template.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.TemplateDetailInfo
import com.tencent.devops.process.pojo.template.TemplateType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

@Suppress("ALL")
@Service
class PipelineTemplateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao
) {

    fun getTemplateDetailInfo(templateCode: String): Result<TemplateDetailInfo?> {
        logger.info("getTemplateDetailInfo templateCode is:$templateCode")
        var templateRecord = templateDao.getLatestTemplate(dslContext, templateCode)
        if (templateRecord.srcTemplateId != null && templateRecord.type == TemplateType.CONSTRAINT.name) {
            templateRecord = templateDao.getLatestTemplate(dslContext, templateRecord.srcTemplateId)
        }
        return Result(
            TemplateDetailInfo(
                templateCode = templateRecord.id,
                templateName = templateRecord.templateName,
                templateModel = if (!StringUtils.isEmpty(templateRecord.template)) JsonUtil.to(
                    templateRecord.template,
                    Model::class.java
                ) else null
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateService::class.java)
    }
}
