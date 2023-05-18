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

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.dao.template.StoreTemplateDao
import com.tencent.devops.store.pojo.common.Classify
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.REJECT
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.template.ApproveReq
import com.tencent.devops.store.pojo.template.OpTemplateItem
import com.tencent.devops.store.pojo.template.OpTemplateResp
import com.tencent.devops.store.pojo.template.enums.OpTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import com.tencent.devops.store.pojo.template.enums.TemplateTypeEnum
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.template.OpTemplateService
import com.tencent.devops.store.service.template.TemplateCategoryService
import com.tencent.devops.store.service.template.TemplateLabelService
import com.tencent.devops.store.service.template.TemplateNotifyService
import com.tencent.devops.store.service.template.TemplateReleaseService
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList", "ComplexMethod")
class OpTemplateServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeTemplateDao: StoreTemplateDao,
    private val marketTemplateDao: MarketTemplateDao,
    private val templateLabelService: TemplateLabelService,
    private val templateCategoryService: TemplateCategoryService,
    private val templateClassifyService: ClassifyService,
    private val templateReleaseService: TemplateReleaseService,
    private val templateNotifyService: TemplateNotifyService
) : OpTemplateService {

    private val logger = LoggerFactory.getLogger(OpTemplateServiceImpl::class.java)

    override fun list(
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
        val categoryList = if (category.isNullOrEmpty()) listOf() else category.split(",")
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode.split(",")
        val count = storeTemplateDao.count(dslContext, templateName, templateStatus?.status?.toByte(),
            templateType?.type?.toByte(), classifyCode, categoryList, labelCodeList, latestFlag)
        val templates = storeTemplateDao.list(
            dslContext, templateName, templateStatus?.status?.toByte(), templateType?.type?.toByte(), classifyCode,
            categoryList, labelCodeList, latestFlag, sortType?.sortType, desc, page, pageSize)
        val classifyListTemp = templateClassifyService.getAllClassify(StoreTypeEnum.TEMPLATE.type.toByte()).data
        val classifyMap = mutableMapOf<String, Classify>()
        classifyListTemp?.forEach {
            classifyMap[it.id] = it
        }

        val ret = mutableListOf<OpTemplateItem>()
        templates?.forEach {
            val status = it["TEMPLATE_STATUS"] as Byte
            val type = it["TEMPLATE_TYPE"] as Byte
            val templateId = it["ID"] as String
            val classifyId = it["CLASSIFY_ID"] as String
            val createTime = it["CREATE_TIME"] as? LocalDateTime
            val updateTime = it["UPDATE_TIME"] as? LocalDateTime
            ret.add(OpTemplateItem(
                templateId = templateId,
                templateName = it["TEMPLATE_NAME"] as String,
                templateCode = it["TEMPLATE_CODE"] as String,
                logoUrl = if (it["LOGO_URL"] != null) it["LOGO_URL"] as String else "",
                classifyId = classifyId,
                classifyCode = classifyMap[classifyId]?.classifyCode,
                classifyName = classifyMap[classifyId]?.classifyName,
                summary = if (it["SUMMARY"] != null) it["SUMMARY"] as String else "",
                templateStatus = TemplateStatusEnum.getTemplateStatus(status.toInt()),
                description = if (it["DESCRIPTION"] != null) it["DESCRIPTION"] as String else "",
                version = if (it["VERSION"] != null) it["VERSION"] as String else "",
                templateType = TemplateTypeEnum.getTemplateType(type.toInt()),
                categoryList = templateCategoryService.getCategorysByTemplateId(templateId).data,
                labelList = templateLabelService.getLabelsByTemplateId(templateId).data,
                latestFlag = it["LATEST_FLAG"] as Boolean,
                publisher = if (it["PUBLISHER"] != null) it["PUBLISHER"] as String else "",
                pubDescription = if (it["PUB_DESCRIPTION"] != null) it["PUB_DESCRIPTION"] as String else "",
                creator = if (it["CREATOR"] != null) it["CREATOR"] as String else "",
                modifier = if (it["MODIFIER"] != null) it["MODIFIER"] as String else "",
                createTime = if (createTime != null) DateTimeUtil.toDateTime(createTime) else "",
                updateTime = if (updateTime != null) DateTimeUtil.toDateTime(updateTime) else ""
            ))
        }

        return Result(OpTemplateResp(
            count = count,
            page = page,
            pageSize = pageSize,
            records = ret
        ))
    }

    /**
     * 审核模版
     */
    override fun approveTemplate(userId: String, templateId: String, approveReq: ApproveReq): Result<Boolean> {
        logger.info("approveTemplate userId is :$userId,templateId is :$templateId,approveReq is :$approveReq")
        // 判断模版是否存在
        val template = marketTemplateDao.getTemplate(dslContext, templateId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(templateId),
                language = I18nUtil.getLanguage(userId)
            )
        val oldStatus = template.templateStatus
        if (oldStatus != TemplateStatusEnum.AUDITING.status.toByte()) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(templateId),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val approveResult = approveReq.result
        if (approveResult != PASS && approveResult != REJECT) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(approveResult),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val templateStatus =
            if (approveResult == PASS) {
                TemplateStatusEnum.RELEASED.status.toByte()
            } else {
                TemplateStatusEnum.AUDIT_REJECT.status.toByte()
            }
        val type = if (approveResult == PASS) AuditTypeEnum.AUDIT_SUCCESS else AuditTypeEnum.AUDIT_REJECT
        val templateStatusMsg = approveReq.message
        dslContext.transaction { t ->
            val context = DSL.using(t)
            templateReleaseService.handleTemplateRelease(
                context = context,
                userId = userId,
                approveResult = approveResult,
                template = template,
                templateStatus = templateStatus,
                templateStatusMsg = templateStatusMsg
            )
        }

        // 发送通知消息
        templateNotifyService.sendTemplateReleaseAuditNotifyMessage(templateId, type)
        return Result(true)
    }
}
