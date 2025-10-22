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

package com.tencent.devops.store.common.service

import com.tencent.devops.store.common.dao.BusinessConfigDao
import com.tencent.devops.store.pojo.common.config.BusinessConfigRequest
import com.tencent.devops.store.pojo.common.config.BusinessConfigResponse
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/12/1
 * @Version 1.0
 */
@Suppress("ALL")
@Service
class BusinessConfigService @Autowired constructor(
    private val dslContext: DSLContext,
    private val businessConfigDao: BusinessConfigDao
) {

    fun add(businessConfigRequest: BusinessConfigRequest): Boolean {
        if (businessConfigDao.existFeatureConfig(
                dslContext = dslContext,
                business = businessConfigRequest.business,
                feature = businessConfigRequest.feature,
                businessValue = businessConfigRequest.businessValue
            )) {
            return false
        } else {
            businessConfigDao.add(dslContext, businessConfigRequest)
        }
        return true
    }

    fun update(id: Int, businessConfigRequest: BusinessConfigRequest): Int {
        if (id < 0) {
            return -1
        }
        val record = businessConfigDao.get(
            dslContext = dslContext,
            business = businessConfigRequest.business,
            feature = businessConfigRequest.feature,
            businessValue = businessConfigRequest.businessValue
        )
        return if (record != null && record.id != id) {
            -1
        } else {
            businessConfigDao.updateById(dslContext, id, businessConfigRequest)
        }
    }

    fun listAllBusinessConfigs(): List<BusinessConfigResponse>? {
        return businessConfigDao.listAll(dslContext)?.map {
            BusinessConfigResponse(
                it.id,
                it.business,
                it.feature,
                it.businessValue,
                it.configValue,
                it.description
            )
        }?.toList() ?: emptyList()
    }

    fun getBusinessConfigById(id: Int): BusinessConfigResponse? {
        val record = businessConfigDao.get(dslContext, id)
        return if (record != null) {
            BusinessConfigResponse(
                record.id,
                record.business,
                record.feature,
                record.businessValue,
                record.configValue,
                record.description
            )
        } else {
            null
        }
    }

    fun deleteBusinessConfigById(id: Int): Int {
        return businessConfigDao.delete(dslContext, id)
    }
}
