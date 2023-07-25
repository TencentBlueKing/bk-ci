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

import com.tencent.devops.model.store.tables.TStoreIndexBaseInfo
import com.tencent.devops.model.store.tables.TStoreIndexElementDetail
import com.tencent.devops.model.store.tables.TStoreIndexLevelInfo
import com.tencent.devops.model.store.tables.TStoreIndexResult
import com.tencent.devops.model.store.tables.records.TStoreIndexBaseInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexElementDetailRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexLevelInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexResultRecord
import com.tencent.devops.store.pojo.common.enums.IndexExecuteTimeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record7
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class StoreIndexManageInfoDao {

    fun getIndexCodesByAtomCode(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        atomCode: String,
        executeTimeType: IndexExecuteTimeTypeEnum? = null
    ): List<String> {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            conditions.add(ATOM_CODE.eq(atomCode))
            executeTimeType?.let { conditions.add(EXECUTE_TIME_TYPE.eq(executeTimeType.name)) }
            return dslContext.select(INDEX_CODE).from(this)
                .where(conditions).fetchInto(String::class.java)
        }
    }

    fun createStoreIndexBaseInfo(dslContext: DSLContext, tStoreIndexBaseInfoRecord: TStoreIndexBaseInfoRecord): Int {
        return dslContext.executeInsert(tStoreIndexBaseInfoRecord)
    }

    fun batchCreateStoreIndexLevelInfo(
        dslContext: DSLContext,
        tStoreIndexLevelInfoRecord: List<TStoreIndexLevelInfoRecord>
    ) {
        dslContext.batchInsert(tStoreIndexLevelInfoRecord).execute()
    }

    fun getStoreIndexBaseInfoById(dslContext: DSLContext, indexId: String): TStoreIndexBaseInfoRecord? {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            return dslContext.selectFrom(this)
                .where(ID.eq(indexId))
                .fetchOne()
        }
    }

    fun getStoreIndexBaseInfoByCode(dslContext: DSLContext, storeType: StoreTypeEnum, indexCode: String): Int {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            return dslContext.selectCount()
                .from(this)
                .where(INDEX_CODE.eq(indexCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun getStoreIndexBaseInfoByName(dslContext: DSLContext, storeType: StoreTypeEnum, indexName: String): Int {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            return dslContext.selectCount()
                .from(this)
                .where(INDEX_NAME.eq(indexName).and(STORE_TYPE.eq(storeType.type.toByte())))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun count(dslContext: DSLContext, keyWords: String?): Long {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            val condition = mutableListOf<Condition>()
            keyWords?.let {
                condition.add(INDEX_NAME.eq(it))
            }
            return dslContext.selectCount()
                .from(this)
                .where(condition)
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun list(dslContext: DSLContext, keyWords: String?, page: Int, pageSize: Int): List<TStoreIndexBaseInfoRecord> {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            val condition = mutableListOf<Condition>()
            keyWords?.let {
                condition.add(INDEX_NAME.eq(it))
            }
            return dslContext.select(
                ID,
                INDEX_CODE,
                INDEX_NAME,
                DESCRIPTION,
                OPERATION_TYPE,
                ATOM_CODE,
                ATOM_VERSION,
                FINISH_TASK_NUM,
                TOTAL_TASK_NUM,
                EXECUTE_TIME_TYPE,
                STORE_TYPE,
                WEIGHT,
                CREATOR,
                MODIFIER,
                UPDATE_TIME,
                CREATE_TIME
            )
                .from(this)
                .where(condition)
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetchInto(TStoreIndexBaseInfoRecord::class.java)
        }
    }

    fun deleteStoreIndexResulById(dslContext: DSLContext, indexId: String) {
        with(TStoreIndexResult.T_STORE_INDEX_RESULT) {
            dslContext.deleteFrom(this)
                .where(INDEX_ID.eq(indexId))
                .execute()
        }
    }

    fun deleteStoreIndexElementById(dslContext: DSLContext, indexId: String) {
        with(TStoreIndexElementDetail.T_STORE_INDEX_ELEMENT_DETAIL) {
            dslContext.deleteFrom(this)
                .where(INDEX_ID.eq(indexId))
                .execute()
        }
    }

    fun deleteTStoreIndexLevelInfo(dslContext: DSLContext, indexId: String) {
        with(TStoreIndexLevelInfo.T_STORE_INDEX_LEVEL_INFO) {
            dslContext.deleteFrom(this)
                .where(INDEX_ID.eq(indexId))
                .execute()
        }
    }

    fun deleteTStoreIndexBaseInfo(dslContext: DSLContext, indexId: String) {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            dslContext.deleteFrom(this)
                .where(ID.eq(indexId))
                .execute()
        }
    }

    fun batchCreateStoreIndexResult(dslContext: DSLContext, tStoreIndexResultRecords: List<TStoreIndexResultRecord>) {
        with(TStoreIndexResult.T_STORE_INDEX_RESULT) {
            dslContext.batch(tStoreIndexResultRecords.map {
                dslContext.insertInto(
                    this,
                    ID,
                    STORE_CODE,
                    STORE_TYPE,
                    INDEX_ID,
                    INDEX_CODE,
                    ICON_TIPS,
                    LEVEL_ID,
                    CREATOR,
                    MODIFIER,
                    UPDATE_TIME,
                    CREATE_TIME
                ).values(
                    it.id,
                    it.storeCode,
                    it.storeType,
                    it.indexId,
                    it.indexCode,
                    it.iconTips,
                    it.levelId,
                    it.creator,
                    it.modifier,
                    it.updateTime,
                    it.createTime
                ).onDuplicateKeyUpdate()
                    .set(ICON_TIPS, it.iconTips)
                    .set(LEVEL_ID, it.levelId)
                    .set(MODIFIER, it.modifier)
                    .set(UPDATE_TIME, it.updateTime)
            }).execute()
        }
    }

    fun getStoreIndexBaseInfo(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        indexCode: String
    ): String? {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            return dslContext.select(ID)
                .from(this)
                .where(INDEX_CODE.eq(indexCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .fetchOne(0, String::class.java)
        }
    }

    fun getStoreIndexInfosByStoreCodes(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeCodes: List<String>
    ): Result<Record7<String, String, String, String, String, String, String>> {
        with(TStoreIndexResult.T_STORE_INDEX_RESULT) {
            val tStoreIndexBaseInfo = TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO
            val tStoreIndexLevelInfo = TStoreIndexLevelInfo.T_STORE_INDEX_LEVEL_INFO
            return dslContext.select(
                this.STORE_CODE,
                tStoreIndexBaseInfo.INDEX_CODE,
                tStoreIndexBaseInfo.INDEX_NAME,
                tStoreIndexLevelInfo.ICON_URL,
                tStoreIndexBaseInfo.DESCRIPTION,
                this.ICON_TIPS,
                tStoreIndexLevelInfo.LEVEL_NAME
            ).from(this)
                .leftJoin(tStoreIndexBaseInfo)
                .on(INDEX_ID.eq(tStoreIndexBaseInfo.ID))
                .join(tStoreIndexLevelInfo).on(INDEX_ID.eq(tStoreIndexLevelInfo.INDEX_ID)
                    .and(LEVEL_ID.eq(tStoreIndexLevelInfo.ID)))
                .where(STORE_CODE.`in`(storeCodes).and(STORE_TYPE.eq(storeType.type.toByte())))
                .orderBy(tStoreIndexBaseInfo.WEIGHT.desc())
                .fetch()
        }
    }

    fun getStoreIndexLevelInfo(
        dslContext: DSLContext,
        indexId: String,
        levelName: String
    ): TStoreIndexLevelInfoRecord? {
        with(TStoreIndexLevelInfo.T_STORE_INDEX_LEVEL_INFO) {
            return dslContext.selectFrom(this)
                .where(INDEX_ID.eq(indexId).and(LEVEL_NAME.eq(levelName)))
                .fetchOne()
        }
    }

    fun getStoreCodeByElementName(dslContext: DSLContext, indexCode: String, elementName: String): List<String> {
        with(TStoreIndexElementDetail.T_STORE_INDEX_ELEMENT_DETAIL) {
            return dslContext.select(STORE_CODE).from(this)
                .where(INDEX_CODE.eq(indexCode).and(ELEMENT_NAME.eq(elementName)))
                .fetchInto(String::class.java)
        }
    }

    fun deleteStoreIndexResultByStoreCode(
        dslContext: DSLContext,
        indexCode: String,
        storeType: StoreTypeEnum,
        storeCodes: List<String>
    ) {
        with(TStoreIndexResult.T_STORE_INDEX_RESULT) {
            dslContext.deleteFrom(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(INDEX_CODE.eq(indexCode).and(STORE_CODE.`in`(storeCodes)))
                .execute()
        }
    }

    fun deleteStoreIndexElementDetailByStoreCode(
        dslContext: DSLContext,
        indexCode: String,
        storeType: StoreTypeEnum,
        storeCodes: List<String>
    ) {
        with(TStoreIndexElementDetail.T_STORE_INDEX_ELEMENT_DETAIL) {
            dslContext.deleteFrom(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(INDEX_CODE.eq(indexCode).and(STORE_CODE.`in`(storeCodes)))
                .execute()
        }
    }

    fun batchCreateElementDetail(
        dslContext: DSLContext,
        tStoreIndexElementDetailRecords: List<TStoreIndexElementDetailRecord>
    ) {
        with(TStoreIndexElementDetail.T_STORE_INDEX_ELEMENT_DETAIL) {
            dslContext.batch(tStoreIndexElementDetailRecords.map {
                dslContext.insertInto(
                    this,
                    ID,
                    STORE_CODE,
                    STORE_TYPE,
                    INDEX_ID,
                    INDEX_CODE,
                    ELEMENT_NAME,
                    ELEMENT_VALUE,
                    REMARK,
                    CREATOR,
                    MODIFIER,
                    UPDATE_TIME,
                    CREATE_TIME
                ).values(
                    it.id,
                    it.storeCode,
                    it.storeType,
                    it.indexId,
                    it.indexCode,
                    it.elementName,
                    it.elementValue,
                    it.remark,
                    it.creator,
                    it.modifier,
                    it.updateTime,
                    it.createTime
                ).onDuplicateKeyUpdate()
                    .set(ELEMENT_VALUE, it.elementValue)
                    .set(REMARK, it.remark)
                    .set(UPDATE_TIME, it.updateTime)
            }).execute()
        }
    }

    fun updateIndexCalculateProgress(
        dslContext: DSLContext,
        indexId: String,
        totalTaskNum: Int,
        finishTaskNum: Int
    ) {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            dslContext.update(this)
                .set(TOTAL_TASK_NUM, totalTaskNum)
                .set(FINISH_TASK_NUM, finishTaskNum)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(indexId))
                .execute()
        }
    }
}
