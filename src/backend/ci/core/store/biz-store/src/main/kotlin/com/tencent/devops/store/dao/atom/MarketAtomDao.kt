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

package com.tencent.devops.store.dao.atom

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomEnvInfo
import com.tencent.devops.model.store.tables.TAtomFeature
import com.tencent.devops.model.store.tables.TAtomLabelRel
import com.tencent.devops.model.store.tables.TAtomVersionLog
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.pojo.atom.ApproveReq
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.UpdateAtomInfo
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SelectOnConditionStep
import org.jooq.UpdateSetFirstStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class MarketAtomDao : AtomBaseDao() {

    /**
     * 插件商店搜索结果，总数
     */
    fun count(
        dslContext: DSLContext,
        keyword: String?,
        classifyCode: String?,
        labelCodeList: List<String>?,
        score: Int?,
        rdType: AtomTypeEnum?,
        yamlFlag: Boolean?,
        recommendFlag: Boolean?
    ): Int {
        val (ta, conditions) = formatConditions(keyword, rdType, classifyCode, dslContext)
        val taf = TAtomFeature.T_ATOM_FEATURE.`as`("taf")
        val baseStep = dslContext.select(ta.ID.countDistinct()).from(ta).leftJoin(taf)
            .on(ta.ATOM_CODE.eq(taf.ATOM_CODE))

        val storeType = StoreTypeEnum.ATOM.type.toByte()
        handleMainListBaseStep(
            dslContext = dslContext,
            ta = ta,
            taf = taf,
            baseStep = baseStep,
            conditions = conditions,
            labelCodeList = labelCodeList,
            storeType = storeType,
            score = score,
            yamlFlag = yamlFlag,
            recommendFlag = recommendFlag
        )
        return baseStep.where(conditions).fetchOne(0, Int::class.java)
    }

    private fun formatConditions(
        keyword: String?,
        rdType: AtomTypeEnum?,
        classifyCode: String?,
        dslContext: DSLContext
    ): Pair<TAtom, MutableList<Condition>> {
        val ta = TAtom.T_ATOM.`as`("ta")
        val storeType = StoreTypeEnum.ATOM.type.toByte()
        val conditions = setAtomVisibleCondition(ta)
        conditions.add(ta.DELETE_FLAG.eq(false)) // 只查没有被删除的插件
        if (!keyword.isNullOrEmpty()) {
            conditions.add(ta.NAME.contains(keyword).or(ta.SUMMARY.contains(keyword)))
        }
        if (rdType != null) {
            conditions.add(ta.ATOM_TYPE.eq(rdType.type.toByte()))
        }
        if (!classifyCode.isNullOrEmpty()) {
            val a = TClassify.T_CLASSIFY.`as`("a")
            val classifyId = dslContext.select(a.ID)
                .from(a)
                .where(a.CLASSIFY_CODE.eq(classifyCode).and(a.TYPE.eq(storeType)))
                .fetchOne(0, String::class.java)
            conditions.add(ta.CLASSIFY_ID.eq(classifyId))
        }
        return Pair(ta, conditions)
    }

    /**
     * 插件商店搜索结果列表
     */
    fun list(
        dslContext: DSLContext,
        keyword: String?,
        classifyCode: String?,
        labelCodeList: List<String>?,
        score: Int?,
        rdType: AtomTypeEnum?,
        yamlFlag: Boolean?,
        recommendFlag: Boolean?,
        sortType: MarketAtomSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val (ta, conditions) = formatConditions(keyword, rdType, classifyCode, dslContext)
        val taf = TAtomFeature.T_ATOM_FEATURE.`as`("taf")
        val baseStep = dslContext.select(
            ta.ID,
            ta.NAME,
            ta.JOB_TYPE,
            ta.ATOM_TYPE,
            ta.CLASSIFY_ID,
            ta.CATEGROY,
            ta.ATOM_CODE,
            ta.LOGO_URL,
            ta.PUBLISHER,
            ta.SUMMARY,
            ta.DEFAULT_FLAG,
            ta.OS,
            ta.BUILD_LESS_RUN_FLAG,
            ta.DOCS_LINK,
            taf.RECOMMEND_FLAG,
            taf.YAML_FLAG
        ).from(ta)
            .leftJoin(taf)
            .on(ta.ATOM_CODE.eq(taf.ATOM_CODE))

        val storeType = StoreTypeEnum.ATOM.type.toByte()
        handleMainListBaseStep(
            dslContext = dslContext,
            ta = ta,
            taf = taf,
            baseStep = baseStep,
            conditions = conditions,
            labelCodeList = labelCodeList,
            storeType = storeType,
            score = score,
            yamlFlag = yamlFlag,
            recommendFlag = recommendFlag
        )

        if (null != sortType) {
            if (sortType == MarketAtomSortTypeEnum.DOWNLOAD_COUNT && score == null) {
                val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
                val t =
                    dslContext.select(tas.STORE_CODE, tas.DOWNLOADS.`as`(MarketAtomSortTypeEnum.DOWNLOAD_COUNT.name))
                        .from(tas).where(tas.STORE_TYPE.eq(storeType)).asTable("t")
                baseStep.leftJoin(t).on(ta.ATOM_CODE.eq(t.field("STORE_CODE", String::class.java)))
            }

            val realSortType = if (sortType == MarketAtomSortTypeEnum.DOWNLOAD_COUNT) {
                DSL.field(sortType.name)
            } else {
                ta.field(sortType.name)
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

    private fun handleMainListBaseStep(
        dslContext: DSLContext,
        ta: TAtom,
        taf: TAtomFeature,
        baseStep: SelectOnConditionStep<out Record>,
        conditions: MutableList<Condition>,
        labelCodeList: List<String>?,
        storeType: Byte,
        score: Int?,
        yamlFlag: Boolean?,
        recommendFlag: Boolean?
    ) {
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val c = TLabel.T_LABEL.`as`("c")
            val labelIdList = dslContext.select(c.ID)
                .from(c)
                .where(c.LABEL_CODE.`in`(labelCodeList)).and(c.TYPE.eq(storeType))
                .fetch().map { it["ID"] as String }
            val talr = TAtomLabelRel.T_ATOM_LABEL_REL.`as`("talr")
            baseStep.leftJoin(talr).on(ta.ID.eq(talr.ATOM_ID))
            conditions.add(talr.LABEL_ID.`in`(labelIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.STORE_TYPE,
                tas.DOWNLOADS.`as`(MarketAtomSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(ta.ATOM_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java).ge(BigDecimal.valueOf(score.toLong())))
            conditions.add(t.field("STORE_TYPE", Byte::class.java).eq(storeType))
        }
        if (null != yamlFlag) {
            conditions.add(taf.YAML_FLAG.eq(yamlFlag))
        }
        if (null != recommendFlag) {
            conditions.add(taf.RECOMMEND_FLAG.eq(recommendFlag))
        }
    }

    fun countReleaseAtomByCode(dslContext: DSLContext, atomCode: String): Int {
        with(TAtom.T_ATOM) {
            return dslContext.selectCount().from(this)
                .where(ATOM_CODE.eq(atomCode).and(ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte())))
                .fetchOne(0, Int::class.java)
        }
    }

    private fun generateGetMyAtomConditions(a: TAtom, userId: String, b: TStoreMember, atomName: String?): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.DELETE_FLAG.eq(false)) // 只查没有被删除的插件
        conditions.add(b.USERNAME.eq(userId))
        conditions.add(b.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        if (null != atomName) {
            conditions.add(a.NAME.contains(atomName))
        }
        return conditions
    }

    fun countMyAtoms(
        dslContext: DSLContext,
        userId: String,
        atomName: String?
    ): Int {
        val a = TAtom.T_ATOM.`as`("a")
        val b = TStoreMember.T_STORE_MEMBER.`as`("b")
        val conditions = generateGetMyAtomConditions(a, userId, b, atomName)
        return dslContext.select(a.ATOM_CODE.countDistinct())
            .from(a)
            .leftJoin(b)
            .on(a.ATOM_CODE.eq(b.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Int::class.java)
    }

    fun getMyAtoms(
        dslContext: DSLContext,
        userId: String,
        atomName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val a = TAtom.T_ATOM.`as`("a")
        val b = TStoreMember.T_STORE_MEMBER.`as`("b")
        val d = TAtomEnvInfo.T_ATOM_ENV_INFO.`as`("d")
        val t = dslContext.select(a.ATOM_CODE.`as`("atomCode"), a.CREATE_TIME.max().`as`("createTime")).from(a).groupBy(a.ATOM_CODE) // 查找每组atomCode最新的记录
        val conditions = generateGetMyAtomConditions(a, userId, b, atomName)
        val baseStep = dslContext.select(
            a.ID.`as`("atomId"),
            a.ATOM_CODE.`as`("atomCode"),
            a.NAME.`as`("name"),
            a.CATEGROY.`as`("category"),
            d.LANGUAGE.`as`("language"),
            a.LOGO_URL.`as`("logoUrl"),
            a.VERSION.`as`("version"),
            a.ATOM_STATUS.`as`("atomStatus"),
            a.CREATOR.`as`("creator"),
            a.CREATE_TIME.`as`("createTime"),
            a.MODIFIER.`as`("modifier"),
            a.UPDATE_TIME.`as`("updateTime")
        )
            .from(a)
            .join(t)
            .on(a.ATOM_CODE.eq(t.field("atomCode", String::class.java)).and(a.CREATE_TIME.eq(t.field("createTime", LocalDateTime::class.java))))
            .join(b)
            .on(a.ATOM_CODE.eq(b.STORE_CODE))
            .leftJoin(d)
            .on(a.ID.eq(d.ATOM_ID))
            .where(conditions)
            .groupBy(a.ATOM_CODE)
            .orderBy(a.UPDATE_TIME.desc())
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    fun addMarketAtom(
        dslContext: DSLContext,
        userId: String,
        id: String,
        repositoryHashId: String?,
        codeSrc: String?,
        docsLink: String,
        marketAtomCreateRequest: MarketAtomCreateRequest
    ) {
        with(TAtom.T_ATOM) {
            dslContext.insertInto(this,
                ID,
                NAME,
                ATOM_CODE,
                CLASS_TYPE,
                SERVICE_SCOPE,
                OS,
                CLASSIFY_ID,
                DOCS_LINK,
                ATOM_TYPE,
                ATOM_STATUS,
                VERSION,
                DEFAULT_FLAG,
                LATEST_FLAG,
                REPOSITORY_HASH_ID,
                CODE_SRC,
                DOCS_LINK,
                VISIBILITY_LEVEL,
                PRIVATE_REASON,
                PUBLISHER,
                CREATOR,
                MODIFIER
            )
                .values(
                    id,
                    marketAtomCreateRequest.name,
                    marketAtomCreateRequest.atomCode,
                    "",
                    "",
                    "",
                    "",
                    "",
                    AtomTypeEnum.THIRD_PARTY.type.toByte(),
                    AtomStatusEnum.INIT.status.toByte(),
                    "",
                    false,
                    true,
                    repositoryHashId,
                    codeSrc,
                    docsLink,
                    marketAtomCreateRequest.visibilityLevel?.level,
                    marketAtomCreateRequest.privateReason,
                    userId,
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun updateMarketAtom(
        dslContext: DSLContext,
        userId: String,
        id: String,
        atomStatus: AtomStatusEnum,
        classType: String,
        props: String,
        iconData: String?,
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ) {
        val a = TClassify.T_CLASSIFY.`as`("a")
        val classifyId = dslContext.select(a.ID).from(a).where(a.CLASSIFY_CODE.eq(marketAtomUpdateRequest.classifyCode).and(a.TYPE.eq(0))).fetchOne(0, String::class.java)
        with(TAtom.T_ATOM) {
            dslContext.update(this)
                .set(NAME, marketAtomUpdateRequest.name)
                .set(JOB_TYPE, marketAtomUpdateRequest.jobType.name)
                .set(OS, JsonUtil.getObjectMapper().writeValueAsString(marketAtomUpdateRequest.os))
                .set(CLASSIFY_ID, classifyId)
                .set(SUMMARY, marketAtomUpdateRequest.summary)
                .set(DESCRIPTION, marketAtomUpdateRequest.description)
                .set(CATEGROY, marketAtomUpdateRequest.category.category.toByte())
                .set(VERSION, marketAtomUpdateRequest.version)
                .set(ATOM_STATUS, atomStatus.status.toByte())
                .set(PUBLISHER, marketAtomUpdateRequest.publisher)
                .set(CLASS_TYPE, classType)
                .set(PROPS, props)
                .set(LOGO_URL, marketAtomUpdateRequest.logoUrl)
                .set(ICON, iconData)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun upgradeMarketAtom(
        dslContext: DSLContext,
        userId: String,
        id: String,
        atomStatus: AtomStatusEnum,
        classType: String,
        props: String,
        iconData: String?,
        atomRecord: TAtomRecord,
        atomRequest: MarketAtomUpdateRequest
    ) {
        val a = TClassify.T_CLASSIFY.`as`("a")
        val classifyId = dslContext.select(a.ID).from(a).where(a.CLASSIFY_CODE.eq(atomRequest.classifyCode).and(a.TYPE.eq(0))).fetchOne(0, String::class.java)
        with(TAtom.T_ATOM) {
            dslContext.insertInto(this,
                ID,
                NAME,
                ATOM_CODE,
                SERVICE_SCOPE,
                JOB_TYPE,
                OS,
                CLASSIFY_ID,
                DOCS_LINK,
                ATOM_TYPE,
                ATOM_STATUS,
                ATOM_STATUS_MSG,
                SUMMARY,
                DESCRIPTION,
                CATEGROY,
                VERSION,
                ICON,
                DEFAULT_FLAG,
                LATEST_FLAG,
                REPOSITORY_HASH_ID,
                CODE_SRC,
                PAY_FLAG,
                PROPS,
                DATA,
                LOGO_URL,
                CLASS_TYPE,
                BUILD_LESS_RUN_FLAG,
                HTML_TEMPLATE_VERSION,
                VISIBILITY_LEVEL,
                PUBLISHER,
                WEIGHT,
                CREATOR,
                MODIFIER
            )
                .values(id,
                    atomRequest.name,
                    atomRecord.atomCode,
                    atomRecord.serviceScope,
                    atomRequest.jobType.name,
                    JsonUtil.getObjectMapper().writeValueAsString(atomRequest.os),
                    classifyId,
                    atomRecord.docsLink,
                    atomRecord.atomType,
                    atomStatus.status.toByte(),
                    "",
                    atomRequest.summary,
                    atomRequest.description,
                    atomRequest.category.category.toByte(),
                    atomRequest.version,
                    iconData,
                    atomRecord.defaultFlag,
                    false,
                    atomRecord.repositoryHashId,
                    atomRecord.codeSrc,
                    atomRecord.payFlag,
                    props,
                    atomRecord.data,
                    atomRequest.logoUrl,
                    classType,
                    atomRecord.buildLessRunFlag,
                    atomRecord.htmlTemplateVersion,
                    atomRecord.visibilityLevel,
                    atomRequest.publisher,
                    atomRecord.weight,
                    userId,
                    userId
                )
                .execute()
        }
    }

    /**
     * 更新原子props信息
     */
    fun updateMarketAtomProps(dslContext: DSLContext, atomId: String, props: String, userId: String) {
        with(TAtom.T_ATOM) {
            dslContext.update(this)
                .set(PROPS, props)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(atomId))
                .execute()
        }
    }

    fun deleteByAtomCode(dslContext: DSLContext, atomCode: String) {
        with(TAtom.T_ATOM) {
            dslContext.deleteFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .execute()
        }
    }

    fun getAtomsByAtomCode(dslContext: DSLContext, atomCode: String): Result<TAtomRecord>? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun getAtomRecordById(dslContext: DSLContext, atomId: String): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ID.eq(atomId))
                .fetchOne()
        }
    }

    fun getAtomById(dslContext: DSLContext, atomId: String): Record? {
        val a = TAtom.T_ATOM.`as`("a")
        val b = TAtomVersionLog.T_ATOM_VERSION_LOG.`as`("b")
        val c = TClassify.T_CLASSIFY.`as`("c")
        val d = TAtomEnvInfo.T_ATOM_ENV_INFO.`as`("d")

        return dslContext.select(
            a.ATOM_CODE.`as`("atomCode"),
            a.NAME.`as`("name"),
            a.LOGO_URL.`as`("logoUrl"),
            c.CLASSIFY_CODE.`as`("classifyCode"),
            c.CLASSIFY_NAME.`as`("classifyName"),
            a.CATEGROY.`as`("category"),
            a.DOCS_LINK.`as`("docsLink"),
            a.ATOM_TYPE.`as`("atomType"),
            a.JOB_TYPE.`as`("jobType"),
            a.OS.`as`("os"),
            a.SUMMARY.`as`("summary"),
            a.DESCRIPTION.`as`("description"),
            a.VERSION.`as`("version"),
            a.ATOM_STATUS.`as`("atomStatus"),
            a.ATOM_STATUS_MSG.`as`("atomStatusMsg"),
            a.CODE_SRC.`as`("codeSrc"),
            a.REPOSITORY_HASH_ID.`as`("repositoryHashId"),
            a.HTML_TEMPLATE_VERSION.`as`("htmlTemplateVersion"),
            a.PUBLISHER.`as`("publisher"),
            a.CREATOR.`as`("creator"),
            a.CREATE_TIME.`as`("createTime"),
            a.MODIFIER.`as`("modifier"),
            a.UPDATE_TIME.`as`("updateTime"),
            a.DEFAULT_FLAG.`as`("defaultFlag"),
            b.RELEASE_TYPE.`as`("releaseType"),
            b.CONTENT.`as`("versionContent"),
            d.LANGUAGE.`as`("language"),
            a.VISIBILITY_LEVEL.`as`("visibilityLevel"),
            a.PRIVATE_REASON.`as`("privateReason")
        )
            .from(a)
            .leftJoin(b)
            .on(a.ID.eq(b.ATOM_ID))
            .leftJoin(c)
            .on(a.CLASSIFY_ID.eq(c.ID))
            .leftJoin(d)
            .on(a.ID.eq(d.ATOM_ID))
            .where(a.ID.eq(atomId))
            .fetchOne()
    }

    /**
     * 设置原子状态（单个版本）
     */
    fun setAtomStatusById(dslContext: DSLContext, atomId: String, atomStatus: Byte, userId: String, msg: String?) {
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.update(this)
                .set(ATOM_STATUS, atomStatus)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(ATOM_STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(atomId))
                .execute()
        }
    }

    /**
     * 设置可用的原子版本状态为下架中、已下架
     */
    fun setAtomStatusByCode(
        dslContext: DSLContext,
        atomCode: String,
        atomOldStatus: Byte,
        atomNewStatus: Byte,
        userId: String,
        msg: String?,
        latestFlag: Boolean?
    ) {
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.update(this)
                .set(ATOM_STATUS, atomNewStatus)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(ATOM_STATUS_MSG, msg)
            }
            if (null != latestFlag) {
                baseStep.set(LATEST_FLAG, latestFlag)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ATOM_CODE.eq(atomCode))
                .and(ATOM_STATUS.eq(atomOldStatus))
                .execute()
        }
    }

    fun updateAtomInfoById(dslContext: DSLContext, userId: String, atomId: String, updateAtomInfo: UpdateAtomInfo) {
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.update(this)
            setUpdateAtomInfo(updateAtomInfo, baseStep)
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(atomId))
                .execute()
        }
    }

    fun updateAtomInfoByCode(dslContext: DSLContext, userId: String, atomCode: String, updateAtomInfo: UpdateAtomInfo) {
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.update(this)
            setUpdateAtomInfo(updateAtomInfo, baseStep)
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ATOM_CODE.eq(atomCode))
                .execute()
        }
    }

    private fun TAtom.setUpdateAtomInfo(updateAtomInfo: UpdateAtomInfo, baseStep: UpdateSetFirstStep<TAtomRecord>) {
        val atomStatus = updateAtomInfo.atomStatus
        if (null != atomStatus) {
            baseStep.set(ATOM_STATUS, atomStatus)
        }
        val msg = updateAtomInfo.atomStatusMsg
        if (!msg.isNullOrEmpty()) {
            baseStep.set(ATOM_STATUS_MSG, msg)
        }
        val latestFlag = updateAtomInfo.latestFlag
        if (null != latestFlag) {
            baseStep.set(LATEST_FLAG, latestFlag)
        }
        val pubTime = updateAtomInfo.pubTime
        if (null != pubTime) {
            baseStep.set(PUB_TIME, pubTime)
        }
        val deleteFlag = updateAtomInfo.deleteFlag
        if (null != deleteFlag) {
            baseStep.set(DELETE_FLAG, deleteFlag)
        }
    }

    /**
     * 清空LATEST_FLAG
     */
    fun cleanLatestFlag(dslContext: DSLContext, atomCode: String) {
        with(TAtom.T_ATOM) {
            dslContext.update(this)
                .set(LATEST_FLAG, false)
                .where(ATOM_CODE.eq(atomCode))
                .execute()
        }
    }

    /**
     * 审核原子时，更新状态、类型等信息
     */
    fun approveAtomFromOp(
        dslContext: DSLContext,
        userId: String,
        atomId: String,
        atomStatus: Byte,
        approveReq: ApproveReq,
        latestFlag: Boolean,
        pubTime: LocalDateTime? = null
    ) {
        with(TAtom.T_ATOM) {
            dslContext.update(this)
                .set(ATOM_STATUS, atomStatus)
                .set(ATOM_STATUS_MSG, approveReq.message)
                .set(ATOM_TYPE, approveReq.atomType.type.toByte())
                .set(DEFAULT_FLAG, approveReq.defaultFlag)
                .set(WEIGHT, approveReq.weight)
                .set(BUILD_LESS_RUN_FLAG, approveReq.buildLessRunFlag)
                .set(SERVICE_SCOPE, JsonUtil.getObjectMapper().writeValueAsString(approveReq.serviceScope))
                .set(LATEST_FLAG, latestFlag)
                .set(PUB_TIME, pubTime)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(atomId))
                .execute()
        }
    }

    fun getNewestUndercarriagedAtomsByCode(dslContext: DSLContext, atomCode: String): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .and(ATOM_STATUS.eq(AtomStatusEnum.UNDERCARRIAGED.status.toByte()))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getReleaseAtomsByCode(dslContext: DSLContext, atomCode: String, num: Int? = null): Result<TAtomRecord>? {
        return with(TAtom.T_ATOM) {
            val baseStep = dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .and(ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
                .orderBy(CREATE_TIME.desc())
            if (null != num) {
                baseStep.limit(num)
            }
            baseStep.fetch()
        }
    }
}