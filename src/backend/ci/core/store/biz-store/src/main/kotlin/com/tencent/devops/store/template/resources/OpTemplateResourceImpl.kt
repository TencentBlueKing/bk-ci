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

package com.tencent.devops.store.template.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.template.OpTemplateResource
import com.tencent.devops.store.pojo.template.ApproveReq
import com.tencent.devops.store.pojo.template.OpTemplateResp
import com.tencent.devops.store.pojo.template.enums.OpTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import com.tencent.devops.store.pojo.template.enums.TemplateTypeEnum
import com.tencent.devops.store.template.service.OpTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpTemplateResourceImpl @Autowired constructor(
    private val opTemplateService: OpTemplateService
) : OpTemplateResource {

    override fun listTemplates(
        userId: String,
        templateName: String?,
        templateStatus: TemplateStatusEnum?,
        templateType: TemplateTypeEnum?,
        classifyCode: String?,
        category: String?,
        labelCode: String?,
        latestFlag: Boolean?,
        sortType: OpTemplateSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<OpTemplateResp> {
        return opTemplateService.list(
            userId = userId,
            templateName = templateName,
            templateStatus = templateStatus,
            templateType = templateType,
            classifyCode = classifyCode,
            category = category,
            labelCode = labelCode,
            latestFlag = latestFlag,
            sortType = sortType,
            desc = desc,
            page = page,
            pageSize = pageSize
        )
    }

    override fun approveTemplate(userId: String, templateId: String, approveReq: ApproveReq): Result<Boolean> {
        return opTemplateService.approveTemplate(userId, templateId, approveReq)
    }
}
