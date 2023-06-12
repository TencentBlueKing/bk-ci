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

package com.tencent.devops.process.engine.dao.template

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.model.process.tables.records.TTemplateRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_ID
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Suppress("ALL")
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
        storeFlag: Boolean,
        version: Long? = null
    ): Long {
        with(TTemplate.T_TEMPLATE) {
            val currentTime = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                ID,
                TEMPLATE_NAME,
                VERSION_NAME,
                CREATOR,
                CREATED_TIME,
                UPDATE_TIME,
                TEMPLATE,
                STORE_FLAG,
                VERSION
            )
                .values(
                    projectId,
                    templateId,
                    templateName,
                    versionName,
                    userId,
                    currentTime,
                    currentTime,
                    template,
                    storeFlag,
                    version
                )
                .returning(VERSION)
                .fetchOne()!!.version
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
        weight: Int,
        version: Long? = null
    ): Long {
        with(TTemplate.T_TEMPLATE) {
            val currentTime = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                ID,
                TEMPLATE_NAME,
                VERSION_NAME,
                CREATOR,
                CREATED_TIME,
                UPDATE_TIME,
                TEMPLATE,
                TYPE,
                CATEGORY,
                LOGO_URL,
                SRC_TEMPLATE_ID,
                STORE_FLAG,
                WEIGHT,
                VERSION
            )
                .values(
                    projectId,
                    templateId,
                    templateName,
                    versionName,
                    userId,
                    currentTime,
                    currentTime,
                    template,
                    type,
                    category,
                    logoUrl,
                    srcTemplateId,
                    storeFlag,
                    weight,
                    version
                )
                .returning(VERSION)
                .fetchOne()!!.version
        }
    }

    fun countTemplateVersionNum(
        dslContext: DSLContext,
        projectId: String,
        templateId: String
    ): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.select(DSL.countDistinct(VERSION_NAME))
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(ID.eq(templateId)))
                .fetchOne(0, Int::class.java)!!
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
        projectId: String,
        templateId: String
    ): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(templateId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun listSaveRecordVersions(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        versionName: String,
        saveNum: Int = 2
    ): Result<Record1<Long>>? {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.select(VERSION)
                .from(this)
                .where(ID.eq(templateId))
                .and(VERSION_NAME.eq(versionName))
                .and(PROJECT_ID.eq(projectId))
                .orderBy(UPDATE_TIME.desc(), VERSION.desc())
                .limit(saveNum)
                .fetch()
        }
    }

    fun deleteSpecVersion(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        versionName: String,
        saveVersions: List<Long>
    ): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(templateId))
                .and(VERSION_NAME.eq(versionName))
                .and(VERSION.notIn(saveVersions))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        versions: Set<Long>? = null,
        versionName: String? = null
    ): Int {
        with(TTemplate.T_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
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
        projectId: String? = null,
        version: Long
    ): TTemplateRecord {
        with(TTemplate.T_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(VERSION.eq(version))
            if (!projectId.isNullOrBlank()) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .limit(1)
                .fetchOne() ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
            )
        }
    }

    fun getTemplate(
        dslContext: DSLContext,
        templateId: String,
        versionName: String? = null,
        version: Long? = null
    ): TTemplateRecord {
        with(TTemplate.T_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ID.eq(templateId))
            if (version != null) {
                conditions.add(VERSION.eq(version))
            }
            if (!versionName.isNullOrBlank()) {
                conditions.add(VERSION_NAME.eq(versionName))
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATED_TIME.desc(), VERSION.desc())
                .limit(1)
                .fetchOne() ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
            )
        }
    }

    fun getSrcTemplateId(dslContext: DSLContext, projectId: String, templateId: String, type: String? = null): String? {
        return with(TTemplate.T_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(ID.eq(templateId))
            if (type != null) {
                conditions.add(TYPE.eq(type))
            }
            dslContext.select(SRC_TEMPLATE_ID).from(this)
                .where(conditions)
                .limit(1)
                .fetchOne(0, String::class.java)
        }
    }

    fun getTemplateVersionInfos(
        dslContext: DSLContext,
        projectId: String,
        templateId: String
    ): Result<out Record>? {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.select(
                ID,
                VERSION,
                VERSION_NAME,
                UPDATE_TIME,
                CREATOR
            )
                .from(this)
                .where(ID.eq(templateId))
                .and(PROJECT_ID.eq(projectId))
                .orderBy(VERSION_NAME.desc(), UPDATE_TIME.desc(), VERSION.desc())
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

    fun countTemplate(
        dslContext: DSLContext,
        projectId: String?,
        includePublicFlag: Boolean?,
        templateType: TemplateType?,
        templateName: String?,
        storeFlag: Boolean?
    ): Int {
        with(TTemplate.T_TEMPLATE) {
            val normalConditions = countTemplateBaseCondition(templateType, templateName, storeFlag)
            if (!projectId.isNullOrBlank()) {
                normalConditions.add(PROJECT_ID.eq(projectId))
            }
            var count = dslContext.select(DSL.countDistinct(ID))
                .from(this)
                .where(normalConditions)
                .fetchOne(0, Int::class.java)!!
            if (true == includePublicFlag) {
                val publicConditions = countTemplateBaseCondition(templateType, templateName, storeFlag)
                publicConditions.add((TYPE.eq(TemplateType.PUBLIC.name)))
                count += dslContext.select(DSL.countDistinct(ID))
                    .from(this)
                    .where(publicConditions)
                    .fetchOne(0, Int::class.java)!!
            }
            return count
        }
    }

    private fun TTemplate.countTemplateBaseCondition(
        templateType: TemplateType?,
        templateName: String?,
        storeFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (templateType != null) {
            conditions.add(TYPE.eq(templateType.name))
        }
        if (templateName != null) {
            conditions.add(TEMPLATE_NAME.eq(templateName))
        }
        if (storeFlag != null) {
            conditions.add(STORE_FLAG.eq(storeFlag))
        }
        return conditions
    }

    fun listTemplate(
        dslContext: DSLContext,
        projectId: String?,
        includePublicFlag: Boolean?,
        templateType: TemplateType?,
        templateIdList: Collection<String>?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?,
        queryModelFlag: Boolean = true
    ): Result<out Record>? {
        val tTemplate = TTemplate.T_TEMPLATE

        val conditions = mutableListOf<Condition>()
        if (projectId != null) {
            if (includePublicFlag != null && includePublicFlag) {
                conditions.add(
                    tTemplate.PROJECT_ID.eq(projectId).or(tTemplate.PROJECT_ID.eq("").and(tTemplate.TYPE.eq(TemplateType.PUBLIC.name)))
                )
            } else {
                conditions.add(tTemplate.PROJECT_ID.eq(projectId))
            }
        }

        return listTemplateByProjectCondition(
            dslContext = dslContext,
            templateType = templateType,
            templateIdList = templateIdList,
            storeFlag = storeFlag,
            page = page,
            pageSize = pageSize,
            tTemplate = tTemplate,
            conditions = conditions,
            queryModelFlag = queryModelFlag
        )
    }

    fun listTemplateByProjectCondition(
        dslContext: DSLContext,
        templateType: TemplateType?,
        templateIdList: Collection<String>?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?,
        tTemplate: TTemplate,
        conditions: MutableList<Condition>,
        queryModelFlag: Boolean = true
    ): Result<out Record>? {
        if (templateType != null) {
            conditions.add(tTemplate.TYPE.eq(templateType.name))
        }
        if (templateIdList != null && templateIdList.isNotEmpty()) {
            conditions.add(tTemplate.ID.`in`(templateIdList))
        }
        if (storeFlag != null) {
            conditions.add(tTemplate.STORE_FLAG.eq(storeFlag))
        }
        val t = dslContext.select(tTemplate.ID.`as`(KEY_ID), DSL.max(tTemplate.CREATED_TIME).`as`(KEY_CREATE_TIME))
            .from(tTemplate)
            .where(conditions)
            .groupBy(tTemplate.ID)

        val baseSelect = dslContext.select(
            tTemplate.ID,
            tTemplate.TEMPLATE_NAME,
            tTemplate.VERSION,
            tTemplate.VERSION_NAME,
            tTemplate.TYPE,
            tTemplate.LOGO_URL,
            tTemplate.STORE_FLAG,
            tTemplate.CREATOR,
            tTemplate.CREATED_TIME,
            tTemplate.UPDATE_TIME,
            tTemplate.SRC_TEMPLATE_ID,
            tTemplate.CATEGORY,
            tTemplate.PROJECT_ID
        )
        if (queryModelFlag) {
            // 查询模板model内容
            baseSelect.select(tTemplate.TEMPLATE)
        }

        val baseStep = baseSelect.from(tTemplate)
            .join(t)
            .on(
                tTemplate.ID.eq(t.field(KEY_ID, String::class.java)).and(
                    tTemplate.CREATED_TIME.eq(
                        t.field(
                            KEY_CREATE_TIME,
                            LocalDateTime::class.java
                        )
                    )
                )
            )
            .where(conditions)
            .orderBy(tTemplate.WEIGHT.desc(), tTemplate.CREATED_TIME.desc(), tTemplate.VERSION.desc())

        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
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
                .orderBy(CREATED_TIME.desc(), VERSION.desc())
                .limit(1)
                .fetchOne() ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
            )
        }
    }

    fun getLatestTemplate(
        dslContext: DSLContext,
        templateId: String
    ): TTemplateRecord {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(templateId))
                .orderBy(CREATED_TIME.desc(), VERSION.desc())
                .limit(1)
                .fetchOne() ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
            )
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
                .fetchOne(0, Long::class.java)!! > 0
        }
    }

    /**
     * 获取关联的模版ID列表
     */
    fun listTemplateReferenceId(
        dslContext: DSLContext,
        templateId: String
    ): Result<Record1<String>> {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectDistinct(ID).from(this)
                .where(TYPE.eq(TemplateType.CONSTRAINT.name))
                .and(SRC_TEMPLATE_ID.eq(templateId))
                .fetch()
        }
    }

    fun listTemplateReferenceByProjects(
        dslContext: DSLContext,
        templateId: String,
        projectIds: List<String>
    ): Result<TTemplateRecord> {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(TYPE.eq(TemplateType.CONSTRAINT.name))
                .and(SRC_TEMPLATE_ID.eq(templateId))
                .and(PROJECT_ID.`in`(projectIds))
                .fetch()
        }
    }
}
