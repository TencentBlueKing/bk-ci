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

package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TService
import com.tencent.devops.model.project.tables.records.TServiceRecord
import com.tencent.devops.project.pojo.service.ServiceCreateInfo
import com.tencent.devops.project.pojo.service.ServiceVO
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ServiceDao {

    fun getServiceList(dslContext: DSLContext): Result<TServiceRecord> {
        with(TService.T_SERVICE) {
            return dslContext.selectFrom(this)
                .where(DELETED.eq(false))
                .orderBy(WEIGHT.asc())
                .fetch()
        }
    }

    fun create(
        dslContext: DSLContext,
        userId: String,
        typeId: Long,
        serviceVO: ServiceVO
    ) {
        with(TService.T_SERVICE) {
            dslContext.insertInto(
                this,
                ID,
                NAME,
                SERVICE_TYPE_ID,
                LINK,
                LINK_NEW,
                STATUS,
                INJECT_TYPE,
                IFRAME_URL,
                CSS_URL,
                JS_URL,
                GRAY_CSS_URL,
                GRAY_JS_URL,
                SHOW_PROJECT_LIST,
                SHOW_NAV,
                PROJECT_ID_TYPE,
                CREATED_USER,
                DELETED
            )
                .values(
                    serviceVO.id,
                    serviceVO.name,
                    typeId,
                    serviceVO.link,
                    serviceVO.linkNew,
                    serviceVO.status,
                    serviceVO.injectType,
                    serviceVO.iframeUrl,
                    serviceVO.cssUrl,
                    serviceVO.jsUrl,
                    serviceVO.grayCssUrl,
                    serviceVO.grayJsUrl,
                    serviceVO.showProjectList,
                    serviceVO.showNav,
                    serviceVO.projectIdType,
                    userId,
                    false
                )
                .execute()
        }
    }

    fun create(dslContext: DSLContext, userId: String, serviceCreateInfo: ServiceCreateInfo): TServiceRecord? {
        with(TService.T_SERVICE) {
            return dslContext.insertInto(
                this,
                NAME,
                SERVICE_TYPE_ID,
                LINK,
                LINK_NEW,
                INJECT_TYPE,
                IFRAME_URL,
                CSS_URL,
                JS_URL,
                GRAY_CSS_URL,
                GRAY_JS_URL,
                SHOW_PROJECT_LIST,
                SHOW_NAV,
                PROJECT_ID_TYPE,
                WEIGHT,
                STATUS,
                CREATED_USER,
                CREATED_TIME,
                DELETED
            ).values(
                serviceCreateInfo.name,
                serviceCreateInfo.serviceTypeId,
                serviceCreateInfo.link,
                serviceCreateInfo.linkNew,
                serviceCreateInfo.injectType,
                serviceCreateInfo.iframeUrl,
                serviceCreateInfo.cssUrl,
                serviceCreateInfo.jsUrl,
                serviceCreateInfo.grayCssUrl,
                serviceCreateInfo.grayJsUrl,
                serviceCreateInfo.showProjectList,
                serviceCreateInfo.showNav,
                serviceCreateInfo.projectIdType,
                serviceCreateInfo.weight,
                serviceCreateInfo.status,
                userId,
                LocalDateTime.now(),
                false
            ).returning().fetchOne()
        }
    }

    fun delete(dslContext: DSLContext, userId: String, serviceId: Long): Boolean {
        with(TService.T_SERVICE) {
            val execute = dslContext.update(this)
                .set(DELETED, true)
                .set(UPDATED_USER, userId)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId)).execute()
            return execute > 0
        }
    }

    fun update(dslContext: DSLContext, userId: String, serviceId: Long, serviceCreateInfo: ServiceCreateInfo): Boolean {
        with(TService.T_SERVICE) {
            val execute = dslContext.update(this)
                .set(NAME, serviceCreateInfo.name)
                .set(SERVICE_TYPE_ID, serviceCreateInfo.serviceTypeId)
                .set(LINK, serviceCreateInfo.link)
                .set(LINK_NEW, serviceCreateInfo.linkNew)
                .set(INJECT_TYPE, serviceCreateInfo.injectType)
                .set(IFRAME_URL, serviceCreateInfo.iframeUrl)
                .set(CSS_URL, serviceCreateInfo.cssUrl)
                .set(JS_URL, serviceCreateInfo.jsUrl)
                .set(GRAY_CSS_URL, serviceCreateInfo.grayCssUrl)
                .set(GRAY_JS_URL, serviceCreateInfo.grayJsUrl)
                .set(SHOW_PROJECT_LIST, serviceCreateInfo.showProjectList)
                .set(SHOW_NAV, serviceCreateInfo.showNav)
                .set(PROJECT_ID_TYPE, serviceCreateInfo.projectIdType)
                .set(STATUS, serviceCreateInfo.status)
                .set(WEIGHT, serviceCreateInfo.weight)
                .set(UPDATED_USER, userId)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId))
                .and(DELETED.eq(false))
                .execute()
            return execute > 0
        }
    }

    fun select(dslContext: DSLContext, serviceId: Long): TServiceRecord? {
        with(TService.T_SERVICE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(serviceId))
                .and(DELETED.eq(false))
                .fetchOne()
        }
    }
}