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
import com.tencent.devops.common.api.constant.KEY_ALL
import com.tencent.devops.common.api.constant.KEY_BRANCH_TEST_FLAG
import com.tencent.devops.common.api.constant.KEY_DESCRIPTION
import com.tencent.devops.common.api.constant.KEY_DOCSLINK
import com.tencent.devops.common.api.constant.KEY_OS
import com.tencent.devops.common.api.constant.KEY_SUMMARY
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.KEY_WEIGHT
import com.tencent.devops.common.api.constant.NAME
import com.tencent.devops.common.api.constant.VERSION
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomFeature
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.repository.pojo.AtomRefRepositoryInfo
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.atom.util.AtomJobTypeUtil
import com.tencent.devops.store.atom.util.AtomOsMapUtil
import com.tencent.devops.store.atom.util.JobTypeWriteResult
import com.tencent.devops.store.atom.util.OsWriteResult
import com.tencent.devops.store.pojo.atom.AtomBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomFeatureUpdateRequest
import com.tencent.devops.store.pojo.atom.AtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.KEY_ATOM_CODE
import com.tencent.devops.store.pojo.common.KEY_ATOM_STATUS
import com.tencent.devops.store.pojo.common.KEY_ATOM_TYPE
import com.tencent.devops.store.pojo.common.KEY_AVG_SCORE
import com.tencent.devops.store.pojo.common.KEY_BUILD_LESS_RUN_FLAG
import com.tencent.devops.store.pojo.common.KEY_CATEGORY
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_ID
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_NAME
import com.tencent.devops.store.pojo.common.KEY_CLASS_TYPE
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_DEFAULT_FLAG
import com.tencent.devops.store.pojo.common.KEY_HOT_FLAG
import com.tencent.devops.store.pojo.common.KEY_HTML_TEMPLATE_VERSION
import com.tencent.devops.store.pojo.common.KEY_ICON
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.KEY_INSTALLER
import com.tencent.devops.store.pojo.common.KEY_INSTALL_TIME
import com.tencent.devops.store.pojo.common.KEY_INSTALL_TYPE
import com.tencent.devops.store.pojo.common.KEY_LATEST_FLAG
import com.tencent.devops.store.pojo.common.KEY_LOGO_URL
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_OS_MAP
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.KEY_RECENT_EXECUTE_NUM
import com.tencent.devops.store.pojo.common.KEY_RECOMMEND_FLAG
import com.tencent.devops.store.pojo.common.KEY_SERVICE_SCOPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.ServiceScopeConfig
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.util.ServiceScopeUtil
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Record3
import org.jooq.Record5
import org.jooq.Result
import org.jooq.SelectOnConditionStep
import org.jooq.UpdateSetMoreStep
import org.jooq.impl.DSL
import org.jooq.impl.DSL.countDistinct
import org.springframework.stereotype.Repository
import java.net.URLDecoder
import java.time.LocalDateTime

/**
 * 插件列表查询参数（收拢 getPipelineAtomsAndCount 的公共参数）
 */
data class AtomQueryParam(
    val serviceScope: ServiceScopeEnum?,
    val jobType: String?,
    val os: String?,
    val projectCode: String?,
    val category: String?,
    val classifyId: String?,
    val recommendFlag: Boolean?,
    val keyword: String?,
    val fitOsFlag: Boolean?,
    val queryFitAgentBuildLessAtomFlag: Boolean?,
    val queryProjectAtomFlag: Boolean = true
)

/**
 * 插件列表 + 总数的组合查询结果，避免 list 和 count 各自独立调用 prepareAtomConditions
 */
data class PipelineAtomQueryResult(
    val atoms: Result<out Record>?,
    val totalCount: Long
)

/**
 * 插件列表查询"三分支条件"（默认 / 普通 / 测试）的中间结果，供 list 和 count 共用
 */
private data class AtomConditionSet(
    val defaultConditions: MutableList<Condition>,
    val normalConditions: MutableList<Condition>,
    val testConditions: MutableList<Condition>?,
    val includeTestAtom: Boolean
)

/**
 * ServiceScopeConfig 解析后的多 scope 字段写入值，供 DAO 更新方法复用。
 */
private data class ServiceScopeResolvedFields(
    val serviceScopeJson: String,
    val classifyIdMap: Map<String, String>,
    val jobTypeResult: JobTypeWriteResult,
    val osWriteResult: OsWriteResult
)

@Suppress("ALL")
@Repository
class AtomDao : AtomBaseDao() {

    fun addAtomFromOp(
        dslContext: DSLContext,
        userId: String,
        id: String,
        classType: String,
        atomRequest: AtomCreateRequest
    ) {
        with(TAtom.T_ATOM) {
            val osMapJson = if (atomRequest.os.isNotEmpty() && atomRequest.jobType.isBuildEnv()) {
                JsonUtil.toJson(mapOf(atomRequest.jobType.name to atomRequest.os), formatted = false)
            } else null
            dslContext.insertInto(
                this,
                ID,
                NAME,
                ATOM_CODE,
                CLASS_TYPE,
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
                VERSION,
                DEFAULT_FLAG,
                LATEST_FLAG,
                CATEGROY,
                BUILD_LESS_RUN_FLAG,
                WEIGHT,
                PROPS,
                DATA,
                CREATOR,
                MODIFIER
            )
                .values(
                    id,
                    atomRequest.name,
                    atomRequest.atomCode,
                    classType,
                    JsonUtil.toJson(atomRequest.serviceScope, formatted = false),
                    atomRequest.jobType.name,
                    JsonUtil.toJson(
                        mapOf(ServiceScopeEnum.PIPELINE.name to listOf(atomRequest.jobType.name)),
                        formatted = false
                    ),
                    JsonUtil.toJson(atomRequest.os, formatted = false),
                    osMapJson,
                    atomRequest.classifyId,
                    JsonUtil.toJson(
                        mapOf(ServiceScopeEnum.PIPELINE.name to atomRequest.classifyId),
                        formatted = false
                    ),
                    atomRequest.docsLink,
                    atomRequest.atomType.type.toByte(),
                    AtomStatusEnum.RELEASED.status.toByte(),
                    INIT_VERSION,
                    atomRequest.defaultFlag,
                    true,
                    atomRequest.category.category.toByte(),
                    atomRequest.buildLessRunFlag,
                    atomRequest.weight,
                    atomRequest.props,
                    atomRequest.data,
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun countByIdAndCode(dslContext: DSLContext, atomId: String, atomCode: String): Int {
        with(TAtom.T_ATOM) {
            return dslContext.selectCount()
                .from(this)
                .where(ID.eq(atomId))
                .and(ATOM_CODE.eq(atomCode))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByName(dslContext: DSLContext, name: String, atomCode: String? = null): Int {
        with(TAtom.T_ATOM) {
            val conditions = mutableListOf<Condition>()
            conditions.add(NAME.eq(name))
            if (atomCode != null) {
                conditions.add(ATOM_CODE.eq(atomCode))
            }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java)!!
        }
    }

    fun countByCode(dslContext: DSLContext, atomCode: String): Int {
        with(TAtom.T_ATOM) {
            return dslContext.selectCount().from(this).where(ATOM_CODE.eq(atomCode)).fetchOne(0, Int::class.java)!!
        }
    }

    fun countByUserIdAndCode(dslContext: DSLContext, userId: String, atomCode: String): Int {
        with(TAtom.T_ATOM) {
            return dslContext.selectCount().from(this).where(ATOM_CODE.eq(atomCode).and(CREATOR.eq(userId)))
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 统计分类下处于已发布状态的插件个数
     */
    fun countReleaseAtomNumByClassifyId(
        dslContext: DSLContext,
        classifyId: String,
        serviceScope: ServiceScopeEnum? = null
    ): Int {
        with(TAtom.T_ATOM) {
            val classifyCondition = buildClassifyCondition(
                ta = this,
                classifyId = classifyId,
                serviceScope = serviceScope
            )
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
            classifyCondition?.let { conditions.add(it) }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 统计还在使用处于下架中或者已下架状态的插件的项目的个数
     */
    fun countUndercarriageAtomNumByClassifyId(
        dslContext: DSLContext,
        classifyId: String,
        serviceScope: ServiceScopeEnum? = null
    ): Int {
        val tAtom = TAtom.T_ATOM
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val atomStatusList = listOf(
            AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGED.status.toByte()
        )
        val classifyCondition = buildClassifyCondition(
            ta = tAtom,
            classifyId = classifyId,
            serviceScope = serviceScope
        )
        val conditions = mutableListOf<Condition>()
        conditions.add(tAtom.ATOM_STATUS.`in`(atomStatusList))
        classifyCondition?.let { conditions.add(it) }
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        return dslContext.select(countDistinct(tStoreProjectRel.PROJECT_CODE)).from(tAtom).join(tStoreProjectRel)
            .on(tAtom.ATOM_CODE.eq(tStoreProjectRel.STORE_CODE))
            .where(conditions).fetchOne(0, Int::class.java)!!
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TAtom.T_ATOM) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getPipelineAtom(dslContext: DSLContext, id: String): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getPipelineAtom(dslContext: DSLContext, atomCode: String, version: String): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode).and(VERSION.like(VersionUtils.generateQueryVersion(version))))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getAtomByVersionPrefix(dslContext: DSLContext, atomCode: String, versionPrefix: String): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode).and(VERSION.startsWith(versionPrefix)))
                .orderBy(UPDATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getPipelineAtom(
        dslContext: DSLContext,
        atomCode: String,
        version: String? = null,
        atomStatusList: List<Byte>? = null
    ): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_CODE.eq(atomCode))
            if (version != null) {
                conditions.add(VERSION.like(VersionUtils.generateQueryVersion(version)))
            }
            if (atomStatusList != null) {
                conditions.add(ATOM_STATUS.`in`(atomStatusList))
            }
            dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getPipelineAtom(
        dslContext: DSLContext,
        projectCode: String,
        atomCode: String,
        version: String,
        defaultFlag: Boolean,
        atomStatusList: List<Byte>? = null
    ): TAtomRecord? {
        val tAtom = TAtom.T_ATOM
        val conditions = generateGetPipelineAtomCondition(
            tAtom = tAtom,
            atomCode = atomCode,
            version = version,
            defaultFlag = defaultFlag,
            atomStatusList = atomStatusList
        )
        if (!defaultFlag) {
            conditions.add(
                buildProjectInstalledCondition(tAtom, TStoreProjectRel.T_STORE_PROJECT_REL, projectCode)
            )
        }
        return dslContext.selectFrom(tAtom).where(conditions)
            .orderBy(tAtom.CREATE_TIME.desc()).limit(1).fetchOne()
    }

    private fun generateGetPipelineAtomCondition(
        tAtom: TAtom,
        atomCode: String,
        defaultFlag: Boolean? = null,
        version: String? = null,
        atomStatusList: List<Byte>? = null
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(tAtom.ATOM_CODE.eq(atomCode))
        conditions.add(tAtom.ATOM_STATUS.notEqual(AtomStatusEnum.TESTED.status.toByte()))
        if (version != null) {
            conditions.add(tAtom.VERSION.like(VersionUtils.generateQueryVersion(version)))
        }
        if (defaultFlag != null) {
            conditions.add(tAtom.DEFAULT_FLAG.eq(defaultFlag))
        }
        if (atomStatusList != null) {
            conditions.add(tAtom.ATOM_STATUS.`in`(atomStatusList))
        }
        return conditions
    }

    fun getOpPipelineAtoms(
        dslContext: DSLContext,
        atomName: String?,
        atomCode: String?,
        atomType: AtomTypeEnum?,
        serviceScope: ServiceScopeEnum?,
        os: String?,
        category: String?,
        classifyId: String?,
        atomStatus: AtomStatusEnum?,
        sortType: String?,
        desc: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<TAtomRecord> {
        with(TAtom.T_ATOM) {
            val conditions = queryOpPipelineAtomsConditions(
                atomName = atomName,
                atomCode = atomCode,
                atomType = atomType,
                serviceScope = serviceScope,
                os = os,
                category = category,
                classifyId = classifyId,
                atomStatus = atomStatus
            )
            val baseStep = dslContext.selectFrom(this)
            if (null != sortType) {
                if (desc != null && desc) {
                    baseStep.where(conditions).orderBy(CREATE_TIME.desc(), DSL.field(sortType).desc())
                } else {
                    baseStep.where(conditions).orderBy(CREATE_TIME.desc(), DSL.field(sortType).asc())
                }
            } else {
                baseStep.where(conditions).orderBy(CREATE_TIME.desc())
            }

            return baseStep.limit((page - 1) * pageSize, pageSize)
                .skipCheck() // ATOM 表量小以及 OP 接口频率可忽略索引问题
                .fetch()
        }
    }

    fun getOpPipelineAtomCount(
        dslContext: DSLContext,
        atomName: String?,
        atomCode: String?,
        atomType: AtomTypeEnum?,
        serviceScope: ServiceScopeEnum?,
        os: String?,
        category: String?,
        classifyId: String?,
        atomStatus: AtomStatusEnum?
    ): Long {
        with(TAtom.T_ATOM) {
            val conditions = queryOpPipelineAtomsConditions(
                atomName = atomName,
                atomCode = atomCode,
                atomType = atomType,
                serviceScope = serviceScope,
                os = os,
                category = category,
                classifyId = classifyId,
                atomStatus = atomStatus
            )
            return dslContext.selectCount().from(this).where(conditions)
                .skipCheck() // ATOM 表量小以及 OP 接口频率可忽略索引问题
                .fetchOne(0, Long::class.java)!!
        }
    }

    private fun TAtom.queryOpPipelineAtomsConditions(
        atomName: String?,
        atomCode: String?,
        atomType: AtomTypeEnum?,
        serviceScope: ServiceScopeEnum?,
        os: String?,
        category: String?,
        classifyId: String?,
        atomStatus: AtomStatusEnum?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        atomName?.let { conditions.add(NAME.contains(URLDecoder.decode(atomName, "UTF-8"))) }
        atomCode?.let { conditions.add(ATOM_CODE.contains(atomCode)) }
        atomType?.let { conditions.add(ATOM_TYPE.eq(atomType.type.toByte())) }
        // 使用 JSON_CONTAINS 优化 SERVICE_SCOPE 查询性能
        buildServiceScopeCondition(SERVICE_SCOPE, serviceScope)?.let {
            conditions.add(it)
        }
        os?.let {
            if (!"all".equals(os, true)) {
                conditions.add(OS.contains(os).or(OS_MAP.contains(os)))
            }
        }
        category?.let { conditions.add(CATEGROY.eq(AtomCategoryEnum.valueOf(category).category.toByte())) }
        // 使用 buildClassifyCondition 构建分类查询条件（支持多服务范围）
        buildClassifyCondition(ta = this, classifyId = classifyId, serviceScope = serviceScope)?.let {
            conditions.add(it)
        }
        atomStatus?.let { conditions.add(ATOM_STATUS.eq(atomStatus.status.toByte())) }
        return conditions
    }

    fun getVersionsByAtomCode(
        dslContext: DSLContext,
        atomCode: String,
        atomStatusList: List<Byte>?
    ): Result<out Record>? {
        with(TAtom.T_ATOM) {
            return dslContext.select(
                VERSION.`as`(KEY_VERSION),
                ATOM_STATUS.`as`(KEY_ATOM_STATUS),
                BRANCH_TEST_FLAG.`as`(KEY_BRANCH_TEST_FLAG)
            ).from(this)
                .where(
                    generateGetPipelineAtomCondition(
                        tAtom = this,
                        atomCode = atomCode,
                        atomStatusList = atomStatusList
                    )
                )
                .orderBy(CREATE_TIME.desc()).fetch()
        }
    }

    fun getVersionsByAtomCode(
        dslContext: DSLContext,
        projectCode: String,
        atomCode: String,
        defaultFlag: Boolean,
        atomStatusList: List<Byte>? = null,
        limitNum: Int? = null
    ): Result<out Record>? {
        val tAtom = TAtom.T_ATOM
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val conditions = generateGetPipelineAtomCondition(
            tAtom = tAtom,
            atomCode = atomCode,
            defaultFlag = defaultFlag,
            atomStatusList = atomStatusList
        )
        if (!defaultFlag) {
            conditions.add(
                DSL.exists(
                    DSL.selectOne().from(tStoreProjectRel)
                        .where(tStoreProjectRel.STORE_CODE.eq(tAtom.ATOM_CODE))
                        .and(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
                        .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
                )
            )
        }
        val firstVersion = JooqUtils.subStr(
            str = tAtom.VERSION,
            delim = ".",
            count = 1
        )
        val secondVersion = JooqUtils.subStr(
            str = JooqUtils.subStr(
                str = tAtom.VERSION,
                delim = ".",
                count = -2
            ),
            delim = ".",
            count = 1
        )
        val thirdVersion = JooqUtils.subStr(
            str = tAtom.VERSION,
            delim = ".",
            count = -1
        )
        val queryStep = dslContext.select(
            tAtom.VERSION.`as`(KEY_VERSION),
            tAtom.ATOM_STATUS.`as`(KEY_ATOM_STATUS),
            tAtom.BRANCH_TEST_FLAG.`as`(KEY_BRANCH_TEST_FLAG),
            firstVersion,
            secondVersion,
            thirdVersion
        ).from(tAtom)
            .where(conditions)
            .orderBy(
                tAtom.BRANCH_TEST_FLAG.desc(),
                firstVersion.plus(0).desc(),
                secondVersion.plus(0).desc(),
                thirdVersion.plus(0).desc()
            )
        limitNum?.let { queryStep.limit(it) }
        return queryStep.skipCheck().fetch()
    }

    /**
     * 组合查询：一次 prepareAtomConditions 同时获取分页数据和总数。
     * 当结果行数 < pageSize 时直接推算总数，跳过 count SQL，减少一次 DB 往返。
     */
    fun getPipelineAtomsAndCount(
        dslContext: DSLContext,
        param: AtomQueryParam,
        page: Int?,
        pageSize: Int?
    ): PipelineAtomQueryResult {
        val tAtom = TAtom.T_ATOM
        val tClassify = TClassify.T_CLASSIFY
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val tAtomFeature = TAtomFeature.T_ATOM_FEATURE
        val tStoreStatisticsTotal = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
        val conditionSet = prepareAtomConditions(
            dslContext = dslContext,
            tAtom = tAtom,
            tStoreProjectRel = tStoreProjectRel,
            tAtomFeature = tAtomFeature,
            param = param
        )

        val atoms = executeAtomListQuery(
            dslContext = dslContext,
            tAtom = tAtom,
            tClassify = tClassify,
            tAtomFeature = tAtomFeature,
            tStoreStatisticsTotal = tStoreStatisticsTotal,
            conditionSet = conditionSet,
            param = param,
            page = page,
            pageSize = pageSize
        )
        val resultSize = atoms.size
        val totalCount = if (page != null && pageSize != null && resultSize < pageSize) {
            ((page - 1).toLong() * pageSize + resultSize)
        } else {
            executeAtomCountQuery(
                dslContext = dslContext,
                tAtom = tAtom,
                tAtomFeature = tAtomFeature,
                tStoreStatisticsTotal = tStoreStatisticsTotal,
                conditionSet = conditionSet
            )
        }
        return PipelineAtomQueryResult(atoms, totalCount)
    }

    private fun executeAtomListQuery(
        dslContext: DSLContext,
        tAtom: TAtom,
        tClassify: TClassify,
        tAtomFeature: TAtomFeature,
        tStoreStatisticsTotal: TStoreStatisticsTotal,
        conditionSet: AtomConditionSet,
        param: AtomQueryParam,
        page: Int?,
        pageSize: Int?
    ): Result<out Record> {
        val newBranch = {
            buildAtomSelectStep(dslContext, tAtom, tClassify, tAtomFeature, tStoreStatisticsTotal, param.serviceScope)
        }

        val unionQuery = newBranch().where(conditionSet.normalConditions)
            .unionAll(newBranch().where(conditionSet.defaultConditions))

        val unionWithTest = conditionSet.testConditions?.let { testConds ->
            unionQuery.unionAll(newBranch().where(testConds))
        } ?: unionQuery

        val t = unionWithTest.asTable("t")
        val baseStep = dslContext.select().from(t).orderBy(t.field(KEY_WEIGHT)!!.desc(), t.field(NAME)!!.asc())
        return if (page != null && pageSize != null) {
            baseStep.limit((page - 1) * pageSize, pageSize).skipCheck().fetch()
        } else {
            baseStep.skipCheck().fetch()
        }
    }

    private fun executeAtomCountQuery(
        dslContext: DSLContext,
        tAtom: TAtom,
        tAtomFeature: TAtomFeature,
        tStoreStatisticsTotal: TStoreStatisticsTotal,
        conditionSet: AtomConditionSet
    ): Long {
        val joinCond = tAtom.ATOM_CODE.eq(tStoreStatisticsTotal.STORE_CODE)
            .and(tStoreStatisticsTotal.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))

        val newBranch = { conditions: MutableList<Condition> ->
            dslContext.select(tAtom.ATOM_CODE.`as`(KEY_ATOM_CODE)).from(tAtom)
                .leftJoin(tAtomFeature).on(tAtom.ATOM_CODE.eq(tAtomFeature.ATOM_CODE))
                .leftJoin(tStoreStatisticsTotal).on(joinCond)
                .where(conditions)
        }

        val unionQuery = newBranch(conditionSet.defaultConditions)
            .unionAll(newBranch(conditionSet.normalConditions))

        val unionWithTest = conditionSet.testConditions?.let { testConds ->
            unionQuery.unionAll(newBranch(testConds))
        } ?: unionQuery

        val t = unionWithTest.asTable("t")
        return dslContext.select(countDistinct(t.field(KEY_ATOM_CODE)))
            .from(t).fetchOne(0, Long::class.java)!!
    }

    private fun buildAtomSelectStep(
        dslContext: DSLContext,
        tAtom: TAtom,
        tClassify: TClassify,
        tAtomFeature: TAtomFeature,
        tStoreStatisticsTotal: TStoreStatisticsTotal,
        serviceScope: ServiceScopeEnum? = null
    ): SelectOnConditionStep<Record> {
        val classifyIdField = buildClassifyIdField(tAtom, serviceScope)
        return dslContext.select(
            tAtom.ID.`as`(KEY_ID),
            tAtom.ATOM_CODE.`as`(KEY_ATOM_CODE),
            tAtom.VERSION.`as`(VERSION),
            tAtom.CLASS_TYPE.`as`(KEY_CLASS_TYPE),
            tAtom.NAME.`as`(NAME),
            tAtom.OS.`as`(KEY_OS),
            tAtom.OS_MAP.`as`(KEY_OS_MAP),
            tAtom.SERVICE_SCOPE.`as`(KEY_SERVICE_SCOPE),
            classifyIdField.`as`(KEY_CLASSIFY_ID),
            tClassify.CLASSIFY_CODE.`as`(KEY_CLASSIFY_CODE),
            tClassify.CLASSIFY_NAME.`as`(KEY_CLASSIFY_NAME),
            tAtom.LOGO_URL.`as`(KEY_LOGO_URL),
            tAtom.ICON.`as`(KEY_ICON),
            tAtom.CATEGROY.`as`(KEY_CATEGORY),
            tAtom.SUMMARY.`as`(KEY_SUMMARY),
            tAtom.DOCS_LINK.`as`(KEY_DOCSLINK),
            tAtom.ATOM_TYPE.`as`(KEY_ATOM_TYPE),
            tAtom.ATOM_STATUS.`as`(KEY_ATOM_STATUS),
            tAtom.DESCRIPTION.`as`(KEY_DESCRIPTION),
            tAtom.PUBLISHER.`as`(KEY_PUBLISHER),
            tAtom.CREATOR.`as`(KEY_CREATOR),
            tAtom.MODIFIER.`as`(KEY_MODIFIER),
            tAtom.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tAtom.UPDATE_TIME.`as`(KEY_UPDATE_TIME),
            tAtom.DEFAULT_FLAG.`as`(KEY_DEFAULT_FLAG),
            tAtom.LATEST_FLAG.`as`(KEY_LATEST_FLAG),
            tAtom.BUILD_LESS_RUN_FLAG.`as`(KEY_BUILD_LESS_RUN_FLAG),
            tAtom.WEIGHT.`as`(KEY_WEIGHT),
            tAtom.HTML_TEMPLATE_VERSION.`as`(KEY_HTML_TEMPLATE_VERSION),
            tAtom.BRANCH_TEST_FLAG.`as`(KEY_BRANCH_TEST_FLAG),
            tAtomFeature.RECOMMEND_FLAG.`as`(KEY_RECOMMEND_FLAG),
            tStoreStatisticsTotal.SCORE_AVERAGE.`as`(KEY_AVG_SCORE),
            tStoreStatisticsTotal.RECENT_EXECUTE_NUM.`as`(KEY_RECENT_EXECUTE_NUM),
            tStoreStatisticsTotal.HOT_FLAG.`as`(KEY_HOT_FLAG)
        )
            .from(tAtom)
            .leftJoin(tClassify).on(classifyIdField.eq(tClassify.ID))
            .leftJoin(tAtomFeature).on(tAtom.ATOM_CODE.eq(tAtomFeature.ATOM_CODE))
            .leftJoin(tStoreStatisticsTotal).on(
                tAtom.ATOM_CODE.eq(tStoreStatisticsTotal.STORE_CODE)
                    .and(tStoreStatisticsTotal.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
            )
    }

    /**
     * 统一构建 getPipelineAtomsAndCount 内部三分支查询条件。
     *
     * 性能优化：如果需要排除测试中/审核中的插件，先执行一次轻量查询拿到 atomCode 列表（通常只有几个），
     * 然后用字面值 NOT IN 传入默认/普通分支的条件，避免在每个 UNION ALL 分支里重复执行 NOT IN 子查询
     */
    private fun prepareAtomConditions(
        dslContext: DSLContext,
        tAtom: TAtom,
        tStoreProjectRel: TStoreProjectRel,
        tAtomFeature: TAtomFeature,
        param: AtomQueryParam
    ): AtomConditionSet {
        val effectiveProjectCode = if (param.queryProjectAtomFlag) param.projectCode else null
        val includeTestAtom =
            !param.projectCode.isNullOrBlank() && (param.queryProjectAtomFlag || !param.keyword.isNullOrBlank())
        val defaultConditions = buildDefaultConditions(
            tAtom = tAtom,
            tAtomFeature = tAtomFeature,
            param = param
        )
        val normalConditions = buildNormalConditions(
            tAtom = tAtom,
            tStoreProjectRel = tStoreProjectRel,
            tAtomFeature = tAtomFeature,
            param = param,
            effectiveProjectCode = effectiveProjectCode
        )
        var testUnionConditions: MutableList<Condition>? = null

        if (includeTestAtom) {
            // 先查出测试中/审核中的 atomCode 列表（一般只有个位数），避免在主查询中嵌套 NOT IN 子查询
            val testAtomCodes = dslContext
                .selectDistinct(tAtom.ATOM_CODE)
                .from(tAtom)
                .join(tStoreProjectRel).on(
                    tAtom.ATOM_CODE.eq(tStoreProjectRel.STORE_CODE)
                        .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
                        .and(tStoreProjectRel.PROJECT_CODE.eq(param.projectCode!!))
                        .and(tStoreProjectRel.TYPE.`in`(listOf(StoreProjectTypeEnum.TEST.type.toByte())))
                )
                .where(
                    tAtom.ATOM_STATUS.`in`(
                        listOf(AtomStatusEnum.TESTING.status.toByte(), AtomStatusEnum.AUDITING.status.toByte())
                    )
                )
                .and(tAtom.DELETE_FLAG.eq(false))
                .fetch(tAtom.ATOM_CODE)

            if (testAtomCodes.isNotEmpty()) {
                val exclusion = tAtom.ATOM_CODE.notIn(testAtomCodes)
                defaultConditions.add(exclusion)
                normalConditions.add(exclusion)
            }

            // union 第三分支：测试中/审核中且 LATEST_TEST_FLAG
            val baseTestConditions = buildTestConditions(
                tAtom = tAtom,
                tStoreProjectRel = tStoreProjectRel,
                tAtomFeature = tAtomFeature,
                param = param,
                projectCode = param.projectCode
            )
            testUnionConditions = ArrayList(baseTestConditions)
            testUnionConditions.add(tAtom.LATEST_TEST_FLAG.eq(true))
        }

        return AtomConditionSet(
            defaultConditions = defaultConditions,
            normalConditions = normalConditions,
            testConditions = testUnionConditions,
            includeTestAtom = includeTestAtom
        )
    }

    private fun buildDefaultConditions(
        tAtom: TAtom,
        tAtomFeature: TAtomFeature,
        param: AtomQueryParam
    ): MutableList<Condition> {
        val conditions = buildBaseConditions(tAtom, tAtomFeature, param)
        conditions.add(tAtom.ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
        conditions.add(tAtom.DEFAULT_FLAG.eq(true))
        conditions.add(tAtom.LATEST_FLAG.eq(true))
        return conditions
    }

    private fun buildNormalConditions(
        tAtom: TAtom,
        tStoreProjectRel: TStoreProjectRel,
        tAtomFeature: TAtomFeature,
        param: AtomQueryParam,
        effectiveProjectCode: String?
    ): MutableList<Condition> {
        val conditions = buildBaseConditions(tAtom, tAtomFeature, param)
        conditions.add(tAtom.ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
        conditions.add(tAtom.DEFAULT_FLAG.eq(false))
        conditions.add(tAtom.LATEST_FLAG.eq(true))
        if (!effectiveProjectCode.isNullOrBlank()) {
            conditions.add(buildProjectInstalledCondition(tAtom, tStoreProjectRel, effectiveProjectCode))
        }
        return conditions
    }

    private fun buildTestConditions(
        tAtom: TAtom,
        tStoreProjectRel: TStoreProjectRel,
        tAtomFeature: TAtomFeature,
        param: AtomQueryParam,
        projectCode: String
    ): MutableList<Condition> {
        val conditions = buildBaseConditions(tAtom, tAtomFeature, param)
        conditions.add(
            tAtom.ATOM_STATUS.`in`(
                listOf(AtomStatusEnum.TESTING.status.toByte(), AtomStatusEnum.AUDITING.status.toByte())
            )
        )
        conditions.add(
            DSL.exists(
                DSL.selectOne().from(tStoreProjectRel)
                    .where(tStoreProjectRel.STORE_CODE.eq(tAtom.ATOM_CODE))
                    .and(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
                    .and(tStoreProjectRel.TYPE.`in`(listOf(StoreProjectTypeEnum.TEST.type.toByte())))
                    .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
            )
        )
        return conditions
    }

    // ========== 公共条件 ==========

    /**
     * EXISTS 子查询：检查插件是否已被指定项目安装。
     * 替代 JOIN T_STORE_PROJECT_REL，避免因一对多关系导致的行膨胀。
     * 且 EXISTS 在找到第一条匹配后即短路返回。
     */
    private fun buildProjectInstalledCondition(
        tAtom: TAtom,
        tStoreProjectRel: TStoreProjectRel,
        projectCode: String
    ): Condition {
        return DSL.exists(
            DSL.selectOne().from(tStoreProjectRel)
                .where(tStoreProjectRel.STORE_CODE.eq(tAtom.ATOM_CODE))
                .and(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
                .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        )
    }

    private fun buildBaseConditions(
        tAtom: TAtom,
        tAtomFeature: TAtomFeature,
        param: AtomQueryParam
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        buildServiceScopeCondition(tAtom.SERVICE_SCOPE, param.serviceScope)?.let { conditions.add(it) }
        buildClassifyCondition(tAtom, param.classifyId, param.serviceScope)?.let { conditions.add(it) }
        buildJobTypeCondition(
            ta = tAtom,
            jobType = param.jobType,
            serviceScope = param.serviceScope,
            queryFitAgentBuildLessAtomFlag = param.queryFitAgentBuildLessAtomFlag
        )?.let { conditions.add(it) }
        buildOsCondition(tAtom, param)?.let { conditions.add(it) }
        param.category?.let { category ->
            conditions.add(tAtom.CATEGROY.eq(AtomCategoryEnum.valueOf(category).category.toByte()))
        }
        param.recommendFlag?.let { recommended ->
            val condition = if (recommended) {
                tAtomFeature.RECOMMEND_FLAG.eq(true).or(tAtomFeature.RECOMMEND_FLAG.isNull)
            } else {
                tAtomFeature.RECOMMEND_FLAG.eq(false)
            }
            conditions.add(condition)
        }
        if (!param.keyword.isNullOrEmpty()) {
            conditions.add(tAtom.NAME.contains(param.keyword).or(tAtom.SUMMARY.contains(param.keyword)))
        }
        conditions.add(tAtom.DELETE_FLAG.eq(false))
        return conditions
    }

    private fun buildOsCondition(tAtom: TAtom, param: AtomQueryParam): Condition? {
        val os = param.os
        if (isExplicitNonBuildEnvJobType(param.jobType)) {
            return null
        }
        return when {
            !os.isNullOrBlank() && !os.equals(KEY_ALL, ignoreCase = true) -> {
                val osMatch = buildJobTypeAwareOsMatch(tAtom, os, param.jobType, param.serviceScope)
                if (param.fitOsFlag == false) {
                    osMatch.not()
                        .and(tAtom.BUILD_LESS_RUN_FLAG.ne(true).or(tAtom.BUILD_LESS_RUN_FLAG.isNull))
                        .and(tAtom.CATEGROY.eq(AtomCategoryEnum.TASK.category.toByte()))
                } else {
                    osMatch.or(tAtom.BUILD_LESS_RUN_FLAG.eq(true))
                }
            }
            os.equals(KEY_ALL, ignoreCase = true) && param.fitOsFlag == false -> {
                getAgentLessJobTypeForScope(param.serviceScope)?.let { agentLessType ->
                    buildJobTypeCondition(tAtom, agentLessType, param.serviceScope, null)
                }
            }
            else -> null
        }
    }

    /**
     * 解析 jobType 字符串的编译环境标识：true=编译环境，false=无编译环境，null=为空或无法解析。
     * 供 isExplicitNonBuildEnvJobType / resolveOsMapKey 等方法复用，避免重复 valueOf + isBuildEnv 调用。
     */
    private fun parseBuildEnvFlag(jobType: String?): Boolean? {
        if (jobType.isNullOrBlank()) return null
        return runCatching { JobTypeEnum.valueOf(jobType).isBuildEnv() }.getOrNull()
    }

    private fun isExplicitNonBuildEnvJobType(jobType: String?): Boolean {
        return parseBuildEnvFlag(jobType) == false
    }

    /**
     * 根据 jobType 构建 OS 匹配条件（OS 与 jobType 一对一）。
     *
     * 优先使用请求参数中的 jobType 确定 OS_MAP 的查询 key；jobType 为空时回退到 serviceScope 推导。
     *
     * 性能策略：
     * - AGENT（或 jobType/scope 均为空）：只查 OS 字段（LIKE），与优化前一致，零开销
     * - 非 AGENT 编译环境 jobType（如 CREATIVE_STREAM）：用 JSON_EXTRACT + JSON_CONTAINS 精确定位
     *   OS_MAP 中对应 jobType 的数组，避免 JSON_SEARCH 全路径遍历。同时 OR OS 字段兜底兼容。
     */
    private fun buildJobTypeAwareOsMatch(
        tAtom: TAtom,
        os: String,
        jobType: String?,
        serviceScope: ServiceScopeEnum?
    ): Condition {
        val osMapKey = resolveOsMapKey(jobType, serviceScope)
        if (osMapKey == null || osMapKey == JobTypeEnum.AGENT.name) {
            return tAtom.OS.contains(os)
        }
        val osInMapCondition = DSL.condition(
            "JSON_CONTAINS(JSON_EXTRACT({0}, {1}), CONCAT('\"', {2}, '\"'))",
            tAtom.OS_MAP,
            DSL.inline("\$.${osMapKey}"),
            DSL.inline(os)
        )
        return osInMapCondition.or(tAtom.OS.contains(os))
    }

    /**
     * 确定 OS_MAP 中的查询 key。
     * 优先使用请求参数中的 jobType（OS 与 jobType 一对一）；
     * jobType 为空时回退到 serviceScope 推导（getBuildEnvJobTypeForScope）。
     * 返回 null 表示无需查 OS_MAP，直接用 OS 字段。
     */
    private fun resolveOsMapKey(jobType: String?, serviceScope: ServiceScopeEnum?): String? {
        if (!jobType.isNullOrBlank()) {
            return if (parseBuildEnvFlag(jobType) == true) jobType else null
        }
        return getBuildEnvJobTypeForScope(serviceScope)
    }

    fun updateAtomFromOp(
        dslContext: DSLContext,
        userId: String,
        id: String,
        classType: String,
        atomUpdateRequest: AtomUpdateRequest
    ) {
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.update(this)
                .set(NAME, atomUpdateRequest.name)
                .set(OS, JsonUtil.toJson(atomUpdateRequest.os, formatted = false))
                .set(CLASS_TYPE, classType)
                .set(DOCS_LINK, atomUpdateRequest.docsLink)
                .set(ATOM_TYPE, atomUpdateRequest.atomType.type.toByte())
            if (null != atomUpdateRequest.summary) {
                baseStep.set(SUMMARY, atomUpdateRequest.summary)
            }
            if (null != atomUpdateRequest.description) {
                baseStep.set(DESCRIPTION, atomUpdateRequest.description)
            }
            baseStep.set(DEFAULT_FLAG, atomUpdateRequest.defaultFlag)
                .set(CATEGROY, atomUpdateRequest.category.category.toByte())
            if (null != atomUpdateRequest.buildLessRunFlag) {
                baseStep.set(BUILD_LESS_RUN_FLAG, atomUpdateRequest.buildLessRunFlag)
            }
            if (null != atomUpdateRequest.weight) {
                baseStep.set(WEIGHT, atomUpdateRequest.weight)
            }
            if (null != atomUpdateRequest.props) {
                baseStep.set(PROPS, atomUpdateRequest.props)
            }
            if (null != atomUpdateRequest.data) {
                baseStep.set(DATA, atomUpdateRequest.data)
            }
            if (null != atomUpdateRequest.logoUrl) {
                baseStep.set(LOGO_URL, atomUpdateRequest.logoUrl)
            }
            if (null != atomUpdateRequest.iconData) {
                baseStep.set(ICON, atomUpdateRequest.iconData)
            }
            if (null != atomUpdateRequest.publisher) {
                baseStep.set(PUBLISHER, atomUpdateRequest.publisher)
            }
            val visibilityLevel = atomUpdateRequest.visibilityLevel
            val privateReason = atomUpdateRequest.privateReason
            if (null != visibilityLevel) {
                baseStep.set(VISIBILITY_LEVEL, visibilityLevel.level)
            }
            if (visibilityLevel == VisibilityLevelEnum.LOGIN_PUBLIC) {
                baseStep.set(PRIVATE_REASON, "")
            } else {
                if (null != privateReason) {
                    baseStep.set(PRIVATE_REASON, privateReason)
                }
            }
            val configs = atomUpdateRequest.serviceScopeConfigs
            if (!configs.isNullOrEmpty()) {
                applyResolvedServiceScopeFields(baseStep, resolveServiceScopeFields(dslContext, configs))
            } else {
                baseStep.set(SERVICE_SCOPE, JsonUtil.toJson(atomUpdateRequest.serviceScope, formatted = false))
                baseStep.set(JOB_TYPE, atomUpdateRequest.jobType.name)
                baseStep.set(CLASSIFY_ID, atomUpdateRequest.classifyId)
                baseStep.set(
                    CLASSIFY_ID_MAP,
                    DSL.field(
                        "JSON_SET(COALESCE({0}, '{{}}'), '$.PIPELINE', {1})",
                        String::class.java,
                        CLASSIFY_ID_MAP,
                        DSL.inline(atomUpdateRequest.classifyId)
                    )
                )
                baseStep.set(
                    JOB_TYPE_MAP,
                    DSL.field(
                        "JSON_SET(COALESCE({0}, '{{}}'), '$.PIPELINE', JSON_ARRAY({1}))",
                        String::class.java,
                        JOB_TYPE_MAP,
                        DSL.inline(atomUpdateRequest.jobType.name)
                    )
                )
            }
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getRecentAtomByCode(dslContext: DSLContext, atomCode: String): TAtomRecord? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .limit(1)
                .fetchOne()
        }
    }

    fun updateAtomByCode(
        dslContext: DSLContext,
        userId: String,
        atomCode: String,
        atomFeatureUpdateRequest: AtomFeatureUpdateRequest
    ) {
        return with(TAtom.T_ATOM) {
            val baseStep = dslContext.update(this)
            if (!atomFeatureUpdateRequest.repositoryUrl.isNullOrBlank()) {
                baseStep.set(CODE_SRC, atomFeatureUpdateRequest.repositoryUrl)
            }
            if (atomFeatureUpdateRequest.defaultFlag != null) {
                baseStep.set(DEFAULT_FLAG, atomFeatureUpdateRequest.defaultFlag)
            }
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ATOM_CODE.eq(atomCode))
                .execute()
        }
    }

    /**
     * 获取已安装的插件个数
     */
    fun countInstalledAtoms(
        dslContext: DSLContext,
        projectCode: String? = null,
        classifyCode: String? = null,
        name: String? = null,
        serviceScope: ServiceScopeEnum? = null
    ): Int {
        val (ta, tspr, conditions) = getInstalledConditions(
            projectCode = projectCode,
            classifyCode = classifyCode,
            name = name,
            serviceScope = serviceScope,
            dslContext = dslContext
        )

        val step = dslContext.select(countDistinct(ta.ATOM_CODE)).from(ta)
        if (!projectCode.isNullOrBlank()) {
            step.join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
        }
        val tc = TClassify.T_CLASSIFY
        // 使用公共方法构建分类关联条件（支持 CLASSIFY_ID_MAP）
        return step.join(tc).on(buildClassifyJoinCondition(ta, tc, serviceScope))
            .where(conditions).fetchOne(0, Int::class.java)!!
    }

    /**
     * 获取已安装的插件
     */
    fun getInstalledAtoms(
        dslContext: DSLContext,
        projectCode: String,
        classifyCode: String? = null,
        name: String? = null,
        serviceScope: ServiceScopeEnum? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<out Record>? {

        val (ta, tspr, conditions) = getInstalledConditions(
            projectCode = projectCode,
            classifyCode = classifyCode,
            name = name,
            serviceScope = serviceScope,
            dslContext = dslContext
        )
        val tc = TClassify.T_CLASSIFY

        val sql = dslContext.select(
            ta.ID.`as`(KEY_ID),
            ta.ATOM_CODE.`as`(KEY_ATOM_CODE),
            ta.VERSION.`as`(KEY_VERSION),
            ta.NAME.`as`(NAME),
            ta.LOGO_URL.`as`(KEY_LOGO_URL),
            ta.CATEGROY.`as`(KEY_CATEGORY),
            ta.SUMMARY.`as`(KEY_SUMMARY),
            ta.PUBLISHER.`as`(KEY_PUBLISHER),
            ta.DEFAULT_FLAG.`as`(KEY_DEFAULT_FLAG),
            tc.ID.`as`(KEY_CLASSIFY_ID),
            tc.CLASSIFY_CODE.`as`(KEY_CLASSIFY_CODE),
            tc.CLASSIFY_NAME.`as`(KEY_CLASSIFY_NAME),
            tspr.CREATOR.`as`(KEY_INSTALLER),
            tspr.CREATE_TIME.`as`(KEY_INSTALL_TIME),
            tspr.TYPE.`as`(KEY_INSTALL_TYPE)
        )
            .from(ta)
            .join(tc)
            .on(buildClassifyJoinCondition(ta, tc, serviceScope))
            .join(tspr)
            .on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
            .where(conditions)
            .groupBy(ta.ATOM_CODE)
            .orderBy(tspr.TYPE.asc(), tspr.CREATE_TIME.desc())
        if (page != null && pageSize != null) sql.limit((page - 1) * pageSize, pageSize)
        return sql.fetch()
    }

    private fun getInstalledConditions(
        projectCode: String? = null,
        classifyCode: String?,
        name: String?,
        serviceScope: ServiceScopeEnum? = null,
        dslContext: DSLContext
    ): Triple<TAtom, TStoreProjectRel, MutableList<Condition>> {
        val ta = TAtom.T_ATOM
        val tspr = TStoreProjectRel.T_STORE_PROJECT_REL
        val conditions = mutableListOf<Condition>()
        if (projectCode.isNullOrBlank()) {
            conditions.add(ta.DEFAULT_FLAG.eq(true))
            conditions.add(ta.ATOM_STATUS.`in`(
                listOf(
                    AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
                    AtomStatusEnum.UNDERCARRIAGED.status.toByte(),
                    AtomStatusEnum.RELEASED.status.toByte()
                )
            ))
        } else {
            conditions.add(tspr.PROJECT_CODE.eq(projectCode).and(tspr.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte())))
            conditions.add(ta.DEFAULT_FLAG.eq(false))
        }
        conditions.add(ta.LATEST_FLAG.eq(true))
        buildServiceScopeCondition(ta.SERVICE_SCOPE, serviceScope)?.let {
            conditions.add(it)
        }
        if (!classifyCode.isNullOrEmpty()) {
            // 使用公共方法查询分类ID
            val classifyId = getClassifyIdByCode(
                dslContext = dslContext,
                classifyCode = classifyCode,
                serviceScope = serviceScope
            )
            // 使用公共方法构建分类查询条件
            buildClassifyCondition(
                ta = ta,
                classifyId = classifyId,
                serviceScope = serviceScope
            )?.let {
                conditions.add(it)
            }
        }
        if (!name.isNullOrBlank()) {
            conditions.add(ta.NAME.contains(name))
        }
        return Triple(ta, tspr, conditions)
    }

    fun updateAtomBaseInfo(
        dslContext: DSLContext,
        userId: String,
        atomIdList: List<String>,
        atomBaseInfoUpdateRequest: AtomBaseInfoUpdateRequest
    ) {
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.update(this)
            val atomName = atomBaseInfoUpdateRequest.name
            if (null != atomName) {
                baseStep.set(NAME, atomName)
            }
            val summary = atomBaseInfoUpdateRequest.summary
            if (null != summary) {
                baseStep.set(SUMMARY, summary)
            }
            val description = atomBaseInfoUpdateRequest.description
            if (null != description) {
                baseStep.set(DESCRIPTION, description)
            }
            val logoUrl = atomBaseInfoUpdateRequest.logoUrl
            if (null != logoUrl) {
                baseStep.set(LOGO_URL, logoUrl)
            }
            val publisher = atomBaseInfoUpdateRequest.publisher
            if (null != publisher) {
                baseStep.set(PUBLISHER, publisher)
            }
            val visibilityLevel = atomBaseInfoUpdateRequest.visibilityLevel
            if (null != visibilityLevel) {
                baseStep.set(VISIBILITY_LEVEL, visibilityLevel.level)
            }
            if (visibilityLevel == VisibilityLevelEnum.LOGIN_PUBLIC) {
                baseStep.set(PRIVATE_REASON, "") // 选择开源则清空不开源原因
            } else {
                val privateReason = atomBaseInfoUpdateRequest.privateReason
                if (null != privateReason) {
                    baseStep.set(PRIVATE_REASON, privateReason)
                }
            }
            baseStep.set(MODIFIER, userId)
                .where(ID.`in`(atomIdList))
                .execute()

            val configs = atomBaseInfoUpdateRequest.serviceScopeConfigs
            if (!configs.isNullOrEmpty()) {
                val moreStep = dslContext.update(this).set(MODIFIER, userId)
                applyResolvedServiceScopeFields(moreStep, resolveServiceScopeFields(dslContext, configs))
                moreStep.where(ID.`in`(atomIdList)).execute()
            } else if (atomBaseInfoUpdateRequest.classifyCode != null) {
                val resolvedMap = buildClassifyIdMap(dslContext, atomBaseInfoUpdateRequest)
                if (resolvedMap.isNotEmpty()) {
                    val step = dslContext.update(this)
                        .set(CLASSIFY_ID_MAP, JsonUtil.toJson(resolvedMap, formatted = false))
                        .set(MODIFIER, userId)
                    resolvedMap[ServiceScopeEnum.PIPELINE.name]?.let { step.set(CLASSIFY_ID, it) }
                    step.where(ID.`in`(atomIdList)).execute()
                }
            }
        }
    }

    fun getSelfDevelopAtoms(dslContext: DSLContext): Result<TAtomRecord>? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_TYPE.eq(AtomTypeEnum.SELF_DEVELOPED.type.toByte()))
                .fetch()
        }
    }

    fun batchGetDefaultAtomCode(dslContext: DSLContext): Result<Record1<String>> {
        return with(TAtom.T_ATOM) {
            dslContext.select(ATOM_CODE).from(this)
                .where(
                    LATEST_FLAG.eq(true)
                        .and(DEFAULT_FLAG.eq(true))
                )
                .fetch()
        }
    }

    fun batchGetAtomName(
        dslContext: DSLContext,
        atomCodes: Collection<String>
    ): Result<Record3<String, String, String>>? {
        return with(TAtom.T_ATOM) {
            dslContext.select(ATOM_CODE, NAME, VERSION).from(this)
                .where(
                    LATEST_FLAG.eq(true)
                        .and(ATOM_CODE.`in`(atomCodes))
                )
                .fetch()
        }
    }

    fun getPublishedAtoms(
        dslContext: DSLContext,
        timeDescFlag: Boolean = true,
        page: Int,
        pageSize: Int
    ): List<String> {
        with(TAtom.T_ATOM) {
            val baseStep = dslContext.select(ATOM_CODE)
                .from(this)
                .where(ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
            if (timeDescFlag) {
                baseStep.orderBy(CREATE_TIME.desc(), ID)
            } else {
                baseStep.orderBy(CREATE_TIME.asc(), ID)
            }
            return baseStep.groupBy(ATOM_CODE)
                .limit((page - 1) * pageSize, pageSize).fetchInto(String::class.java)
        }
    }

    fun getPublishedAtomCount(dslContext: DSLContext): Int {
        with(TAtom.T_ATOM) {
            return dslContext.select(DSL.countDistinct(ATOM_CODE))
                .from(this)
                .where(ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getAtomCodeSrc(dslContext: DSLContext, atomCode: String): String? {
        with(TAtom.T_ATOM) {
            return dslContext.select(CODE_SRC)
                .from(this)
                .where(ATOM_CODE.eq(atomCode).and(LATEST_FLAG.eq(true)))
                .fetchOne(0, String::class.java)
        }
    }

    fun getAtomRealVersion(
        dslContext: DSLContext,
        projectCode: String,
        atomCode: String,
        version: String,
        defaultFlag: Boolean,
        atomStatusList: List<Byte>? = null
    ): String? {
        val tAtom = TAtom.T_ATOM
        val conditions = generateGetPipelineAtomCondition(
            tAtom = tAtom,
            atomCode = atomCode,
            version = version,
            defaultFlag = defaultFlag,
            atomStatusList = atomStatusList
        )
        if (!defaultFlag) {
            conditions.add(
                buildProjectInstalledCondition(tAtom, TStoreProjectRel.T_STORE_PROJECT_REL, projectCode)
            )
        }
        return dslContext.select(tAtom.VERSION).from(tAtom)
            .where(conditions)
            .orderBy(tAtom.CREATE_TIME.desc()).limit(1).fetchOne(0, String::class.java)
    }

    fun getAtomRepoInfoByCode(
        dslContext: DSLContext,
        atomCode: String?,
        limit: Int,
        offset: Int
    ): List<AtomRefRepositoryInfo> {
        return with(TAtom.T_ATOM) {
            val conditions = mutableListOf(
                REPOSITORY_HASH_ID.isNotNull,
                LATEST_FLAG.eq(true)
            )
            dslContext.select(
                ATOM_CODE,
                REPOSITORY_HASH_ID
            )
                .from(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map {
                    AtomRefRepositoryInfo(
                        atomCode = it.value1(),
                        repositoryHashId = it.value2()
                    )
                }
        }
    }

    /**
     * 获取默认插件
     */
    fun getDefaultAtoms(
        dslContext: DSLContext,
        classifyCode: String? = null,
        name: String? = null,
        offset: Int? = null,
        limit: Int? = null
    ): Result<out Record>? {

        val (ta, _, conditions) = getInstalledConditions(
            classifyCode = classifyCode,
            name = name,
            dslContext = dslContext
        )
        val tc = TClassify.T_CLASSIFY

        val sql = dslContext.select(
            ta.ID.`as`(KEY_ID),
            ta.ATOM_CODE.`as`(KEY_ATOM_CODE),
            ta.VERSION.`as`(KEY_VERSION),
            ta.NAME.`as`(NAME),
            ta.LOGO_URL.`as`(KEY_LOGO_URL),
            ta.CATEGROY.`as`(KEY_CATEGORY),
            ta.SUMMARY.`as`(KEY_SUMMARY),
            ta.PUBLISHER.`as`(KEY_PUBLISHER),
            ta.DEFAULT_FLAG.`as`(KEY_DEFAULT_FLAG),
            tc.ID.`as`(KEY_CLASSIFY_ID),
            tc.CLASSIFY_CODE.`as`(KEY_CLASSIFY_CODE),
            tc.CLASSIFY_NAME.`as`(KEY_CLASSIFY_NAME)
        )
            .from(ta)
            .join(tc)
            .on(buildClassifyJoinCondition(ta, tc))
            .where(conditions)
            .groupBy(ta.ATOM_CODE)
            .orderBy(ta.CREATE_TIME, ta.ID)
        if (offset != null && limit != null) sql.offset(offset).limit(limit)
        return sql.skipCheck().fetch()
    }

    fun getAtomProps(dslContext: DSLContext, atomCode: String, version: String): String? {
        with(TAtom.T_ATOM) {
            return dslContext.select(PROPS)
                .from(this)
                .where(ATOM_CODE.eq(atomCode).and(VERSION.eq(version)))
                .fetchOne(0, String::class.java)
        }
    }

    fun queryAtomByStatus(
        dslContext: DSLContext,
        atomCode: String? = null,
        statusList: List<Byte>,
        offset: Int? = null,
        limit: Int? = null
    ): Result<Record5<String, String, String, Byte, Boolean>> {
        with(TAtom.T_ATOM) {
            val conditions = mutableListOf<Condition>()
            if (!atomCode.isNullOrBlank()) {
                conditions.add(ATOM_CODE.eq(atomCode))
            }
            conditions.add(ATOM_STATUS.`in`(statusList))
            val step = dslContext.select(
                ATOM_CODE,
                VERSION,
                PROPS,
                ATOM_STATUS,
                LATEST_FLAG
            ).from(this)
                .where(conditions)
                .orderBy(CREATE_TIME, ID)
            if (offset != null && limit != null) step.offset(offset).limit(limit)
            return step.fetch()
        }
    }

    private fun buildClassifyIdMap(
        dslContext: DSLContext,
        atomBaseInfoUpdateRequest: AtomBaseInfoUpdateRequest
    ): Map<String, String> {
        return buildClassifyIdMapFromConfigs(dslContext, atomBaseInfoUpdateRequest.toServiceScopeConfigs())
    }

    /**
     * 从 ServiceScopeConfig 列表解析出 SERVICE_SCOPE、CLASSIFY_ID_MAP、JOB_TYPE/JOB_TYPE_MAP 的写入值。
     * 供 updateAtomFromOp 和 updateAtomBaseInfo 等方法复用。
     */
    private fun resolveServiceScopeFields(
        dslContext: DSLContext,
        configs: List<ServiceScopeConfig>
    ): ServiceScopeResolvedFields {
        return ServiceScopeResolvedFields(
            serviceScopeJson = JsonUtil.toJson(configs.map { it.serviceScope.name }, formatted = false),
            classifyIdMap = buildClassifyIdMapFromConfigs(dslContext, configs),
            jobTypeResult = AtomJobTypeUtil.buildJobTypeFields(configs),
            osWriteResult = AtomOsMapUtil.buildOsFields(configs)
        )
    }

    /**
     * 将 ServiceScopeResolvedFields 中的多 scope 字段统一写入 UPDATE 语句。
     * 供 updateAtomFromOp 和 updateAtomBaseInfo 复用，消除重复的字段赋值代码。
     */
    private fun TAtom.applyResolvedServiceScopeFields(
        step: UpdateSetMoreStep<TAtomRecord>,
        resolved: ServiceScopeResolvedFields
    ) {
        step.set(SERVICE_SCOPE, resolved.serviceScopeJson)
        if (resolved.classifyIdMap.isNotEmpty()) {
            step.set(CLASSIFY_ID_MAP, JsonUtil.toJson(resolved.classifyIdMap, formatted = false))
            resolved.classifyIdMap[ServiceScopeEnum.PIPELINE.name]?.let { step.set(CLASSIFY_ID, it) }
        }
        resolved.jobTypeResult.pipelineJobType?.let { step.set(JOB_TYPE, it) }
        resolved.jobTypeResult.jobTypeMapJson?.let { step.set(JOB_TYPE_MAP, it) }
        step.set(OS, JsonUtil.toJson(resolved.osWriteResult.pipelineOs, formatted = false))
        step.set(OS_MAP, resolved.osWriteResult.osMapJson)
    }

    /**
     * 构建 SERVICE_SCOPE 查询条件（优化版本，使用 JSON_CONTAINS 替代 contains）
     *
     * 性能优化：
     * - 使用 JSON_CONTAINS 进行精确匹配，避免字符串包含查询的全表扫描
     * - 支持大小写兼容查询（标准格式为大写，兼容小写格式的现有数据）
     *
     * 格式标准：
     * - 统一使用大写格式存储，如 ["PIPELINE"]、"CREATIVE_STREAM"
     * - 查询时同时匹配大写格式（标准格式）和小写格式（兼容现有数据）
     *
     * @param serviceScopeField SERVICE_SCOPE 字段
     * @param serviceScope 服务范围值，支持大小写（会自动标准化为大写）
     * @return 查询条件，如果不需要过滤则返回 null
     */
    private fun buildServiceScopeCondition(
        serviceScopeField: Field<String>,
        serviceScope: ServiceScopeEnum?
    ): Condition? {
        if (serviceScope == null) {
            return null
        }
        val normalizedScope = ServiceScopeUtil.normalize(serviceScope.name) ?: serviceScope.name

        val isValidJson = serviceScopeField.isNotNull
            .and(serviceScopeField.ne(""))

        // 使用 CONCAT 拼接 JSON 字符串值，避免 DSL.inline 对双引号的转义问题
        return isValidJson.and(
            DSL.or(
                DSL.condition(
                    "JSON_CONTAINS({0}, CONCAT('\"', {1}, '\"'))",
                    serviceScopeField,
                    DSL.inline(normalizedScope)
                ),
                DSL.condition(
                    "JSON_CONTAINS({0}, CONCAT('\"', {1}, '\"'))",
                    serviceScopeField,
                    DSL.inline(normalizedScope.lowercase())
                )
            )
        )
    }
}
