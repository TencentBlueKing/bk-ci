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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.dao.common

import com.tencent.devops.model.store.tables.TBusinessConfig
import com.tencent.devops.model.store.tables.records.TBusinessConfigRecord
import com.tencent.devops.store.pojo.common.BusinessConfigRequest
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

/**
 * @Description
 * @Date 2019/12/1
 * @Version 1.0
 */
@Repository
class BusinessConfigDao {

    fun add(dslContext: DSLContext, request: BusinessConfigRequest) {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            dslContext.insertInto(
                this,
                BUSINESS,
                FEATURE,
                BUSINESS_VALUE,
                CONFIG_VALUE,
                DESCRIPTION
            )
                .values(
                    request.business.name,
                    request.feature,
                    request.businessValue,
                    request.configValue,
                    request.description
                ).execute()
        }
    }

    fun update(dslContext: DSLContext, request: BusinessConfigRequest): Int {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.update(this)
                .set(CONFIG_VALUE, request.configValue)
                .set(DESCRIPTION, request.description)
                .where(BUSINESS.eq(request.business.name))
                .and(FEATURE.eq(request.feature))
                .and(BUSINESS_VALUE.eq(request.businessValue))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, business: String, feature: String, businessValue: String): Int {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.deleteFrom(this)
                .where(BUSINESS.eq(business))
                .and(FEATURE.eq(feature))
                .and(BUSINESS_VALUE.eq(businessValue))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: Int): Int {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, business: String, feature: String, businessValue: String): TBusinessConfigRecord? {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.selectFrom(this)
                .where(BUSINESS.eq(business))
                .and(FEATURE.eq(feature))
                .and(BUSINESS_VALUE.eq(businessValue))
                .fetchOne()
        }
    }

    fun listFeatureConfig(dslContext: DSLContext, business: String, businessValue: String): Result<TBusinessConfigRecord>? {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.selectFrom(this)
                .where(BUSINESS.eq(business))
                .and(BUSINESS_VALUE.eq(businessValue))
                .fetch()
        }
    }

    /**
     * 查询业务下的哪些取值具有指定的特性
     */
    fun list(dslContext: DSLContext, business: String, feature: String, configValue: String): Result<TBusinessConfigRecord>? {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.selectFrom(this)
                .where(BUSINESS.eq(business))
                .and(FEATURE.eq(feature))
                .and(CONFIG_VALUE.eq(configValue))
                .fetch()
        }
    }

    fun get(dslContext: DSLContext, id: Int): TBusinessConfigRecord? {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun listAll(dslContext: DSLContext): Result<TBusinessConfigRecord>? {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }
}
