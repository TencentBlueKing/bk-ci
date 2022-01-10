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

package com.tencent.devops.statistics.dao.process.template

import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.process.pojo.template.TemplateType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class TemplateDao {

    /**
     * 获取某一些项目下的模板总量
     */
    fun countTemplateByProjectIds(
        dslContext: DSLContext,
        projectIds: Set<String>,
        includePublicFlag: Boolean?,
        templateType: TemplateType?,
        templateName: String?,
        storeFlag: Boolean?
    ): Int {
        with(TTemplate.T_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.`in`(projectIds))
            if (templateType != null) {
                conditions.add(TYPE.eq(templateType.name))
            }
            if (templateName != null) {
                conditions.add(TEMPLATE_NAME.eq(templateName))
            }
            if (storeFlag != null) {
                conditions.add(STORE_FLAG.eq(storeFlag))
            }

            return dslContext
                .select(DSL.countDistinct(ID))
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countTemplate(
        dslContext: DSLContext,
        projectId: String?,
        includePublicFlag: Boolean?,
        templateType: TemplateType?,
        templateName: String?,
        storeFlag: Boolean?
    ): Int {
        with(TTemplate.T_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            if (projectId != null) {
                if (includePublicFlag != null && includePublicFlag) {
                    conditions.add(PROJECT_ID.eq(projectId).or(TYPE.eq(TemplateType.PUBLIC.name)))
                } else {
                    conditions.add(PROJECT_ID.eq(projectId))
                }
            }
            if (templateType != null) {
                conditions.add(TYPE.eq(templateType.name))
            }
            if (templateName != null) {
                conditions.add(TEMPLATE_NAME.eq(templateName))
            }
            if (storeFlag != null) {
                conditions.add(STORE_FLAG.eq(storeFlag))
            }

            return dslContext
                .select(DSL.countDistinct(ID))
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun listTemplate(
        dslContext: DSLContext,
        projectId: String?,
        includePublicFlag: Boolean?,
        templateType: TemplateType?,
        templateIdList: Collection<String>?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val a = TTemplate.T_TEMPLATE.`as`("a")

        val conditions = mutableListOf<Condition>()
        if (projectId != null) {
            if (includePublicFlag != null && includePublicFlag) {
                conditions.add(a.PROJECT_ID.eq(projectId).or(a.TYPE.eq(TemplateType.PUBLIC.name)))
            } else {
                conditions.add(a.PROJECT_ID.eq(projectId))
            }
        }

        return listTemplateByProjectCondition(
            dslContext = dslContext,
            templateType = templateType,
            templateIdList = templateIdList,
            storeFlag = storeFlag,
            page = page,
            pageSize = pageSize,
            a = a,
            conditions = conditions
        )
    }

    /**
     * 批量获取某一些Project下的模板，用做统计
     */
    fun listTemplateByProjectIds(
        dslContext: DSLContext,
        projectIds: Set<String>,
        includePublicFlag: Boolean?,
        templateType: TemplateType?,
        templateIdList: Collection<String>?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val a = TTemplate.T_TEMPLATE.`as`("a")

        val conditions = mutableListOf<Condition>()
        conditions.add(a.PROJECT_ID.`in`(projectIds))

        return listTemplateByProjectCondition(
            dslContext = dslContext,
            templateType = templateType,
            templateIdList = templateIdList,
            storeFlag = storeFlag,
            page = page,
            pageSize = pageSize,
            a = a,
            conditions = conditions
        )
    }

    fun listTemplateByProjectCondition(
        dslContext: DSLContext,
        templateType: TemplateType?,
        templateIdList: Collection<String>?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?,
        a: TTemplate,
        conditions: MutableList<Condition>
    ): Result<out Record>? {
        if (templateType != null) {
            conditions.add(a.TYPE.eq(templateType.name))
        }
        if (templateIdList != null && templateIdList.isNotEmpty()) {
            conditions.add(a.ID.`in`(templateIdList))
        }
        if (storeFlag != null) {
            conditions.add(a.STORE_FLAG.eq(storeFlag))
        }
        val t = dslContext.select(a.ID.`as`("templateId"), DSL.max(a.VERSION).`as`("version"))
            .from(a).where(conditions).groupBy(a.ID)

        val baseStep = dslContext.select(
            a.ID.`as`("templateId"),
            a.TEMPLATE_NAME.`as`("name"),
            a.VERSION.`as`("version"),
            a.VERSION_NAME.`as`("versionName"),
            a.TYPE.`as`("templateType"),
            a.LOGO_URL.`as`("logoUrl"),
            a.STORE_FLAG.`as`("storeFlag"),
            a.CREATOR.`as`("creator"),
            a.CREATED_TIME.`as`("createdTime"),
            a.SRC_TEMPLATE_ID.`as`("srcTemplateId"),
            a.TEMPLATE.`as`("template"),
            a.CATEGORY.`as`("category"),
            a.PROJECT_ID.`as`("projectId")
        )
            .from(a)
            .join(t)
            .on(
                a.ID.eq(t.field("templateId", String::class.java)).and(
                    a.VERSION.eq(
                        t.field(
                            "version",
                            Long::class.java
                        )
                    )
                )
            )
            .orderBy(a.WEIGHT.desc(), a.CREATED_TIME.desc())

        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    /**
     * 统计用户自定义模板总数
     * 统计规则：template字段不为NULL
     */
    fun getCustomizedTemplate(
        dslContext: DSLContext,
        projectIds: Set<String>
    ): Result<Record1<String>> {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectDistinct(ID).from(this)
                .where(PROJECT_ID.`in`(projectIds))
                .and(TEMPLATE.isNotNull)
                .fetch()
        }
    }

    /**
     * 统计被复制使用的原始模板总数
     * 统计规则：template为NULL的模板的src_template去重
     */
    fun getOriginalTemplate(
        dslContext: DSLContext,
        projectIds: Set<String>
    ): Result<Record1<String>> {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectDistinct(SRC_TEMPLATE_ID).from(this)
                .where(PROJECT_ID.`in`(projectIds))
                .and(TEMPLATE.isNull)
                .fetch()
        }
    }
}
