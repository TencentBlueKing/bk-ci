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

import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreEnvVar
import com.tencent.devops.model.store.tables.records.TStoreEnvVarRecord
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_ENCRYPT_FLAG
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_SCOPE
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.KEY_STORE_TYPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.KEY_VAR_DESC
import com.tencent.devops.store.pojo.common.KEY_VAR_NAME
import com.tencent.devops.store.pojo.common.KEY_VAR_VALUE
import com.tencent.devops.store.pojo.common.StoreEnvVarRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class StoreEnvVarDao {

    @Value("\${aes.aesKey}")
    private lateinit var aesKey: String

    fun create(
        dslContext: DSLContext,
        userId: String,
        version: Int,
        storeEnvVarRequest: StoreEnvVarRequest
    ) {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            val encryptFlag = storeEnvVarRequest.encryptFlag
            dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                STORE_TYPE,
                VAR_NAME,
                VAR_VALUE,
                VAR_DESC,
                SCOPE,
                ENCRYPT_FLAG,
                VERSION,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    storeEnvVarRequest.storeCode,
                    StoreTypeEnum.valueOf(storeEnvVarRequest.storeType).type.toByte(),
                    storeEnvVarRequest.varName,
                    if (encryptFlag) AESUtil.encrypt(
                        aesKey,
                        storeEnvVarRequest.varValue
                    ) else storeEnvVarRequest.varValue,
                    storeEnvVarRequest.varDesc,
                    storeEnvVarRequest.scope,
                    encryptFlag,
                    version,
                    userId,
                    userId
                ).execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun batchDelete(
        dslContext: DSLContext,
        storeType: Byte,
        storeCode: String,
        scope: String,
        varNameList: List<String>
    ) {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode)
                    .and(STORE_TYPE.eq(storeType))
                    .and(SCOPE.eq(scope))
                    .and(VAR_NAME.`in`(varNameList)))
                .execute()
        }
    }

    /**
     * 查询环境变量
     */
    fun queryEnvironmentVariable(
        dslContext: DSLContext,
        userId: String,
        storeType: Byte,
        storeCode: String,
        scope: String,
        varName: String
    ): Record? {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            return dslContext.select().from(this)
                .where(STORE_CODE.eq(storeCode)
                    .and(STORE_TYPE.eq(storeType))
                    .and(SCOPE.eq(scope))
                    .and(VAR_NAME.eq(varName)))
                .fetchOne()
        }
    }

    /**
     * 修改环境下所有同一名字变量的环境与变量名
     */
    fun updateVariableEnvironment(
        dslContext: DSLContext,
        userId: String,
        storeType: Byte,
        storeCode: String,
        pastScope: String,
        scope: String,
        pastName: String,
        varName: String
    ): Int {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            return dslContext.update(this)
                .set(this.SCOPE, scope)
                .set(this.VAR_NAME, varName)
                .where(STORE_CODE.eq(storeCode)
                    .and(STORE_TYPE.eq(storeType))
                    .and(SCOPE.eq(pastScope))
                    .and(VAR_NAME.eq(pastName)))
                .execute()
        }
    }

    fun updateVariable(
        dslContext: DSLContext,
        storeType: Byte,
        storeCode: String,
        variableId: String,
        varValue: String,
        varDesc: String,
        encryptFlag: Boolean
    ): Int {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            return dslContext.update(this)
                .set(this.VAR_VALUE, varValue)
                .set(this.VAR_DESC, varDesc)
                .set(this.ENCRYPT_FLAG, encryptFlag)
                .where(STORE_CODE.eq(storeCode)
                    .and(STORE_TYPE.eq(storeType))
                    .and(ID.eq(variableId)))
                .execute()
        }
    }

    fun getEnvVarMaxVersion(
        dslContext: DSLContext,
        storeType: Byte,
        storeCode: String,
        varName: String
    ): Int? {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            return dslContext.select(DSL.max(VERSION)).from(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)).and(VAR_NAME.eq(varName)))
                .fetchOne(0, Int::class.java)
        }
    }

    /**
     * 获取环境变量最新记录
     */
    fun getNewEnvVar(
        dslContext: DSLContext,
        storeType: Byte,
        storeCode: String,
        variableId: String
    ): Record? {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            return dslContext.select(
                ID.`as`(KEY_ID),
                STORE_CODE.`as`(KEY_STORE_CODE),
                STORE_TYPE.`as`(KEY_STORE_TYPE),
                VAR_NAME.`as`(KEY_VAR_NAME),
                VAR_VALUE.`as`(KEY_VAR_VALUE),
                VAR_DESC.`as`(KEY_VAR_DESC),
                SCOPE.`as`(KEY_SCOPE),
                ENCRYPT_FLAG.`as`(KEY_ENCRYPT_FLAG),
                VERSION.`as`(KEY_VERSION),
                CREATOR.`as`(KEY_CREATOR),
                MODIFIER.`as`(KEY_MODIFIER),
                CREATE_TIME.`as`(KEY_CREATE_TIME),
                UPDATE_TIME.`as`(KEY_UPDATE_TIME)
            ).from(this)
                .where(STORE_CODE.eq(storeCode)
                    .and(STORE_TYPE.eq(storeType))
                    .and(ID.eq(variableId)))
                .fetchOne()
        }
    }

    fun getEnvVarList(
        dslContext: DSLContext,
        storeType: Byte,
        storeCode: String,
        scope: String,
        varName: String
    ): Result<TStoreEnvVarRecord>? {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            return dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode)
                    .and(STORE_TYPE.eq(storeType))
                    .and(SCOPE.eq(scope))
                    .and(VAR_NAME.eq(varName)))
                .orderBy(VERSION.desc())
                .fetch()
        }
    }

    fun getLatestEnvVarList(
        dslContext: DSLContext,
        storeType: Byte,
        storeCode: String,
        scopeList: List<String>? = null,
        varName: String? = null
    ): Result<out Record>? {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            // 查找每组storeCode最新的记录
            val t = dslContext.select(
                STORE_CODE.`as`(KEY_STORE_CODE),
                STORE_TYPE.`as`(KEY_STORE_TYPE),
                VAR_NAME.`as`(KEY_VAR_NAME),
                DSL.max(VERSION).`as`(KEY_VERSION)
            ).from(this).groupBy(STORE_CODE, STORE_TYPE, SCOPE, VAR_NAME)
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            if (!scopeList.isNullOrEmpty()) {
                conditions.add(SCOPE.`in`(scopeList))
            }
            if (varName != null) {
                conditions.add(VAR_NAME.contains(varName))
            }
            val baseStep = dslContext.select(
                ID.`as`(KEY_ID),
                STORE_CODE.`as`(KEY_STORE_CODE),
                STORE_TYPE.`as`(KEY_STORE_TYPE),
                VAR_NAME.`as`(KEY_VAR_NAME),
                VAR_VALUE.`as`(KEY_VAR_VALUE),
                VAR_DESC.`as`(KEY_VAR_DESC),
                SCOPE.`as`(KEY_SCOPE),
                ENCRYPT_FLAG.`as`(KEY_ENCRYPT_FLAG),
                VERSION.`as`(KEY_VERSION),
                CREATOR.`as`(KEY_CREATOR),
                MODIFIER.`as`(KEY_MODIFIER),
                CREATE_TIME.`as`(KEY_CREATE_TIME),
                UPDATE_TIME.`as`(KEY_UPDATE_TIME)
            ).from(this)
                .join(t)
                .on(
                    STORE_CODE.eq(t.field(KEY_STORE_CODE, String::class.java))
                        .and(STORE_TYPE.eq(t.field(KEY_STORE_TYPE, Byte::class.java)))
                        .and(VAR_NAME.eq(t.field(KEY_VAR_NAME, String::class.java)))
                        .and(VERSION.eq(t.field(KEY_VERSION, Int::class.java)))
                )
                .where(conditions)
            return baseStep.fetch()
        }
    }

    fun deleteEnvVar(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .execute()
        }
    }
}
