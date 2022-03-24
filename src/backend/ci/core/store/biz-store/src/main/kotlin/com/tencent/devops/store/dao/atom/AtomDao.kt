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
import com.tencent.devops.common.service.utils.JooqUtils
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomFeature
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.pojo.atom.AtomBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomFeatureUpdateRequest
import com.tencent.devops.store.pojo.atom.AtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.common.KEY_ATOM_STATUS
import com.tencent.devops.store.pojo.common.KEY_VERSION
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.net.URLDecoder
import java.time.LocalDateTime

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
                    JsonUtil.getObjectMapper().writeValueAsString(atomRequest.serviceScope),
                    atomRequest.jobType.name,
                    JsonUtil.getObjectMapper().writeValueAsString(atomRequest.os),
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
        atomType: AtomTypeEnum?,
        serviceScope: String?,
        os: String?,
        category: String?,
        classifyId: String?,
        atomStatus: AtomStatusEnum?,
        sortType: String?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<TAtomRecord> {
        with(TAtom.T_ATOM) {
            val conditions =
                queryOpPipelineAtomsConditions(atomName, atomType, serviceScope, os, category, classifyId, atomStatus)
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

            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    fun getOpPipelineAtomCount(
        dslContext: DSLContext,
        atomName: String?,
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
        atomType: AtomTypeEnum?,
        serviceScope: String?,
        os: String?,
        category: String?,
        classifyId: String?,
        atomStatus: AtomStatusEnum?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        atomName?.let { conditions.add(NAME.contains(URLDecoder.decode(atomName, "UTF-8"))) }
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
        atomStatusList: List<Byte>?
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
                .groupBy(tAtom.ATOM_CODE, tAtom.ATOM_STATUS)
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
        return dslContext.select(
            t.field(KEY_VERSION),
            t.field(KEY_ATOM_STATUS),
            firstVersion,
            secondVersion,
            thirdVersion
        ).from(t)
            .orderBy(firstVersion.plus(0).desc(), secondVersion.plus(0).desc(), thirdVersion.plus(0).desc())
            .fetch()
    }

    fun getPipelineAtoms(
        dslContext: DSLContext,
        serviceScope: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val a = TAtom.T_ATOM.`as`("a")
        val b = TClassify.T_CLASSIFY.`as`("b")
        val c = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("c")
        val d = TAtomFeature.T_ATOM_FEATURE.`as`("d")
        val defaultAtomCondition = queryDefaultAtomCondition(
            a = a,
            serviceScope = serviceScope,
            os = os,
            category = category,
            classifyId = classifyId
        ) // 默认插件查询条件组装
        val normalAtomConditions =
            queryNormalAtomCondition(
                a = a,
                c = c,
                serviceScope = serviceScope,
                os = os,
                projectCode = projectCode,
                category = category,
                classifyId = classifyId
            ) // 普通插件查询条件组装
        val initTestAtomCondition =
            queryTestAtomCondition(
                a = a,
                c = c,
                serviceScope = serviceScope,
                os = os,
                projectCode = projectCode,
                category = category,
                classifyId = classifyId
            ) // 开发者测试插件查询条件组装
        // 默认插件和普通插件需排除初始化项目下面有处于测试中或者审核中的插件
        defaultAtomCondition.add(
            a.ATOM_CODE.notIn(
                dslContext.select(a.ATOM_CODE).from(a).join(c).on(a.ATOM_CODE.eq(c.STORE_CODE))
                    .where(initTestAtomCondition)
            )
        )
        normalAtomConditions.add(
            a.ATOM_CODE.notIn(
                dslContext.select(a.ATOM_CODE).from(a).join(c).on(a.ATOM_CODE.eq(c.STORE_CODE))
                    .where(initTestAtomCondition)
            )
        )
        val t = getPipelineAtomBaseStep(dslContext, a, b, d)
            .join(c)
            .on(a.ATOM_CODE.eq(c.STORE_CODE))
            .where(normalAtomConditions)
            .union(
                getPipelineAtomBaseStep(dslContext, a, b, d).where(defaultAtomCondition)
            )
            .union(
                getPipelineAtomBaseStep(dslContext, a, b, d)
                    .join(c)
                    .on(a.ATOM_CODE.eq(c.STORE_CODE))
                    .where(initTestAtomCondition)
            )
            .asTable("t")
        val baseStep = dslContext.select().from(t).orderBy(t.field("weight")!!.desc(), t.field("name")!!.asc())
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    private fun getPipelineAtomBaseStep(
        dslContext: DSLContext,
        a: TAtom,
        b: TClassify,
        d: TAtomFeature
    ): SelectOnConditionStep<Record> {
        return dslContext.select(
            a.ATOM_CODE.`as`("atomCode"),
            a.VERSION.`as`("version"),
            a.CLASS_TYPE.`as`("classType"),
            a.NAME.`as`("name"),
            a.OS.`as`("os"),
            a.SERVICE_SCOPE.`as`("serviceScope"),
            b.ID.`as`("classifyId"),
            b.CLASSIFY_CODE.`as`("classifyCode"),
            b.CLASSIFY_NAME.`as`("classifyName"),
            a.LOGO_URL.`as`("logoUrl"),
            a.ICON.`as`("icon"),
            a.CATEGROY.`as`("category"),
            a.SUMMARY.`as`("summary"),
            a.DOCS_LINK.`as`("docsLink"),
            a.ATOM_TYPE.`as`("atomType"),
            a.ATOM_STATUS.`as`("atomStatus"),
            a.DESCRIPTION.`as`("description"),
            a.PUBLISHER.`as`("publisher"),
            a.CREATOR.`as`("creator"),
            a.CREATE_TIME.`as`("createTime"),
            a.DEFAULT_FLAG.`as`("defaultFlag"),
            a.LATEST_FLAG.`as`("latestFlag"),
            a.BUILD_LESS_RUN_FLAG.`as`("buildLessRunFlag"),
            a.WEIGHT.`as`("weight"),
            a.HTML_TEMPLATE_VERSION.`as`("htmlTemplateVersion"),
            d.RECOMMEND_FLAG.`as`("recommendFlag")
        )
            .from(a)
            .join(b)
            .on(a.CLASSIFY_ID.eq(b.ID))
            .leftJoin(d)
            .on(a.ATOM_CODE.eq(d.ATOM_CODE))
    }

    fun getPipelineAtomCount(
        dslContext: DSLContext,
        serviceScope: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?
    ): Long {
        val a = TAtom.T_ATOM.`as`("a")
        val c = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("c")
        val defaultAtomCondition = queryDefaultAtomCondition(
            a = a,
            serviceScope = serviceScope,
            os = os,
            category = category,
            classifyId = classifyId
        ) // 默认插件查询条件组装
        val normalAtomConditions = queryNormalAtomCondition(
            a = a,
            c = c,
            serviceScope = serviceScope,
            os = os,
            projectCode = projectCode,
            category = category,
            classifyId = classifyId
        ) // 普通插件查询条件组装
        val initTestAtomCondition = queryTestAtomCondition(
            a = a,
            c = c,
            serviceScope = serviceScope,
            os = os,
            projectCode = projectCode,
            category = category,
            classifyId = classifyId
        ) // 开发者测试插件查询条件组装
        // 默认插件和普通插件需排除初始化项目下面有处于测试中或者审核中的插件
        defaultAtomCondition.add(
            a.ATOM_CODE.notIn(
                dslContext.select(a.ATOM_CODE)
                    .from(a).join(c).on(a.ATOM_CODE.eq(c.STORE_CODE)).where(initTestAtomCondition)
            )
        )
        normalAtomConditions.add(
            a.ATOM_CODE.notIn(
                dslContext.select(a.ATOM_CODE)
                    .from(a).join(c).on(a.ATOM_CODE.eq(c.STORE_CODE)).where(initTestAtomCondition)
            )
        )
        val defaultAtomCount = dslContext.selectCount().from(a)
            .where(defaultAtomCondition).fetchOne(0, Long::class.java)!!
        val normalAtomCount =
            dslContext.selectCount().from(a).join(c).on(a.ATOM_CODE.eq(c.STORE_CODE)).where(normalAtomConditions)
                .fetchOne(0, Long::class.java)!!
        val initTestAtomCount =
            dslContext.selectCount().from(a).join(c).on(a.ATOM_CODE.eq(c.STORE_CODE)).where(initTestAtomCondition)
                .fetchOne(0, Long::class.java)!!
        return defaultAtomCount + normalAtomCount + initTestAtomCount
    }

    private fun queryDefaultAtomCondition(
        a: TAtom,
        serviceScope: String?,
        os: String?,
        category: String?,
        classifyId: String?
    ): MutableList<Condition> {
        val conditions = setQueryAtomBaseCondition(serviceScope, a, os, category, classifyId)
        conditions.add(a.ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte())) // 只查已发布的
        conditions.add(a.DEFAULT_FLAG.eq(true)) // 查默认插件（所有项目都可用）
        conditions.add(a.LATEST_FLAG.eq(true)) // 只查最新版本的插件
        return conditions
    }

    private fun setQueryAtomBaseCondition(
        serviceScope: String?,
        a: TAtom,
        os: String?,
        category: String?,
        classifyId: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        serviceScope?.let {
            if (!"all".equals(serviceScope, true)) {
                conditions.add(a.SERVICE_SCOPE.contains(serviceScope))
            }
        }
        // 当筛选有构建环境的插件时也需加上那些无构建环境插件可以在有构建环境运行的插件
        os?.let {
            if (!"all".equals(os, true)) {
                conditions.add(a.OS.contains(os).or(a.BUILD_LESS_RUN_FLAG.eq(true)))
            }
        }
        category?.let { conditions.add(a.CATEGROY.eq(AtomCategoryEnum.valueOf(category).category.toByte())) }
        classifyId?.let { conditions.add(a.CLASSIFY_ID.eq(classifyId)) }
        conditions.add(a.DELETE_FLAG.eq(false)) // 只查没有被删除的插件
        return conditions
    }

    private fun queryNormalAtomCondition(
        a: TAtom,
        c: TStoreProjectRel,
        serviceScope: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?
    ): MutableList<Condition> {
        val conditions = setQueryAtomBaseCondition(serviceScope, a, os, category, classifyId)
        conditions.add(a.ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte())) // 只查已发布的
        conditions.add(a.DEFAULT_FLAG.eq(false)) // 查普通插件
        conditions.add(a.LATEST_FLAG.eq(true)) // 只查最新版本的插件
        conditions.add(c.PROJECT_CODE.eq(projectCode))
        conditions.add(c.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        return conditions
    }

    private fun queryTestAtomCondition(
        a: TAtom,
        c: TStoreProjectRel,
        serviceScope: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?
    ): MutableList<Condition> {
        val conditions = setQueryAtomBaseCondition(
            serviceScope = serviceScope,
            a = a,
            os = os,
            category = category,
            classifyId = classifyId
        )
        conditions.add(
            a.ATOM_STATUS.`in`(
                listOf(
                    AtomStatusEnum.TESTING.status.toByte(),
                    AtomStatusEnum.AUDITING.status.toByte()
                )
            )
        ) // 只查测试中和审核中的插件
        conditions.add(c.PROJECT_CODE.eq(projectCode))
        conditions.add(c.TYPE.`in`(listOf(StoreProjectTypeEnum.TEST.type.toByte()))) // 调试项目
        conditions.add(c.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
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
                .set(SERVICE_SCOPE, JsonUtil.getObjectMapper().writeValueAsString(atomUpdateRequest.serviceScope))
                .set(JOB_TYPE, atomUpdateRequest.jobType.name)
                .set(OS, JsonUtil.getObjectMapper().writeValueAsString(atomUpdateRequest.os))
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
        classifyCode: String?
    ): Int {
        val (ta, tspr, conditions) = getInstalledConditions(projectCode, classifyCode, dslContext)

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
        classifyCode: String?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {

        val (ta, tspr, conditions) = getInstalledConditions(projectCode, classifyCode, dslContext)
        val tc = TClassify.T_CLASSIFY.`as`("tc")
        // 查找每组atomCode最新的记录
        val t = dslContext.select(ta.ATOM_CODE.`as`("atomCode"), DSL.max(ta.CREATE_TIME).`as`("createTime"))
            .from(ta).groupBy(ta.ATOM_CODE)

        val sql = dslContext.select(
            ta.ID.`as`("atomId"),
            ta.ATOM_CODE.`as`("atomCode"),
            ta.NAME.`as`("atomName"),
            ta.LOGO_URL.`as`("logoUrl"),
            ta.CATEGROY.`as`("category"),
            ta.SUMMARY.`as`("summary"),
            ta.PUBLISHER.`as`("publisher"),
            tc.ID.`as`("classifyId"),
            tc.CLASSIFY_CODE.`as`("classifyCode"),
            tc.CLASSIFY_NAME.`as`("classifyName"),
            tspr.CREATOR.`as`("installer"),
            tspr.CREATE_TIME.`as`("installTime"),
            tspr.TYPE.`as`("installType")
        )
            .from(ta)
            .join(t)
            .on(
                ta.ATOM_CODE.eq(t.field("atomCode", String::class.java))
                    .and(ta.CREATE_TIME.eq(t.field("createTime", LocalDateTime::class.java)))
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
        dslContext: DSLContext
    ): Triple<TAtom, TStoreProjectRel, MutableList<Condition>> {
        val ta = TAtom.T_ATOM.`as`("ta")
        val tspr = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("tspr")
        val conditions = mutableListOf<Condition>()
        conditions.add(tspr.PROJECT_CODE.eq(projectCode))
        conditions.add(tspr.STORE_TYPE.eq(0))
        if (!classifyCode.isNullOrEmpty()) {
            val a = TClassify.T_CLASSIFY.`as`("a")
            val classifyId = dslContext.select(a.ID)
                .from(a)
                .where(a.CLASSIFY_CODE.eq(classifyCode).and(a.TYPE.eq(0)))
                .fetchOne(0, String::class.java)
            conditions.add(ta.CLASSIFY_ID.eq(classifyId))
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
                val a = TClassify.T_CLASSIFY.`as`("a")
                val classifyId = dslContext.select(a.ID)
                    .from(a)
                    .where(a.CLASSIFY_CODE.eq(classifyCode).and(a.TYPE.eq(0)))
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

    fun getDefaultAtoms(dslContext: DSLContext, atomList: List<String>): Result<TAtomRecord>? {
        return with(TAtom.T_ATOM) {
            dslContext.selectFrom(this)
                .where(ATOM_CODE.`in`(atomList).and(DEFAULT_FLAG.eq(true)))
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
