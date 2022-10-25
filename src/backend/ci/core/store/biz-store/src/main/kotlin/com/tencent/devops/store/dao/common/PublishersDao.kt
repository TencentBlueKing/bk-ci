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

import com.tencent.devops.model.store.tables.TStorePublisherInfo
import com.tencent.devops.model.store.tables.records.TStorePublisherInfoRecord
import com.tencent.devops.store.pojo.common.PublisherInfo
import com.tencent.devops.store.pojo.common.PublishersRequest
import com.tencent.devops.store.pojo.common.enums.PublisherType
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublishersDao {

    fun batchCreate(dslContext: DSLContext, storePublisherInfos: List<TStorePublisherInfoRecord>): Int {
        return dslContext.batchInsert(storePublisherInfos).execute().size
    }

    fun create(dslContext: DSLContext, publisherInfo: PublisherInfo): Int {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.insertInto(this)
                .set(ID, publisherInfo.id)
                    .set(PUBLISHER_CODE, publisherInfo.publisherCode)
                    .set(PUBLISHER_NAME, publisherInfo.publisherName)
                    .set(PUBLISHER_TYPE, publisherInfo.publisherType.name)
                    .set(OWNERS, publisherInfo.owners)
                    .set(HELPER, publisherInfo.helper)
                    .set(FIRST_LEVEL_DEPT_ID, publisherInfo.firstLevelDeptId.toLong())
                    .set(FIRST_LEVEL_DEPT_NAME, publisherInfo.firstLevelDeptName)
                    .set(SECOND_LEVEL_DEPT_ID, publisherInfo.secondLevelDeptId.toLong())
                    .set(SECOND_LEVEL_DEPT_NAME, publisherInfo.secondLevelDeptName)
                    .set(THIRD_LEVEL_DEPT_ID, publisherInfo.thirdLevelDeptId.toLong())
                    .set(THIRD_LEVEL_DEPT_NAME, publisherInfo.thirdLevelDeptName)
                    .set(FOURTH_LEVEL_DEPT_ID, publisherInfo.fourthLevelDeptId?.toLong())
                    .set(FOURTH_LEVEL_DEPT_NAME, publisherInfo.fourthLevelDeptName)
                    .set(ORGANIZATION_NAME, publisherInfo.organizationName)
                    .set(BG_NAME, publisherInfo.bgName)
                    .set(CERTIFICATION_FLAG, publisherInfo.certificationFlag)
                    .set(STORE_TYPE, publisherInfo.storeType.type.toByte())
                    .set(CREATOR, publisherInfo.creator)
                    .set(MODIFIER, publisherInfo.modifier)
                    .set(CREATE_TIME, publisherInfo.createTime)
                    .set(UPDATE_TIME, publisherInfo.updateTime)
                .execute()
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
        publisherCode: String
    ): PublisherInfo? {
        with(TStorePublisherInfo.T_STORE_PUBLISHER_INFO) {
            return dslContext.selectFrom(this)
                .where(PUBLISHER_CODE.eq(publisherCode))
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
                    fourthLevelDeptId = it.fourthLevelDeptId.toInt(),
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
}
