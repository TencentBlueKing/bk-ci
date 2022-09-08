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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStorePkgRunEnvInfo
import com.tencent.devops.model.store.tables.records.TStorePkgRunEnvInfoRecord
import com.tencent.devops.store.pojo.common.StorePkgRunEnvInfo
import com.tencent.devops.store.pojo.common.StorePkgRunEnvRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StorePkgRunEnvInfoDao {

    fun add(
        dslContext: DSLContext,
        userId: String,
        storePkgRunEnvRequest: StorePkgRunEnvRequest
    ) {
        with(TStorePkgRunEnvInfo.T_STORE_PKG_RUN_ENV_INFO) {
            dslContext.insertInto(
                this,
                ID,
                STORE_TYPE,
                LANGUAGE,
                OS_NAME,
                OS_ARCH,
                RUNTIME_VERSION,
                PKG_NAME,
                PKG_DOWNLOAD_PATH,
                DEFAULT_FLAG,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    StoreTypeEnum.valueOf(storePkgRunEnvRequest.storeType).type.toByte(),
                    storePkgRunEnvRequest.language,
                    storePkgRunEnvRequest.osName,
                    storePkgRunEnvRequest.osArch,
                    storePkgRunEnvRequest.runtimeVersion,
                    storePkgRunEnvRequest.pkgName,
                    storePkgRunEnvRequest.pkgDownloadPath,
                    storePkgRunEnvRequest.defaultFlag,
                    userId,
                    userId
                )
                .onDuplicateKeyUpdate()
                .set(PKG_NAME, storePkgRunEnvRequest.pkgName)
                .set(PKG_DOWNLOAD_PATH, storePkgRunEnvRequest.pkgDownloadPath)
                .set(DEFAULT_FLAG, storePkgRunEnvRequest.defaultFlag)
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: String,
        userId: String,
        storePkgRunEnvRequest: StorePkgRunEnvRequest
    ) {
        with(TStorePkgRunEnvInfo.T_STORE_PKG_RUN_ENV_INFO) {
            dslContext.update(this)
                .set(STORE_TYPE, StoreTypeEnum.valueOf(storePkgRunEnvRequest.storeType).type.toByte())
                .set(LANGUAGE, storePkgRunEnvRequest.language)
                .set(OS_NAME, storePkgRunEnvRequest.osName)
                .set(OS_ARCH, storePkgRunEnvRequest.osArch)
                .set(RUNTIME_VERSION, storePkgRunEnvRequest.runtimeVersion)
                .set(PKG_NAME, storePkgRunEnvRequest.pkgName)
                .set(PKG_DOWNLOAD_PATH, storePkgRunEnvRequest.pkgDownloadPath)
                .set(DEFAULT_FLAG, storePkgRunEnvRequest.defaultFlag)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TStorePkgRunEnvInfo.T_STORE_PKG_RUN_ENV_INFO) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getStorePkgRunEnvInfo(
        dslContext: DSLContext,
        storeType: Byte,
        language: String,
        osName: String,
        osArch: String,
        runtimeVersion: String
    ): TStorePkgRunEnvInfoRecord? {
        with(TStorePkgRunEnvInfo.T_STORE_PKG_RUN_ENV_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType))
            conditions.add(LANGUAGE.eq(language))
            conditions.add(OS_NAME.eq(osName))
            conditions.add(OS_ARCH.eq(osArch))
            conditions.add(RUNTIME_VERSION.like("$runtimeVersion%"))
            return dslContext.selectFrom(this)
                .where(conditions)
                .limit(1)
                .fetchOne()
        }
    }

    fun getDefaultStorePkgRunEnvInfo(
        dslContext: DSLContext,
        storeType: Byte,
        language: String,
        osName: String,
        osArch: String
    ): TStorePkgRunEnvInfoRecord? {
        with(TStorePkgRunEnvInfo.T_STORE_PKG_RUN_ENV_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType))
            conditions.add(LANGUAGE.eq(language))
            conditions.add(OS_NAME.eq(osName))
            conditions.add(OS_ARCH.eq(osArch))
            conditions.add(DEFAULT_FLAG.eq(true))
            return dslContext.selectFrom(this)
                .where(conditions)
                .limit(1)
                .fetchOne()
        }
    }

    fun convert(record: TStorePkgRunEnvInfoRecord): StorePkgRunEnvInfo {
        with(record) {
            return StorePkgRunEnvInfo(
                id = id,
                storeType = StoreTypeEnum.getStoreType(storeType.toInt()),
                language = language,
                osName = osName,
                osArch = osArch,
                runtimeVersion = runtimeVersion,
                pkgName = pkgName,
                pkgDownloadPath = pkgDownloadPath,
                defaultFlag = defaultFlag,
                creator = creator,
                modifier = modifier,
                createTime = DateTimeUtil.toDateTime(createTime),
                updateTime = DateTimeUtil.toDateTime(updateTime)
            )
        }
    }
}
