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

package com.tencent.devops.process.template.dao

import com.tencent.devops.model.process.tables.TPipelineTemplate
import com.tencent.devops.model.process.tables.records.TPipelineTemplateRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class PipelineTemplateDao {

    fun listTemplates(dslContext: DSLContext, projectCode: String): Result<TPipelineTemplateRecord> {
        return with(TPipelineTemplate.T_PIPELINE_TEMPLATE) {
            dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .or(PUBLIC_FLAG.eq(true))
                .orderBy(CREATE_TIME.asc())
                .fetch()
        }
    }

    fun listAllTemplates(dslContext: DSLContext): Result<TPipelineTemplateRecord> {
        return with(TPipelineTemplate.T_PIPELINE_TEMPLATE) {
            dslContext.selectFrom(this)
                .orderBy(CREATE_TIME.asc())
                .fetch()
        }
    }

    fun addTemplate(
        dslContext: DSLContext,
        userId: String,
        name: String,
        type: String,
        category: String,
        icon: String?,
        logoUrl: String?,
        projectCode: String,
        author: String,
        atomNum: Int,
        template: String?,
        srcTemplateId: String?
    ) {
        with(TPipelineTemplate.T_PIPELINE_TEMPLATE) {
            val count = dslContext.selectCount()
                .from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(SRC_TEMPLATE_ID.eq(srcTemplateId))
                .fetchOne(0, Int::class.java)
            if (count == 0) {
                dslContext.insertInto(
                    this,
                    TEMPLATE_NAME,
                    TYPE,
                    CATEGORY,
                    ICON,
                    LOGO_URL,
                    PROJECT_CODE,
                    AUTHOR,
                    ATOMNUM,
                    TEMPLATE,
                    SRC_TEMPLATE_ID,
                    CREATOR
                )
                    .values(
                        name,
                        type,
                        category,
                        icon ?: "",
                        logoUrl ?: "",
                        projectCode,
                        author,
                        atomNum,
                        template,
                        srcTemplateId,
                        userId
                    )
                    .execute()
            } else {
                dslContext.update(this)
                    .set(TEMPLATE_NAME, name)
                    .set(CATEGORY, category)
                    .set(ICON, icon ?: "")
                    .set(LOGO_URL, logoUrl ?: "")
                    .set(AUTHOR, author)
                    .set(ATOMNUM, atomNum)
                    .set(TEMPLATE, template)
                    .where(PROJECT_CODE.eq(projectCode))
                    .and(SRC_TEMPLATE_ID.eq(srcTemplateId))
                    .execute()
            }
        }
    }

    fun updateTemplateReference(
        dslContext: DSLContext,
        srcTemplateId: String,
        name: String,
        category: String,
        logoUrl: String?,
        author: String
    ) {
        with(TPipelineTemplate.T_PIPELINE_TEMPLATE) {
            dslContext.update(this)
                .set(TEMPLATE_NAME, name)
                .set(CATEGORY, category)
                .set(LOGO_URL, logoUrl)
                .set(AUTHOR, author)
                .where(SRC_TEMPLATE_ID.eq(srcTemplateId))
                .execute()
        }
    }

    fun getTemplate(dslContext: DSLContext, templateId: Int): TPipelineTemplateRecord? {
        with(TPipelineTemplate.T_PIPELINE_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(templateId))
                .fetchOne()
        }
    }

    /**
     * 判断是否有关联的模版
     */
    fun isExistAssociateTemplate(dslContext: DSLContext, templateId: String): Boolean {
        with(TPipelineTemplate.T_PIPELINE_TEMPLATE) {
            return dslContext.selectCount()
                .from(this)
                .where(SRC_TEMPLATE_ID.eq(templateId))
                .fetchOne(0, Long::class.java) > 0
        }
    }
}