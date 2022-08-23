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
import com.tencent.devops.model.store.tables.records.TIdeAtomRecord
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.ideatom.IdeAtomBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomCreateRequest
import com.tencent.devops.store.pojo.ideatom.IdeAtomUpdateRequest
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomStatusEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record2
import org.jooq.Result
import org.jooq.SelectHavingStep
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class IdeAtomDao {

    fun countByName(dslContext: DSLContext, atomName: String, atomCode: String? = null): Long {
        with(TIdeAtom.T_IDE_ATOM) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_NAME.eq(atomName))
            if (atomCode != null) {
                conditions.add(ATOM_CODE.eq(atomCode))
            }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Long::class.java)!!
        }
    }

    fun countByCode(dslContext: DSLContext, atomCode: String): Long {
        with(TIdeAtom.T_IDE_ATOM) {
            return dslContext.selectCount().from(this)
                .where(ATOM_CODE.eq(atomCode))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun countByIdAndCode(dslContext: DSLContext, atomId: String, atomCode: String): Long {
        with(TIdeAtom.T_IDE_ATOM) {
            return dslContext.selectCount()
                .from(this)
                .where(ID.eq(atomId))
                .and(ATOM_CODE.eq(atomCode))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getIdeAtomById(dslContext: DSLContext, atomId: String): TIdeAtomRecord? {
        return with(TIdeAtom.T_IDE_ATOM) {
            dslContext.selectFrom(this)
                .where(ID.eq(atomId))
                .fetchOne()
        }
    }

    fun getIdeAtom(dslContext: DSLContext, atomCode: String, version: String): TIdeAtomRecord? {
        return with(TIdeAtom.T_IDE_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode).and(VERSION.like(VersionUtils.generateQueryVersion(version))))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getIdeAtomsByAtomCode(dslContext: DSLContext, atomCode: String): Result<TIdeAtomRecord>? {
        return with(TIdeAtom.T_IDE_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun getNewestAtomByCode(dslContext: DSLContext, atomCode: String): TIdeAtomRecord? {
        return with(TIdeAtom.T_IDE_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getLatestAtomByCode(dslContext: DSLContext, atomCode: String): TIdeAtomRecord? {
        return with(TIdeAtom.T_IDE_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .and(LATEST_FLAG.eq(true))
                .fetchOne()
        }
    }

    fun getLatestReleaseAtomByCode(dslContext: DSLContext, atomCode: String): TIdeAtomRecord? {
        return with(TIdeAtom.T_IDE_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .and(LATEST_FLAG.eq(true))
                .and(ATOM_STATUS.eq(IdeAtomStatusEnum.RELEASED.status.toByte()))
                .fetchOne()
        }
    }

    fun getReleaseAtomsByCode(dslContext: DSLContext, atomCode: String): Result<TIdeAtomRecord>? {
        return with(TIdeAtom.T_IDE_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .and(ATOM_STATUS.eq(IdeAtomStatusEnum.RELEASED.status.toByte()))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun getNewestUndercarriagedAtomsByCode(dslContext: DSLContext, atomCode: String): TIdeAtomRecord? {
        return with(TIdeAtom.T_IDE_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .and(ATOM_STATUS.eq(IdeAtomStatusEnum.UNDERCARRIAGED.status.toByte()))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun deleteIdeAtomById(dslContext: DSLContext, atomId: String) {
        with(TIdeAtom.T_IDE_ATOM) {
            dslContext.deleteFrom(this)
                .where(ID.eq(atomId))
                .execute()
        }
    }

    fun countReleaseAtomById(dslContext: DSLContext, atomId: String): Int {
        with(TIdeAtom.T_IDE_ATOM) {
            return dslContext.selectCount().from(this)
                .where(ID.eq(atomId).and(ATOM_STATUS.eq(IdeAtomStatusEnum.RELEASED.status.toByte())))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun updateAtomBaseInfoById(
        dslContext: DSLContext,
        atomId: String,
        userId: String,
        ideAtomBaseInfoUpdateRequest: IdeAtomBaseInfoUpdateRequest
    ) {
        with(TIdeAtom.T_IDE_ATOM) {
            val baseStep = dslContext.update(this)
            val atomStatus = ideAtomBaseInfoUpdateRequest.atomStatus
            if (null != atomStatus) {
                baseStep.set(ATOM_STATUS, atomStatus.status.toByte())
            }
            val atomStatusMsg = ideAtomBaseInfoUpdateRequest.atomStatusMsg
            if (null != atomStatusMsg) {
                baseStep.set(ATOM_STATUS_MSG, atomStatusMsg)
            }
            val pubTime = ideAtomBaseInfoUpdateRequest.pubTime
            if (null != pubTime) {
                baseStep.set(PUB_TIME, pubTime)
            }
            val latestFlag = ideAtomBaseInfoUpdateRequest.latestFlag
            if (null != latestFlag) {
                baseStep.set(LATEST_FLAG, latestFlag)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(atomId))
                .execute()
        }
    }

    fun updateAtomStatusByCode(
        dslContext: DSLContext,
        atomCode: String,
        latestFlag: Boolean,
        atomOldStatus: Byte,
        atomNewStatus: Byte,
        userId: String,
        msg: String?
    ) {
        with(TIdeAtom.T_IDE_ATOM) {
            val baseStep = dslContext.update(this)
                .set(ATOM_STATUS, atomNewStatus)
                .set(LATEST_FLAG, latestFlag)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(ATOM_STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ATOM_CODE.eq(atomCode))
                .and(ATOM_STATUS.eq(atomOldStatus))
                .execute()
        }
    }

    fun addIdeAtomFromOp(
        dslContext: DSLContext,
        userId: String,
        atomId: String,
        latestFlag: Boolean,
        ideAtomCreateRequest: IdeAtomCreateRequest
    ) {
        val a = TClassify.T_CLASSIFY.`as`("a")
        val classifyId = dslContext.select(a.ID).from(a)
            .where(a.CLASSIFY_CODE.eq(ideAtomCreateRequest.classifyCode).and(a.TYPE.eq(StoreTypeEnum.IDE_ATOM.type.toByte())))
            .fetchOne(0, String::class.java)
        with(TIdeAtom.T_IDE_ATOM) {
            dslContext.insertInto(this,
                ID,
                ATOM_NAME,
                ATOM_CODE,
                CLASSIFY_ID,
                VERSION,
                ATOM_STATUS,
                LOGO_URL,
                SUMMARY,
                DESCRIPTION,
                PUBLISHER,
                LATEST_FLAG,
                VISIBILITY_LEVEL,
                PRIVATE_REASON,
                CREATOR,
                MODIFIER
            )
                .values(
                    atomId,
                    ideAtomCreateRequest.atomName,
                    ideAtomCreateRequest.atomCode,
                    classifyId,
                    ideAtomCreateRequest.version,
                    IdeAtomStatusEnum.INIT.status.toByte(),
                    ideAtomCreateRequest.logoUrl,
                    ideAtomCreateRequest.summary,
                    ideAtomCreateRequest.description,
                    ideAtomCreateRequest.publisher,
                    latestFlag,
                    ideAtomCreateRequest.visibilityLevel.level,
                    ideAtomCreateRequest.privateReason,
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun updateAtomFromOp(dslContext: DSLContext, userId: String, atomId: String, atomUpdateRequest: IdeAtomUpdateRequest) {
        with(TIdeAtom.T_IDE_ATOM) {
            val baseStep = dslContext.update(this)
            if (null != atomUpdateRequest.atomName) {
                baseStep.set(ATOM_NAME, atomUpdateRequest.atomName)
            }
            if (null != atomUpdateRequest.classifyCode) {
                val a = TClassify.T_CLASSIFY.`as`("a")
                val classifyId = dslContext.select(a.ID).from(a)
                    .where(a.CLASSIFY_CODE.eq(atomUpdateRequest.classifyCode).and(a.TYPE.eq(StoreTypeEnum.IDE_ATOM.type.toByte())))
                    .fetchOne(0, String::class.java)
                baseStep.set(CLASSIFY_ID, classifyId)
            }
            if (null != atomUpdateRequest.logoUrl) {
                baseStep.set(LOGO_URL, atomUpdateRequest.logoUrl)
            }
            if (null != atomUpdateRequest.summary) {
                baseStep.set(SUMMARY, atomUpdateRequest.summary)
            }
            if (null != atomUpdateRequest.description) {
                baseStep.set(DESCRIPTION, atomUpdateRequest.description)
            }
            if (null != atomUpdateRequest.publisher) {
                baseStep.set(PUBLISHER, atomUpdateRequest.publisher)
            }
            val visibilityLevel = atomUpdateRequest.visibilityLevel
            if (null != visibilityLevel) {
                baseStep.set(VISIBILITY_LEVEL, visibilityLevel.level)
            }
            if (null != atomUpdateRequest.privateReason) {
                baseStep.set(PRIVATE_REASON, atomUpdateRequest.privateReason)
            }
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(atomId))
                .execute()
        }
    }

    fun countOpIdeAtom(
        dslContext: DSLContext,
        atomName: String?,
        atomType: IdeAtomTypeEnum?,
        classifyCode: String?,
        categoryCodeList: List<String>?,
        labelCodeList: List<String>?,
        processFlag: Boolean?
    ): Long {
        val a = TIdeAtom.T_IDE_ATOM.`as`("a")
        val b = TClassify.T_CLASSIFY.`as`("b")
        val c = TIdeAtomCategoryRel.T_IDE_ATOM_CATEGORY_REL.`as`("c")
        val d = TIdeAtomLabelRel.T_IDE_ATOM_LABEL_REL.`as`("d")
        val tiaf = TIdeAtomFeature.T_IDE_ATOM_FEATURE.`as`("tiaf")
        val tmp = dslContext.select(
            a.ATOM_CODE.`as`("atomCode"),
            a.CREATE_TIME.max().`as`("createTime")
        ).from(a).groupBy(a.ATOM_CODE)
        val t = dslContext.select(a.ATOM_CODE.`as`("atomCode"), a.ATOM_STATUS.`as`("atomStatus")).from(a).join(tmp)
            .on(a.ATOM_CODE.eq(tmp.field("atomCode", String::class.java)).and(a.CREATE_TIME.eq(tmp.field("createTime", LocalDateTime::class.java))))
        val conditions = generateQueryOpIdeAtomCondition(a, atomName, atomType, tiaf, classifyCode, b, processFlag, t)
        val baseStep = dslContext.select(a.ID.countDistinct()).from(a)
            .join(tiaf)
            .on(a.ATOM_CODE.eq(tiaf.ATOM_CODE))
            .join(b)
            .on(a.CLASSIFY_ID.eq(b.ID))
            .join(t)
            .on(a.ATOM_CODE.eq(t.field("atomCode", String::class.java)))
        if (categoryCodeList != null && categoryCodeList.isNotEmpty()) {
            val tc = TCategory.T_CATEGORY.`as`("tc")
            val categoryIdList = dslContext.select(tc.ID)
                .from(tc)
                .where(tc.CATEGORY_CODE.`in`(categoryCodeList)).and(tc.TYPE.eq(StoreTypeEnum.IDE_ATOM.type.toByte()))
                .fetch().map { it["ID"] as String }
            baseStep.join(c).on(a.ID.eq(c.ATOM_ID))
            conditions.add(c.CATEGORY_ID.`in`(categoryIdList))
        }
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val tl = TLabel.T_LABEL.`as`("tl")
            val labelIdList = dslContext.select(tl.ID)
                .from(tl)
                .where(tl.LABEL_CODE.`in`(labelCodeList)).and(tl.TYPE.eq(StoreTypeEnum.IDE_ATOM.type.toByte()))
                .fetch().map { it["ID"] as String }
            baseStep.join(d).on(a.ID.eq(d.ATOM_ID))
            conditions.add(d.LABEL_ID.`in`(labelIdList))
        }
        return baseStep.where(conditions).fetchOne(0, Long::class.java)!!
    }

    fun listOpIdeAtoms(
        dslContext: DSLContext,
        atomName: String?,
        atomType: IdeAtomTypeEnum?,
        classifyCode: String?,
        categoryCodeList: List<String>?,
        labelCodeList: List<String>?,
        processFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<out Record>? {
        val a = TIdeAtom.T_IDE_ATOM.`as`("a")
        val b = TClassify.T_CLASSIFY.`as`("b")
        val c = TIdeAtomCategoryRel.T_IDE_ATOM_CATEGORY_REL.`as`("c")
        val d = TIdeAtomLabelRel.T_IDE_ATOM_LABEL_REL.`as`("d")
        val tiaf = TIdeAtomFeature.T_IDE_ATOM_FEATURE.`as`("tiaf")
        // 查找每组atomCode最新的记录
        val tmp = dslContext.select(
            a.ATOM_CODE.`as`("atomCode"),
            a.CREATE_TIME.max().`as`("createTime")
        ).from(a).groupBy(a.ATOM_CODE)
        val t = dslContext.select(a.ATOM_CODE.`as`("atomCode"), a.ATOM_STATUS.`as`("atomStatus")).from(a).join(tmp)
            .on(a.ATOM_CODE.eq(tmp.field("atomCode", String::class.java)).and(a.CREATE_TIME.eq(tmp.field("createTime", LocalDateTime::class.java))))
        val conditions = generateQueryOpIdeAtomCondition(a, atomName, atomType, tiaf, classifyCode, b, processFlag, t)
        val baseStep = dslContext.select(
            a.ID.`as`("atomId"),
            a.ATOM_NAME.`as`("atomName"),
            a.ATOM_CODE.`as`("atomCode"),
            tiaf.ATOM_TYPE.`as`("atomType"),
            a.VERSION.`as`("atomVersion"),
            a.ATOM_STATUS.`as`("atomStatus"),
            b.CLASSIFY_CODE.`as`("classifyCode"),
            b.CLASSIFY_NAME.`as`("classifyName"),
            a.PUBLISHER.`as`("publisher"),
            a.PUB_TIME.`as`("pubTime"),
            a.LATEST_FLAG.`as`("latestFlag"),
            tiaf.PUBLIC_FLAG.`as`("publicFlag"),
            tiaf.RECOMMEND_FLAG.`as`("recommendFlag"),
            tiaf.WEIGHT.`as`("weight"),
            a.CREATOR.`as`("creator"),
            a.CREATE_TIME.`as`("createTime"),
            a.MODIFIER.`as`("modifier"),
            a.UPDATE_TIME.`as`("updateTime")
        ).from(a)
            .join(tiaf)
            .on(a.ATOM_CODE.eq(tiaf.ATOM_CODE))
            .join(b)
            .on(a.CLASSIFY_ID.eq(b.ID))
            .join(t)
            .on(a.ATOM_CODE.eq(t.field("atomCode", String::class.java)))
        if (categoryCodeList != null && categoryCodeList.isNotEmpty()) {
            val tc = TCategory.T_CATEGORY.`as`("tc")
            val categoryIdList = dslContext.select(tc.ID)
                .from(tc)
                .where(tc.CATEGORY_CODE.`in`(categoryCodeList)).and(tc.TYPE.eq(StoreTypeEnum.IDE_ATOM.type.toByte()))
                .fetch().map { it["ID"] as String }
            baseStep.join(c).on(a.ID.eq(c.ATOM_ID))
            conditions.add(c.CATEGORY_ID.`in`(categoryIdList))
        }
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val tl = TLabel.T_LABEL.`as`("tl")
            val labelIdList = dslContext.select(tl.ID)
                .from(tl)
                .where(tl.LABEL_CODE.`in`(labelCodeList)).and(tl.TYPE.eq(StoreTypeEnum.IDE_ATOM.type.toByte()))
                .fetch().map { it["ID"] as String }
            baseStep.join(d).on(a.ID.eq(d.ATOM_ID))
            conditions.add(d.LABEL_ID.`in`(labelIdList))
        }
        return baseStep.where(conditions).orderBy(a.UPDATE_TIME.desc()).limit((page - 1) * pageSize, pageSize).fetch()
    }

    private fun generateQueryOpIdeAtomCondition(
        a: TIdeAtom,
        atomName: String?,
        atomType: IdeAtomTypeEnum?,
        tiaf: TIdeAtomFeature,
        classifyCode: String?,
        b: TClassify,
        processFlag: Boolean?,
        t: SelectHavingStep<Record2<String, Byte>>
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.LATEST_FLAG.eq(true)) // 最新可见版本
        if (!atomName.isNullOrEmpty()) {
            conditions.add(a.ATOM_NAME.contains(atomName))
        }
        if (null != atomType) {
            conditions.add(tiaf.ATOM_TYPE.eq(atomType.type.toByte()))
        }
        if (!classifyCode.isNullOrEmpty()) {
            conditions.add(b.CLASSIFY_CODE.eq(classifyCode))
        }
        if (null != processFlag) {
            if (processFlag) {
                val atomStatusList = listOf(
                    IdeAtomStatusEnum.INIT.status.toByte(),
                    IdeAtomStatusEnum.AUDITING.status.toByte()
                )
                conditions.add(t.field("atomStatus")!!.`in`(atomStatusList))
            } else {
                val atomStatusList = listOf(
                    IdeAtomStatusEnum.AUDIT_REJECT.status.toByte(),
                    IdeAtomStatusEnum.RELEASED.status.toByte(),
                    IdeAtomStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
                    IdeAtomStatusEnum.UNDERCARRIAGED.status.toByte()
                )
                conditions.add(t.field("atomStatus")!!.`in`(atomStatusList))
            }
        }
        return conditions
    }

    fun updateAtomLatestFlag(
        dslContext: DSLContext,
        userId: String,
        atomId: String,
        latestFlag: Boolean
    ) {
        with(TIdeAtom.T_IDE_ATOM) {
            val baseStep = dslContext.update(this)
                .set(LATEST_FLAG, latestFlag)
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(atomId))
                .execute()
        }
    }

    /**
     * 清空LATEST_FLAG
     */
    fun cleanLatestFlag(dslContext: DSLContext, atomCode: String) {
        with(TIdeAtom.T_IDE_ATOM) {
            dslContext.update(this)
                .set(LATEST_FLAG, false)
                .where(ATOM_CODE.eq(atomCode))
                .execute()
        }
    }

    /**
     * 统计分类下处于已发布状态的IDE插件个数
     */
    fun countReleaseAtomNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        with(TIdeAtom.T_IDE_ATOM) {
            return dslContext.selectCount().from(this)
                .where(ATOM_STATUS.eq(IdeAtomStatusEnum.RELEASED.status.toByte()).and(CLASSIFY_ID.eq(classifyId)))
                .fetchOne(0, Int::class.java)!!
        }
    }
}
