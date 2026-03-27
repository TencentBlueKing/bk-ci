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

package com.tencent.devops.store.atom.dao

import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomEnvInfo
import com.tencent.devops.model.store.tables.TAtomFeature
import com.tencent.devops.model.store.tables.TAtomLabelRel
import com.tencent.devops.model.store.tables.TAtomVersionLog
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.atom.util.AtomJobTypeUtil
import com.tencent.devops.store.atom.util.AtomOsMapUtil
import com.tencent.devops.store.utils.VersionUtils
import com.tencent.devops.store.pojo.atom.ApproveReq
import com.tencent.devops.store.pojo.atom.MarketAtomDaoQuery
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.UpdateAtomInfo
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.ServiceScopeConfig
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
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

@Suppress("ALL")
@Repository
class MarketAtomDao : AtomBaseDao() {

    /**
     * 插件商店搜索结果，总数
     */
    fun count(dslContext: DSLContext, query: MarketAtomDaoQuery): Int {
        val (ta, conditions) = formatConditions(query)
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
            query = query,
            storeType = storeType
        )
        return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
    }

    private fun formatConditions(
        query: MarketAtomDaoQuery
    ): Pair<TAtom, MutableList<Condition>> {
        val ta = TAtom.T_ATOM
        val conditions = setAtomVisibleCondition(ta).apply {
            add(ta.DELETE_FLAG.eq(false))
            query.serviceScope?.let { add(buildServiceScopeCondition(ta, it)) }
            // 关键字模糊搜索：匹配名称、简介或插件代码
            query.keyword?.takeIf { it.isNotEmpty() }?.let { keyword ->
                add(
                    ta.NAME.contains(keyword)
                        .or(ta.SUMMARY.contains(keyword))
                        .or(ta.ATOM_CODE.contains(keyword))
                )
            }
            // 研发类型过滤
            query.rdType?.let { add(ta.ATOM_TYPE.eq(it.type.toByte())) }
            // 分类过滤：根据 classifyId 和 serviceScope 构建分类条件
            query.classifyId?.let {
                buildClassifyCondition(ta, it, query.serviceScope)?.let(::add)
            }
        }
        return ta to conditions
    }

    /**
     * 插件商店搜索结果列表
     */
    fun list(dslContext: DSLContext, query: MarketAtomDaoQuery): Result<out Record>? {
        val (ta, conditions) = formatConditions(query)
        val taf = TAtomFeature.T_ATOM_FEATURE
        val classifyIdField = buildClassifyIdField(ta, query.serviceScope)
        val baseStep = dslContext.select(
            ta.ID, ta.NAME, ta.JOB_TYPE, ta.ATOM_TYPE,
            classifyIdField.`as`("CLASSIFY_ID"),
            ta.CATEGROY, ta.ATOM_CODE, ta.VERSION, ta.ATOM_STATUS,
            ta.LOGO_URL, ta.PUBLISHER, ta.SUMMARY, ta.DEFAULT_FLAG,
            ta.OS, ta.OS_MAP, ta.BUILD_LESS_RUN_FLAG, ta.DOCS_LINK,
            ta.MODIFIER, ta.UPDATE_TIME,
            taf.RECOMMEND_FLAG, taf.YAML_FLAG
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
            query = query,
            storeType = storeType
        )

        // 排序处理
        val statSortTypes = setOf(
            MarketAtomSortTypeEnum.DOWNLOAD_COUNT,
            MarketAtomSortTypeEnum.RECENT_EXECUTE_NUM
        )
        query.sortType?.let { sortType ->
            val isStatSortType = sortType in statSortTypes
            // 统计排序且未按评分过滤时，需额外关联统计表
            if (isStatSortType && query.score == null) {
                val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
                val t = dslContext.select(
                    tas.STORE_CODE,
                    tas.DOWNLOADS.`as`(MarketAtomSortTypeEnum.DOWNLOAD_COUNT.name),
                    tas.RECENT_EXECUTE_NUM.`as`(MarketAtomSortTypeEnum.RECENT_EXECUTE_NUM.name)
                ).from(tas).where(tas.STORE_TYPE.eq(storeType)).asTable("t")
                baseStep.leftJoin(t).on(ta.ATOM_CODE.eq(t.field("STORE_CODE", String::class.java)))
            }

            val realSortType = if (isStatSortType) DSL.field(sortType.name) else ta.field(sortType.name)
            val orderField = if (query.desc == true) realSortType!!.desc() else realSortType!!.asc()
            baseStep.where(conditions).orderBy(orderField)
        } ?: baseStep.where(conditions)

        // 分页返回
        return query.page?.let { page ->
            query.pageSize?.let { pageSize ->
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            }
        } ?: baseStep.fetch()
    }

    private fun handleMainListBaseStep(
        dslContext: DSLContext,
        ta: TAtom,
        taf: TAtomFeature,
        baseStep: SelectOnConditionStep<out Record>,
        conditions: MutableList<Condition>,
        query: MarketAtomDaoQuery,
        storeType: Byte
    ) {
        // 标签过滤：关联标签关系表，筛选匹配指定标签的插件
        query.labelIdList?.takeIf { it.isNotEmpty() }?.let { labelIds ->
            val talr = TAtomLabelRel.T_ATOM_LABEL_REL
            baseStep.leftJoin(talr).on(ta.ID.eq(talr.ATOM_ID))
            conditions.add(talr.LABEL_ID.`in`(labelIds))
        }
        // 评分过滤：关联统计表，筛选平均评分达标的插件
        query.score?.let { score ->
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.STORE_TYPE,
                tas.DOWNLOADS.`as`(MarketAtomSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.RECENT_EXECUTE_NUM.`as`(MarketAtomSortTypeEnum.RECENT_EXECUTE_NUM.name),
                tas.SCORE_AVERAGE
            ).from(tas)
            baseStep.leftJoin(t).on(ta.ATOM_CODE.eq(t.field(tas.STORE_CODE.name, String::class.java)))
            conditions.add(
                t.field(tas.SCORE_AVERAGE.name, BigDecimal::class.java)!!
                    .ge(BigDecimal.valueOf(score.toLong()))
            )
            conditions.add(t.field(tas.STORE_TYPE.name, Byte::class.java)!!.eq(storeType))
        }
        // 特性标志过滤
        query.yamlFlag?.let { conditions.add(taf.YAML_FLAG.eq(it)) }
        query.recommendFlag?.let { conditions.add(taf.RECOMMEND_FLAG.eq(it)) }
        query.qualityFlag?.let { conditions.add(taf.QUALITY_FLAG.eq(it)) }
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
            tAtom.UPDATE_TIME,
            tAtom.CODE_SRC
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
            dslContext.insertInto(
                this,
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
                    "[ \"${ServiceScopeEnum.PIPELINE.name}\" ]",
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
        val serviceScopeConfigs: List<ServiceScopeConfig> = marketAtomUpdateRequest.toServiceScopeConfigs()
        val jobTypeResult = AtomJobTypeUtil.buildJobTypeFields(serviceScopeConfigs, marketAtomUpdateRequest.jobType?.name)
        val osWriteResult = AtomOsMapUtil.buildOsFields(serviceScopeConfigs, marketAtomUpdateRequest.os)

        val classifyIdMap = buildClassifyIdMap(
            dslContext = dslContext,
            serviceScopeConfigs = serviceScopeConfigs
        )
        val pipelineClassifyCode = serviceScopeConfigs
            .firstOrNull { it.serviceScope == ServiceScopeEnum.PIPELINE }?.classifyCode
            ?: marketAtomUpdateRequest.classifyCode
        val classifyId = getClassifyIdByCode(
            dslContext = dslContext,
            classifyCode = pipelineClassifyCode,
            serviceScope = ServiceScopeEnum.PIPELINE
        ) ?: classifyIdMap?.get(ServiceScopeEnum.PIPELINE.name)
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.update(this)
                .set(NAME, marketAtomUpdateRequest.name)
                .set(OS, JsonUtil.toJson(osWriteResult.pipelineOs, formatted = false))
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
            classifyId?.let {
                baseStep.set(CLASSIFY_ID, it)
            }
            classifyIdMap?.let {
                baseStep.set(CLASSIFY_ID_MAP, JsonUtil.toJson(it, formatted = false))
            }
            if (jobTypeResult.jobTypeMapJson != null) {
                baseStep.set(JOB_TYPE, jobTypeResult.pipelineJobType)
                baseStep.set(JOB_TYPE_MAP, jobTypeResult.jobTypeMapJson)
            } else {
                jobTypeResult.pipelineJobType?.let {
                    baseStep.set(JOB_TYPE, it)
                }
            }
            baseStep.set(OS_MAP, osWriteResult.osMapJson)
            val serviceScopeJson = JsonUtil.toJson(
                serviceScopeConfigs.map { it.serviceScope.name },
                formatted = false
            )
            baseStep.set(SERVICE_SCOPE, serviceScopeJson)
            baseStep.where(ID.eq(id)).execute()
        }
    }

    /**
     * 构建 CLASSIFY_ID_MAP：以用户传入的 serviceScopeConfigs 为准，从零构建新的 map。
     * 插件升级时，CLASSIFY_ID_MAP 的值应完全以用户传入的服务范围配置为准，
     * 不再保留旧 map 中多余的 scope 条目。
     */
    private fun buildClassifyIdMap(
        dslContext: DSLContext,
        serviceScopeConfigs: List<ServiceScopeConfig>?
    ): Map<String, String>? {
        if (serviceScopeConfigs.isNullOrEmpty()) {
            return null
        }
        val classifyIdMap = mutableMapOf<String, String>()
        for (config in serviceScopeConfigs) {
            val classifyId = getClassifyIdByCode(
                dslContext = dslContext,
                classifyCode = config.classifyCode,
                serviceScope = config.serviceScope
            )
            classifyId?.let {
                classifyIdMap[config.serviceScope.name] = it
            }
        }
        return classifyIdMap.ifEmpty { null }
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
        val serviceScopeConfigs: List<ServiceScopeConfig> = atomRequest.toServiceScopeConfigs()

        val pipelineClassifyCode = serviceScopeConfigs
            .firstOrNull { it.serviceScope == ServiceScopeEnum.PIPELINE }?.classifyCode
            ?: atomRequest.classifyCode
        val classifyId = getClassifyIdByCode(
            dslContext = dslContext,
            classifyCode = pipelineClassifyCode,
            serviceScope = ServiceScopeEnum.PIPELINE
        ) ?: atomRecord.classifyId

        val currentJobType = atomRecord.jobType
        val jobTypeResult = AtomJobTypeUtil.buildJobTypeFields(serviceScopeConfigs, currentJobType)
        val currentJobTypeMap = atomRecord.jobTypeMap
        val osWriteResult = AtomOsMapUtil.buildOsFields(serviceScopeConfigs, atomRequest.os)
        val serviceScopeJson = JsonUtil.toJson(
            serviceScopeConfigs.map { it.serviceScope.name },
            formatted = false
        )

        val classifyIdMap = buildClassifyIdMap(
            dslContext = dslContext,
            serviceScopeConfigs = serviceScopeConfigs
        )
        val classifyIdMapJson = classifyIdMap?.let { JsonUtil.toJson(it, formatted = false) }
            ?: atomRecord.classifyIdMap

        with(TAtom.T_ATOM) {
            dslContext.insertInto(
                this,
                ID,
                NAME,
                ATOM_CODE,
                SERVICE_SCOPE,
                JOB_TYPE,
                JOB_TYPE_MAP,
                OS,
                OS_MAP,
                CLASSIFY_ID,
                CLASSIFY_ID_MAP,
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
                MODIFIER,
                BRANCH_TEST_FLAG
            )
                .values(
                    id,
                    atomRequest.name,
                    atomRecord.atomCode,
                    serviceScopeJson,
                    if (jobTypeResult.jobTypeMapJson != null) {
                        jobTypeResult.pipelineJobType
                    } else {
                        jobTypeResult.pipelineJobType ?: currentJobType
                    },
                    jobTypeResult.jobTypeMapJson ?: currentJobTypeMap,
                    JsonUtil.toJson(osWriteResult.pipelineOs, formatted = false),
                    osWriteResult.osMapJson,
                    classifyId,
                    classifyIdMapJson,
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
                    userId,
                    atomRequest.isBranchTestVersion
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
                .and(BRANCH_TEST_FLAG.eq(false))
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

    fun getAtomBranchTestVersion(
        dslContext: DSLContext,
        atomCode: String,
        versionPrefix: String
    ): TAtomRecord? {
        with(TAtom.T_ATOM) {
            return dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .and(VERSION.startsWith(versionPrefix))
                .and(ATOM_STATUS.eq(AtomStatusEnum.TESTING.status.toByte()))
                .orderBy(UPDATE_TIME.desc())
                .fetchOne()
        }
    }

    fun getAtomById(
        dslContext: DSLContext,
        atomId: String,
        serviceScope: ServiceScopeEnum? = null
    ): Record? {
        val tAtom = TAtom.T_ATOM
        val tAtomVersionLog = TAtomVersionLog.T_ATOM_VERSION_LOG
        val tClassify = TClassify.T_CLASSIFY
        return dslContext.select(
            tAtom.ATOM_CODE,
            tAtom.NAME,
            tAtom.LOGO_URL,
            tAtom.CLASSIFY_ID,
            tClassify.CLASSIFY_CODE,
            tClassify.CLASSIFY_NAME,
            tAtom.CATEGROY,
            tAtom.DOCS_LINK,
            tAtom.ATOM_TYPE,
            tAtom.JOB_TYPE,
            tAtom.OS,
            tAtom.OS_MAP,
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
            tAtom.PRIVATE_REASON,
            tAtom.SERVICE_SCOPE,
            tAtom.CLASSIFY_ID_MAP,
            tAtom.JOB_TYPE_MAP
        )
            .from(tAtom)
            .leftJoin(tAtomVersionLog)
            .on(tAtom.ID.eq(tAtomVersionLog.ATOM_ID))
            .leftJoin(tClassify)
            .on(buildClassifyJoinCondition(tAtom, tClassify, serviceScope))
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

    fun setupAtomLatestTestFlagById(
        dslContext: DSLContext,
        userId: String,
        atomId: String,
        latestFlag: Boolean
    ) {
        with(TAtom.T_ATOM) {
            dslContext.update(this)
                .set(LATEST_TEST_FLAG, latestFlag)
                .set(MODIFIER, userId)
                .where(ID.eq(atomId))
                .execute()
        }
    }

    fun resetAtomLatestTestFlagByCode(
        dslContext: DSLContext,
        atomCode: String
    ) {
        with(TAtom.T_ATOM) {
            dslContext.update(this)
                .set(LATEST_TEST_FLAG, false)
                .where(ATOM_CODE.eq(atomCode))
                .execute()
        }
    }

    fun queryAtomLatestTestVersionId(dslContext: DSLContext, atomCode: String, atomId: String): String? {
        with(TAtom.T_ATOM) {
            return dslContext.select(ID).from(this)
                .where(ATOM_CODE.eq(atomCode).and(ID.notEqual(atomId)))
                .and(
                    ATOM_STATUS.`in`(
                        listOf(AtomStatusEnum.TESTING.status.toByte(), AtomStatusEnum.AUDITING.status.toByte())
                    )
                )
                .orderBy(UPDATE_TIME.desc())
                .limit(1)
                .fetchOne(0, String::class.java)
        }
    }

    fun isAtomLatestTestVersion(dslContext: DSLContext, atomId: String): Int {
        with(TAtom.T_ATOM) {
            return dslContext.select(ID).from(this)
                .where(ID.eq(atomId).and(LATEST_TEST_FLAG.eq(true))).execute()
        }
    }

    private fun TAtom.setUpdateAtomInfo(updateAtomInfo: UpdateAtomInfo, baseStep: UpdateSetFirstStep<TAtomRecord>) {
        updateAtomInfo.atomStatus?.let { baseStep.set(ATOM_STATUS, it) }
        updateAtomInfo.atomStatusMsg?.takeIf { it.isNotEmpty() }?.let { baseStep.set(ATOM_STATUS_MSG, it) }
        updateAtomInfo.latestFlag?.let { baseStep.set(LATEST_FLAG, it) }
        updateAtomInfo.pubTime?.let { baseStep.set(PUB_TIME, it) }
        updateAtomInfo.deleteFlag?.let { baseStep.set(DELETE_FLAG, it) }
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
        approveReq: ApproveReq
    ) {
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.update(this)
                .set(ATOM_STATUS, atomStatus)
                .set(ATOM_STATUS_MSG, approveReq.message)
                .set(ATOM_TYPE, approveReq.atomType.type.toByte())
                .set(DEFAULT_FLAG, approveReq.defaultFlag)
                .set(SERVICE_SCOPE, JsonUtil.toJson(approveReq.serviceScope, formatted = false))
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
            approveReq.weight?.let { baseStep.set(WEIGHT, it) }
            approveReq.buildLessRunFlag?.let { baseStep.set(BUILD_LESS_RUN_FLAG, it) }
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
