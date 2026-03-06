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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.model.store.tables.TStoreBaseEnv
import com.tencent.devops.model.store.tables.TStoreVersionLog
import com.tencent.devops.model.store.tables.records.TStoreVersionLogRecord
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import org.jooq.DSLContext
import org.jooq.Record3
import org.jooq.Record4
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreVersionLogDao {

    fun saveStoreVersion(
        dslContext: DSLContext,
        userId: String,
        storeId: String,
        releaseType: ReleaseTypeEnum,
        versionContent: String
    ) {
        with(TStoreVersionLog.T_STORE_VERSION_LOG) {
            dslContext.insertInto(
                this,
                ID,
                STORE_ID,
                RELEASE_TYPE,
                CONTENT,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    storeId,
                    releaseType.releaseType.toByte(),
                    versionContent,
                    userId,
                    userId
                )
                .onDuplicateKeyUpdate()
                .set(RELEASE_TYPE, releaseType.releaseType.toByte())
                .set(CONTENT, versionContent)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun getStoreVersion(dslContext: DSLContext, storeId: String): TStoreVersionLogRecord? {
        with(TStoreVersionLog.T_STORE_VERSION_LOG) {
            return dslContext.selectFrom(this)
                .where(STORE_ID.eq(storeId))
                .fetchOne()
        }
    }

    fun getStoreVersions(
        dslContext: DSLContext,
        storeIds: List<String>,
        getTestVersionFlag: Boolean = false
    ): Result<TStoreVersionLogRecord>? {
        with(TStoreVersionLog.T_STORE_VERSION_LOG) {
            var step = dslContext.selectFrom(this)
                .where(STORE_ID.`in`(storeIds))
            if (!getTestVersionFlag) {
                step = step.and(RELEASE_TYPE.notEqual(ReleaseTypeEnum.BRANCH_TEST.releaseType.toByte()))
            }
            return step.fetch()
        }
    }

    fun deleteByStoreCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte
    ) {
        val tsb = TStoreBase.T_STORE_BASE
        val storeIds = dslContext.select(tsb.ID).from(tsb)
            .where(tsb.STORE_CODE.eq(storeCode).and(tsb.STORE_TYPE.eq(storeType))).fetch()
        with(TStoreVersionLog.T_STORE_VERSION_LOG) {
            dslContext.deleteFrom(this)
                .where(STORE_ID.`in`(storeIds))
                .execute()
        }
    }

    fun getStoreComponentVersionLogs(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        page: Int,
        pageSize: Int
    ): Result<Record3<String, String, LocalDateTime>>? {
        val tsb = TStoreBase.T_STORE_BASE
        val tsvl = TStoreVersionLog.T_STORE_VERSION_LOG
        val baseStep = dslContext.select(tsb.VERSION, tsvl.CONTENT, tsb.UPDATE_TIME)
            .from(tsb)
            .join(tsvl)
            .on(tsb.ID.eq(tsvl.STORE_ID))
            .where(
                tsb.STORE_CODE.eq(storeCode)
                    .and(tsb.STORE_TYPE.eq(storeType).and(tsb.STATUS.eq(StoreStatusEnum.RELEASED.name)))
            ).orderBy(tsb.UPDATE_TIME.desc())
        baseStep.limit((page - 1) * pageSize, pageSize)
        return baseStep.fetch()
    }

    fun countStoreComponentVersionLogs(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte
    ): Long {

        val tsb = TStoreBase.T_STORE_BASE
        val tsvl = TStoreVersionLog.T_STORE_VERSION_LOG
        val baseStep = dslContext.selectCount()
            .from(tsb)
            .join(tsvl)
            .on(tsb.ID.eq(tsvl.STORE_ID))
            .where(
                tsb.STORE_CODE.eq(storeCode)
                    .and(tsb.STORE_TYPE.eq(storeType).and(tsb.STATUS.eq(StoreStatusEnum.RELEASED.name)))
            )
        return baseStep.fetchOne(0, Long::class.java) ?: 0L
    }

    fun updateComponentVersionInfo(dslContext: DSLContext, storeId: String, pkgSize: String) {
        val tsvl = TStoreVersionLog.T_STORE_VERSION_LOG
        dslContext.update(tsvl).set(tsvl.PACKAGE_SIZE, pkgSize)
            .where(tsvl.STORE_ID.eq(storeId)).execute()
    }

    fun getComponentVersionSizeInfo(dslContext: DSLContext, storeId: String): String? {
        val tsvl = TStoreVersionLog.T_STORE_VERSION_LOG
        return dslContext.select(tsvl.PACKAGE_SIZE).from(tsvl)
            .where(tsvl.STORE_ID.eq(storeId)).orderBy(tsvl.CREATE_TIME.desc()).limit(1)
            .fetchOne(0, String::class.java)
    }

    fun countComponent(dslContext: DSLContext, storeStatus: Byte): Long {
        with(TStoreBase.T_STORE_BASE) {
            return dslContext.selectCount().from(this)
                .where(STORE_TYPE.eq(storeStatus).and(STATUS.eq(AtomStatusEnum.RELEASED.name)))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun selectComponentEnvInfoByStoreIds(
        dslContext: DSLContext,
        storeIds: List<String>
    ): Result<Record4<String, String, String, String>>? {
        with(TStoreBaseEnv.T_STORE_BASE_ENV) {
            return dslContext.select(STORE_ID, OS_NAME, OS_ARCH, PKG_PATH).from(this)
                .where(STORE_ID.`in`(storeIds)).fetch()
        }
    }

    fun selectComponentIds(dslContext: DSLContext, offset: Long, batchSize: Long): List<String>? {
        with(TStoreBase.T_STORE_BASE) {
            return dslContext.select(ID).from(this).where(STATUS.eq(AtomStatusEnum.RELEASED.name))
                .limit(offset, batchSize)
                .fetch().into(String::class.java)
        }
    }

    fun getComponentSizeByVersionAndCode(
        dslContext: DSLContext,
        storeCode: String,
        version: String,
        storeType: Byte
    ): String? {
        val tsb = TStoreBase.T_STORE_BASE
        val tsvl = TStoreVersionLog.T_STORE_VERSION_LOG
        val baseStep = dslContext.select(tsvl.PACKAGE_SIZE).from(tsb)
            .join(tsvl)
            .on(tsb.ID.eq(tsvl.STORE_ID))
            .where(
                tsb.STORE_CODE.eq(storeCode)
                    .and(tsb.STORE_TYPE.eq(storeType))
                    .and(tsb.VERSION.eq(version))
            )
        return baseStep.fetchOne(0, String::class.java)
    }
}