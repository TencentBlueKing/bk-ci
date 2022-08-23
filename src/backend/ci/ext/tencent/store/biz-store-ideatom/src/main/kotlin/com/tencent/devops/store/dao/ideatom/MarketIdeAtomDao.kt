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

package com.tencent.devops.store.dao.ideatom

import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TIdeAtom
import com.tencent.devops.model.store.tables.TIdeAtomCategoryRel
import com.tencent.devops.model.store.tables.TIdeAtomFeature
import com.tencent.devops.model.store.tables.TIdeAtomLabelRel
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomStatusEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class MarketIdeAtomDao {

    fun count(
        dslContext: DSLContext,
        keyword: String?,
        categoryCode: String?,
        classifyCode: String?,
        labelCodeList: List<String>?,
        score: Int?,
        rdType: IdeAtomTypeEnum?
    ): Int {
        val (tia, conditions) = formatConditions(keyword, classifyCode, dslContext)
        val tiaf = TIdeAtomFeature.T_IDE_ATOM_FEATURE.`as`("tiaf")

        val baseStep = dslContext.select(tia.ID.countDistinct()).from(tia)
                .leftJoin(tiaf)
                .on(tia.ATOM_CODE.eq(tiaf.ATOM_CODE))
        val storeType = StoreTypeEnum.IDE_ATOM.type.toByte()

        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val c = TLabel.T_LABEL.`as`("c")
            val labelIdList = dslContext.select(c.ID)
                    .from(c)
                    .where(c.LABEL_CODE.`in`(labelCodeList)).and(c.TYPE.eq(storeType))
                    .fetch().map { it["ID"] as String }
            val tialr = TIdeAtomLabelRel.T_IDE_ATOM_LABEL_REL.`as`("tialr")
            baseStep.leftJoin(tialr).on(tia.ID.eq(tialr.ATOM_ID))
            conditions.add(tialr.LABEL_ID.`in`(labelIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                    tas.STORE_CODE,
                    tas.STORE_TYPE,
                    tas.DOWNLOADS.`as`(MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT.name),
                    tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(tia.ATOM_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java)!!.ge(BigDecimal.valueOf(score.toLong())))
            conditions.add(t.field("STORE_TYPE", Byte::class.java)!!.eq(storeType))
        }
        if (categoryCode != null) {
            val tc = TCategory.T_CATEGORY.`as`("tc")
            val categoryId = dslContext.select(tc.ID)
                    .from(tc)
                    .where(tc.CATEGORY_CODE.eq(categoryCode).and(tc.TYPE.eq(storeType)))
            val tiacr = TIdeAtomCategoryRel.T_IDE_ATOM_CATEGORY_REL.`as`("tiacr")
            baseStep.leftJoin(tiacr).on(tia.ID.eq(tiacr.ATOM_ID))
            conditions.add(tiacr.CATEGORY_ID.eq(categoryId))
        }
        if (rdType != null) {
            conditions.add(tiaf.ATOM_TYPE.eq(rdType.type.toByte()))
        }

        return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
    }

    private fun formatConditions(
        keyword: String?,
        classifyCode: String?,
        dslContext: DSLContext
    ): Pair<TIdeAtom, MutableList<Condition>> {
        val tia = TIdeAtom.T_IDE_ATOM.`as`("tia")
        val storeType = StoreTypeEnum.IDE_ATOM.type.toByte()

        val conditions = mutableListOf<Condition>()
        conditions.add(tia.ATOM_STATUS.eq(IdeAtomStatusEnum.RELEASED.status.toByte())) // 已发布的
        conditions.add(tia.LATEST_FLAG.eq(true)) // 最新版本
        if (!keyword.isNullOrEmpty()) {
            conditions.add(tia.ATOM_NAME.contains(keyword).or(tia.SUMMARY.contains(keyword)))
        }
        if (!classifyCode.isNullOrEmpty()) {
            val a = TClassify.T_CLASSIFY.`as`("a")
            val classifyId = dslContext.select(a.ID)
                    .from(a)
                    .where(a.CLASSIFY_CODE.eq(classifyCode).and(a.TYPE.eq(storeType)))
                    .fetchOne(0, String::class.java)
            conditions.add(tia.CLASSIFY_ID.eq(classifyId))
        }
        return Pair(tia, conditions)
    }

    fun list(
        dslContext: DSLContext,
        keyword: String?,
        categoryCode: String?,
        classifyCode: String?,
        labelCodeList: List<String>?,
        score: Int?,
        rdType: IdeAtomTypeEnum?,
        sortType: MarketIdeAtomSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val (tia, conditions) = formatConditions(keyword, classifyCode, dslContext)
        val tiaf = TIdeAtomFeature.T_IDE_ATOM_FEATURE.`as`("tiaf")

        val baseStep = dslContext.select(
                tia.ID,
                tia.ATOM_NAME,
                tia.ATOM_CODE,
                tia.CLASSIFY_ID,
                tia.VERSION,
                tia.ATOM_STATUS,
                tia.LOGO_URL,
                tia.SUMMARY,
                tia.PUBLISHER,
                tia.PUB_TIME,
                tia.LATEST_FLAG,
                tia.CREATOR,
                tia.MODIFIER,
                tia.CREATE_TIME,
                tia.UPDATE_TIME,
                tiaf.ATOM_TYPE,
                tiaf.PUBLIC_FLAG,
                tiaf.RECOMMEND_FLAG,
                tiaf.WEIGHT,
                tiaf.CODE_SRC
        ).from(tia)
                .leftJoin(tiaf)
                .on(tia.ATOM_CODE.eq(tiaf.ATOM_CODE))

        val storeType = StoreTypeEnum.IDE_ATOM.type.toByte()
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val c = TLabel.T_LABEL.`as`("c")
            val labelIdList = dslContext.select(c.ID)
                    .from(c)
                    .where(c.LABEL_CODE.`in`(labelCodeList)).and(c.TYPE.eq(storeType))
                    .fetch().map { it["ID"] as String }
            val tialr = TIdeAtomLabelRel.T_IDE_ATOM_LABEL_REL.`as`("tialr")
            baseStep.leftJoin(tialr).on(tia.ID.eq(tialr.ATOM_ID))
            conditions.add(tialr.LABEL_ID.`in`(labelIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                    tas.STORE_CODE,
                    tas.STORE_TYPE,
                    tas.DOWNLOADS.`as`(MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT.name),
                    tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(tia.ATOM_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java)!!.ge(BigDecimal.valueOf(score.toLong())))
            conditions.add(t.field("STORE_TYPE", Byte::class.java)!!.eq(storeType))
        }
        if (categoryCode != null) {
            val tc = TCategory.T_CATEGORY.`as`("tc")
            val categoryId = dslContext.select(tc.ID)
                    .from(tc)
                    .where(tc.CATEGORY_CODE.eq(categoryCode).and(tc.TYPE.eq(storeType)))
            val tiacr = TIdeAtomCategoryRel.T_IDE_ATOM_CATEGORY_REL.`as`("tiacr")
            baseStep.leftJoin(tiacr).on(tia.ID.eq(tiacr.ATOM_ID))
            conditions.add(tiacr.CATEGORY_ID.eq(categoryId))
        }
        if (rdType != null) {
            conditions.add(tiaf.ATOM_TYPE.eq(rdType.type.toByte()))
        }

        if (null != sortType) {
            if (sortType == MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT && score == null) {
                val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
                val t = dslContext.select(tas.STORE_CODE, tas.DOWNLOADS
                        .`as`(MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT.name))
                        .from(tas)
                        .where(tas.STORE_TYPE.eq(storeType))
                        .asTable("t")
                baseStep.leftJoin(t).on(tia.ATOM_CODE.eq(t.field("STORE_CODE", String::class.java)))
            }

            val sortTypeField = MarketIdeAtomSortTypeEnum.getSortType(sortType.name)
            val realSortType = if (sortType == MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT) {
                DSL.field(sortTypeField)
            } else if (sortType == MarketIdeAtomSortTypeEnum.WEIGHT) {
                tiaf.field(sortTypeField)
            } else {
                tia.field(sortTypeField)
            }

            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(realSortType!!.desc())
            } else {
                baseStep.where(conditions).orderBy(realSortType!!.asc())
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
}
