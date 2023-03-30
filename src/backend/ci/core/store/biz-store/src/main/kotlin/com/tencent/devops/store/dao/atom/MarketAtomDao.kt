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

package com.tencent.devops.store.dao.atom

import com.tencent.devops.common.api.constant.INIT_VERSION
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
import com.tencent.devops.store.utils.VersionUtils
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

@Suppress("ALL")
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
        recommendFlag: Boolean?,
        qualityFlag: Boolean?
    ): Int {
        val (ta, conditions) = formatConditions(keyword, rdType, classifyCode, dslContext)
        val taf = TAtomFeature.T_ATOM_FEATURE
        val baseStep = dslContext.select(DSL.countDistinct(ta.ID)).from(ta).leftJoin(taf)
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
            recommendFlag = recommendFlag,
            qualityFlag = qualityFlag
        )
        return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
    }

    private fun formatConditions(
        keyword: String?,
        rdType: AtomTypeEnum?,
        classifyCode: String?,
        dslContext: DSLContext
    ): Pair<TAtom, MutableList<Condition>> {
        val ta = TAtom.T_ATOM
        val storeType = StoreTypeEnum.ATOM.type.toByte()
        val conditions = setAtomVisibleCondition(ta)
        conditions.add(ta.DELETE_FLAG.eq(false)) // 只查没有被删除的插件
        if (!keyword.isNullOrEmpty()) {
            conditions.add(
                ta.NAME.contains(keyword)
                .or(ta.SUMMARY.contains(keyword))
                .or(ta.ATOM_CODE.contains(keyword))
            )
        }
        if (rdType != null) {
            conditions.add(ta.ATOM_TYPE.eq(rdType.type.toByte()))
        }
        if (!classifyCode.isNullOrEmpty()) {
            val tClassify = TClassify.T_CLASSIFY
            val classifyId = dslContext.select(tClassify.ID)
                .from(tClassify)
                .where(tClassify.CLASSIFY_CODE.eq(classifyCode).and(tClassify.TYPE.eq(storeType)))
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
        qualityFlag: Boolean?,
        sortType: MarketAtomSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val (ta, conditions) = formatConditions(keyword, rdType, classifyCode, dslContext)
        val taf = TAtomFeature.T_ATOM_FEATURE
        val baseStep = dslContext.select(
            ta.ID,
            ta.NAME,
            ta.JOB_TYPE,
            ta.ATOM_TYPE,
            ta.CLASSIFY_ID,
            ta.CATEGROY,
            ta.ATOM_CODE,
            ta.VERSION,
            ta.LOGO_URL,
            ta.PUBLISHER,
            ta.SUMMARY,
            ta.DEFAULT_FLAG,
            ta.OS,
            ta.BUILD_LESS_RUN_FLAG,
            ta.DOCS_LINK,
            ta.MODIFIER,
            ta.UPDATE_TIME,
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
            recommendFlag = recommendFlag,
            qualityFlag = qualityFlag
        )

        if (null != sortType) {
            val flag =
                sortType == MarketAtomSortTypeEnum.DOWNLOAD_COUNT || sortType == MarketAtomSortTypeEnum.RECENT_EXECUTE_NUM
            if (flag && score == null) {
                val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
                val t =
                    dslContext.select(
                        tas.STORE_CODE,
                        tas.DOWNLOADS.`as`(MarketAtomSortTypeEnum.DOWNLOAD_COUNT.name),
                        tas.RECENT_EXECUTE_NUM.`as`(MarketAtomSortTypeEnum.RECENT_EXECUTE_NUM.name)
                    )
                        .from(tas).where(tas.STORE_TYPE.eq(storeType)).asTable("t")
                baseStep.leftJoin(t).on(ta.ATOM_CODE.eq(t.field("STORE_CODE", String::class.java)))
            }

            val realSortType =
                if (flag) {
                    DSL.field(sortType.name)
                } else {
                    ta.field(sortType.name)
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
        recommendFlag: Boolean?,
        qualityFlag: Boolean?
    ) {
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val tLabel = TLabel.T_LABEL
            val labelIdList = dslContext.select(tLabel.ID)
                .from(tLabel)
                .where(tLabel.LABEL_CODE.`in`(labelCodeList)).and(tLabel.TYPE.eq(storeType))
                .fetch().map { it["ID"] as String }
            val talr = TAtomLabelRel.T_ATOM_LABEL_REL
            baseStep.leftJoin(talr).on(ta.ID.eq(talr.ATOM_ID))
            conditions.add(talr.LABEL_ID.`in`(labelIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.STORE_TYPE,
                tas.DOWNLOADS.`as`(MarketAtomSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.RECENT_EXECUTE_NUM.`as`(MarketAtomSortTypeEnum.RECENT_EXECUTE_NUM.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(ta.ATOM_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java)!!.ge(BigDecimal.valueOf(score.toLong())))
            conditions.add(t.field("STORE_TYPE", Byte::class.java)!!.eq(storeType))
        }
        if (null != yamlFlag) {
            conditions.add(taf.YAML_FLAG.eq(yamlFlag))
        }
        if (null != recommendFlag) {
            conditions.add(taf.RECOMMEND_FLAG.eq(recommendFlag))
        }
        if (null != qualityFlag) {
            conditions.add(taf.QUALITY_FLAG.eq(qualityFlag))
        }
    }

    fun countReleaseAtomByCode(dslContext: DSLContext, atomCode: String, version: String? = null): Int {
        with(TAtom.T_ATOM) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_CODE.eq(atomCode))
            conditions.add(ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
            if (version != null) {
                conditions.add(VERSION.like(VersionUtils.generateQueryVersion(version)))
            }
            return dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    private fun generateGetMyAtomConditions(
        tAtom: TAtom,
        userId: String,
        tStoreMember: TStoreMember,
        atomName: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(tAtom.DELETE_FLAG.eq(false)) // 只查没有被删除的插件
        conditions.add(tAtom.LATEST_FLAG.eq(true))
        conditions.add(tStoreMember.USERNAME.eq(userId))
        conditions.add(tStoreMember.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        if (null != atomName) {
            conditions.add(tAtom.NAME.contains(atomName))
        }
        return conditions
    }

    fun countMyAtoms(
        dslContext: DSLContext,
        userId: String,
        atomName: String?
    ): Int {
        val tAtom = TAtom.T_ATOM
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val conditions = generateGetMyAtomConditions(tAtom, userId, tStoreMember, atomName)
        return dslContext.select(DSL.countDistinct(tAtom.ATOM_CODE))
            .from(tAtom)
            .leftJoin(tStoreMember)
            .on(tAtom.ATOM_CODE.eq(tStoreMember.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Int::class.java)!!
    }

    fun getMyAtoms(
        dslContext: DSLContext,
        userId: String,
        atomName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val tAtom = TAtom.T_ATOM
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val tAtomEnvInfo = TAtomEnvInfo.T_ATOM_ENV_INFO
        val conditions = generateGetMyAtomConditions(tAtom, userId, tStoreMember, atomName)
        val baseStep = dslContext.select(
            tAtom.ID,
            tAtom.ATOM_CODE,
            tAtom.NAME,
            tAtom.CATEGROY,
            tAtomEnvInfo.LANGUAGE,
            tAtom.LOGO_URL,
            tAtom.VERSION,
            tAtom.ATOM_STATUS,
            tAtom.CREATOR,
            tAtom.CREATE_TIME,
            tAtom.MODIFIER,
            tAtom.UPDATE_TIME
        )
            .from(tAtom)
            .join(tStoreMember)
            .on(tAtom.ATOM_CODE.eq(tStoreMember.STORE_CODE))
            .leftJoin(tAtomEnvInfo)
            .on(tAtom.ID.eq(tAtomEnvInfo.ATOM_ID))
            .where(conditions)
            .groupBy(tAtom.ID)
            .orderBy(tAtom.UPDATE_TIME.desc())
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
                HTML_TEMPLATE_VERSION,
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
                    INIT_VERSION,
                    false,
                    true,
                    repositoryHashId,
                    codeSrc,
                    docsLink,
                    marketAtomCreateRequest.visibilityLevel?.level,
                    marketAtomCreateRequest.privateReason,
                    marketAtomCreateRequest.frontendType.typeVersion,
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
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ) {
        val a = TClassify.T_CLASSIFY.`as`("a")
        val classifyId = dslContext.select(a.ID)
            .from(a)
            .where(a.CLASSIFY_CODE.eq(marketAtomUpdateRequest.classifyCode)
                .and(a.TYPE.eq(0)))
            .fetchOne(0, String::class.java)
        with(TAtom.T_ATOM) {
            dslContext.update(this)
                .set(NAME, marketAtomUpdateRequest.name)
                .set(JOB_TYPE, marketAtomUpdateRequest.jobType.name)
                .set(OS, JsonUtil.toJson(marketAtomUpdateRequest.os, formatted = false))
                .set(CLASSIFY_ID, classifyId)
                .set(SUMMARY, marketAtomUpdateRequest.summary)
                .set(DESCRIPTION, marketAtomUpdateRequest.description)
                .set(CATEGROY, marketAtomUpdateRequest.category.category.toByte())
                .set(VERSION, marketAtomUpdateRequest.version)
                .set(ATOM_STATUS, atomStatus.status.toByte())
                .set(PUBLISHER, marketAtomUpdateRequest.publisher)
                .set(CLASS_TYPE, classType)
                .set(PROPS, JsonUtil.toJson(props, formatted = false))
                .set(LOGO_URL, marketAtomUpdateRequest.logoUrl)
                .set(HTML_TEMPLATE_VERSION, marketAtomUpdateRequest.frontendType.typeVersion)
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
        atomRecord: TAtomRecord,
        atomRequest: MarketAtomUpdateRequest
    ) {
        val a = TClassify.T_CLASSIFY.`as`("a")
        val classifyId = dslContext.select(a.ID)
            .from(a)
            .where(a.CLASSIFY_CODE.eq(atomRequest.classifyCode)
                .and(a.TYPE.eq(0)))
            .fetchOne(0, String::class.java)
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
                DEFAULT_FLAG,
                LATEST_FLAG,
                REPOSITORY_HASH_ID,
                BRANCH,
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
                    JsonUtil.toJson(atomRequest.os, formatted = false),
                    classifyId,
                    atomRecord.docsLink,
                    atomRecord.atomType,
                    atomStatus.status.toByte(),
                    "",
                    atomRequest.summary,
                    atomRequest.description,
                    atomRequest.category.category.toByte(),
                    atomRequest.version,
                    atomRecord.defaultFlag,
                    false,
                    atomRecord.repositoryHashId,
                    atomRequest.branch ?: atomRecord.branch,
                    atomRecord.codeSrc,
                    atomRecord.payFlag,
                    JsonUtil.toJson(props, formatted = false),
                    atomRecord.data,
                    atomRequest.logoUrl,
                    classType,
                    atomRecord.buildLessRunFlag,
                    atomRequest.frontendType.typeVersion,
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

    fun getAtomsByAtomCode(
        dslContext: DSLContext,
        atomCode: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<TAtomRecord>? {
        return with(TAtom.T_ATOM) {
            val baseStep = dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .orderBy(CREATE_TIME.desc())
            if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    fun getAtomsByConditions(
        dslContext: DSLContext,
        atomCodeList: List<String>,
        atomStatusList: List<Byte>? = null
    ): Result<TAtomRecord>? {
        return with(TAtom.T_ATOM) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_CODE.`in`(atomCodeList))
            if (atomStatusList != null) {
                conditions.add(ATOM_STATUS.`in`(atomStatusList))
            }
            dslContext.selectFrom(this).where(conditions).orderBy(CREATE_TIME.desc()).fetch()
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
        val tAtom = TAtom.T_ATOM
        val tAtomVersionLog = TAtomVersionLog.T_ATOM_VERSION_LOG
        val tClassify = TClassify.T_CLASSIFY

        return dslContext.select(
            tAtom.ATOM_CODE,
            tAtom.NAME,
            tAtom.LOGO_URL,
            tClassify.CLASSIFY_CODE,
            tClassify.CLASSIFY_NAME,
            tAtom.CATEGROY,
            tAtom.DOCS_LINK,
            tAtom.ATOM_TYPE,
            tAtom.JOB_TYPE,
            tAtom.OS,
            tAtom.SUMMARY,
            tAtom.DESCRIPTION,
            tAtom.VERSION,
            tAtom.ATOM_STATUS,
            tAtom.ATOM_STATUS_MSG,
            tAtom.CODE_SRC,
            tAtom.REPOSITORY_HASH_ID,
            tAtom.HTML_TEMPLATE_VERSION,
            tAtom.PUBLISHER,
            tAtom.CREATOR,
            tAtom.CREATE_TIME,
            tAtom.MODIFIER,
            tAtom.UPDATE_TIME,
            tAtom.DEFAULT_FLAG,
            tAtomVersionLog.RELEASE_TYPE,
            tAtomVersionLog.CONTENT,
            tAtom.VISIBILITY_LEVEL,
            tAtom.PRIVATE_REASON
        )
            .from(tAtom)
            .leftJoin(tAtomVersionLog)
            .on(tAtom.ID.eq(tAtomVersionLog.ATOM_ID))
            .leftJoin(tClassify)
            .on(tAtom.CLASSIFY_ID.eq(tClassify.ID))
            .where(tAtom.ID.eq(atomId))
            .limit(1)
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
        latestFlag: Boolean? = null,
        pubTime: LocalDateTime? = null
    ) {
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.update(this)
                .set(ATOM_STATUS, atomStatus)
                .set(ATOM_STATUS_MSG, approveReq.message)
                .set(ATOM_TYPE, approveReq.atomType.type.toByte())
                .set(DEFAULT_FLAG, approveReq.defaultFlag)
                .set(BUILD_LESS_RUN_FLAG, approveReq.buildLessRunFlag)
                .set(SERVICE_SCOPE, JsonUtil.toJson(approveReq.serviceScope, formatted = false))
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
            val weight = approveReq.weight
            if (null != weight) {
                baseStep.set(WEIGHT, weight)
            }
            val buildLessRunFlag = approveReq.buildLessRunFlag
            if (null != buildLessRunFlag) {
                baseStep.set(BUILD_LESS_RUN_FLAG, buildLessRunFlag)
            }
            if (null != latestFlag) {
                baseStep.set(LATEST_FLAG, latestFlag)
            }
            if (null != pubTime) {
                baseStep.set(PUB_TIME, pubTime)
            }
            baseStep.where(ID.eq(atomId)).execute()
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

    fun listAtomByStatus(
        dslContext: DSLContext,
        atomStatus: Byte,
        page: Int?,
        pageSize: Int?,
        timeDescFlag: Boolean? = null
    ): Result<TAtomRecord>? {
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.selectFrom(this)
                .where(DELETE_FLAG.eq(false)).and(ATOM_STATUS.eq(atomStatus))
            if (timeDescFlag != null && timeDescFlag) {
                baseStep.orderBy(CREATE_TIME.desc())
            } else {
                baseStep.orderBy(CREATE_TIME.asc())
            }
            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }
}
