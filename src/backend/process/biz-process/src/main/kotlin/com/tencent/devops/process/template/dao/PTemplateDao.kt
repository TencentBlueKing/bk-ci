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

import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.model.process.tables.records.TTemplateRecord
import com.tencent.devops.process.pojo.template.TemplateType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class PTemplateDao {

    /**
     * 创建模板
     */
    fun create(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        templateName: String,
        versionName: String,
        userId: String,
        template: String,
        storeFlag: Boolean
    ): Long {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.insertInto(
                this,
                PROJECT_ID, ID, TEMPLATE_NAME, VERSION_NAME, CREATOR, CREATED_TIME, TEMPLATE, STORE_FLAG
            )
                .values(
                    projectId,
                    templateId,
                    templateName,
                    versionName,
                    userId,
                    java.time.LocalDateTime.now(),
                    template,
                    storeFlag
                )
                .returning(VERSION)
                .fetchOne().version
        }
    }

    fun createTemplate(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        templateName: String,
        versionName: String,
        userId: String,
        template: String?,
        type: String,
        category: String?,
        logoUrl: String?,
        srcTemplateId: String?,
        storeFlag: Boolean,
        weight: Int
    ): Long {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                ID,
                TEMPLATE_NAME,
                VERSION_NAME,
                CREATOR,
                CREATED_TIME,
                TEMPLATE,
                TYPE,
                CATEGORY,
                LOGO_URL,
                SRC_TEMPLATE_ID,
                STORE_FLAG,
                WEIGHT
            )
                .values(
                    projectId,
                    templateId,
                    templateName,
                    versionName,
                    userId,
                    java.time.LocalDateTime.now(),
                    template,
                    type,
                    category,
                    logoUrl,
                    srcTemplateId,
                    storeFlag,
                    weight
                )
                .returning(VERSION)
                .fetchOne().version
        }
    }

    fun updateTemplateReference(
        dslContext: DSLContext,
        srcTemplateId: String,
        name: String,
        category: String,
        logoUrl: String?
    ) {
        with(TTemplate.T_TEMPLATE) {
            dslContext.update(this)
                .set(TEMPLATE_NAME, name)
                .set(CATEGORY, category)
                .set(LOGO_URL, logoUrl)
                .where(SRC_TEMPLATE_ID.eq(srcTemplateId))
                .execute()
        }
    }

    fun updateStoreFlag(dslContext: DSLContext, userId: String, templateId: String, storeFlag: Boolean): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.update(this).set(STORE_FLAG, storeFlag).where(ID.eq(templateId)).execute()
        }
    }

    fun exist(dslContext: DSLContext, projectId: String, templateName: String): Boolean {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(TEMPLATE_NAME.eq(templateName))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne() != null
        }
    }

    fun exist(
        dslContext: DSLContext,
        projectId: String,
        templateName: String,
        templateId: String
    ): Boolean {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(TEMPLATE_NAME.eq(templateName))
                .and(PROJECT_ID.eq(projectId))
                .and(ID.ne(templateId))
                .fetchOne() != null
        }
    }

    fun delete(
        dslContext: DSLContext,
        templateId: String
    ): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(templateId))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        templateId: String,
        version: Long
    ): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(templateId))
                .and(VERSION.eq(version))
                .execute()
        }
    }

    fun getTemplate(
        dslContext: DSLContext,
        version: Long
    ): TTemplateRecord {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(VERSION.eq(version))
                .fetchOne() ?: throw com.tencent.devops.common.api.exception.OperationException("流水线模板不存在")
        }
    }

    fun listTemplate(
        dslContext: DSLContext,
        projectId: String,
        templateId: String
    ): Result<TTemplateRecord> {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(templateId))
                .and(PROJECT_ID.eq(projectId))
                .orderBy(VERSION.desc())
                .fetch()
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
            val conditions = kotlin.collections.mutableListOf<Condition>()
            if (projectId != null) {
                if (includePublicFlag != null && includePublicFlag) {
                    conditions.add(PROJECT_ID.eq(projectId).or(TYPE.eq(com.tencent.devops.process.pojo.template.TemplateType.PUBLIC.name)))
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
                .select(ID.countDistinct())
                .from(this)
                .where(conditions)
                .fetchOne(0, kotlin.Int::class.java)
        }
    }

    fun listTemplate(
        dslContext: DSLContext,
        projectId: String?,
        includePublicFlag: Boolean?,
        templateType: TemplateType?,
        templateIdList: List<String>?,
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
        if (templateType != null) {
            conditions.add(a.TYPE.eq(templateType.name))
        }
        if (templateIdList != null && templateIdList.isNotEmpty()) {
            conditions.add(a.ID.`in`(templateIdList))
        }
        if (storeFlag != null) {
            conditions.add(a.STORE_FLAG.eq(storeFlag))
        }
        val t = dslContext.select(a.ID.`as`("templateId"), a.VERSION.max().`as`("version")).from(a).groupBy(a.ID)

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
            a.CATEGORY.`as`("category")
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
            .where(conditions)
            .orderBy(a.WEIGHT.desc(), a.CREATED_TIME.desc())

        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    fun listTemplateByIds(
        dslContext: DSLContext,
        templateList: List<String>
    ): Result<TTemplateRecord> {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(ID.`in`(templateList))
                .orderBy(VERSION.desc())
                .fetch()
        }
    }

    /**
     * 批量获取模版的最新版本
     */
    fun listLatestTemplateByIds(
        dslContext: DSLContext,
        templateList: List<String>
    ): Result<out Record> {
        val a = TTemplate.T_TEMPLATE.`as`("a")
        val b = dslContext.select(
            a.ID,
            a.VERSION.max().`as`("VERSION")
        ).from(a).where(a.ID.`in`(templateList)).groupBy(a.ID).asTable("b")

        return dslContext.select(
            a.VERSION,
            a.ID,
            a.TEMPLATE_NAME,
            a.PROJECT_ID,
            a.VERSION_NAME,
            a.CREATOR,
            a.CREATED_TIME,
            a.TEMPLATE,
            a.TYPE,
            a.LOGO_URL
        ).from(a)
            .innerJoin(b)
            .on(a.ID.eq(b.field("ID", String::class.java)))
            .where(a.VERSION.eq(b.field("VERSION", Long::class.java)))
            .and(a.ID.`in`(templateList))
            .fetch()
    }

    fun getLatestTemplate(
        dslContext: DSLContext,
        projectId: String,
        templateId: String
    ): TTemplateRecord {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ID.eq(templateId))
                .orderBy(VERSION.desc())
                .limit(1)
                .fetchOne() ?: throw javax.ws.rs.NotFoundException("流水线模板不存在")
        }
    }

    fun getLatestTemplate(
        dslContext: DSLContext,
        templateId: String
    ): TTemplateRecord {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(templateId))
                .orderBy(VERSION.desc())
                .limit(1)
                .fetchOne() ?: throw javax.ws.rs.NotFoundException("流水线模板不存在")
        }
    }

    /**
     * 判断是否有关联的模版
     */
    fun isExistInstalledTemplate(dslContext: DSLContext, templateId: String): Boolean {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount()
                .from(this)
                .where(TYPE.eq(com.tencent.devops.process.pojo.template.TemplateType.CONSTRAINT.name))
                .and(SRC_TEMPLATE_ID.eq(templateId))
                .fetchOne(0, kotlin.Long::class.java) > 0
        }
    }

    /**
     * 获取关联的模版列表
     */
    fun listTemplateReference(
        dslContext: DSLContext,
        templateId: String
    ): Result<TTemplateRecord> {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(SRC_TEMPLATE_ID.eq(templateId))
                .fetch()
        }
    }
}