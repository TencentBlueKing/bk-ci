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

package com.tencent.devops.project.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.project.tables.TService
import com.tencent.devops.model.project.tables.records.TServiceRecord
import com.tencent.devops.project.pojo.service.ServiceCreateInfo
import com.tencent.devops.project.pojo.service.ServiceUpdateInfo
import com.tencent.devops.project.pojo.service.ServiceVO
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("ComplexMethod", "LongMethod")
class ServiceDao {

    fun getServiceList(dslContext: DSLContext, clusterType: String? = null): Result<TServiceRecord> {
        with(TService.T_SERVICE) {
            return dslContext.selectFrom(this)
                .where(DELETED.eq(false))
                .let { if (clusterType == null) it else it.and(CLUSTER_TYPE.eq(clusterType)) }
                .skipCheck()
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
                DELETED,
                GRAY_IFRAME_URL,
                CLUSTER_TYPE
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
                    false,
                    serviceVO.grayIframeUrl,
                    serviceVO.clusterType
                )
                .execute()
        }
    }

    fun create(dslContext: DSLContext, userId: String, serviceCreateInfo: ServiceCreateInfo): TServiceRecord? {
        with(TService.T_SERVICE) {
            return dslContext.insertInto(
                this,
                NAME,
                ENGLISH_NAME,
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
                STATUS,
                CREATED_USER,
                CREATED_TIME,
                DELETED,
                LOGO_URL,
                WEB_SOCKET,
                GRAY_IFRAME_URL,
                WEIGHT,
                CLUSTER_TYPE
            ).values(
                serviceCreateInfo.name,
                serviceCreateInfo.englishName,
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
                serviceCreateInfo.status,
                userId,
                LocalDateTime.now(),
                false,
                serviceCreateInfo.logoUrl,
                serviceCreateInfo.webSocket,
                serviceCreateInfo.grayIframeUrl,
                serviceCreateInfo.weight,
                serviceCreateInfo.clusterType
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

    fun update(dslContext: DSLContext, userId: String, serviceUpdateInfo: ServiceUpdateInfo): Boolean {
        with(TService.T_SERVICE) {

            val whereCondition = if (serviceUpdateInfo.serviceId != null) {
                ID.eq(serviceUpdateInfo.serviceId)
            } else if (!serviceUpdateInfo.englishName.isNullOrBlank()) {
                ENGLISH_NAME.eq(serviceUpdateInfo.englishName)
            } else {
                return false
            }

            val execute = dslContext.update(this)
            if (!serviceUpdateInfo.name.isNullOrBlank()) {
                execute.set(NAME, serviceUpdateInfo.name)
            }
            if (serviceUpdateInfo.serviceTypeId != null) {
                execute.set(SERVICE_TYPE_ID, serviceUpdateInfo.serviceTypeId)
            }
            if (!serviceUpdateInfo.link.isNullOrBlank()) {
                execute.set(LINK, serviceUpdateInfo.link)
            }
            if (!serviceUpdateInfo.linkNew.isNullOrBlank()) {
                execute.set(LINK_NEW, serviceUpdateInfo.linkNew)
            }
            if (!serviceUpdateInfo.injectType.isNullOrBlank()) {
                execute.set(INJECT_TYPE, serviceUpdateInfo.injectType)
            }
            if (!serviceUpdateInfo.iframeUrl.isNullOrBlank()) {
                execute.set(IFRAME_URL, serviceUpdateInfo.iframeUrl)
            }
            if (!serviceUpdateInfo.grayIframeUrl.isNullOrBlank()) {
                execute.set(GRAY_IFRAME_URL, serviceUpdateInfo.grayIframeUrl)
            }
            if (!serviceUpdateInfo.cssUrl.isNullOrBlank()) {
                execute.set(CSS_URL, serviceUpdateInfo.cssUrl)
            }
            if (!serviceUpdateInfo.jsUrl.isNullOrBlank()) {
                execute.set(JS_URL, serviceUpdateInfo.jsUrl)
            }
            if (!serviceUpdateInfo.grayCssUrl.isNullOrBlank()) {
                execute.set(GRAY_CSS_URL, serviceUpdateInfo.grayCssUrl)
            }
            if (!serviceUpdateInfo.grayJsUrl.isNullOrBlank()) {
                execute.set(GRAY_JS_URL, serviceUpdateInfo.grayJsUrl)
            }
            if (serviceUpdateInfo.showProjectList != null) {
                execute.set(SHOW_PROJECT_LIST, serviceUpdateInfo.showProjectList)
            }
            if (serviceUpdateInfo.showNav != null) {
                execute.set(SHOW_NAV, serviceUpdateInfo.showNav)
            }
            if (!serviceUpdateInfo.projectIdType.isNullOrBlank()) {
                execute.set(PROJECT_ID_TYPE, serviceUpdateInfo.projectIdType)
            }
            if (!serviceUpdateInfo.status.isNullOrBlank()) {
                execute.set(STATUS, serviceUpdateInfo.status)
            }
            if (!serviceUpdateInfo.logoUrl.isNullOrBlank()) {
                execute.set(LOGO_URL, serviceUpdateInfo.logoUrl)
            }
            if (!serviceUpdateInfo.webSocket.isNullOrBlank()) {
                execute.set(WEB_SOCKET, serviceUpdateInfo.webSocket)
            }
            if (!serviceUpdateInfo.englishName.isNullOrBlank()) {
                execute.set(ENGLISH_NAME, serviceUpdateInfo.englishName)
            }
            if (serviceUpdateInfo.deleted != null) {
                execute.set(DELETED, serviceUpdateInfo.deleted)
            }
            if (serviceUpdateInfo.weight != null) {
                execute.set(WEIGHT, serviceUpdateInfo.weight)
            }
            execute.set(CLUSTER_TYPE, serviceUpdateInfo.clusterType)
            return execute.set(UPDATED_USER, userId)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(whereCondition)
                .execute() > 0
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
