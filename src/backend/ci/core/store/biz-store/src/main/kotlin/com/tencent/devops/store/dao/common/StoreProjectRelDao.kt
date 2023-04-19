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

package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TStoreProjectRelRecord
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record2
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class StoreProjectRelDao {

    fun addStoreProjectRel(
        dslContext: DSLContext,
        userId: String,
        storeCode: String,
        projectCode: String,
        type: Byte,
        storeType: Byte
    ): Int {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                PROJECT_CODE,
                TYPE,
                STORE_TYPE,
                CREATOR,
                MODIFIER
            ).values(
                UUIDUtil.generate(),
                storeCode,
                projectCode,
                type,
                storeType,
                userId,
                userId
            ).onDuplicateKeyUpdate()
                .set(PROJECT_CODE, projectCode)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun getInitProjectCodeByStoreCode(dslContext: DSLContext, storeCode: String, storeType: Byte): String? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.select(PROJECT_CODE).from(this)
                .where(STORE_CODE.eq(storeCode)
                    .and(STORE_TYPE.eq(storeType))
                    .and(TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
                )
                .fetchOne(0, String::class.java)
        }
    }

    fun getTestProjectCodesByStoreCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<Record1<String>>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.select(PROJECT_CODE).from(this)
                .where(
                    STORE_CODE.eq(storeCode)
                        .and(STORE_TYPE.eq(storeType.type.toByte()))
                        .and(TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
                )
                .fetch()
        }
    }

    fun countInstalledProject(dslContext: DSLContext, projectCode: String, storeCode: String, storeType: Byte): Int {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_CODE.eq(projectCode)
                    .and(STORE_CODE.eq(storeCode))
                    .and(STORE_TYPE.eq(storeType))
                )
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countInstalledProject(dslContext: DSLContext, storeCode: String, storeType: Byte): Int {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectCount().from(this)
                .where(STORE_CODE.eq(storeCode)
                    .and(STORE_TYPE.eq(storeType))
                    .and(TYPE.eq(1))
                )
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 根据商城组件标识和用户已授权的项目列表，查询已安装商城组件的项目列表
     */
    fun getInstalledProject(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        authorizedProjectCodeList: Set<String>
    ): Result<TStoreProjectRelRecord>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)))
                .and(PROJECT_CODE.`in`(authorizedProjectCodeList))
                .groupBy(PROJECT_CODE)
                .fetch()
        }
    }

    fun getStoreInitProjectCount(
        dslContext: DSLContext,
        storeType: Byte,
        descFlag: Boolean = true,
        specProjectCodeList: Set<String>? = null,
        grayFlag: Boolean? = null,
        grayProjectCodeList: Set<String>? = null
    ): Long {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions =
                getStoreInitProjectsCondition(
                    storeType = storeType,
                    specProjectCodeList = specProjectCodeList,
                    grayFlag = grayFlag,
                    grayProjectCodeList = grayProjectCodeList
                )
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Long::class.java)!!
        }
    }

    fun getStoreInitProjects(
        dslContext: DSLContext,
        storeType: Byte,
        descFlag: Boolean = true,
        specProjectCodeList: Set<String>? = null,
        grayFlag: Boolean? = null,
        grayProjectCodeList: Set<String>? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<TStoreProjectRelRecord>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions =
                getStoreInitProjectsCondition(
                    storeType = storeType,
                    specProjectCodeList = specProjectCodeList,
                    grayFlag = grayFlag,
                    grayProjectCodeList = grayProjectCodeList
                )
            val baseStep = dslContext.selectFrom(this).where(conditions)
            if (descFlag) {
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

    private fun TStoreProjectRel.getStoreInitProjectsCondition(
        storeType: Byte,
        specProjectCodeList: Set<String>?,
        grayFlag: Boolean?,
        grayProjectCodeList: Set<String>?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(STORE_TYPE.eq(storeType))
        conditions.add(TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
        if (specProjectCodeList != null) {
            conditions.add(PROJECT_CODE.`in`(specProjectCodeList))
        }
        if (grayFlag != null && grayProjectCodeList != null) {
            if (grayFlag) {
                conditions.add(PROJECT_CODE.`in`(grayProjectCodeList))
            } else {
                conditions.add(PROJECT_CODE.notIn(grayProjectCodeList))
            }
        }
        return conditions
    }

    /**
     * 获取项目下已安装的插件
     */
    fun getInstalledComponent(
        dslContext: DSLContext,
        projectCode: String,
        storeType: Byte,
        offset: Int? = 0,
        limit: Int? = -1
    ): Result<TStoreProjectRelRecord>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val baseQuery = dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(STORE_TYPE.eq(storeType))
            if (offset != null && offset >= 0) {
                baseQuery.offset(offset)
            }
            if (limit != null && limit > 0) {
                baseQuery.limit(limit)
            }
            return baseQuery.fetch()
        }
    }

    /**
     * 卸载时删除关联关系
     */
    fun deleteRel(dslContext: DSLContext, storeCode: String, storeType: Byte, projectCode: String) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode)
                    .and(PROJECT_CODE.eq(projectCode))
                    .and(STORE_TYPE.eq(storeType))
                )
                .and(TYPE.eq(1))
                .execute()
        }
    }

    /**
     * 删除关联关系
     */
    fun deleteAllRel(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            dslContext.deleteFrom(this).where(STORE_CODE.eq(storeCode)).and(STORE_TYPE.eq(storeType)).execute()
        }
    }

    /**
     * 判断用户是否为安装人
     */
    fun isInstaller(dslContext: DSLContext, userId: String, storeCode: String, storeType: Byte): Boolean {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectCount()
                .from(this)
                .where(STORE_CODE.eq(storeCode))
                .and(CREATOR.eq(userId))
                .and(TYPE.eq(StoreProjectTypeEnum.COMMON.type.toByte()))
                .and(STORE_TYPE.eq(storeType))
                .fetchOne(0, Long::class.java) != 0L
        }
    }

    /**
     * 判断组件是否被项目安装
     * 无论初始化项目、调试项目还是协作项目，均视为已安装
     */
    fun isInstalledByProject(dslContext: DSLContext, projectCode: String, storeCode: String, storeType: Byte): Boolean {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .fetchOne(0, Long::class.java) != 0L
        }
    }

    /**
     * 判断用户是否为store组件创建人
     */
    fun isStoreCreator(dslContext: DSLContext, userId: String, storeCode: String, storeType: Byte): Boolean {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectCount()
                .from(this)
                .where(STORE_CODE.eq(storeCode))
                .and(CREATOR.eq(userId))
                .and(TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
                .and(STORE_TYPE.eq(storeType))
                .fetchOne(0, Long::class.java) != 0L
        }
    }

    /**
     * 判断项目是否为调试项目
     */
    fun isTestProjectCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        projectCode: String
    ): Boolean {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectCount()
                .from(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .and(PROJECT_CODE.eq(projectCode))
                .and(TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
                .fetchOne(0, Long::class.java) != 0L
        }
    }

    /**
     * 获取用户的组件设定的调试项目
     */
    fun getUserStoreTestProjectCode(
        dslContext: DSLContext,
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): String? {
        val a = TStoreMember.T_STORE_MEMBER.`as`("a")
        val b = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("b")
        val finalStep = dslContext.select(b.PROJECT_CODE)
            .from(a)
            .join(b)
            .on(a.STORE_CODE.eq(b.STORE_CODE).and(a.STORE_TYPE.eq(b.STORE_TYPE)))
            .where(a.USERNAME.eq(userId))
            .and(b.STORE_CODE.eq(storeCode))
            .and(b.TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
            .and(b.CREATOR.eq(userId))
            .and(a.STORE_TYPE.eq(storeType.type.toByte()))
        return finalStep.fetchOne(0, String::class.java)
    }

    /**
     * 更新用户的组件设定的调试项目
     */
    fun updateUserStoreTestProject(
        dslContext: DSLContext,
        userId: String,
        projectCode: String,
        storeProjectType: StoreProjectTypeEnum,
        storeCode: String,
        storeType: StoreTypeEnum
    ) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val record = dslContext.selectFrom(this)
                .where(CREATOR.eq(userId))
                .and(TYPE.eq(storeProjectType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .fetchOne()
            if (null == record) {
                addStoreProjectRel(
                    dslContext = dslContext,
                    userId = userId,
                    storeCode = storeCode,
                    projectCode = projectCode,
                    type = storeProjectType.type.toByte(),
                    storeType = storeType.type.toByte()
                )
            } else {
                dslContext.update(this)
                    .set(PROJECT_CODE, projectCode)
                    .set(MODIFIER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(CREATOR.eq(userId))
                    .and(TYPE.eq(storeProjectType.type.toByte()))
                    .and(STORE_CODE.eq(storeCode))
                    .and(STORE_TYPE.eq(storeType.type.toByte()))
                    .execute()
            }
        }
    }

    /**
     * 删除用户的组件设定的调试项目
     */
    fun deleteUserStoreTestProject(
        dslContext: DSLContext,
        userId: String,
        storeProjectType: StoreProjectTypeEnum,
        storeCode: String,
        storeType: StoreTypeEnum
    ) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            dslContext.deleteFrom(this)
                .where(CREATOR.eq(userId))
                .and(TYPE.eq(storeProjectType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .execute()
        }
    }

    /**
     * 获取项目的调试组件
     */
    fun getTestStoreCodes(
        dslContext: DSLContext,
        projectCode: String,
        storeType: StoreTypeEnum,
        storeCodeList: List<String>? = null
    ): Result<Record1<String>>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_CODE.eq(projectCode))
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            conditions.add(TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
            if (storeCodeList != null) {
                conditions.add(STORE_CODE.`in`(storeCodeList))
            }
            return dslContext.select(STORE_CODE).from(this).where(conditions).fetch()
        }
    }

    fun getValidStoreCodes(
        dslContext: DSLContext,
        projectCode: String,
        storeType: StoreTypeEnum
    ): Result<Record2<String, Byte>>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_CODE.eq(projectCode))
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            return dslContext.select(STORE_CODE, TYPE).from(this).where(conditions).fetch()
        }
    }

    fun countInstallNumByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null
    ): Int {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            conditions.add(TYPE.eq(StoreProjectTypeEnum.COMMON.type.toByte()))
            if (startTime != null) {
                conditions.add(CREATE_TIME.ge(startTime))
            }
            if (endTime != null) {
                conditions.add(CREATE_TIME.lt(endTime))
            }
            return dslContext.selectCount().from(this).where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 获取该项目可用的组件
     */
    fun getValidStoreCodesByProject(
        dslContext: DSLContext,
        projectCode: String,
        storeCodes: Collection<String>,
        storeType: StoreTypeEnum
    ): Result<Record1<String>>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.select(STORE_CODE).from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(STORE_CODE.`in`(storeCodes))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .groupBy(STORE_CODE)
                .fetch()
        }
    }
}
