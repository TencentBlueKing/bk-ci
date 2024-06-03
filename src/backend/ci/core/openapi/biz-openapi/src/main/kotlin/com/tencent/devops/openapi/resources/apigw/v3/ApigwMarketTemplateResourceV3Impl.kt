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
package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwMarketTemplateResourceV3
import com.tencent.devops.process.api.template.ServicePTemplateResource
import com.tencent.devops.process.pojo.PipelineTemplateInfo
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.template.InstallTemplateReq
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwMarketTemplateResourceV3Impl @Autowired constructor(
    private val client: Client
) : ApigwMarketTemplateResourceV3 {

    override fun installTemplateFromStore(
        appCode: String?,
        apigwType: String?,
        userId: String,
        installTemplateReq: InstallTemplateReq
    ): Result<Boolean> {
        // 可见与可安装鉴权在store服务marketTemplateService中已实现
        logger.info("OPENAPI_MARKET_TEMPLATE_V3|$userId|install template from store|$installTemplateReq")
        return client.get(ServiceTemplateResource::class).installTemplate(userId, installTemplateReq)
    }

    override fun installTemplateFromStoreNew(
        appCode: String?,
        apigwType: String?,
        userId: String,
        installTemplateReq: InstallTemplateReq
    ): Result<List<PipelineTemplateInfo>> {
        logger.info("OPENAPI_MARKET_TEMPLATE_V3|$userId|install template from store new|$installTemplateReq")
        val install = client.get(ServiceTemplateResource::class)
            .installTemplate(userId, installTemplateReq).data ?: false
        return if (install) {
            val templateProjectInfos = client.get(ServicePTemplateResource::class)
                .getTemplateIdBySrcCode(installTemplateReq.templateCode, installTemplateReq.projectCodeList).data
            if (templateProjectInfos.isNullOrEmpty()) {
                return Result(emptyList())
            }
            Result(templateProjectInfos)
        } else {
            Result(emptyList())
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwMarketTemplateResourceV3Impl::class.java)
    }
}
