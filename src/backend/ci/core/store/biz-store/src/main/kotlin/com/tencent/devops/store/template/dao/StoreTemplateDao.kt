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

package com.tencent.devops.store.template.dao

import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.TStoreStatistics
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.model.store.tables.TTemplateCategoryRel
import com.tencent.devops.model.store.tables.TTemplateLabelRel
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SelectJoinStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class StoreTemplateDao {

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
        val tt = TTemplate.T_TEMPLATE
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

        val conditions = getQueryTemplateCondition(
            templateStatus = templateStatus,
            tt = tt,
            latestFlag = latestFlag,
            templateName = templateName,
            templateType = templateType,
            classifyCode = classifyCode,
            dslContext = dslContext,
            categoryList = categoryList,
            baseStep = baseStep,
            labelCodeList = labelCodeList
        )

        if (null != sortType) {
            if (sortType == MarketTemplateSortTypeEnum.DOWNLOAD_COUNT.name) {
                val tas = TStoreStatistics.T_STORE_STATISTICS
                val t = dslContext
                    .select(tas.STORE_CODE, DSL.sum(tas.DOWNLOADS).`as`(MarketTemplateSortTypeEnum.DOWNLOAD_COUNT.name))
                    .from(tas).groupBy(tas.STORE_CODE).asTable("t")
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
        val tt = TTemplate.T_TEMPLATE
        val baseStep = dslContext.selectCount().from(tt)
        val conditions = getQueryTemplateCondition(
            templateStatus = templateStatus,
            tt = tt,
            latestFlag = latestFlag,
            templateName = templateName,
            templateType = templateType,
            classifyCode = classifyCode,
            dslContext = dslContext,
            categoryList = categoryList,
            baseStep = baseStep,
            labelCodeList = labelCodeList
        )
        return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
    }

    private fun getQueryTemplateCondition(
        templateStatus: Byte?,
        tt: TTemplate,
        latestFlag: Boolean?,
        templateName: String?,
        templateType: Byte?,
        classifyCode: String?,
        dslContext: DSLContext,
        categoryList: List<String>?,
        baseStep: SelectJoinStep<out Record>,
        labelCodeList: List<String>?
    ): MutableList<Condition> {
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
            val tClassify = TClassify.T_CLASSIFY
            val classifyId = dslContext.select(tClassify.ID)
                .from(tClassify)
                .where(
                    tClassify.CLASSIFY_CODE.eq(classifyCode)
                        .and(tClassify.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
                )
                .fetchOne(0, String::class.java)
            conditions.add(tt.CLASSIFY_ID.eq(classifyId))
        }

        // 根据应用范畴和功能标签筛选
        if (!categoryList.isNullOrEmpty()) {
            val tCategory = TCategory.T_CATEGORY
            val categoryIdList = dslContext.select(tCategory.ID)
                .from(tCategory)
                .where(tCategory.CATEGORY_CODE.`in`(categoryList))
                .and(tCategory.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
                .fetch().map { it[tCategory.ID] as String }
            val ttcr = TTemplateCategoryRel.T_TEMPLATE_CATEGORY_REL
            baseStep.leftJoin(ttcr).on(tt.ID.eq(ttcr.TEMPLATE_ID))
            conditions.add(ttcr.CATEGORY_ID.`in`(categoryIdList))
        }
        if (!labelCodeList.isNullOrEmpty()) {
            val tLabel = TLabel.T_LABEL
            val labelIdList = dslContext.select(tLabel.ID)
                .from(tLabel)
                .where(tLabel.LABEL_CODE.`in`(labelCodeList)).and(tLabel.TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
                .fetch().map { it[tLabel.ID] as String }
            val ttlr = TTemplateLabelRel.T_TEMPLATE_LABEL_REL
            baseStep.leftJoin(ttlr).on(tt.ID.eq(ttlr.TEMPLATE_ID))
            conditions.add(ttlr.LABEL_ID.`in`(labelIdList))
        }
        return conditions
    }

    /**
     * 统计分类下处于已发布状态的模板个数
     */
    fun countReleaseTemplateNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectCount().from(this)
                .where(TEMPLATE_STATUS.eq(TemplateStatusEnum.RELEASED.status.toByte())
                    .and(CLASSIFY_ID.eq(classifyId)))
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 统计还在使用处于下架中或者已下架状态的模板的项目的个数
     */
    fun countUndercarriageTemplateNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        val tTemplate = TTemplate.T_TEMPLATE
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val templateStatusList = listOf(TemplateStatusEnum.UNDERCARRIAGED.status.toByte())
        return dslContext.select(DSL.countDistinct(tStoreProjectRel.PROJECT_CODE)).from(tTemplate)
            .join(tStoreProjectRel)
            .on(tTemplate.TEMPLATE_CODE.eq(tStoreProjectRel.STORE_CODE))
            .where(
                tTemplate.TEMPLATE_STATUS.`in`(templateStatusList)
                    .and(tTemplate.CLASSIFY_ID.eq(classifyId))
                    .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.TEMPLATE.type.toByte()))
            ).fetchOne(0, Int::class.java)!!
    }
}
