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

import com.tencent.devops.common.db.utils.JooqUtils.values
import com.tencent.devops.model.store.tables.TStoreBaseEnv
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvDataPO
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreBaseEnvManageDao {

    /**
     * 批量保存商店基础环境配置数据
     *
     * 使用MySQL的INSERT ... ON DUPLICATE KEY UPDATE语法实现批量插入或更新操作
     * 当主键冲突时自动更新现有记录，避免重复插入导致的异常
     *
     * @param dslContext JOOQ数据库上下文
     * @param storeBaseEnvDataPOs 商店基础环境数据对象列表
     *
     * 字段更新策略：
     * - 使用coalesce(values(field), field)逻辑：新值不为null则更新，否则保留原值
     * - MODIFIER字段：总是使用新值
     * - UPDATE_TIME字段：总是更新为当前时间
     * - DEFAULT_FLAG字段：默认值为true（当原值为null时）
     */
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
                SHA256_CONTENT,
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
                        storeBaseEnvDataPO.sha256Content,
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
                // 冲突更新策略：新值不为null则更新，否则保留原值
                .set(LANGUAGE, DSL.coalesce(values(LANGUAGE), LANGUAGE))
                .set(MIN_VERSION, DSL.coalesce(values(MIN_VERSION), MIN_VERSION))
                .set(PKG_NAME, DSL.coalesce(values(PKG_NAME), PKG_NAME))
                .set(PKG_PATH, DSL.coalesce(values(PKG_PATH), PKG_PATH))
                .set(TARGET, DSL.coalesce(values(TARGET), TARGET))
                .set(SHA_CONTENT, DSL.coalesce(values(SHA_CONTENT), SHA_CONTENT))
                .set(SHA256_CONTENT, DSL.coalesce(values(SHA256_CONTENT), SHA256_CONTENT))
                .set(PRE_CMD, DSL.coalesce(values(PRE_CMD), PRE_CMD))
                .set(OS_NAME, DSL.coalesce(values(OS_NAME), OS_NAME))
                .set(OS_ARCH, DSL.coalesce(values(OS_ARCH), OS_ARCH))
                .set(RUNTIME_VERSION, DSL.coalesce(values(RUNTIME_VERSION), RUNTIME_VERSION))
                .set(DEFAULT_FLAG, DSL.coalesce(values(DEFAULT_FLAG), DEFAULT_FLAG))
                .set(MODIFIER, values(MODIFIER))
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
