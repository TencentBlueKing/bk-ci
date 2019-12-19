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

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.api.template.ServiceTemplateResource
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.service.template.TemplateModelService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TemplateModelServiceImpl : TemplateModelService {

    @Autowired
    private lateinit var dslContext: DSLContext
    @Autowired
    private lateinit var marketTemplateDao: MarketTemplateDao
    @Autowired
    private lateinit var client: Client

    private val logger = LoggerFactory.getLogger(TemplateModelServiceImpl::class.java)

    override fun getTemplateModel(templateCode: String): Result<Model?> {
        val templateRecord = marketTemplateDao.getUpToDateTemplateByCode(dslContext, templateCode)
        logger.info("the templateRecord is :$templateRecord")
        if (null == templateRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(templateCode))
        }
        val result = client.get(ServiceTemplateResource::class).getTemplateDetailInfo(templateCode, templateRecord.publicFlag)
        logger.info("the result is :$result")
        if (result.isNotOk()) {
            // 抛出错误提示
            return Result(result.status, result.message ?: "")
        }
        val templateDetailInfo = result.data
        val templateModel = templateDetailInfo?.templateModel
        return Result(templateModel)
    }
}