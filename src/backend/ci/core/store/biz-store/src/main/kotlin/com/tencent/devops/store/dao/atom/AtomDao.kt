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
import com.tencent.devops.common.api.constant.KEY_ALL
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
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomFeature
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
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
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.KEY_RECENT_EXECUTE_NUM
import com.tencent.devops.store.pojo.common.KEY_RECOMMEND_FLAG
import com.tencent.devops.store.pojo.common.KEY_SERVICE_SCOPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.utils.VersionUtils
import java.net.URLDecoder
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Record3
import org.jooq.Result
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

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
            dslContext.insertInto(
                this,
                ID,
                NAME,
                ATOM_CODE,
                CLASS_TYPE,
                SERVICE_SCOPE,
                JOB_TYPE,
                OS,
                CLASSIFY_ID,
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
                    JsonUtil.toJson(atomRequest.os, formatted = false),
                    atomRequest.classifyId,
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
    fun countReleaseAtomNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        with(TAtom.T_ATOM) {
            return dslContext.selectCount().from(this).where(
                ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte())
                    .and(CLASSIFY_ID.eq(classifyId))
            ).fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 统计还在使用处于下架中或者已下架状态的插件的项目的个数
     */
    fun countUndercarriageAtomNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        val a = TAtom.T_ATOM.`as`("a")
        val b = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("b")
        val atomStatusList = listOf(
            AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGED.status.toByte()
        )
        return dslContext.selectCount().from(a).join(b).on(a.ATOM_CODE.eq(b.STORE_CODE))
            .where(
                a.ATOM_STATUS.`in`(atomStatusList)
                    .and(a.CLASSIFY_ID.eq(classifyId))
            )
            .fetchOne(0, Int::class.java)!!
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TAtom.T_ATOM) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun deleteByClassifyId(dslContext: DSLContext, classifyId: String) {
        with(TAtom.T_ATOM) {
            dslContext.deleteFrom(this)
                .where(CLASSIFY_ID.eq(classifyId))
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
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        return if (defaultFlag) {
            val conditions = generateGetPipelineAtomCondition(
                tAtom = tAtom,
                atomCode = atomCode,
                version = version,
                defaultFlag = true,
                atomStatusList = atomStatusList
            )
            dslContext.selectFrom(tAtom).where(conditions).orderBy(tAtom.CREATE_TIME.desc()).limit(1).fetchOne()
        } else {
            val conditions = generateGetPipelineAtomCondition(
                tAtom = tAtom,
                atomCode = atomCode,
                version = version,
                defaultFlag = false,
                atomStatusList = atomStatusList
            )
            dslContext.selectFrom(tAtom).where(conditions)
                .andExists(
                    dslContext.selectOne().from(tStoreProjectRel).where(
                        tAtom.ATOM_CODE.eq(tStoreProjectRel.STORE_CODE)
                            .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
                            .and(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
                    )
                )
                .orderBy(tAtom.CREATE_TIME.desc()).limit(1).fetchOne()
        }
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
        serviceScope: String?,
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

            return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        }
    }

    fun getOpPipelineAtomCount(
        dslContext: DSLContext,
        atomName: String?,
        atomCode: String?,
        atomType: AtomTypeEnum?,
        serviceScope: String?,
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
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Long::class.java)!!
        }
    }

    private fun TAtom.queryOpPipelineAtomsConditions(
        atomName: String?,
        atomCode: String?,
        atomType: AtomTypeEnum?,
        serviceScope: String?,
        os: String?,
        category: String?,
        classifyId: String?,
        atomStatus: AtomStatusEnum?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        atomName?.let { conditions.add(NAME.contains(URLDecoder.decode(atomName, "UTF-8"))) }
        atomCode?.let { conditions.add(ATOM_CODE.contains(atomCode)) }
        atomType?.let { conditions.add(ATOM_TYPE.eq(atomType.type.toByte())) }
        serviceScope?.let {
            if (!"all".equals(serviceScope, true)) {
                conditions.add(SERVICE_SCOPE.contains(serviceScope))
            }
        }
        os?.let { if (!"all".equals(os, true)) conditions.add(OS.contains(os)) }
        category?.let { conditions.add(CATEGROY.eq(AtomCategoryEnum.valueOf(category).category.toByte())) }
        classifyId?.let { conditions.add(CLASSIFY_ID.eq(classifyId)) }
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
                ATOM_STATUS.`as`(KEY_ATOM_STATUS)
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

    @Suppress("UNCHECKED_CAST")
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
        val baseStep = dslContext.select(
            tAtom.VERSION.`as`(KEY_VERSION),
            tAtom.ATOM_STATUS.`as`(KEY_ATOM_STATUS)
        ).from(tAtom)
        val t = if (defaultFlag) {
            val conditions = generateGetPipelineAtomCondition(
                tAtom = tAtom,
                atomCode = atomCode,
                defaultFlag = true,
                atomStatusList = atomStatusList
            )
            baseStep.where(conditions)
        } else {
            val conditions = generateGetPipelineAtomCondition(
                tAtom = tAtom,
                atomCode = atomCode,
                defaultFlag = false,
                atomStatusList = atomStatusList
            )
            conditions.add(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
            conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
            baseStep.join(tStoreProjectRel).on(tAtom.ATOM_CODE.eq(tStoreProjectRel.STORE_CODE))
                .where(conditions)
                .groupBy(tAtom.VERSION, tAtom.ATOM_STATUS)
        }
        val firstVersion = JooqUtils.subStr(
            str = t.field(KEY_VERSION) as Field<String>,
            delim = ".",
            count = 1
        )
        val secondVersion = JooqUtils.subStr(
            str = JooqUtils.subStr(
                str = t.field(KEY_VERSION) as Field<String>,
                delim = ".",
                count = -2
            ),
            delim = ".",
            count = 1
        )
        val thirdVersion = JooqUtils.subStr(
            str = t.field(KEY_VERSION) as Field<String>,
            delim = ".",
            count = -1
        )
        val queryStep = dslContext.select(
            t.field(KEY_VERSION),
            t.field(KEY_ATOM_STATUS),
            firstVersion,
            secondVersion,
            thirdVersion
        ).from(t)
            .orderBy(firstVersion.plus(0).desc(), secondVersion.plus(0).desc(), thirdVersion.plus(0).desc())
        limitNum?.let { queryStep.limit(it) }
        return queryStep.fetch()
    }

    fun getPipelineAtoms(
        dslContext: DSLContext,
        serviceScope: String?,
        jobType: String?,
        os: String?,
        projectCode: String?,
        category: String?,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        queryProjectAtomFlag: Boolean,
        fitOsFlag: Boolean?,
        queryFitAgentBuildLessAtomFlag: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val ta = TAtom.T_ATOM
        val tc = TClassify.T_CLASSIFY
        val tspr = TStoreProjectRel.T_STORE_PROJECT_REL
        val taf = TAtomFeature.T_ATOM_FEATURE
        val tst = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
        val defaultAtomCondition = queryDefaultAtomCondition(
            ta = ta,
            taf = taf,
            tsst = tst,
            serviceScope = serviceScope,
            jobType = jobType,
            os = os,
            category = category,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            keyword = keyword,
            fitOsFlag = fitOsFlag,
            queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag
        ) // 默认插件查询条件组装
        val normalAtomConditions =
            queryNormalAtomCondition(
                ta = ta,
                tspr = tspr,
                taf = taf,
                tsst = tst,
                serviceScope = serviceScope,
                jobType = jobType,
                os = os,
                projectCode = if (queryProjectAtomFlag) projectCode else null,
                category = category,
                classifyId = classifyId,
                recommendFlag = recommendFlag,
                keyword = keyword,
                fitOsFlag = fitOsFlag,
                queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag
            ) // 普通插件查询条件组装
        val queryNormalAtomStep = getPipelineAtomBaseStep(dslContext, ta, tc, taf, tst)
        var queryInitTestAtomStep: SelectOnConditionStep<Record>? = null
        var initTestAtomCondition: MutableList<Condition>? = null
        if (!projectCode.isNullOrBlank() && (queryProjectAtomFlag || !keyword.isNullOrBlank())) {
            queryInitTestAtomStep = getPipelineAtomBaseStep(dslContext, ta, tc, taf, tst)
            initTestAtomCondition =
                queryTestAtomCondition(
                    ta = ta,
                    tspr = tspr,
                    taf = taf,
                    tsst = tst,
                    serviceScope = serviceScope,
                    jobType = jobType,
                    os = os,
                    projectCode = projectCode,
                    category = category,
                    classifyId = classifyId,
                    recommendFlag = recommendFlag,
                    keyword = keyword,
                    fitOsFlag = fitOsFlag,
                    queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag
                ) // 开发者测试插件查询条件组装
            // 默认插件和普通插件需排除初始化项目下面有处于测试中或者审核中的插件
            defaultAtomCondition.add(
                ta.ATOM_CODE.notIn(
                    dslContext.select(ta.ATOM_CODE).from(ta).join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
                        .leftJoin(taf).on(ta.ATOM_CODE.eq(taf.ATOM_CODE))
                        .leftJoin(tst).on(ta.ATOM_CODE.eq(tst.STORE_CODE))
                        .where(initTestAtomCondition)
                )
            )
            normalAtomConditions.add(
                ta.ATOM_CODE.notIn(
                    dslContext.select(ta.ATOM_CODE).from(ta).join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
                        .leftJoin(taf).on(ta.ATOM_CODE.eq(taf.ATOM_CODE))
                        .leftJoin(tst).on(ta.ATOM_CODE.eq(tst.STORE_CODE))
                        .where(initTestAtomCondition)
                )
            )
            queryNormalAtomStep.join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
            queryInitTestAtomStep.join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
        }
        val queryAtomStep = queryNormalAtomStep
            .where(normalAtomConditions)
            .union(
                getPipelineAtomBaseStep(dslContext, ta, tc, taf, tst).where(defaultAtomCondition)
            )
        if (queryInitTestAtomStep != null && initTestAtomCondition != null) {
            queryAtomStep.union(
                getPipelineAtomBaseStep(dslContext, ta, tc, taf, tst)
                    .join(tspr)
                    .on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
                    .where(initTestAtomCondition)
            )
        }
        val t = queryAtomStep.asTable("t")
        val baseStep = dslContext.select().from(t).orderBy(t.field(KEY_WEIGHT)!!.desc(), t.field(NAME)!!.asc())
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    private fun getPipelineAtomBaseStep(
        dslContext: DSLContext,
        ta: TAtom,
        tc: TClassify,
        taf: TAtomFeature,
        tsst: TStoreStatisticsTotal
    ): SelectOnConditionStep<Record> {
        return dslContext.select(
            ta.ID.`as`(KEY_ID),
            ta.ATOM_CODE.`as`(KEY_ATOM_CODE),
            ta.VERSION.`as`(VERSION),
            ta.CLASS_TYPE.`as`(KEY_CLASS_TYPE),
            ta.NAME.`as`(NAME),
            ta.OS.`as`(KEY_OS),
            ta.SERVICE_SCOPE.`as`(KEY_SERVICE_SCOPE),
            tc.ID.`as`(KEY_CLASSIFY_ID),
            tc.CLASSIFY_CODE.`as`(KEY_CLASSIFY_CODE),
            tc.CLASSIFY_NAME.`as`(KEY_CLASSIFY_NAME),
            ta.LOGO_URL.`as`(KEY_LOGO_URL),
            ta.ICON.`as`(KEY_ICON),
            ta.CATEGROY.`as`(KEY_CATEGORY),
            ta.SUMMARY.`as`(KEY_SUMMARY),
            ta.DOCS_LINK.`as`(KEY_DOCSLINK),
            ta.ATOM_TYPE.`as`(KEY_ATOM_TYPE),
            ta.ATOM_STATUS.`as`(KEY_ATOM_STATUS),
            ta.DESCRIPTION.`as`(KEY_DESCRIPTION),
            ta.PUBLISHER.`as`(KEY_PUBLISHER),
            ta.CREATOR.`as`(KEY_CREATOR),
            ta.MODIFIER.`as`(KEY_MODIFIER),
            ta.CREATE_TIME.`as`(KEY_CREATE_TIME),
            ta.UPDATE_TIME.`as`(KEY_UPDATE_TIME),
            ta.DEFAULT_FLAG.`as`(KEY_DEFAULT_FLAG),
            ta.LATEST_FLAG.`as`(KEY_LATEST_FLAG),
            ta.BUILD_LESS_RUN_FLAG.`as`(KEY_BUILD_LESS_RUN_FLAG),
            ta.WEIGHT.`as`(KEY_WEIGHT),
            ta.HTML_TEMPLATE_VERSION.`as`(KEY_HTML_TEMPLATE_VERSION),
            taf.RECOMMEND_FLAG.`as`(KEY_RECOMMEND_FLAG),
            tsst.SCORE_AVERAGE.`as`(KEY_AVG_SCORE),
            tsst.RECENT_EXECUTE_NUM.`as`(KEY_RECENT_EXECUTE_NUM),
            tsst.HOT_FLAG.`as`(KEY_HOT_FLAG)
        )
            .from(ta)
            .join(tc)
            .on(ta.CLASSIFY_ID.eq(tc.ID))
            .leftJoin(taf)
            .on(ta.ATOM_CODE.eq(taf.ATOM_CODE))
            .leftJoin(tsst)
            .on(ta.ATOM_CODE.eq(tsst.STORE_CODE))
    }

    fun getPipelineAtomCount(
        dslContext: DSLContext,
        serviceScope: String?,
        jobType: String?,
        os: String?,
        projectCode: String?,
        category: String?,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        fitOsFlag: Boolean?,
        queryProjectAtomFlag: Boolean,
        queryFitAgentBuildLessAtomFlag: Boolean?
    ): Long {
        val ta = TAtom.T_ATOM
        val tspr = TStoreProjectRel.T_STORE_PROJECT_REL
        val taf = TAtomFeature.T_ATOM_FEATURE
        val tsst = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
        val defaultAtomCondition = queryDefaultAtomCondition(
            ta = ta,
            taf = taf,
            tsst = tsst,
            serviceScope = serviceScope,
            jobType = jobType,
            os = os,
            category = category,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            keyword = keyword,
            fitOsFlag = fitOsFlag,
            queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag
        ) // 默认插件查询条件组装
        val normalAtomConditions = queryNormalAtomCondition(
            ta = ta,
            tspr = tspr,
            taf = taf,
            tsst = tsst,
            serviceScope = serviceScope,
            jobType = jobType,
            os = os,
            projectCode = if (queryProjectAtomFlag) projectCode else null,
            category = category,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            keyword = keyword,
            fitOsFlag = fitOsFlag,
            queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag
        ) // 普通插件查询条件组装
        val queryNormalAtomStep = getPipelineAtomCountBaseStep(dslContext, ta, taf, tsst)
        var queryInitTestAtomStep: SelectOnConditionStep<Record1<Int>>? = null
        var initTestAtomCondition: MutableList<Condition>? = null
        if (!projectCode.isNullOrBlank() && (queryProjectAtomFlag || !keyword.isNullOrBlank())) {
            queryInitTestAtomStep = getPipelineAtomCountBaseStep(dslContext, ta, taf, tsst)
            initTestAtomCondition = queryTestAtomCondition(
                ta = ta,
                tspr = tspr,
                taf = taf,
                tsst = tsst,
                serviceScope = serviceScope,
                jobType = jobType,
                os = os,
                projectCode = projectCode,
                category = category,
                classifyId = classifyId,
                recommendFlag = recommendFlag,
                keyword = keyword,
                fitOsFlag = fitOsFlag,
                queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag
            ) // 开发者测试插件查询条件组装
            // 默认插件和普通插件需排除初始化项目下面有处于测试中或者审核中的插件
            defaultAtomCondition.add(
                ta.ATOM_CODE.notIn(
                    dslContext.select(ta.ATOM_CODE)
                        .from(ta).join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
                        .leftJoin(taf).on(ta.ATOM_CODE.eq(taf.ATOM_CODE))
                        .leftJoin(tsst).on(ta.ATOM_CODE.eq(tsst.STORE_CODE))
                        .where(initTestAtomCondition)
                )
            )
            normalAtomConditions.add(
                ta.ATOM_CODE.notIn(
                    dslContext.select(ta.ATOM_CODE)
                        .from(ta).join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
                        .leftJoin(taf).on(ta.ATOM_CODE.eq(taf.ATOM_CODE))
                        .leftJoin(tsst).on(ta.ATOM_CODE.eq(tsst.STORE_CODE))
                        .where(initTestAtomCondition)
                )
            )
            queryNormalAtomStep.join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
            queryInitTestAtomStep.join(tspr).on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
        }
        val defaultAtomCount = getPipelineAtomCountBaseStep(dslContext, ta, taf, tsst)
            .where(defaultAtomCondition).fetchOne(0, Long::class.java)!!
        val normalAtomCount = queryNormalAtomStep.where(normalAtomConditions).fetchOne(0, Long::class.java)!!
        val initTestAtomCount = if (initTestAtomCondition != null && queryInitTestAtomStep != null) {
            queryInitTestAtomStep.where(initTestAtomCondition).fetchOne(0, Long::class.java)!!
        } else {
            0
        }
        return defaultAtomCount + normalAtomCount + initTestAtomCount
    }

    private fun getPipelineAtomCountBaseStep(
        dslContext: DSLContext,
        ta: TAtom,
        taf: TAtomFeature,
        tsst: TStoreStatisticsTotal
    ): SelectOnConditionStep<Record1<Int>> {
        return dslContext.select(DSL.countDistinct(ta.ATOM_CODE)).from(ta)
            .leftJoin(taf)
            .on(ta.ATOM_CODE.eq(taf.ATOM_CODE))
            .leftJoin(tsst)
            .on(ta.ATOM_CODE.eq(tsst.STORE_CODE))
    }

    private fun queryDefaultAtomCondition(
        ta: TAtom,
        taf: TAtomFeature,
        tsst: TStoreStatisticsTotal,
        serviceScope: String?,
        jobType: String?,
        os: String?,
        category: String?,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        fitOsFlag: Boolean?,
        queryFitAgentBuildLessAtomFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = setQueryAtomBaseCondition(
            serviceScope = serviceScope,
            ta = ta,
            taf = taf,
            tsst = tsst,
            jobType = jobType,
            os = os,
            category = category,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            keyword = keyword,
            fitOsFlag = fitOsFlag,
            queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag
        )
        conditions.add(ta.ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte())) // 只查已发布的
        conditions.add(ta.DEFAULT_FLAG.eq(true)) // 查默认插件（所有项目都可用）
        conditions.add(ta.LATEST_FLAG.eq(true)) // 只查最新版本的插件
        return conditions
    }

    private fun setQueryAtomBaseCondition(
        serviceScope: String?,
        ta: TAtom,
        taf: TAtomFeature,
        tsst: TStoreStatisticsTotal,
        jobType: String?,
        os: String?,
        category: String?,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        fitOsFlag: Boolean?,
        queryFitAgentBuildLessAtomFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (!serviceScope.isNullOrBlank() && !KEY_ALL.equals(serviceScope, true)) {
            conditions.add(ta.SERVICE_SCOPE.contains(serviceScope))
        }
        // 当筛选有构建环境的插件时也需加上那些无构建环境插件可以在有构建环境运行的插件
        if (!jobType.isNullOrBlank()) {
            if (jobType == JobTypeEnum.AGENT.name && queryFitAgentBuildLessAtomFlag != false) {
                conditions.add(ta.JOB_TYPE.eq(jobType).or(ta.BUILD_LESS_RUN_FLAG.eq(true)))
            } else {
                conditions.add(ta.JOB_TYPE.eq(jobType))
                if (queryFitAgentBuildLessAtomFlag == false) {
                    conditions.add(ta.BUILD_LESS_RUN_FLAG.ne(true))
                }
            }
        }
        if (!os.isNullOrBlank() && !KEY_ALL.equals(os, true)) {
            if (fitOsFlag == false) {
                conditions.add(
                    (ta.OS.notLike("%$os%")
                        .and(ta.BUILD_LESS_RUN_FLAG.ne(true).or(ta.BUILD_LESS_RUN_FLAG.isNull)))
                        .and(ta.CATEGROY.eq(AtomCategoryEnum.TASK.category.toByte()))
                )
            } else {
                conditions.add(ta.OS.contains(os).or(ta.BUILD_LESS_RUN_FLAG.eq(true)))
            }
        } else if (KEY_ALL.equals(os, true)) {
            if (fitOsFlag == false) {
                conditions.add(ta.JOB_TYPE.eq(JobTypeEnum.AGENT_LESS.name))
            }
        }
        if (null != category) conditions.add(ta.CATEGROY.eq(AtomCategoryEnum.valueOf(category).category.toByte()))
        if (!classifyId.isNullOrBlank()) conditions.add(ta.CLASSIFY_ID.eq(classifyId))
        if (null != recommendFlag) {
            if (recommendFlag) {
                conditions.add(taf.RECOMMEND_FLAG.eq(recommendFlag).or(taf.RECOMMEND_FLAG.isNull))
            } else {
                conditions.add(taf.RECOMMEND_FLAG.eq(recommendFlag))
            }
        }
        if (!keyword.isNullOrEmpty()) {
            conditions.add(ta.NAME.contains(keyword).or(ta.SUMMARY.contains(keyword)))
        }
        conditions.add(ta.DELETE_FLAG.eq(false)) // 只查没有被删除的插件
        conditions.add(tsst.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        return conditions
    }

    private fun queryNormalAtomCondition(
        ta: TAtom,
        tspr: TStoreProjectRel,
        taf: TAtomFeature,
        tsst: TStoreStatisticsTotal,
        serviceScope: String?,
        jobType: String?,
        os: String?,
        projectCode: String?,
        category: String?,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        fitOsFlag: Boolean?,
        queryFitAgentBuildLessAtomFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = setQueryAtomBaseCondition(
            serviceScope = serviceScope,
            ta = ta,
            taf = taf,
            tsst = tsst,
            jobType = jobType,
            os = os,
            category = category,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            keyword = keyword,
            fitOsFlag = fitOsFlag,
            queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag
        )
        conditions.add(ta.ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte())) // 只查已发布的
        conditions.add(ta.DEFAULT_FLAG.eq(false)) // 查普通插件
        conditions.add(ta.LATEST_FLAG.eq(true)) // 只查最新版本的插件
        if (!projectCode.isNullOrBlank()) {
            conditions.add(tspr.PROJECT_CODE.eq(projectCode))
            conditions.add(tspr.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        }
        return conditions
    }

    private fun queryTestAtomCondition(
        ta: TAtom,
        tspr: TStoreProjectRel,
        taf: TAtomFeature,
        tsst: TStoreStatisticsTotal,
        serviceScope: String?,
        jobType: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        fitOsFlag: Boolean?,
        queryFitAgentBuildLessAtomFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = setQueryAtomBaseCondition(
            serviceScope = serviceScope,
            ta = ta,
            taf = taf,
            tsst = tsst,
            jobType = jobType,
            os = os,
            category = category,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            keyword = keyword,
            fitOsFlag = fitOsFlag,
            queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag
        )
        conditions.add(
            ta.ATOM_STATUS.`in`(
                listOf(
                    AtomStatusEnum.TESTING.status.toByte(),
                    AtomStatusEnum.AUDITING.status.toByte()
                )
            )
        ) // 只查测试中和审核中的插件
        conditions.add(tspr.PROJECT_CODE.eq(projectCode))
        conditions.add(tspr.TYPE.`in`(listOf(StoreProjectTypeEnum.TEST.type.toByte()))) // 调试项目
        conditions.add(tspr.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        return conditions
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
                .set(SERVICE_SCOPE, JsonUtil.toJson(atomUpdateRequest.serviceScope, formatted = false))
                .set(JOB_TYPE, atomUpdateRequest.jobType.name)
                .set(OS, JsonUtil.toJson(atomUpdateRequest.os, formatted = false))
                .set(CLASS_TYPE, classType)
                .set(CLASSIFY_ID, atomUpdateRequest.classifyId)
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
                baseStep.set(PRIVATE_REASON, "") // 选择开源则清空不开源原因
            } else {
                if (null != privateReason) {
                    baseStep.set(PRIVATE_REASON, privateReason)
                }
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
        projectCode: String,
        classifyCode: String? = null,
        name: String? = null
    ): Int {
        val (ta, tspr, conditions) = getInstalledConditions(projectCode, classifyCode, name, dslContext)

        return dslContext.select(DSL.countDistinct(ta.ATOM_CODE))
            .from(ta)
            .join(tspr)
            .on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Int::class.java)!!
    }

    /**
     * 获取已安装的插件
     */
    fun getInstalledAtoms(
        dslContext: DSLContext,
        projectCode: String,
        classifyCode: String? = null,
        name: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<out Record>? {

        val (ta, tspr, conditions) = getInstalledConditions(projectCode, classifyCode, name, dslContext)
        val tc = TClassify.T_CLASSIFY
        // 查找每组atomCode最新的记录
        val t = dslContext.select(ta.ATOM_CODE.`as`(KEY_ATOM_CODE), DSL.max(ta.CREATE_TIME).`as`(KEY_CREATE_TIME))
            .from(ta).groupBy(ta.ATOM_CODE)

        val sql = dslContext.select(
            ta.ID.`as`(KEY_ID),
            ta.ATOM_CODE.`as`(KEY_ATOM_CODE),
            ta.VERSION.`as`(KEY_VERSION),
            ta.NAME.`as`(NAME),
            ta.LOGO_URL.`as`(KEY_LOGO_URL),
            ta.CATEGROY.`as`(KEY_CATEGORY),
            ta.SUMMARY.`as`(KEY_SUMMARY),
            ta.PUBLISHER.`as`(KEY_PUBLISHER),
            tc.ID.`as`(KEY_CLASSIFY_ID),
            tc.CLASSIFY_CODE.`as`(KEY_CLASSIFY_CODE),
            tc.CLASSIFY_NAME.`as`(KEY_CLASSIFY_NAME),
            tspr.CREATOR.`as`(KEY_INSTALLER),
            tspr.CREATE_TIME.`as`(KEY_INSTALL_TIME),
            tspr.TYPE.`as`(KEY_INSTALL_TYPE)
        )
            .from(ta)
            .join(t)
            .on(
                ta.ATOM_CODE.eq(t.field(KEY_ATOM_CODE, String::class.java))
                    .and(ta.CREATE_TIME.eq(t.field(KEY_CREATE_TIME, LocalDateTime::class.java)))
            )
            .join(tc)
            .on(ta.CLASSIFY_ID.eq(tc.ID))
            .join(tspr)
            .on(ta.ATOM_CODE.eq(tspr.STORE_CODE))
            .where(conditions)
            .groupBy(ta.ATOM_CODE)
            .orderBy(tspr.TYPE.asc(), tspr.CREATE_TIME.desc())
        if (page != null && pageSize != null) sql.limit((page - 1) * pageSize, pageSize)
        return sql.fetch()
    }

    private fun getInstalledConditions(
        projectCode: String,
        classifyCode: String?,
        name: String?,
        dslContext: DSLContext
    ): Triple<TAtom, TStoreProjectRel, MutableList<Condition>> {
        val ta = TAtom.T_ATOM
        val tspr = TStoreProjectRel.T_STORE_PROJECT_REL
        val conditions = mutableListOf<Condition>()
        conditions.add(tspr.PROJECT_CODE.eq(projectCode))
        conditions.add(tspr.STORE_TYPE.eq(0))
        if (!classifyCode.isNullOrEmpty()) {
            val tClassify = TClassify.T_CLASSIFY
            val classifyId = dslContext.select(tClassify.ID)
                .from(tClassify)
                .where(tClassify.CLASSIFY_CODE.eq(classifyCode).and(tClassify.TYPE.eq(0)))
                .fetchOne(0, String::class.java)
            conditions.add(ta.CLASSIFY_ID.eq(classifyId))
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
            val classifyCode = atomBaseInfoUpdateRequest.classifyCode
            if (null != classifyCode) {
                val tClassify = TClassify.T_CLASSIFY
                val classifyId = dslContext.select(tClassify.ID)
                    .from(tClassify)
                    .where(tClassify.CLASSIFY_CODE.eq(classifyCode).and(tClassify.TYPE.eq(0)))
                    .fetchOne(0, String::class.java)
                baseStep.set(CLASSIFY_ID, classifyId)
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

    fun getPublishedAtomCount(
        dslContext: DSLContext
    ): Int {
        with(TAtom.T_ATOM) {
            return dslContext.select()
                .from(this)
                .where(ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))
                .groupBy(ATOM_CODE)
                .execute()
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
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        return if (defaultFlag) {
            val conditions = generateGetPipelineAtomCondition(
                tAtom = tAtom,
                atomCode = atomCode,
                version = version,
                defaultFlag = true,
                atomStatusList = atomStatusList
            )
            dslContext.select(tAtom.VERSION).from(tAtom)
                .where(conditions)
                .orderBy(tAtom.CREATE_TIME.desc()).limit(1).fetchOne(0, String::class.java)
        } else {
            val conditions = generateGetPipelineAtomCondition(
                tAtom = tAtom,
                atomCode = atomCode,
                version = version,
                defaultFlag = false,
                atomStatusList = atomStatusList
            )
            dslContext.select(tAtom.VERSION).from(tAtom)
                .where(conditions)
                .andExists(
                    dslContext.selectOne().from(tStoreProjectRel).where(
                        tAtom.ATOM_CODE.eq(tStoreProjectRel.STORE_CODE)
                            .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
                            .and(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
                    )
                )
                .orderBy(tAtom.CREATE_TIME.desc()).limit(1).fetchOne(0, String::class.java)
        }
    }
}
