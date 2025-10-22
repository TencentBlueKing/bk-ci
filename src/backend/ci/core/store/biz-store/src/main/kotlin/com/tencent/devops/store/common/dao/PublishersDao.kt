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

import com.tencent.devops.model.store.tables.TStorePublisherInfo
import com.tencent.devops.model.store.tables.records.TStorePublisherInfoRecord
import com.tencent.devops.store.pojo.common.publication.PublisherDeptInfo
import com.tencent.devops.store.pojo.common.publication.PublisherInfo
import com.tencent.devops.store.pojo.common.publication.PublishersRequest
import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublishersDao {

    fun batchCreate(dslContext: DSLContext, storePublisherInfos: List<TStorePublisherInfoRecord>): Int {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.batch(storePublisherInfos.map {
                dslContext.insertInto(this)
                    .set(ID, it.id)
                    .set(PUBLISHER_CODE, it.publisherCode)
                    .set(PUBLISHER_NAME, it.publisherName)
                    .set(PUBLISHER_TYPE, it.publisherType)
                    .set(OWNERS, it.owners)
                    .set(HELPER, it.helper)
                    .set(FIRST_LEVEL_DEPT_ID, it.firstLevelDeptId.toLong())
                    .set(FIRST_LEVEL_DEPT_NAME, it.firstLevelDeptName)
                    .set(SECOND_LEVEL_DEPT_ID, it.secondLevelDeptId.toLong())
                    .set(SECOND_LEVEL_DEPT_NAME, it.secondLevelDeptName)
                    .set(THIRD_LEVEL_DEPT_ID, it.thirdLevelDeptId.toLong())
                    .set(THIRD_LEVEL_DEPT_NAME, it.thirdLevelDeptName)
                    .set(FOURTH_LEVEL_DEPT_ID, it.fourthLevelDeptId?.toLong())
                    .set(FOURTH_LEVEL_DEPT_NAME, it.fourthLevelDeptName)
                    .set(ORGANIZATION_NAME, it.organizationName)
                    .set(BG_NAME, it.bgName)
                    .set(CERTIFICATION_FLAG, it.certificationFlag)
                    .set(STORE_TYPE, it.storeType)
                    .set(CREATOR, it.creator)
                    .set(MODIFIER, it.modifier)
                    .set(CREATE_TIME, it.createTime)
                    .set(UPDATE_TIME, it.updateTime)
                    .onDuplicateKeyUpdate()
                    .set(PUBLISHER_NAME, it.publisherName)
                    .set(OWNERS, it.owners)
                    .set(HELPER, it.helper)
                    .set(FIRST_LEVEL_DEPT_ID, it.firstLevelDeptId.toLong())
                    .set(FIRST_LEVEL_DEPT_NAME, it.firstLevelDeptName)
                    .set(SECOND_LEVEL_DEPT_ID, it.secondLevelDeptId.toLong())
                    .set(SECOND_LEVEL_DEPT_NAME, it.secondLevelDeptName)
                    .set(THIRD_LEVEL_DEPT_ID, it.thirdLevelDeptId.toLong())
                    .set(THIRD_LEVEL_DEPT_NAME, it.thirdLevelDeptName)
                    .set(FOURTH_LEVEL_DEPT_ID, it.fourthLevelDeptId?.toLong())
                    .set(FOURTH_LEVEL_DEPT_NAME, it.fourthLevelDeptName)
                    .set(ORGANIZATION_NAME, it.organizationName)
                    .set(BG_NAME, it.bgName)
                    .set(CERTIFICATION_FLAG, it.certificationFlag)
                    .set(MODIFIER, it.modifier)
                    .set(UPDATE_TIME, it.updateTime)
            }
            ).execute().size
        }
    }

    fun batchDelete(dslContext: DSLContext, publishers: List<PublishersRequest>): Int {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.batch(publishers.map {
                dslContext.deleteFrom(this)
                    .where(PUBLISHER_CODE.eq(it.publishersCode)
                        .and(PUBLISHER_TYPE.eq(it.publishersType.name))
                        .and(STORE_TYPE.eq(it.storeType.type.toByte())))
                        .and(BG_NAME.eq(it.bgName))
                }
            ).execute().size
        }
    }

    fun batchUpdate(dslContext: DSLContext, storePublisherInfos: List<TStorePublisherInfoRecord>): Int {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.batch(storePublisherInfos.map {
                dslContext.update(this)
                    .set(PUBLISHER_NAME, it.publisherName)
                    .set(FIRST_LEVEL_DEPT_ID, it.firstLevelDeptId)
                    .set(FIRST_LEVEL_DEPT_NAME, it.firstLevelDeptName)
                    .set(SECOND_LEVEL_DEPT_ID, it.secondLevelDeptId)
                    .set(SECOND_LEVEL_DEPT_NAME, it.secondLevelDeptName)
                    .set(THIRD_LEVEL_DEPT_ID, it.thirdLevelDeptId)
                    .set(THIRD_LEVEL_DEPT_NAME, it.thirdLevelDeptName)
                    .set(PUBLISHER_TYPE, it.publisherType)
                    .set(OWNERS, it.owners)
                    .set(CERTIFICATION_FLAG, it.certificationFlag)
                    .set(ORGANIZATION_NAME, it.organizationName)
                    .set(BG_NAME, it.bgName)
                    .set(HELPER, it.helper)
                    .set(UPDATE_TIME, it.updateTime)
                    .set(MODIFIER, it.modifier)
                    .where(ID.eq(it.id))
                    .and(STORE_TYPE.eq(it.storeType))
            }
            ).execute().size
        }
    }

    fun getPublisherIdsByCode(dslContext: DSLContext, publisherCodes: List<String>): List<String> {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.select(ID).from(this)
                .where(PUBLISHER_CODE.`in`(publisherCodes))
                .fetchInto(String::class.java)
        }
    }

    fun getPublisherId(dslContext: DSLContext, publisherCode: String): String? {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.select(ID).from(this)
                .where(PUBLISHER_CODE.eq(publisherCode))
                .fetchOne(0, String::class.java)
        }
    }

    fun getPublisherInfoById(
        dslContext: DSLContext,
        publisherId: String
    ): PublisherInfo? {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.selectFrom(this)
                .where(ID.eq(publisherId))
                .fetchOne { PublisherInfo(
                    id = it.id,
                    publisherCode = it.publisherCode,
                    publisherName = it.publisherName,
                    publisherType = PublisherType.valueOf(it.publisherType),
                    owners = it.owners,
                    helper = it.helper,
                    firstLevelDeptId = it.firstLevelDeptId.toInt(),
                    firstLevelDeptName = it.firstLevelDeptName,
                    secondLevelDeptId = it.secondLevelDeptId.toInt(),
                    secondLevelDeptName = it.secondLevelDeptName,
                    thirdLevelDeptId = it.thirdLevelDeptId.toInt(),
                    thirdLevelDeptName = it.thirdLevelDeptName,
                    fourthLevelDeptId = it.fourthLevelDeptId?.toInt(),
                    fourthLevelDeptName = it.fourthLevelDeptName,
                    organizationName = it.organizationName,
                    bgName = it.bgName,
                    certificationFlag = it.certificationFlag,
                    storeType = StoreTypeEnum.getStoreTypeObj(it.storeType.toInt())!!,
                    creator = it.creator,
                    modifier = it.modifier,
                    createTime = it.createTime,
                    updateTime = it.updateTime
                ) }
        }
    }

    fun getPublisherInfoByCode(
        dslContext: DSLContext,
        publisherCode: String,
        storeType: StoreTypeEnum
    ): TStorePublisherInfoRecord? {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.selectFrom(this)
                .where(PUBLISHER_CODE.eq(publisherCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .fetchOne()
        }
    }

    fun listPersonPublish(dslContext: DSLContext, limit: Int, offset: Int): List<String> {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.select(PUBLISHER_CODE)
                .from(this)
                .where(PUBLISHER_TYPE.eq(PublisherType.PERSON.name))
                .groupBy(PUBLISHER_CODE)
                .orderBy(PUBLISHER_CODE, CREATE_TIME)
                .limit(limit)
                .offset(offset)
                .fetchInto(String::class.java)
        }
    }

    fun batchUpdatePublishDept(dslContext: DSLContext, publisherDeptInfo: List<PublisherDeptInfo>) {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            dslContext.batch(publisherDeptInfo.map {
                dslContext.update(this)
                    .set(FIRST_LEVEL_DEPT_ID, it.firstLevelDeptId)
                    .set(FIRST_LEVEL_DEPT_NAME, it.firstLevelDeptName)
                    .set(SECOND_LEVEL_DEPT_ID, it.secondLevelDeptId)
                    .set(SECOND_LEVEL_DEPT_NAME, it.secondLevelDeptName)
                    .set(THIRD_LEVEL_DEPT_ID, it.thirdLevelDeptId)
                    .set(THIRD_LEVEL_DEPT_NAME, it.thirdLevelDeptName)
                    .set(ORGANIZATION_NAME, it.organizationName)
                    .set(BG_NAME, it.bgName)
                    .where(PUBLISHER_CODE.eq(it.publisherCode))
            }).execute()
        }
    }
}
