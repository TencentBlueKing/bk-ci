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

package com.tencent.devops.store.common.dao

import com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_NAME
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TStoreProjectRelRecord
import com.tencent.devops.store.pojo.common.StoreProjectInfo
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record2
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class StoreProjectRelDao {

    fun addStoreProjectRel(
        dslContext: DSLContext,
        userId: String,
        storeCode: String,
        projectCode: String,
        type: Byte,
        storeType: Byte,
        instanceId: String? = null,
        instanceName: String? = null,
        version: String? = null
    ): Int {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val baseStep = dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                PROJECT_CODE,
                TYPE,
                STORE_TYPE,
                INSTANCE_ID,
                INSTANCE_NAME,
                VERSION,
                CREATOR,
                MODIFIER
            ).values(
                UUIDUtil.generate(),
                storeCode,
                projectCode,
                type,
                storeType,
                instanceId,
                instanceName,
                version,
                userId,
                userId
            ).onDuplicateKeyUpdate()
                .set(PROJECT_CODE, projectCode)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
            instanceId?.let {
                baseStep.set(INSTANCE_ID, instanceId)
            }
            instanceName?.let {
                baseStep.set(INSTANCE_NAME, instanceName)
            }
            version?.let {
                baseStep.set(VERSION, version)
            }
            return baseStep.execute()
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

    fun countStoreProject(
        dslContext: DSLContext,
        projectCode: String,
        storeCode: String,
        storeType: Byte,
        storeProjectType: StoreProjectTypeEnum? = null,
        instanceId: String? = null,
        version: String? = null
    ): Int {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_CODE.eq(projectCode))
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            storeProjectType?.let {
                conditions.add(TYPE.eq(storeProjectType.type.toByte()))
            }
            instanceId?.let {
                conditions.add(INSTANCE_ID.eq(instanceId))
            }
            version?.let {
                conditions.add(VERSION.eq(version))
            }
            return dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countInstalledProject(dslContext: DSLContext, storeCode: String, storeType: Byte): Int {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.selectCount().from(this)
                .where(STORE_CODE.eq(storeCode)
                    .and(STORE_TYPE.eq(storeType))
                    .and(TYPE.eq(StoreProjectTypeEnum.COMMON.type.toByte()))
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
     * 获取项目下关联的组件版本信息
     */
    fun getProjectComponentVersionMap(
        dslContext: DSLContext,
        projectCode: String,
        storeType: Byte,
        storeProjectTypes: List<Byte>? = null,
        instanceId: String? = null
    ): Map<String, String?>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions = mutableListOf(PROJECT_CODE.eq(projectCode))
            storeProjectTypes?.let {
                conditions.add(TYPE.`in`(storeProjectTypes))
            }
            if (!instanceId.isNullOrBlank()) {
                conditions.add(INSTANCE_ID.eq(instanceId))
            }
            val baseQuery = dslContext.select(STORE_CODE, VERSION)
                .from(this)
                .where(conditions)
                .and(STORE_TYPE.eq(storeType))
            return baseQuery.groupBy(STORE_CODE).fetch().intoMap(STORE_CODE, VERSION)
        }
    }

    /**
     * 卸载时删除关联关系
     */
    fun deleteRel(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        projectCode: String,
        instanceIdList: List<String>? = null
    ) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            conditions.add(PROJECT_CODE.eq(projectCode))
            if (!instanceIdList.isNullOrEmpty()) {
                conditions.add(INSTANCE_ID.`in`(instanceIdList))
            }
            conditions.add(TYPE.eq(StoreProjectTypeEnum.COMMON.type.toByte()))
            dslContext.deleteFrom(this)
                .where(conditions)
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
    fun isInstalledByProject(
        dslContext: DSLContext,
        projectCode: String,
        storeCode: String,
        storeType: Byte,
        instanceId: String? = null
    ): Boolean {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions = mutableListOf<Condition>().apply {
                add(STORE_CODE.eq(storeCode))
                add(STORE_TYPE.eq(storeType))
                add(PROJECT_CODE.eq(projectCode))
                if (!instanceId.isNullOrBlank()) {
                    add(INSTANCE_ID.eq(instanceId))
                }
            }
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
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
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val finalStep = dslContext.select(tStoreProjectRel.PROJECT_CODE)
            .from(tStoreMember)
            .join(tStoreProjectRel)
            .on(
                tStoreMember.STORE_CODE.eq(tStoreProjectRel.STORE_CODE)
                    .and(tStoreMember.STORE_TYPE.eq(tStoreProjectRel.STORE_TYPE))
            )
            .where(tStoreMember.USERNAME.eq(userId))
            .and(tStoreProjectRel.STORE_CODE.eq(storeCode))
            .and(tStoreProjectRel.TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
            .and(tStoreProjectRel.CREATOR.eq(userId))
            .and(tStoreMember.STORE_TYPE.eq(storeType.type.toByte()))
            .limit(1)
        return finalStep.fetchOne(0, String::class.java)
    }

    /**
     * 更新组件关联初始化项目信息
     */
    fun updateStoreInitProject(dslContext: DSLContext, userId: String, storeProjectInfo: StoreProjectInfo) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            dslContext.update(this)
                .set(PROJECT_CODE, storeProjectInfo.projectId)
                .set(CREATOR, storeProjectInfo.userId)
                .set(MODIFIER, userId)
                .where(STORE_CODE.eq(storeProjectInfo.storeCode))
                .and(STORE_TYPE.eq(storeProjectInfo.storeType.type.toByte()))
                .and(TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
                .execute()
        }
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
     * 删除组件的项目
     */
    fun deleteStoreProject(
        dslContext: DSLContext,
        storeProjectType: StoreProjectTypeEnum,
        storeCode: String,
        storeType: StoreTypeEnum,
        userId: String? = null
    ) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            conditions.add(TYPE.eq(storeProjectType.type.toByte()))
            if (userId != null) {
                conditions.add(CREATOR.eq(userId))
            }
            dslContext.deleteFrom(this).where(conditions).execute()
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

    fun updateProjectStoreVersion(
        dslContext: DSLContext,
        userId: String,
        projectCode: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        storeProjectType: StoreProjectTypeEnum,
        instanceId: String,
        version: String
    ) {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            dslContext.update(this)
                .set(VERSION, version)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_CODE.eq(projectCode))
                .and(TYPE.eq(storeProjectType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .and(INSTANCE_ID.eq(instanceId))
                .execute()
        }
    }

    fun getProjectRelInfo(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        storeProjectType: StoreProjectTypeEnum? = null,
        projectCode: String? = null,
        instanceId: String? = null
    ): Result<TStoreProjectRelRecord>? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions = mutableListOf<Condition>().apply {
                add(STORE_CODE.eq(storeCode))
                add(STORE_TYPE.eq(storeType))
                storeProjectType?.let { add(TYPE.eq(it.type.toByte())) }
                projectCode?.let { add(PROJECT_CODE.eq(it)) }
                instanceId?.takeIf { it.isNotBlank() }?.let { add(INSTANCE_ID.eq(it)) }
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun getUserTestProjectRelByStoreCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        projectCode: String,
        userId: String
    ): TStoreProjectRelRecord? {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            conditions.add(CREATOR.eq(userId))
            conditions.add(PROJECT_CODE.eq(projectCode))
            conditions.add(TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
            return dslContext.selectFrom(this)
                .where(conditions)
                .limit(1)
                .fetchOne()
        }
    }

    fun listStoreInitProjectCode(
        dslContext: DSLContext,
        storeType: Byte,
        offset: Int,
        limit: Int
    ): Result<Record2<String, String>> {
        with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            return dslContext.select(STORE_CODE, PROJECT_CODE).from(this)
                .where(STORE_TYPE.eq(storeType))
                .and(TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
                .orderBy(CREATE_TIME.asc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }
}
