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

package com.tencent.devops.process.engine.dao.template

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.model.process.tables.records.TTemplateRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.template.TemplateType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
class TemplateDao {

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
                    LocalDateTime.now(),
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
                    LocalDateTime.now(),
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

    fun deleteVersion(
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

    /**
     * 同一个版本名字可能对应多个版本
     */
    fun getTemplatesWithSameVersionName(
        dslContext: DSLContext,
        templateId: String,
        version: Long
    ): Result<TTemplateRecord> {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(
                    VERSION_NAME.eq(
                        dslContext.select(VERSION_NAME)
                            .from(this)
                            .where(ID.eq(templateId))
                            .and(VERSION.eq(version))
                    )
                )
                .and(ID.eq(templateId))
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        templateId: String,
        versions: Set<Long>? = null,
        versionName: String? = null
    ): Int {
        with(TTemplate.T_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ID.eq(templateId))
            if (null != versions) {
                conditions.add(VERSION.`in`(versions))
            }
            if (null != versionName) {
                conditions.add(VERSION_NAME.eq(versionName))
            }
            return dslContext.deleteFrom(this)
                .where(conditions)
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
                .fetchOne() ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS,
                defaultMessage = "模板不存在"
            )
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

    fun getSrcTemplateCodes(dslContext: DSLContext, projectId: String): List<String> {
        return with(TTemplate.T_TEMPLATE) {
            dslContext.select(SRC_TEMPLATE_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(STORE_FLAG.eq(true))
                .fetch(SRC_TEMPLATE_ID, String::class.java)
        }
    }

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
                .select(ID.countDistinct())
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)
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
                .select(ID.countDistinct())
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)
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
        val t = dslContext.select(a.ID.`as`("templateId"), a.VERSION.max().`as`("version")).from(a).where(conditions).groupBy(a.ID)

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
                .fetchOne() ?: throw NotFoundException("流水线模板不存在")
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
                .fetchOne() ?: throw NotFoundException("流水线模板不存在")
        }
    }

    /**
     * 判断是否有关联的模版
     */
    fun isExistInstalledTemplate(dslContext: DSLContext, templateId: String): Boolean {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount()
                .from(this)
                .where(TYPE.eq(TemplateType.CONSTRAINT.name))
                .and(SRC_TEMPLATE_ID.eq(templateId))
                .fetchOne(0, Long::class.java) > 0
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
                .where(TYPE.eq(TemplateType.CONSTRAINT.name))
                .and(SRC_TEMPLATE_ID.eq(templateId))
                .fetch()
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