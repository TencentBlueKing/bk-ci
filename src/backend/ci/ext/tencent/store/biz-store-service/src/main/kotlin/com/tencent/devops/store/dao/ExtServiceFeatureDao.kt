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

package com.tencent.devops.store.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TExtensionServiceFeature
import com.tencent.devops.model.store.tables.records.TExtensionServiceFeatureRecord
import com.tencent.devops.store.pojo.ExtServiceFeatureCreateInfo
import com.tencent.devops.store.pojo.ExtServiceFeatureUpdateInfo
import com.tencent.devops.store.pojo.QueryServiceFeatureParam
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExtServiceFeatureDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        extServiceFeatureCreateInfo: ExtServiceFeatureCreateInfo
    ) {
        with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_CODE,
                PUBLIC_FLAG,
                RECOMMEND_FLAG,
                CERTIFICATION_FLAG,
                WEIGHT,
                DELETE_FLAG,
                VISIBILITY_LEVEL,
                REPOSITORY_HASH_ID,
                DESC_INPUT_TYPE,
                CODE_SRC,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    UUIDUtil.generate(),
                    extServiceFeatureCreateInfo.serviceCode,
                    extServiceFeatureCreateInfo.publicFlag,
                    extServiceFeatureCreateInfo.recommentFlag,
                    extServiceFeatureCreateInfo.certificationFlag,
                    extServiceFeatureCreateInfo.weight,
                    extServiceFeatureCreateInfo.deleteFlag,
                    extServiceFeatureCreateInfo.visibilityLevel,
                    extServiceFeatureCreateInfo.repositoryHashId,
                    extServiceFeatureCreateInfo.descInputType!!.name,
                    extServiceFeatureCreateInfo.codeSrc,
                    extServiceFeatureCreateInfo.creatorUser,
                    extServiceFeatureCreateInfo.modifierUser,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun updateExtServiceFeatureBaseInfo(
        dslContext: DSLContext,
        userId: String,
        serviceCode: String,
        extServiceFeatureUpdateInfo: ExtServiceFeatureUpdateInfo
    ) {
        with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            val baseStep = dslContext.update(this)
            val publicFlag = extServiceFeatureUpdateInfo.publicFlag
            if (null != publicFlag) {
                baseStep.set(PUBLIC_FLAG, publicFlag)
            }
            val recommentFlag = extServiceFeatureUpdateInfo.recommentFlag
            if (null != recommentFlag) {
                baseStep.set(RECOMMEND_FLAG, recommentFlag)
            }
            val certificationFlag = extServiceFeatureUpdateInfo.certificationFlag
            if (null != certificationFlag) {
                baseStep.set(CERTIFICATION_FLAG, certificationFlag)
            }

            val descInputType = extServiceFeatureUpdateInfo.descInputType
            if (null != descInputType) {
                baseStep.set(DESC_INPUT_TYPE, descInputType.name)
            }
            val weight = extServiceFeatureUpdateInfo.weight
            if (null != weight) {
                baseStep.set(WEIGHT, weight)
            }
            val serviceType = extServiceFeatureUpdateInfo.serviceTypeEnum
            if (null != serviceType) {
                baseStep.set(SERVICE_TYPE, serviceType.type.toByte())
            }
            val visibilityLevel = extServiceFeatureUpdateInfo.visibilityLevel
            if (null != visibilityLevel) {
                baseStep.set(VISIBILITY_LEVEL, visibilityLevel)
            }
            val killGrayAppFlag = extServiceFeatureUpdateInfo.killGrayAppFlag
            baseStep.set(KILL_GRAY_APP_FLAG, killGrayAppFlag)
            val killGrayAppMarkTime = extServiceFeatureUpdateInfo.killGrayAppMarkTime
            baseStep.set(KILL_GRAY_APP_MARK_TIME, killGrayAppMarkTime)
            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_CODE.eq(serviceCode))
                .execute()
        }
    }

    fun deleteExtFeatureService(
        dslContext: DSLContext,
        userId: String,
        serviceCode: String
    ) {
        with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            dslContext.update(this).set(DELETE_FLAG, true).set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_CODE.eq(serviceCode)).execute()
        }
    }

    fun deleteExtFeatureServiceData(
        dslContext: DSLContext,
        serviceCode: String
    ) {
        with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            dslContext.deleteFrom(this).where(SERVICE_CODE.eq(serviceCode)).execute()
        }
    }

    fun getServiceByCode(dslContext: DSLContext, serviceCode: String): TExtensionServiceFeatureRecord? {
        return with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode)).fetchOne()
        }
    }

    fun getLatestServiceByCode(dslContext: DSLContext, serviceCode: String): TExtensionServiceFeatureRecord? {
        return with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            dslContext.selectFrom(this)
                .where(SERVICE_CODE.eq(serviceCode))
                .fetchOne()
        }
    }

    fun getExtFeatureServices(
        dslContext: DSLContext,
        queryServiceFeatureParam: QueryServiceFeatureParam
    ): Result<TExtensionServiceFeatureRecord>? {
        with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            val conditions = mutableListOf<Condition>()
            val serviceCode = queryServiceFeatureParam.serviceCode
            if (serviceCode != null) {
                conditions.add(SERVICE_CODE.eq(serviceCode))
            }
            val deleteFlag = queryServiceFeatureParam.deleteFlag
            if (deleteFlag != null) {
                conditions.add(DELETE_FLAG.eq(deleteFlag))
            }
            val killGrayAppFlag = queryServiceFeatureParam.killGrayAppFlag
            if (killGrayAppFlag != null) {
                conditions.add(KILL_GRAY_APP_FLAG.eq(killGrayAppFlag))
            }
            val killGrayAppIntervalTime = queryServiceFeatureParam.killGrayAppIntervalTime
            if (killGrayAppIntervalTime != null) {
                conditions.add(KILL_GRAY_APP_MARK_TIME.lt(timestampSubHour(killGrayAppIntervalTime)))
            }
            val baseStep = dslContext.selectFrom(this)
                .where(conditions)
            if (queryServiceFeatureParam.descFlag) {
                baseStep.orderBy(CREATE_TIME.desc())
            } else {
                baseStep.orderBy(CREATE_TIME.asc())
            }
            val page = queryServiceFeatureParam.page
            val pageSize = queryServiceFeatureParam.pageSize
            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    fun batchUpdateServiceFeature(dslContext: DSLContext, serviceFeatureRecords: List<TExtensionServiceFeatureRecord>) {
        if (serviceFeatureRecords.isEmpty()) {
            return
        }
        dslContext.batchUpdate(serviceFeatureRecords).execute()
    }

    fun timestampSubHour(hour: Long): Field<LocalDateTime> {
        return DSL.field("date_sub(NOW(), INTERVAL $hour HOUR)",
            LocalDateTime::class.java)
    }
}
