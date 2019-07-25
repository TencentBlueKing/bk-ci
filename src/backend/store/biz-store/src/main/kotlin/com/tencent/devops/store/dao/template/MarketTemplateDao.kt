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

package com.tencent.devops.store.dao.template

import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.model.store.tables.TTemplateCategoryRel
import com.tencent.devops.model.store.tables.TTemplateLabelRel
import com.tencent.devops.model.store.tables.records.TTemplateRecord
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.enums.TemplateStatusEnum
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class MarketTemplateDao {

    /**
     * 模版市场搜索结果总数
     */
    fun count(
        dslContext: DSLContext,
        templateName: String?,
        classifyCode: String?,
        categoryList: List<String>?,
        labelCodeList: List<String>?,
        score: Int?,
        rdType: TemplateRdTypeEnum?
    ): Int {
        val (tt, conditions) = formatConditions(templateName, rdType, classifyCode, dslContext)

        val baseStep = dslContext.select(tt.ID.countDistinct()).from(tt)

        // 根据应用范畴和功能标签筛选
        if (categoryList != null && categoryList.isNotEmpty()) {
            val b = TCategory.T_CATEGORY.`as`("b")
            val categoryIdList = dslContext.select(b.ID)
                .from(b)
                .where(b.CATEGORY_CODE.`in`(categoryList)).and(b.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
                .fetch().map { it["ID"] as String }
            val ttcr = TTemplateCategoryRel.T_TEMPLATE_CATEGORY_REL.`as`("ttcr")
            baseStep.leftJoin(ttcr).on(tt.ID.eq(ttcr.TEMPLATE_ID))
            conditions.add(ttcr.CATEGORY_ID.`in`(categoryIdList))
        }
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val c = TLabel.T_LABEL.`as`("c")
            val labelIdList = dslContext.select(c.ID)
                .from(c)
                .where(c.LABEL_CODE.`in`(labelCodeList)).and(c.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
                .fetch().map { it["ID"] as String }
            val ttlr = TTemplateLabelRel.T_TEMPLATE_LABEL_REL.`as`("ttlr")
            baseStep.leftJoin(ttlr).on(tt.ID.eq(ttlr.TEMPLATE_ID))
            conditions.add(ttlr.LABEL_ID.`in`(labelIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.DOWNLOADS.`as`(MarketTemplateSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(tt.TEMPLATE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java).ge(BigDecimal.valueOf(score.toLong())))
        }

        return baseStep.where(conditions).fetchOne(0, Int::class.java)
    }

    private fun formatConditions(
        templateName: String?,
        rdType: TemplateRdTypeEnum?,
        classifyCode: String?,
        dslContext: DSLContext
    ): Pair<TTemplate, MutableList<Condition>> {
        val tt = TTemplate.T_TEMPLATE.`as`("tt")

        val conditions = mutableListOf<Condition>()
        conditions.add(tt.TEMPLATE_STATUS.eq(TemplateStatusEnum.RELEASED.status.toByte())) // 已发布的
        conditions.add(tt.LATEST_FLAG.eq(true)) // 最新版本
        if (!templateName.isNullOrEmpty()) {
            conditions.add(tt.TEMPLATE_NAME.contains(templateName))
        }
        if (rdType != null) {
            conditions.add(tt.TEMPLATE_RD_TYPE.eq(rdType.type.toByte()))
        }
        if (!classifyCode.isNullOrEmpty()) {
            val a = TClassify.T_CLASSIFY.`as`("a")
            val classifyId = dslContext.select(a.ID)
                .from(a)
                .where(a.CLASSIFY_CODE.eq(classifyCode).and(a.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte())))
                .fetchOne(0, String::class.java)
            conditions.add(tt.CLASSIFY_ID.eq(classifyId))
        }
        return Pair(tt, conditions)
    }

    /**
     * 模版市场搜索结果列表
     */
    fun list(
        dslContext: DSLContext,
        templateName: String?,
        classifyCode: String?,
        categoryList: List<String>?,
        labelCodeList: List<String>?,
        score: Int?,
        rdType: TemplateRdTypeEnum?,
        sortType: MarketTemplateSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val (tt, conditions) = formatConditions(templateName, rdType, classifyCode, dslContext)

        val baseStep = dslContext.select(
            tt.ID,
            tt.TEMPLATE_NAME,
            tt.TEMPLATE_CODE,
            tt.TEMPLATE_RD_TYPE,
            tt.CLASSIFY_ID,
            tt.LOGO_URL,
            tt.PUBLISHER,
            tt.SUMMARY,
            tt.PUBLIC_FLAG
        ).from(tt)

        // 根据应用范畴和功能标签筛选
        if (categoryList != null && categoryList.isNotEmpty()) {
            val b = TCategory.T_CATEGORY.`as`("b")
            val categoryIdList = dslContext.select(b.ID)
                .from(b)
                .where(b.CATEGORY_CODE.`in`(categoryList)).and(b.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
                .fetch().map { it["ID"] as String }
            val ttcr = TTemplateCategoryRel.T_TEMPLATE_CATEGORY_REL.`as`("ttcr")
            baseStep.leftJoin(ttcr).on(tt.ID.eq(ttcr.TEMPLATE_ID))
            conditions.add(ttcr.CATEGORY_ID.`in`(categoryIdList))
        }
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val c = TLabel.T_LABEL.`as`("c")
            val labelIdList = dslContext.select(c.ID)
                .from(c)
                .where(c.LABEL_CODE.`in`(labelCodeList)).and(c.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
                .fetch().map { it["ID"] as String }
            val ttlr = TTemplateLabelRel.T_TEMPLATE_LABEL_REL.`as`("ttlr")
            baseStep.leftJoin(ttlr).on(tt.ID.eq(ttlr.TEMPLATE_ID))
            conditions.add(ttlr.LABEL_ID.`in`(labelIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.DOWNLOADS.`as`(MarketTemplateSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(tt.TEMPLATE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java).ge(BigDecimal.valueOf(score.toLong())))
        }

        if (null != sortType) {
            if (sortType == MarketTemplateSortTypeEnum.DOWNLOAD_COUNT && score == null) {
                val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
                val t = dslContext.select(
                    tas.STORE_CODE,
                    tas.DOWNLOADS.`as`(MarketTemplateSortTypeEnum.DOWNLOAD_COUNT.name)
                ).from(tas).asTable("t")
                baseStep.leftJoin(t).on(tt.TEMPLATE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            }

            val realSortType = if (sortType == MarketTemplateSortTypeEnum.DOWNLOAD_COUNT) {
                DSL.field(MarketTemplateSortTypeEnum.getSortType(sortType.name))
            } else {
                tt.field(MarketTemplateSortTypeEnum.getSortType(sortType.name))
            }

            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(realSortType.desc())
            } else {
                baseStep.where(conditions).orderBy(realSortType.asc())
            }
        } else {
            baseStep.where(conditions)
        }
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    fun addMarketTemplate(
        dslContext: DSLContext,
        userId: String,
        templateId: String,
        templateCode: String,
        marketTemplateRelRequest: MarketTemplateRelRequest
    ) {
        with(TTemplate.T_TEMPLATE) {
            dslContext.insertInto(
                this,
                ID,
                TEMPLATE_NAME,
                TEMPLATE_CODE,
                CLASSIFY_ID,
                VERSION,
                TEMPLATE_TYPE,
                LATEST_FLAG,
                TEMPLATE_STATUS,
                PUBLISHER,
                CREATOR,
                MODIFIER
            )
                .values(
                    templateId,
                    marketTemplateRelRequest.templateName,
                    templateCode,
                    "",
                    "",
                    1,
                    true,
                    TemplateStatusEnum.INIT.status.toByte(),
                    "",
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun updateMarketTemplate(
        dslContext: DSLContext,
        userId: String,
        templateId: String,
        version: String,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ) {
        val a = TClassify.T_CLASSIFY.`as`("a")
        val classifyId = dslContext.select(a.ID).from(a)
            .where(a.CLASSIFY_CODE.eq(marketTemplateUpdateRequest.classifyCode).and(a.TYPE.eq(1)))
            .fetchOne(0, String::class.java)
        with(TTemplate.T_TEMPLATE) {
            dslContext.update(this)
                .set(TEMPLATE_NAME, marketTemplateUpdateRequest.templateName)
                .set(CLASSIFY_ID, classifyId)
                .set(LOGO_URL, marketTemplateUpdateRequest.logoUrl)
                .set(TEMPLATE_STATUS, TemplateStatusEnum.RELEASED.status.toByte())
                .set(SUMMARY, marketTemplateUpdateRequest.summary)
                .set(DESCRIPTION, marketTemplateUpdateRequest.description)
                .set(PUBLISHER, marketTemplateUpdateRequest.publisher)
                .set(PUB_DESCRIPTION, marketTemplateUpdateRequest.pubDescription)
                .set(VERSION, version)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(templateId))
                .execute()
        }
    }

    fun upgradeMarketTemplate(
        dslContext: DSLContext,
        userId: String,
        templateId: String,
        version: String,
        templateRecord: TTemplateRecord,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ) {
        val a = TClassify.T_CLASSIFY.`as`("a")
        val classifyId = dslContext.select(a.ID).from(a)
            .where(a.CLASSIFY_CODE.eq(marketTemplateUpdateRequest.classifyCode).and(a.TYPE.eq(1)))
            .fetchOne(0, String::class.java)
        with(TTemplate.T_TEMPLATE) {
            dslContext.insertInto(
                this,
                ID,
                TEMPLATE_NAME,
                TEMPLATE_CODE,
                CLASSIFY_ID,
                VERSION,
                TEMPLATE_STATUS,
                LOGO_URL,
                SUMMARY,
                DESCRIPTION,
                PUBLISHER,
                PUB_DESCRIPTION,
                PUBLIC_FLAG,
                LATEST_FLAG,
                CREATOR,
                MODIFIER
            )
                .values(
                    templateId,
                    marketTemplateUpdateRequest.templateName,
                    templateRecord.templateCode,
                    classifyId,
                    version,
                    TemplateStatusEnum.RELEASED.status.toByte(),
                    marketTemplateUpdateRequest.logoUrl,
                    marketTemplateUpdateRequest.summary,
                    marketTemplateUpdateRequest.description,
                    marketTemplateUpdateRequest.publisher,
                    marketTemplateUpdateRequest.pubDescription,
                    templateRecord.publicFlag,
                    true,
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, templateCode: String): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.deleteFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .execute()
        }
    }

    fun countByName(dslContext: DSLContext, templateName: String): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount().from(this).where(TEMPLATE_NAME.eq(templateName))
                .fetchOne(0, Int::class.java)
        }
    }

    fun countByCode(dslContext: DSLContext, templateCode: String): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount().from(this).where(TEMPLATE_CODE.eq(templateCode))
                .fetchOne(0, Int::class.java)
        }
    }

    fun countByIdAndCode(dslContext: DSLContext, templateId: String, templateCode: String): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount().from(this).where(ID.eq(templateId).and(TEMPLATE_CODE.eq(templateCode)))
                .fetchOne(0, Int::class.java)
        }
    }

    fun countReleaseTemplateByCode(dslContext: DSLContext, templateCode: String): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount().from(this)
                .where(TEMPLATE_CODE.eq(templateCode).and(TEMPLATE_STATUS.eq(TemplateStatusEnum.RELEASED.status.toByte())))
                .fetchOne(0, Int::class.java)
        }
    }

    fun getLatestTemplateByCode(dslContext: DSLContext, templateCode: String): TTemplateRecord? {
        return with(TTemplate.T_TEMPLATE) {
            dslContext.selectFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .and(LATEST_FLAG.eq(true))
                .fetchOne()
        }
    }

    fun getUpToDateTemplateByCode(dslContext: DSLContext, templateCode: String): TTemplateRecord? {
        return with(TTemplate.T_TEMPLATE) {
            dslContext.selectFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getTemplatesByTemplateCode(dslContext: DSLContext, templateCode: String): Result<TTemplateRecord>? {
        return with(TTemplate.T_TEMPLATE) {
            dslContext.selectFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun getTemplate(dslContext: DSLContext, templateId: String): TTemplateRecord? {
        return with(TTemplate.T_TEMPLATE) {
            dslContext.selectFrom(this)
                .where(ID.eq(templateId))
                .fetchOne()
        }
    }

    fun getTemplate(dslContext: DSLContext, templateCode: String, version: String): TTemplateRecord? {
        return with(TTemplate.T_TEMPLATE) {
            dslContext.selectFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode).and(VERSION.eq(version)))
                .fetchOne()
        }
    }

    /**
     * 清空LATEST_FLAG
     */
    fun cleanLatestFlag(dslContext: DSLContext, templateCode: String) {
        with(TTemplate.T_TEMPLATE) {
            dslContext.update(this)
                .set(LATEST_FLAG, false)
                .where(TEMPLATE_CODE.eq(templateCode))
                .execute()
        }
    }

    fun getMyTemplates(
        dslContext: DSLContext,
        userId: String,
        templateName: String?,
        page: Int,
        pageSize: Int
    ): Result<out Record>? {
        val a = TTemplate.T_TEMPLATE.`as`("a")
        val b = TStoreMember.T_STORE_MEMBER.`as`("b")
        val c = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("c")
        val t = dslContext.select(a.TEMPLATE_CODE.`as`("templateCode"), a.CREATE_TIME.max().`as`("createTime")).from(a)
            .groupBy(a.TEMPLATE_CODE) // 查找每组templateCode最新的记录
        val conditions = generateGetMyTemplatesConditions(a, userId, b, c, templateName)
        return dslContext.select(
            a.ID.`as`("templateId"),
            a.TEMPLATE_CODE.`as`("templateCode"),
            a.TEMPLATE_NAME.`as`("templateName"),
            a.LOGO_URL.`as`("logoUrl"),
            a.VERSION.`as`("version"),
            a.TEMPLATE_STATUS.`as`("templateStatus"),
            a.CREATOR.`as`("creator"),
            a.CREATE_TIME.`as`("createTime"),
            a.MODIFIER.`as`("modifier"),
            a.UPDATE_TIME.`as`("updateTime"),
            c.PROJECT_CODE.`as`("projectCode")
        )
            .from(a)
            .join(t)
            .on(
                a.TEMPLATE_CODE.eq(
                    t.field(
                        "templateCode",
                        String::class.java
                    )
                ).and(a.CREATE_TIME.eq(t.field("createTime", LocalDateTime::class.java)))
            )
            .leftJoin(b)
            .on(a.TEMPLATE_CODE.eq(b.STORE_CODE))
            .join(c)
            .on(a.TEMPLATE_CODE.eq(c.STORE_CODE))
            .where(conditions)
            .groupBy(a.TEMPLATE_CODE)
            .orderBy(a.UPDATE_TIME.desc())
            .limit((page - 1) * pageSize, pageSize)
            .fetch()
    }

    fun getMyTemplatesCount(
        dslContext: DSLContext,
        userId: String,
        templateName: String?
    ): Long {
        val a = TTemplate.T_TEMPLATE.`as`("a")
        val b = TStoreMember.T_STORE_MEMBER.`as`("b")
        val c = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("c")
        val conditions = generateGetMyTemplatesConditions(a, userId, b, c, templateName)
        return dslContext.select(
            a.TEMPLATE_CODE.countDistinct()
        )
            .from(a)
            .leftJoin(b)
            .on(a.TEMPLATE_CODE.eq(b.STORE_CODE))
            .join(c)
            .on(a.TEMPLATE_CODE.eq(c.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Long::class.java)
    }

    private fun generateGetMyTemplatesConditions(
        a: TTemplate,
        userId: String,
        b: TStoreMember,
        c: TStoreProjectRel,
        templateName: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.CREATOR.eq(userId).or(b.USERNAME.eq(userId)))
        conditions.add(c.TYPE.eq(0))
        conditions.add(c.STORE_TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
        if (null != templateName) {
            conditions.add(a.TEMPLATE_NAME.contains(templateName))
        }
        return conditions
    }

    fun updateTemplateStatusById(
        dslContext: DSLContext,
        templateId: String,
        templateStatus: Byte,
        userId: String,
        msg: String?
    ) {
        with(TTemplate.T_TEMPLATE) {
            val baseStep = dslContext.update(this)
                .set(TEMPLATE_STATUS, templateStatus)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(TEMPLATE_STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(templateId))
                .execute()
        }
    }

    fun updateTemplateStatusByCode(
        dslContext: DSLContext,
        templateCode: String,
        templateOldStatus: Byte,
        templateNewStatus: Byte,
        userId: String,
        msg: String?
    ) {
        with(TTemplate.T_TEMPLATE) {
            val baseStep = dslContext.update(this)
                .set(TEMPLATE_STATUS, templateNewStatus)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(TEMPLATE_STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(TEMPLATE_CODE.eq(templateCode))
                .and(TEMPLATE_STATUS.eq(templateOldStatus))
                .execute()
        }
    }
}