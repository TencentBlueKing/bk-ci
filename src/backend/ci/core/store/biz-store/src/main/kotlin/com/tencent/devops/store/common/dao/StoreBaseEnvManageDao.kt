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

import com.tencent.devops.model.store.tables.TStoreBaseEnv
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvDataPO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.util.mysql.MySQLDSL
import org.springframework.stereotype.Repository

@Repository
class StoreBaseEnvManageDao {

    fun batchSave(
        dslContext: DSLContext,
        storeBaseEnvDataPOs: List<StoreBaseEnvDataPO>
    ) {
        with(TStoreBaseEnv.T_STORE_BASE_ENV) {
            dslContext.insertInto(
                this,
                ID,
                STORE_ID,
                LANGUAGE,
                MIN_VERSION,
                PKG_NAME,
                PKG_PATH,
                TARGET,
                SHA_CONTENT,
                PRE_CMD,
                OS_NAME,
                OS_ARCH,
                RUNTIME_VERSION,
                DEFAULT_FLAG,
                CREATOR,
                MODIFIER,
                UPDATE_TIME,
                CREATE_TIME
            ).also { insert ->
                storeBaseEnvDataPOs.forEach { storeBaseEnvDataPO ->
                    insert.values(
                        storeBaseEnvDataPO.id,
                        storeBaseEnvDataPO.storeId,
                        storeBaseEnvDataPO.language,
                        storeBaseEnvDataPO.minVersion,
                        storeBaseEnvDataPO.pkgName,
                        storeBaseEnvDataPO.pkgPath,
                        storeBaseEnvDataPO.target,
                        storeBaseEnvDataPO.shaContent,
                        storeBaseEnvDataPO.preCmd,
                        storeBaseEnvDataPO.osName,
                        storeBaseEnvDataPO.osArch,
                        storeBaseEnvDataPO.runtimeVersion,
                        storeBaseEnvDataPO.defaultFlag ?: true,
                        storeBaseEnvDataPO.creator,
                        storeBaseEnvDataPO.modifier,
                        storeBaseEnvDataPO.updateTime,
                        storeBaseEnvDataPO.createTime
                    )
                }
            }
                .onDuplicateKeyUpdate()
                .set(
                    LANGUAGE,
                    DSL.`when`(MySQLDSL.values(LANGUAGE).isNotNull, MySQLDSL.values(LANGUAGE)).otherwise(LANGUAGE)
                )
                .set(
                    MIN_VERSION,
                    DSL.`when`(MySQLDSL.values(MIN_VERSION).isNotNull, MySQLDSL.values(MIN_VERSION))
                        .otherwise(MIN_VERSION)
                )
                .set(
                    PKG_NAME,
                    DSL.`when`(MySQLDSL.values(PKG_NAME).isNotNull, MySQLDSL.values(PKG_NAME)).otherwise(PKG_NAME)
                )
                .set(
                    PKG_PATH,
                    DSL.`when`(MySQLDSL.values(PKG_PATH).isNotNull, MySQLDSL.values(PKG_PATH)).otherwise(PKG_PATH)
                )
                .set(
                    TARGET,
                    DSL.`when`(MySQLDSL.values(TARGET).isNotNull, MySQLDSL.values(TARGET)).otherwise(TARGET)
                )
                .set(
                    SHA_CONTENT,
                    DSL.`when`(MySQLDSL.values(SHA_CONTENT).isNotNull, MySQLDSL.values(SHA_CONTENT))
                        .otherwise(SHA_CONTENT)
                )
                .set(
                    PRE_CMD,
                    DSL.`when`(MySQLDSL.values(PRE_CMD).isNotNull, MySQLDSL.values(PRE_CMD)).otherwise(PRE_CMD)
                )
                .set(
                    OS_NAME,
                    DSL.`when`(MySQLDSL.values(OS_NAME).isNotNull, MySQLDSL.values(OS_NAME)).otherwise(OS_NAME)
                )
                .set(
                    OS_ARCH,
                    DSL.`when`(MySQLDSL.values(OS_ARCH).isNotNull, MySQLDSL.values(OS_ARCH)).otherwise(OS_ARCH)
                )
                .set(
                    RUNTIME_VERSION,
                    DSL.`when`(MySQLDSL.values(RUNTIME_VERSION).isNotNull, MySQLDSL.values(RUNTIME_VERSION))
                        .otherwise(RUNTIME_VERSION)
                )
                .set(
                    DEFAULT_FLAG,
                    DSL.`when`(MySQLDSL.values(DEFAULT_FLAG).isNotNull, MySQLDSL.values(DEFAULT_FLAG))
                        .otherwise(DEFAULT_FLAG)
                )
                .set(MODIFIER, MySQLDSL.values(MODIFIER))
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun deleteStoreEnvInfo(dslContext: DSLContext, storeId: String) {
        with(TStoreBaseEnv.T_STORE_BASE_ENV) {
            dslContext.deleteFrom(this)
                .where(STORE_ID.eq(storeId))
                .execute()
        }
    }

    fun batchDeleteStoreEnvInfo(dslContext: DSLContext, storeIds: List<String>) {
        with(TStoreBaseEnv.T_STORE_BASE_ENV) {
            dslContext.deleteFrom(this)
                .where(STORE_ID.`in`(storeIds))
                .execute()
        }
    }
}
