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
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_PROJECT_CODE
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SelectJoinStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class MarketTemplateDao {

    /**
     * 模版市场搜索结果 总数
     */
    fun count(
        dslContext: DSLContext,
        keyword: String?,
        classifyCode: String?,
        categoryList: List<String>?,
        labelCodeList: List<String>?,
        score: Int?,
        rdType: TemplateRdTypeEnum?
    ): Int {
        val (tt, conditions) = formatConditions(
            keyword = keyword,
            rdType = rdType,
            classifyCode = classifyCode,
            dslContext = dslContext
        )

        val baseStep = dslContext.select(DSL.countDistinct(tt.ID)).from(tt)
        handleTemplateQueryCondition(
            categoryList = categoryList,
            dslContext = dslContext,
            baseStep = baseStep,
            tTemplate = tt,
            conditions = conditions,
            labelCodeList = labelCodeList,
            score = score
        )

        return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
    }

    private fun handleTemplateQueryCondition(
        categoryList: List<String>?,
        dslContext: DSLContext,
        baseStep: SelectJoinStep<out Record>,
        tTemplate: TTemplate,
        conditions: MutableList<Condition>,
        labelCodeList: List<String>?,
        score: Int?
    ) {
        val storeType = StoreTypeEnum.TEMPLATE.type.toByte()
        // 根据应用范畴和功能标签筛选
        if (!categoryList.isNullOrEmpty()) {
            val tCategory = TCategory.T_CATEGORY
            val categoryIdList = dslContext.select(tCategory.ID)
                .from(tCategory)
                .where(tCategory.CATEGORY_CODE.`in`(categoryList)).and(tCategory.TYPE.eq(storeType))
                .fetch().map { it[tCategory.ID] as String }
            val tTemplateCategoryRel = TTemplateCategoryRel.T_TEMPLATE_CATEGORY_REL
            baseStep.leftJoin(tTemplateCategoryRel).on(tTemplate.ID.eq(tTemplateCategoryRel.TEMPLATE_ID))
            conditions.add(tTemplateCategoryRel.CATEGORY_ID.`in`(categoryIdList))
        }
        if (!labelCodeList.isNullOrEmpty()) {
            val tLabel = TLabel.T_LABEL
            val labelIdList = dslContext.select(tLabel.ID)
                .from(tLabel)
                .where(tLabel.LABEL_CODE.`in`(labelCodeList)).and(tLabel.TYPE.eq(storeType))
                .fetch().map { it[tLabel.ID] as String }
            val tTemplateLabelRel = TTemplateLabelRel.T_TEMPLATE_LABEL_REL
            baseStep.leftJoin(tTemplateLabelRel).on(tTemplate.ID.eq(tTemplateLabelRel.TEMPLATE_ID))
            conditions.add(tTemplateLabelRel.LABEL_ID.`in`(labelIdList))
        }
        if (score != null) {
            val tStoreStatisticsTotal = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
            val t = dslContext.select(
                tStoreStatisticsTotal.STORE_CODE,
                tStoreStatisticsTotal.STORE_TYPE,
                tStoreStatisticsTotal.DOWNLOADS.`as`(MarketTemplateSortTypeEnum.DOWNLOAD_COUNT.name),
                tStoreStatisticsTotal.SCORE_AVERAGE
            ).from(tStoreStatisticsTotal)
            baseStep.leftJoin(t)
                .on(tTemplate.TEMPLATE_CODE.eq(t.field(tStoreStatisticsTotal.STORE_CODE.name, String::class.java)))
            conditions.add(
                t.field(tStoreStatisticsTotal.SCORE_AVERAGE.name, BigDecimal::class.java)!!
                    .ge(BigDecimal.valueOf(score.toLong()))
            )
            conditions.add(t.field(tStoreStatisticsTotal.STORE_TYPE.name, Byte::class.java)!!.eq(storeType))
        }
    }

    private fun formatConditions(
        keyword: String?,
        rdType: TemplateRdTypeEnum?,
        classifyCode: String?,
        dslContext: DSLContext
    ): Pair<TTemplate, MutableList<Condition>> {
        val tTemplate = TTemplate.T_TEMPLATE
        val conditions = mutableListOf<Condition>()
        conditions.add(tTemplate.TEMPLATE_STATUS.eq(TemplateStatusEnum.RELEASED.status.toByte())) // 已发布的
        conditions.add(tTemplate.LATEST_FLAG.eq(true)) // 最新版本
        if (!keyword.isNullOrEmpty()) {
            conditions.add(tTemplate.TEMPLATE_NAME.contains(keyword).or(tTemplate.SUMMARY.contains(keyword)))
        }
        if (rdType != null) {
            conditions.add(tTemplate.TEMPLATE_RD_TYPE.eq(rdType.type.toByte()))
        }
        if (!classifyCode.isNullOrEmpty()) {
            val tClassify = TClassify.T_CLASSIFY
            val classifyId = dslContext.select(tClassify.ID)
                .from(tClassify)
                .where(
                    tClassify.CLASSIFY_CODE.eq(classifyCode)
                        .and(tClassify.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
                )
                .fetchOne(0, String::class.java)
            conditions.add(tTemplate.CLASSIFY_ID.eq(classifyId))
        }
        return Pair(tTemplate, conditions)
    }

    /**
     * 模版市场搜索结果列表
     */
    fun list(
        dslContext: DSLContext,
        keyword: String?,
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
        val (tt, conditions) = formatConditions(
            keyword = keyword,
            rdType = rdType,
            classifyCode = classifyCode,
            dslContext = dslContext
        )

        val baseStep = dslContext.select(
            tt.ID,
            tt.TEMPLATE_NAME,
            tt.TEMPLATE_CODE,
            tt.VERSION,
            tt.TEMPLATE_RD_TYPE,
            tt.CLASSIFY_ID,
            tt.LOGO_URL,
            tt.PUBLISHER,
            tt.SUMMARY,
            tt.PUBLIC_FLAG,
            tt.MODIFIER,
            tt.UPDATE_TIME
        ).from(tt)

        handleTemplateQueryCondition(
            categoryList = categoryList,
            dslContext = dslContext,
            baseStep = baseStep,
            tTemplate = tt,
            conditions = conditions,
            labelCodeList = labelCodeList,
            score = score
        )

        if (null != sortType) {
            if (sortType == MarketTemplateSortTypeEnum.DOWNLOAD_COUNT && score == null) {
                val tStoreStatisticsTotal = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
                val t = dslContext.select(
                    tStoreStatisticsTotal.STORE_CODE,
                    tStoreStatisticsTotal.DOWNLOADS.`as`(MarketTemplateSortTypeEnum.DOWNLOAD_COUNT.name)
                ).from(tStoreStatisticsTotal)
                baseStep.leftJoin(t)
                    .on(tt.TEMPLATE_CODE.eq(t.field(tStoreStatisticsTotal.STORE_CODE.name, String::class.java)))
            }

            val realSortType = if (sortType == MarketTemplateSortTypeEnum.DOWNLOAD_COUNT) {
                DSL.field(MarketTemplateSortTypeEnum.getSortType(sortType.name))
            } else {
                tt.field(MarketTemplateSortTypeEnum.getSortType(sortType.name))
            }

            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(realSortType!!.desc())
            } else {
                baseStep.where(conditions).orderBy(realSortType!!.asc())
            }
        } else {
            baseStep.where(conditions)
                .orderBy(tt.field(MarketTemplateSortTypeEnum.getSortType(MarketTemplateSortTypeEnum.NAME.name)))
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
            dslContext.insertInto(this,
                ID,
                TEMPLATE_NAME,
                TEMPLATE_CODE,
                CLASSIFY_ID,
                VERSION,
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
        val tClassify = TClassify.T_CLASSIFY
        val classifyId = dslContext.select(tClassify.ID).from(tClassify)
            .where(tClassify.CLASSIFY_CODE.eq(marketTemplateUpdateRequest.classifyCode)
                .and(tClassify.TYPE.eq(1)))
            .fetchOne(0, String::class.java)
        with(TTemplate.T_TEMPLATE) {
            dslContext.update(this)
                .set(TEMPLATE_NAME, marketTemplateUpdateRequest.templateName)
                .set(CLASSIFY_ID, classifyId)
                .set(LOGO_URL, marketTemplateUpdateRequest.logoUrl)
                .set(TEMPLATE_STATUS, TemplateStatusEnum.AUDITING.status.toByte())
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
        templateStatus: Byte,
        version: String,
        templateRecord: TTemplateRecord,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ) {
        val tClassify = TClassify.T_CLASSIFY
        val classifyId = dslContext.select(tClassify.ID)
            .from(tClassify)
            .where(tClassify.CLASSIFY_CODE.eq(marketTemplateUpdateRequest.classifyCode)
                .and(tClassify.TYPE.eq(1)))
            .fetchOne(0, String::class.java)
        with(TTemplate.T_TEMPLATE) {
            dslContext.insertInto(this,
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
                    templateStatus,
                    marketTemplateUpdateRequest.logoUrl,
                    marketTemplateUpdateRequest.summary,
                    marketTemplateUpdateRequest.description,
                    marketTemplateUpdateRequest.publisher,
                    marketTemplateUpdateRequest.pubDescription,
                    templateRecord.publicFlag,
                    false,
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

    fun countByName(dslContext: DSLContext, templateName: String, templateCode: String? = null): Int {
        with(TTemplate.T_TEMPLATE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(TEMPLATE_NAME.eq(templateName))
            if (templateCode != null) {
                conditions.add(TEMPLATE_CODE.eq(templateCode))
            }
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByCode(dslContext: DSLContext, templateCode: String): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount().from(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByIdAndCode(dslContext: DSLContext, templateId: String, templateCode: String): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount().from(this)
                .where(ID.eq(templateId)
                    .and(TEMPLATE_CODE.eq(templateCode)))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countReleaseTemplateByCode(dslContext: DSLContext, templateCode: String): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount().from(this)
                .where(TEMPLATE_CODE.eq(templateCode)
                    .and(TEMPLATE_STATUS.eq(TemplateStatusEnum.RELEASED.status.toByte())))
                .fetchOne(0, Int::class.java)!!
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

    fun getReleaseTemplatesByCode(dslContext: DSLContext, templateCode: String): Result<TTemplateRecord>? {
        return with(TTemplate.T_TEMPLATE) {
            dslContext.selectFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .and(TEMPLATE_STATUS.eq(TemplateStatusEnum.RELEASED.status.toByte()))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun getNewestUndercarriagedTemplatesByCode(dslContext: DSLContext, templateCode: String): TTemplateRecord? {
        return with(TTemplate.T_TEMPLATE) {
            dslContext.selectFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .and(TEMPLATE_STATUS.eq(TemplateStatusEnum.UNDERCARRIAGED.status.toByte()))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
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

    /**
     * 更新状态等信息
     */
    fun updateTemplateStatusInfo(
        dslContext: DSLContext,
        userId: String,
        templateId: String,
        templateStatus: Byte,
        templateStatusMsg: String,
        latestFlag: Boolean,
        pubTime: LocalDateTime? = null
    ) {
        with(TTemplate.T_TEMPLATE) {
            dslContext.update(this)
                .set(TEMPLATE_STATUS, templateStatus)
                .set(TEMPLATE_STATUS_MSG, templateStatusMsg)
                .set(LATEST_FLAG, latestFlag)
                .set(PUB_TIME, pubTime)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(templateId))
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
        val tTemplate = TTemplate.T_TEMPLATE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val t = dslContext.select(
            tTemplate.TEMPLATE_CODE.`as`(tTemplate.TEMPLATE_CODE.name),
            DSL.max(tTemplate.CREATE_TIME).`as`(KEY_CREATE_TIME)
        )
            .from(tTemplate)
            .groupBy(tTemplate.TEMPLATE_CODE) // 查找每组templateCode最新的记录
        val conditions = generateGetMyTemplatesConditions(tTemplate, userId, tStoreMember, tStoreProjectRel, templateName)
        return dslContext.select(
            tTemplate.ID,
            tTemplate.TEMPLATE_CODE,
            tTemplate.TEMPLATE_NAME,
            tTemplate.LOGO_URL,
            tTemplate.VERSION,
            tTemplate.TEMPLATE_STATUS,
            tTemplate.CREATOR,
            tTemplate.CREATE_TIME,
            tTemplate.MODIFIER,
            tTemplate.UPDATE_TIME,
            tStoreProjectRel.PROJECT_CODE.`as`(KEY_PROJECT_CODE)
        )
            .from(tTemplate)
            .join(t)
            .on(tTemplate.TEMPLATE_CODE.eq(t.field(tTemplate.TEMPLATE_CODE.name, String::class.java))
                .and(tTemplate.CREATE_TIME.eq(t.field(KEY_CREATE_TIME, LocalDateTime::class.java))))
            .leftJoin(tStoreMember)
            .on(tTemplate.TEMPLATE_CODE.eq(tStoreMember.STORE_CODE))
            .join(tStoreProjectRel)
            .on(tTemplate.TEMPLATE_CODE.eq(tStoreProjectRel.STORE_CODE))
            .where(conditions)
            .groupBy(tTemplate.TEMPLATE_CODE)
            .orderBy(tTemplate.UPDATE_TIME.desc())
            .limit((page - 1) * pageSize, pageSize)
            .fetch()
    }

    fun getMyTemplatesCount(
        dslContext: DSLContext,
        userId: String,
        templateName: String?
    ): Long {
        val tTemplate = TTemplate.T_TEMPLATE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val conditions = generateGetMyTemplatesConditions(
            tTemplate = tTemplate,
            userId = userId,
            tStoreMember = tStoreMember,
            tStoreProjectRel = tStoreProjectRel,
            templateName = templateName
        )
        return dslContext.select(
            DSL.countDistinct(tTemplate.TEMPLATE_CODE)
        )
            .from(tTemplate)
            .leftJoin(tStoreMember)
            .on(tTemplate.TEMPLATE_CODE.eq(tStoreMember.STORE_CODE))
            .join(tStoreProjectRel)
            .on(tTemplate.TEMPLATE_CODE.eq(tStoreProjectRel.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Long::class.java)!!
    }

    private fun generateGetMyTemplatesConditions(
        tTemplate: TTemplate,
        userId: String,
        tStoreMember: TStoreMember,
        tStoreProjectRel: TStoreProjectRel,
        templateName: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(tTemplate.CREATOR.eq(userId).or(tStoreMember.USERNAME.eq(userId)))
        conditions.add(tStoreProjectRel.TYPE.eq(0))
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
        if (null != templateName) {
            conditions.add(tTemplate.TEMPLATE_NAME.contains(templateName))
        }
        return conditions
    }

    fun updateTemplateStatusById(
        dslContext: DSLContext,
        templateId: String,
        templateStatus: Byte,
        userId: String,
        msg: String? = null,
        latestFlag: Boolean? = null
    ) {
        with(TTemplate.T_TEMPLATE) {
            val baseStep = dslContext.update(this)
                .set(TEMPLATE_STATUS, templateStatus)
            if (null != msg) {
                baseStep.set(TEMPLATE_STATUS_MSG, msg)
            }
            if (null != latestFlag) {
                baseStep.set(LATEST_FLAG, latestFlag)
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
        msg: String? = null,
        latestFlag: Boolean? = null
    ) {
        with(TTemplate.T_TEMPLATE) {
            val baseStep = dslContext.update(this)
                .set(TEMPLATE_STATUS, templateNewStatus)
            if (null != msg) {
                baseStep.set(TEMPLATE_STATUS_MSG, msg)
            }
            if (null != latestFlag) {
                baseStep.set(LATEST_FLAG, latestFlag)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(TEMPLATE_CODE.eq(templateCode))
                .and(TEMPLATE_STATUS.eq(templateOldStatus))
                .execute()
        }
    }
}
