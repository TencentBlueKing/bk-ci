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
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.TStoreStatistics
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.model.store.tables.TTemplateCategoryRel
import com.tencent.devops.model.store.tables.TTemplateLabelRel
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.enums.TemplateStatusEnum
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class TemplateDao {

    fun list(
        dslContext: DSLContext,
        templateName: String?,
        templateStatus: Byte?,
        templateType: Byte?,
        classifyCode: String?,
        categoryList: List<String>?,
        labelCodeList: List<String>?,
        latestFlag: Boolean?,
        sortType: String?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val tt = TTemplate.T_TEMPLATE.`as`("tt")

        val conditions = mutableListOf<Condition>()
        if (templateStatus != null) {
            conditions.add(tt.TEMPLATE_STATUS.eq(templateStatus))
        }
        if (latestFlag != null) {
            conditions.add(tt.LATEST_FLAG.eq(latestFlag)) // 最新版本
        }
        if (!templateName.isNullOrEmpty()) {
            conditions.add(tt.TEMPLATE_NAME.contains(templateName))
        }
        if (templateType != null) {
            conditions.add(tt.TEMPLATE_TYPE.eq(templateType))
        }
        if (!classifyCode.isNullOrEmpty()) {
            val a = TClassify.T_CLASSIFY.`as`("a")
            val classifyId = dslContext.select(a.ID)
                .from(a)
                .where(a.CLASSIFY_CODE.eq(classifyCode).and(a.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte())))
                .fetchOne(0, String::class.java)
            conditions.add(tt.CLASSIFY_ID.eq(classifyId))
        }

        val baseStep = dslContext.select(
            tt.ID,
            tt.TEMPLATE_NAME,
            tt.TEMPLATE_CODE,
            tt.CLASSIFY_ID,
            tt.VERSION,
            tt.TEMPLATE_TYPE,
            tt.TEMPLATE_STATUS,
            tt.TEMPLATE_STATUS_MSG,
            tt.LOGO_URL,
            tt.SUMMARY,
            tt.DESCRIPTION,
            tt.PUBLISHER,
            tt.PUB_DESCRIPTION,
            tt.PUBLIC_FLAG,
            tt.LATEST_FLAG,
            tt.CREATOR,
            tt.MODIFIER,
            tt.CREATE_TIME,
            tt.UPDATE_TIME
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

        if (null != sortType) {
            if (sortType == MarketTemplateSortTypeEnum.DOWNLOAD_COUNT.name) {
                val tas = TStoreStatistics.T_STORE_STATISTICS.`as`("tas")
                val t = dslContext.select(
                    tas.STORE_CODE,
                    tas.DOWNLOADS.sum().`as`(MarketTemplateSortTypeEnum.DOWNLOAD_COUNT.name)
                ).from(tas).groupBy(tas.STORE_CODE).asTable("t")
                baseStep.leftJoin(t).on(tt.TEMPLATE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            }

            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(DSL.field(sortType).desc())
            } else {
                baseStep.where(conditions).orderBy(DSL.field(sortType).asc())
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

    fun count(
        dslContext: DSLContext,
        templateName: String?,
        templateStatus: Byte?,
        templateType: Byte?,
        classifyCode: String?,
        categoryList: List<String>?,
        labelCodeList: List<String>?,
        latestFlag: Boolean?
    ): Int {
        val tt = TTemplate.T_TEMPLATE.`as`("tt")

        val conditions = mutableListOf<Condition>()
        if (templateStatus != null) {
            conditions.add(tt.TEMPLATE_STATUS.eq(templateStatus))
        }
        if (latestFlag != null) {
            conditions.add(tt.LATEST_FLAG.eq(latestFlag)) // 最新版本
        }
        if (!templateName.isNullOrEmpty()) {
            conditions.add(tt.TEMPLATE_NAME.contains(templateName))
        }
        if (templateType != null) {
            conditions.add(tt.TEMPLATE_TYPE.eq(templateType))
        }
        if (!classifyCode.isNullOrEmpty()) {
            val a = TClassify.T_CLASSIFY.`as`("a")
            val classifyId = dslContext.select(a.ID)
                .from(a)
                .where(a.CLASSIFY_CODE.eq(classifyCode).and(a.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte())))
                .fetchOne(0, String::class.java)
            conditions.add(tt.CLASSIFY_ID.eq(classifyId))
        }

        val baseStep = dslContext.selectCount().from(tt)

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

        return baseStep.where(conditions).fetchOne(0, Int::class.java)
    }

    /**
     * 统计分类下处于已发布状态的模板个数
     */
    fun countReleaseTemplateNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount().from(this)
                .where(TEMPLATE_STATUS.eq(TemplateStatusEnum.RELEASED.status.toByte()).and(CLASSIFY_ID.eq(classifyId)))
                .fetchOne(0, Int::class.java)
        }
    }

    /**
     * 统计还在使用处于下架中或者已下架状态的模板的项目的个数
     */
    fun countShelvesTemplateNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        val a = TTemplate.T_TEMPLATE.`as`("a")
        val b = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("b")
        val templateStatusList = listOf(TemplateStatusEnum.RELEASED.status.toByte())
        return dslContext.selectCount().from(a).join(b).on(a.TEMPLATE_CODE.eq(b.STORE_CODE))
            .where(a.TEMPLATE_STATUS.`in`(templateStatusList).and(a.CLASSIFY_ID.eq(classifyId)))
            .fetchOne(0, Int::class.java)
    }
}